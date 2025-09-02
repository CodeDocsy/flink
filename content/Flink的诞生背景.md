---
title: 快速理解Flink架构
order: 2
---
# 为什么应该选择Flink？
任何的计算机系统或者分布式系统，总是要考虑两个问题:存储和计算。
作为流计算框架家族的典型代表，相比较诸如`Spark Streaming`，`Storm`，`Kafka Streams`，`Samza`等其他的流计算框架，Flink有着天然的优势。
在了解Flink的优势以前，先要对流处理有所了解。

## 什么是流/流处理
流处理的最优雅的定义是：一种数据处理引擎，其设计时考虑了无限的数据集。与批处理不同，批处理有明显的开始和结束，而处理是在生成有限数据之后才开始完成的，而流处理则是指连续不断地即时处理永久到来的无边界数据。
为了理解任何Streaming框架的优点和局限性，我们应该了解与Stream处理相关的一些重要特征和术语：

### 交付保证

 这意味着无论如何，流引擎中的特定传入记录都将得到处理的保证。可以是at least once（至少一次）（即使发生故障也至少处理一次），at most once : 至多一次（如果发生故障则可能不处理）或Exactly-once（即使失败在这种情况下也只能处理一次））。显然，只处理一次是最好的，但是实现起来相对困难，并且需要权衡性能。

### 容错

相比较批处理，出现错误只需要重新算一次。对于流计算框架而验，原始的数据可能已经不存在了，因此如果发生诸如节点故障，网络故障等故障的，框架应该能够恢复，并且应该从其离开的位置开始重新处理，并且对已经计算的数据要能够合理的保存。

### 状态管理

在有状态处理需求的情况下，我们需要保持某种状态（例如，记录中每个不重复单词的计数），框架应该能够提供某种机制来保存和更新状态信息。

### 性能

 这包括延迟（可以多久处理一条记录），吞吐量（每秒处理的记录数）和可伸缩性。延迟应尽可能小，而吞吐量应尽可能大。很难同时获得两者。

### 其他的高级功能

如果流处理要求很复杂，像事件时间处理，水印，窗口化等这些是必需的功能。例如，根据在源中生成记录的时间来处理记录（事件时间处理）。

### 成熟度

从采用的角度来看很重要，如果框架已经过大公司的验证和大规模测试，那就太好了。更有可能获得良好的社区支持并在堆栈溢出方面提供帮助。

从以上的对于流计算的要求来看，Flink在架构之初就是为流计算场景而设计的，并且在后续的发展中完美的符合了以上所有的要求。项目的主导方也是阿里巴巴，因此中文社区及其活跃，文档丰富且对于问题的响应速度也快。随着Flink的发展，Flink逐渐朝着流批一体的计算路径上发展，尤其是**Flink CDC**的出现，一些ETL框架就可以到彻底丢弃了——这一点更是其他计算框架无可比拟的优势。

# Flink诞生的背景

## MapReduce的缺陷

我们知道，谷歌在2003到2006年间发表了三篇论文，《MapReduce: Simplified Data Processing on Large Clusters》，《Bigtable: A Distributed Storage System for Structured Data》和《The Google File System》介绍了Google如何对大规模数据进行存储和分析。这三篇论文开启了工业界的大数据时代，被称为Google的三驾马车。

2004年，Doug Cutting和Mike Cafarella就初步实现了HDFS和MapReduce，这是Hadoop的两大核心架构。Map Reduce就成为了当时大数据处理的标准。但是MapReduce却带有天生的缺陷：

- **难以实时计算（MapReduce处理的是存储在本地磁盘上的离线数据）**

- **不能流式计算（MapReduce设计处理的数据源是静态的）**

- **难以DAG计算（有向无环图计算，由于多个任务存在依赖关系，后一个应用的输入是前一个应用的输出。解决这一问题的方式有Apache的Tez计算框架，它是基于hadoop Yarn之上的DAG计算框架，它将MapReduce任务分解为多个子任务同时可以把多个Map/ Reduce任务合并成一个大的DAG任务，这样当前一个任务完成之后，直接将结果输出给下一个任务，不用将结果写到磁盘之上，减少了Map/Reduce之间的文件存储。同时合理的组合其子过程，减少了任务的运行时间。）**

因此，基于要解决上面问题的想法，Flink和Spark都诞生了。只不过走了两条不一样的路（关于Spark这本书可以说是绝对入门的书籍：[大数据处理框架Apache Spark设计与实现](https://book.douban.com/subject/35140409/)）：

Spark走的是优化MapReduce的路径，而Flink直接是采用另外一个计算方式：

我把数据分批加载到内存，然后通过流一样的计算过程直接计算，什么shuffle过程直接不存在。但是要解决一个问题：要尽量减少数据在机器之间传输。因此，Flink的初衷其实是解决上面的问题的，当时并没有想到流处理。

Flink的前身是一个叫做“Stratosphere”的项目。它起源于德国柏林工业大学(Technische Universität Berlin)Volker Markl教授于2008年提出的构想。由于数据库是Volker Markl教授的主要研究方向之一，因此创建该项目的初衷是构建一个以数据库概念为基础、以大规模并行处理(massively parallel processing, MPP)架构为支撑、以MapReduce计算模型为逻辑框架的分布式数据计算引擎。

这个项目从09年就是开始搞，到2014年才基本成熟。这里面全是一群博士生搞的（因此代码看不懂不奇怪）。

github地址：https://github.com/stratosphere/stratosphere

文档：http://markus-h.github.io/stratosphere/docs/internals/pact.html

其产生的论文：http://stratosphere.eu/project/publications/ 从这个论文可以看到，Flink的基本雏形都有了。

值得注意的是，同样是2009年，Spark诞生于伯克利大学AMPLab，同样属于伯克利大学的研究性项目。他们在很多地方都是相似的。只不过在对待数据的思想上，spark和flink走了不一样的路：spark把所有数据都当做批，flink把所有数据当做流来处理。

注意，在这个时候，Flink对于批处理是落后于Spark的！人们对于Flink当时并不看好，直到Google流处理论文的发表。


## Flink关于流计算是如何实现的？

谷歌在VLDB期刊上2015年发表的论文:

> The dataflow model: a practical approach to balancing correctness, latency, and cost in massive-scale, unbounded, out-of-order data processing。

这篇论文标志着流式计算的到来。flink团队一看，简直不就是为我准备的吗？再结合上2013年就发表的论文：[MillWheel: Fault-Tolerant Stream Processing at Internet Scale](https://research.google.com/pubs/archive/41378.pdf)

**flink上马流计算！并在0.7版本发布此功能**

基于上面两篇论文，增加了Flink的流式处理进行。同年，flink团队发表论文，应对分布式有状态处理的快照机制：奠定了checkpoint机制：[Lightweight Asynchronous Snapshots for Distributed Dataflows](https://www.researchgate.net/publication/279458648_Lightweight_Asynchronous_Snapshots_for_Distributed_Dataflows)

值得注意的是，里面采用的分布式快照算法：Chandy-Lamport算法在1985年就发表了（可以关注下Lamport这个大牛，很多现在用到的分布式理论是人家30多年前就发表的，包括**Paxos算法**。 个人网页：https://lamport.azurewebsites.net/

Chandy-Lamport算法：https://zhuanlan.zhihu.com/p/53482103

2015年，flink又发表了一篇论文

> Apache Flink: Stream and Batch Processing in a Single Engine Apache Flink。

说明了流批一体的设计，其借鉴的参考论文基本上就是flink用到的知识。所以说为什么是流批一体？因为flink一开始就是应对批处理的。flink这种并发计算模型后面刚好又可以用到流处理而已。

## 被阿里收购

被阿里收购增量快照没有看到论文。

如果要真的搞懂flink原理，上面的论文是要读一遍的，并且要把Spark搞懂，这样才能更加看出二者设计的差异性。当然，如果要只是搞清楚工程级别的，了解即可（或者看到这个地方再去看论文）。毕竟我们做的还是偏工程的。如果不想看论文（确实上手门槛比较高），可以看这本书：《Streaming Systems》

# Flink发展历程

*发布日期*

- 08/2018: [1.6.0](https://flink.apache.org/news/2018/08/09/release-1.6.0.html)
- 09/2018: [1.6.1](https://flink.apache.org/news/2018/09/20/release-1.6.1.html)
- 10/2018: [1.6.2](https://flink.apache.org/news/2018/10/29/release-1.6.2.html)
- 05/2018: [1.5.0](https://flink.apache.org/news/2018/05/25/release-1.5.0.html)
- 07/2018: [1.5.1](https://flink.apache.org/news/2018/07/12/release-1.5.1.html)
- 07/2018: [1.5.2](https://flink.apache.org/news/2018/07/31/release-1.5.2.html)
- 8/2018:  [1.5.3](https://flink.apache.org/news/2018/08/21/release-1.5.3.html)
- 09/2018: [1.5.4](https://flink.apache.org/news/2018/09/20/release-1.5.4.html)
- 10/2018: [1.5.5](https://flink.apache.org/news/2018/10/29/release-1.5.5.html)
- 12/2017: [1.4.0](https://flink.apache.org/news/2017/12/12/release-1.4.0.html)
- 02/2018: [1.4.1](https://flink.apache.org/news/2018/02/15/release-1.4.1.html)
- 03/2018: [1.4.2](https://flink.apache.org/news/2018/03/08/release-1.4.2.html)
- 06/2017: [1.3](https://flink.apache.org/news/2017/06/01/release-1.3.0.html)
- 06/2017: [1.3.1](https://flink.apache.org/news/2017/06/23/release-1.3.1.html)
- 08/2017: [1.3.2](https://flink.apache.org/news/2017/08/05/release-1.3.2.html)
- 03/2018: [1.3.3](https://flink.apache.org/news/2018/03/15/release-1.3.3.html)
- 02/2017: [1.2.0](https://flink.apache.org/news/2017/02/06/release-1.2.0.html)
- 04/2017: [1.2.1](https://flink.apache.org/news/2017/04/26/release-1.2.1.html)
- 08/2016: [1.1.0](http://flink.apache.org/news/2016/08/08/release-1.1.0.html)
- 08/2016: [1.1.1](https://flink.apache.org/news/2016/08/11/release-1.1.1.html)
- 09/2016:  [v1。1.2](https://flink.apache.org/news/2016/09/05/release-1.1.2.html)
- 10/2016: [v1.1.3](https://flink.apache.org/news/2016/10/12/release-1.1.3.html)
- 12/2016: [v1.1.4](https://flink.apache.org/news/2016/12/21/release-1.1.4.html)
- 03/2017:  [v1.1.5](https://flink.apache.org/news/2017/03/23/release-1.1.5.html)
- 03/2016:[1.0.0](https://flink.apache.org/news/2016/03/08/release-1.0.0.html)
- 04/2016: [1.0.1](https://flink.apache.org/news/2016/04/06/release-1.0.1.html)
- 04/2016: [1.0.2](https://flink.apache.org/news/2016/04/22/release-1.0.2.html)
- 05/2016:  [v1.0.3](http://flink.apache.org/news/2016/05/11/release-1.0.3.html)
- 11/2015: [0.10.0](https://flink.apache.org/news/2015/11/16/release-0.10.0.html) 
- 11/2015: [0.10.1](https://flink.apache.org/news/2015/11/27/release-0.10.1.html)
- 02/2016: [0.10.2](https://flink.apache.org/news/2016/02/11/release-0.10.2.html)
- 06/2015: [Apache Flink0.9](https://flink.apache.org/news/2015/06/24/announcing-apache-flink-0.9.0-release.html)
- 09/2015: [0.9.1](https://flink.apache.org/news/2015/09/01/release-0.9.1.html)
- 04/2015: [Apache Flink0.9-里程碑-1](https://flink.apache.org/news/2015/04/13/release-0.9.0-milestone1.html)

*Apache孵化器发布日期*

- 01/2015: [Apache Flink0.8-孵化](https://flink.apache.org/news/2015/01/21/release-0.8.html)

- 11/2014: [Apache Flink0.7-孵化](https://flink.apache.org/news/2014/11/04/release-0.7.0.html)

- 08/2014: [Apache Flink0.6-孵化](https://flink.apache.org/news/2014/08/26/release-0.6.html)

- 09/2014: [0.6.1-孵化](https://flink.apache.org/news/2014/09/26/release-0.6.1.html)

- 05/2014: Stratosphere 0.5(06/2014:0.5.1;07/2014:0.5.2)

- *Pre-Apache Stratosphere 发布日期*

- 01/2014: Stratosphere 0.4（0.3版本被跳过）

- 08/2012: Stratosphere 0.2

- 05/2011: Stratosphere 0.1（08/2011:0.1.1）

创始人Kostas-Tzoumas的researchgate https://www.researchgate.net/profile/Kostas-Tzoumas  可以看到其相关的论文。

# Flink的文档地址

Flink通过jira和confluence来管理flink项目，可以在这上面找到最权威的资料。

jira：https://issues.apache.org/jira/projects/FLINK/summary

confluence：https://cwiki.apache.org/confluence/display/FLINK

# 运行Flink之前需要理解的概念

## 架构模式

Flink是一种典型的主从模式架构（Master-Work模式），其模式架构如下：
![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/cdd8cadd0a4b439a8f4232c18215a66f~tplv-k3u1fbpfcp-watermark.image?)

关于分布式架构模式，后面会专门抽时间讲解。

## Flink 集群关键角色

Flink 运行时由两种类型的进程组成：一个 *JobManager* 和一个或者多个 *TaskManager*。
![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/50818d8f148843d5924b7d0d42b24d45~tplv-k3u1fbpfcp-watermark.image?)

**JobManager** 其实就是Flink的 master 服务，主要负责自己作业相关的协调工作，包括：向 TaskManager 申请 Slot资源来调度相应的 task 任务、定时触发作业的 checkpoint 和手动 savepoint 的触发、以及作业的容错恢复等。

JobManager还可以细分，但是先忽略它，我们后面会逐步的一步步的学习。现在我们只需要知道他是Master即可。

**TaskManagers**（也称为 *worker*）就是真正干活的，执行作业流的 task，并且缓存和交换数据流。很显然，必须始终至少有一个TaskManager（不然谁来干活？）。在TaskManager中资源调度的最小单位是 *task slot*。

同样的，TaskManagers里面还有更多的角色，先不要太过深入，这样会绕晕，现在我们只需要知道他是Worker即可。

## 部署模式的说明

可以通过多种方式运行Flink，即启动JobManager和TaskManager：

* 直接在机器上运行的就叫**Standalone模式**，不依赖任何外部的力量；
* 另外两种运行方式就是在容器中、或者通过**YARN** 这样的资源框架上启动，由资源管理框架来管理资源。

## 高可用架构

很多人搞不清楚一个分布式系统架构模式和高可用模式有什么区别。所以这里单独说明：一个分布式系统的架构模式和高可用架构没有任何关系。一个分布式系统完全可以没有高可用。同样的，高可用架构或者说高可用部署是随着分布式系统的本身架构+部署形态来来展开的，不能本末倒置。

回到Flink，其高可用部署主要针对JobManager（真正干活的Worker因为随时要存储执行进度并且通知Master，所以无需高可用）：

> 无论哪种模式启动，始终至少有一个JobManager。高可用配置中会有多个JobManager，但是只能有一个在工作（leader），其他的则是standby，准备随时接替Leader。

因此，对于Standalone模式，需要借助外部的框架，比如说Zookeeper来监听JobManager状态，一旦有问题，备份上；
对于像容器模式或者Yarn部署，一主一备不是必须的，因为如果JobManager挂了，可以直接通过资源管理工具拉起来，当然一主一备是更好的。

值得说明的是，为了适应容器化需要，Flink专门为K8s环境设置了一个高可用模式。因此，Flink官方提供了两种高可用服务实现：

- [ZooKeeper](https://nightlies.apache.org/flink/flink-docs-master/zh/docs/deployment/ha/zookeeper_ha/)：每个 Flink 集群部署都可以使用 ZooKeeper HA 服务。它们需要一个运行的 ZooKeeper 复制组（quorum）。
- [Kubernetes](https://nightlies.apache.org/flink/flink-docs-master/zh/docs/deployment/ha/kubernetes_ha/)：Kubernetes HA 服务只能运行在 Kubernetes 上。

当然，关于高可用的细节非常多，还是那句话，先不要太过深入，这样会绕晕，现在我们只需要知道Flink的高可用是怎么回事即可。

# 快速启动Flink

从https://flink.apache.org/downloads.html
下载最新的Flink，目前最新的是1.6.1。注意，不是下载源码：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2ee1ee8343d24332b12a1acee9e9938a~tplv-k3u1fbpfcp-watermark.image?)

解压，运行bin目录下的./start-cluster.sh文件（前提是Java 8安装好，尽量安装Java8，JDK11也可以，但是更高版本就不保证)。如果端口没有被占用，那么基本上都会启动成功。

进入到http://localhost:8081/#/overview 页面如下：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8567444274d94b8d97de1a16618c0716~tplv-k3u1fbpfcp-watermark.image?)

## 端口被占用的处理

进入到log目录，查看日志，如果有如下错误，则意味着端口被占用：
![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/9616cc8603174cf2b7d3cc86c4c7ac22~tplv-k3u1fbpfcp-watermark.image?)

通过`sudo lsof -i tcp:8081`命令查看端口被占用情况（注意，这个时候必须加sudo，否则什么都查不出来）：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6e39971ad2f240fca87f63db0505e298~tplv-k3u1fbpfcp-watermark.image?)

如果端口被占用，进入到conf目录的 `flink-conf.yaml rest.port` 取消注释，换成别的端口即可。
其他的端口类似。

## flink-conf.yaml配置说明

在1.16版本中，conf目录下有如下配置：flink-conf.yaml 配置、日志的配置文件、zk配置以及master/workers文件。

> 在flink1.14.0中已经移除sql-client-defaults.yml配置文件了。相关联的参考地址：https://issues.apache.org/jira/browse/FLINK-21454
> 以及https://cwiki.apache.org/confluence/display/FLINK/FLIP-163%3A+SQL+Client+Improvements
> 还是那句话，先不要管它。后

flink-conf.yaml的原文地址[在这里](https://nightlies.apache.org/flink/flink-docs-master/zh/docs/deployment/config/)，**还是那句话，先只关注当下需要的**：

| 配置                     | 说明                                                                                    |
| ---------------------- | ------------------------------------------------------------------------------------- |
| jobmanager.rpc.address | Jobmanager的IP地址，即master地址。默认是localhost，此参数在HA环境下或者Yarn下无效，仅在local和无HA的standalone集群中有效 |
| jobmanager.rpc.port    | JobMamanger的通信端口，默认是6123，TaskManager以及各种client（如Sql Client）通过这个端口和JobMamanger通信       |
| rest.port              | Job Managener网页管理页面地址                                                                 |

## 运行官方的Sample

进入到Submit New Job，这个是上传jar包的地方：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4f0c96e5ae914d67a393352b780ed936~tplv-k3u1fbpfcp-watermark.image?)

在解压的包中examples/streaming/中选择一个典型的流式处理的sample：SocketWindowWordCount.jar 并上传：
![企业微信截图_5d15e9e3-d8d6-4eb3-9ade-3a8b15de76ef.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/44981677a0424e37a84eaa15bdda52bc~tplv-k3u1fbpfcp-watermark.image?)

点击这一行（注意不要点击到Delete上面）设置参数并且运行。

**注意，要先运行`nc -l 8888` 否则，启动任务会报错。因为任务启动以后，会建立socket连接，任务建立不上就会失败。**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/736245fd9c59424aa089bf672dcead9f~tplv-k3u1fbpfcp-watermark.image?)

成功运行的截图如下：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8d7c6969c8f24c5f94d441f3513252c3~tplv-k3u1fbpfcp-watermark.image?)

这个时候，可能会号好奇很多点：

- parallelism和savepoint Path是什么？有什么用
- 为什么运行参数要这么填写？
- 各个页面的展示信息到底是什么含义？

是的，我们还是先忽略它。在下一章，我们将深入分析这些东西。现在，我们只需要确保能跑起来。

现在，可以在终端进行输入：

```java
bin ajian$ nc -l 8888
q
w
ww
w
ww
w
w
w
hello flink！
hello
```

在TaskManagers页面可以看到如下的输出：
![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6c4bd0304bb8429a88bafc478396f1bb~tplv-k3u1fbpfcp-watermark.image?)

至此，一个Flink的HelloWorld成功了！还是那句话，目标是Hello World，接下来，我们带着问题一步步的深入其中，探寻Flink的神秘地带。


---
title: 快速启动Flink
order: 1
---

# 快速启动Flink

从https://flink.apache.org/downloads/ 下载最新的Flink，目前最新的是1.17.1（注意，不是下载源码）。

![2.1.png](/images/flink/2.1.png)

解压，运行bin目录下的`start-cluster.sh`文件（前提是安装好Java8，尽量安装Java8，Java11也可以，但是更高版本)。如果端口没有被占用，那么应该看到如下界面：

```
Starting cluster.
Starting standalonesession daemon on host bscoder.local.
Starting taskexecutor daemon on host bscoder.local.
```

进入到`http://localhost:8081/#/overview` 页面如下：

![image.png](/images/flink/2.2.png)

## 端口被占用时的处理

如果出现`Could not start rest endpoint on any port in port range 8081`这个错误就是说端口8081被占用了，。进入到log目录，查看日志，如果有如下错
通过`sudo lsof -i tcp:8081`命令查看端口被占用情况（注意，这个时候必须加sudo，否则什么都查不出来）：

如果端口被占用，进入到conf目录的 `flink-conf.yaml`文件， 对`rest.port` 取消注释，换成别的端口即可。
其他的端口类似。

## flink-conf.yaml配置说明

在1.17版本中，conf目录下有如下配置：flink-conf.yaml 配置、日志的配置文件、zk配置以及master/workers文件。

![image.png](/images/flink/2.3.png)

> 在flink1.14.0中已经移除sql-client-defaults.yml配置文件了。相关联的参考地址：https://issues.apache.org/jira/browse/FLINK-21454
> 以及https://cwiki.apache.org/confluence/display/FLINK/FLIP-163%3A+SQL+Client+Improvements
> 还是那句话，先不要管它。后

flink-conf.yaml的原文地址[在这里](https://nightlies.apache.org/flink/flink-docs-master/zh/docs/deployment/config/)，**还是那句话，先只关注当下需要的**：

| 配置                     | 说明                                                                                    |
| ---------------------- | ------------------------------------------------------------------------------------- |
| jobmanager.rpc.address | Jobmanager的IP地址，即master地址。默认是localhost，此参数在HA环境下或者Yarn下无效，仅在local和无HA的standalone集群中有效 |
| jobmanager.rpc.port    | JobMamanger的通信端口，默认是6123，TaskManager以及各种client（如Sql Client）通过这个端口和JobMamanger通信       |
| rest.port              | Job Managener网页管理页面地址                                                                 |

## 运行官方的Sample

进入到Submit New Job，这个是上传jar包的地方：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4f0c96e5ae914d67a393352b780ed936~tplv-k3u1fbpfcp-watermark.image?)

在解压的包中examples/streaming/中选择一个典型的流式处理的sample：SocketWindowWordCount.jar 并上传：
![企业微信截图_5d15e9e3-d8d6-4eb3-9ade-3a8b15de76ef.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/44981677a0424e37a84eaa15bdda52bc~tplv-k3u1fbpfcp-watermark.image?)

点击这一行（注意不要点击到Delete上面）设置参数并且运行。

**注意，要先运行`nc -l 8888` 否则，启动任务会报错。因为任务启动以后，会建立socket连接，任务建立不上就会失败。**

![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/736245fd9c59424aa089bf672dcead9f~tplv-k3u1fbpfcp-watermark.image?)

成功运行的截图如下：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8d7c6969c8f24c5f94d441f3513252c3~tplv-k3u1fbpfcp-watermark.image?)

这个时候，可能会号好奇很多点：

- parallelism和savepoint Path是什么？有什么用
- 为什么运行参数要这么填写？
- 各个页面的展示信息到底是什么含义？

是的，我们还是先忽略它。在下一章，我们将深入分析这些东西。现在，我们只需要确保能跑起来。

现在，可以在终端进行输入：

```java
bin ajian$ nc -l 8888
q
w
ww
w
ww
w
w
w
hello flink！
hello
```

在TaskManagers页面可以看到如下的输出：
![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6c4bd0304bb8429a88bafc478396f1bb~tplv-k3u1fbpfcp-watermark.image?)

至此，一个Flink的HelloWorld成功了！还是那句话，目标是Hello World，接下来，我们带着问题一步步的深入其中，探寻Flink的神秘地带



参考
https://book.douban.com/subject/30329536/
https://github.com/Vonng/ddia