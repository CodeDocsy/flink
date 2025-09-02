https://nightlies.apache.org/flink/flink-docs-release-2.1/docs/dev/configuration/overview/

```xml
mvn archetype:generate                \
  -DarchetypeGroupId=org.apache.flink   \
  -DarchetypeArtifactId=flink-quickstart-java \
  -DarchetypeVersion=2.1.0
```

```xml
这里面哪些是不需要的：<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>com.codedocsy</groupId>
	<artifactId>flink-learning</artifactId>
	<version>1.0</version>
	<packaging>jar</packaging>

	<name>Flink Quickstart Job</name>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<flink.version>2.1.0</flink.version>
		<target.java.version>11</target.java.version>
		<scala.binary.version>2.12</scala.binary.version>
		<maven.compiler.source>${target.java.version}</maven.compiler.source>
		<maven.compiler.target>${target.java.version}</maven.compiler.target>
		<log4j.version>2.24.3</log4j.version>
	</properties>

	<repositories>
		<repository>
			<id>apache.snapshots</id>
			<name>Apache Development Snapshot Repository</name>
			<url>https://repository.apache.org/content/repositories/snapshots/</url>
			<releases>
				<enabled>false</enabled>
			</releases>
			<snapshots>
				<enabled>true</enabled>
			</snapshots>
		</repository>
	</repositories>

	<dependencies>
		<!-- Apache Flink dependencies -->
		<!-- These dependencies are provided, because they should not be packaged into the JAR file. -->
		<dependency>
			<groupId>org.apache.flink</groupId>
			<artifactId>flink-streaming-java</artifactId>
			<version>${flink.version}</version>
		</dependency>
		<dependency>
			<groupId>org.apache.flink</groupId>
			<artifactId>flink-clients</artifactId>
			<version>${flink.version}</version>
			<scope>provided</scope>
		</dependency>

		<!-- Add connector dependencies here. They must be in the default scope (compile). -->

		<!-- Example:

		<dependency>
			<groupId>org.apache.flink</groupId>
			<artifactId>flink-connector-kafka</artifactId>
			<version>3.0.0-1.17</version>
		</dependency>
		-->

		<!-- Add logging framework, to produce console output when running in the IDE. -->
		<!-- These dependencies are excluded from the application JAR by default. -->
		<dependency>
			<groupId>org.apache.logging.log4j</groupId>
			<artifactId>log4j-slf4j-impl</artifactId>
			<version>${log4j.version}</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.apache.logging.log4j</groupId>
			<artifactId>log4j-api</artifactId>
			<version>${log4j.version}</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.apache.logging.log4j</groupId>
			<artifactId>log4j-core</artifactId>
			<version>${log4j.version}</version>
			<scope>runtime</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>

			<!-- Java Compiler -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.1</version>
				<configuration>
					<source>${target.java.version}</source>
					<target>${target.java.version}</target>
				</configuration>
			</plugin>

			<!-- We use the maven-shade plugin to create a fat jar that contains all necessary dependencies. -->
			<!-- Change the value of <mainClass>...</mainClass> if your program entry point changes. -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<version>3.1.1</version>
				<executions>
					<!-- Run shade goal on package phase -->
					<execution>
						<phase>package</phase>
						<goals>
							<goal>shade</goal>
						</goals>
						<configuration>
							<createDependencyReducedPom>false</createDependencyReducedPom>
							<artifactSet>
								<excludes>
									<exclude>org.apache.flink:flink-shaded-force-shading</exclude>
									<exclude>com.google.code.findbugs:jsr305</exclude>
									<exclude>org.slf4j:*</exclude>
									<exclude>org.apache.logging.log4j:*</exclude>
								</excludes>
							</artifactSet>
							<filters>
								<filter>
									<!-- Do not copy the signatures in the META-INF folder.
									Otherwise, this might cause SecurityExceptions when using the JAR. -->
									<artifact>*:*</artifact>
									<excludes>
										<exclude>META-INF/*.SF</exclude>
										<exclude>META-INF/*.DSA</exclude>
										<exclude>META-INF/*.RSA</exclude>
									</excludes>
								</filter>
							</filters>
							<transformers>
								<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
								<transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
									<mainClass>com.codedocsy.DataStreamJob</mainClass>
								</transformer>
							</transformers>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>

		<pluginManagement>
			<plugins>

				<!-- This improves the out-of-the-box experience in Eclipse by resolving some warnings. -->
				<plugin>
					<groupId>org.eclipse.m2e</groupId>
					<artifactId>lifecycle-mapping</artifactId>
					<version>1.0.0</version>
					<configuration>
						<lifecycleMappingMetadata>
							<pluginExecutions>
								<pluginExecution>
									<pluginExecutionFilter>
										<groupId>org.apache.maven.plugins</groupId>
										<artifactId>maven-shade-plugin</artifactId>
										<versionRange>[3.1.1,)</versionRange>
										<goals>
											<goal>shade</goal>
										</goals>
									</pluginExecutionFilter>
									<action>
										<ignore/>
									</action>
								</pluginExecution>
								<pluginExecution>
									<pluginExecutionFilter>
										<groupId>org.apache.maven.plugins</groupId>
										<artifactId>maven-compiler-plugin</artifactId>
										<versionRange>[3.1,)</versionRange>
										<goals>
											<goal>testCompile</goal>
											<goal>compile</goal>
										</goals>
									</pluginExecutionFilter>
									<action>
										<ignore/>
									</action>
								</pluginExecution>
							</pluginExecutions>
						</lifecycleMappingMetadata>
					</configuration>
				</plugin>
			</plugins>
		</pluginManagement>
	</build>
</project>
```
🔍 解释一下

scope: provided 的意思是：编译期需要，但运行时由集群提供，不会打进你的 Jar 包。

这种写法适合 提交到 Flink 集群，因为集群本身已经有这些依赖。

但是在 本地运行/IDE 调试 的时候，provided 依赖不会出现在 classpath 里，IDE 就会报 程序包不存在。
👌 我帮你分析一下这个 `pom.xml`，哪些是必须的、哪些是可选的。

---

## 🚩 必要的部分

这些你 **一定要保留**：

* **Flink 核心依赖**

  ```xml
  <dependency>
      <groupId>org.apache.flink</groupId>
      <artifactId>flink-streaming-java</artifactId>
      <version>${flink.version}</version>
  </dependency>
  ```

  👉 提供 `StreamExecutionEnvironment` 等 API。

* **flink-clients**

  ```xml
  <dependency>
      <groupId>org.apache.flink</groupId>
      <artifactId>flink-clients</artifactId>
      <version>${flink.version}</version>
      <scope>provided</scope>
  </dependency>
  ```

  👉 只在 **集群运行时** 必要，本地调试想直接 `main` 跑，建议去掉 `<scope>provided</scope>`。

* **maven-compiler-plugin**

  ```xml
  <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.1</version>
      <configuration>
          <source>${target.java.version}</source>
          <target>${target.java.version}</target>
      </configuration>
  </plugin>
  ```

  👉 控制 Java 编译版本，必须保留。

* **maven-shade-plugin**
  👉 打包成 fat jar（含依赖），你提交到 Flink 集群时才会用。

---

## ⚖️ 可选的部分

* **log4j 相关依赖**

  ```xml
  <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-slf4j-impl</artifactId>
      ...
  </dependency>
  <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-api</artifactId>
      ...
  </dependency>
  <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
      ...
  </dependency>
  ```

  👉 仅仅为了日志输出，**本地调试方便，但不是必须**。如果不需要看日志，可以删掉。

* **repositories (apache snapshots)**

  ```xml
  <repositories>
      <repository>
          <id>apache.snapshots</id>
          ...
      </repository>
  </repositories>
  ```

  👉 你用稳定版本（比如 1.17.x、1.18.x）时根本不需要，只有依赖快照版 Flink（比如 `1.19-SNAPSHOT`）才需要。

* **pluginManagement -> eclipse m2e lifecycle-mapping**
  👉 这是给 Eclipse IDE 用的，如果你用 IntelliJ IDEA，可以删掉。

---

## ✅ 精简后推荐版本（最小可运行）

如果你只是想 **在本地跑一个 main 方法 demo**，可以精简成：

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-java</artifactId>
        <version>${flink.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-clients</artifactId>
        <version>${flink.version}</version>
    </dependency>
</dependencies>
```

`log4j` 依赖可选，加上后有更清晰的日志输出，不加也能跑。

---

要不要我帮你写两个 profile：

* **local** → 本地调试（不加 `provided`，含日志依赖）
* **cluster** → 提交到 Flink 集群（`provided`，无日志）




```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.codedocsy</groupId>
	<artifactId>flink-learning</artifactId>
	<version>1.0</version>
	<packaging>jar</packaging>
	<name>Flink Learning</name>
	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<flink.version>2.1.0</flink.version>
		<target.java.version>11</target.java.version>
		<scala.binary.version>2.12</scala.binary.version>
		<maven.compiler.source>${target.java.version}</maven.compiler.source>
		<maven.compiler.target>${target.java.version}</maven.compiler.target>
		<log4j.version>2.24.3</log4j.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.apache.flink</groupId>
			<artifactId>flink-streaming-java</artifactId>
			<version>${flink.version}</version>
		</dependency>
		<dependency>
			<groupId>org.apache.flink</groupId>
			<artifactId>flink-clients</artifactId>
			<version>${flink.version}</version>
		</dependency>
		<dependency>
			<groupId>org.apache.logging.log4j</groupId>
			<artifactId>log4j-slf4j-impl</artifactId>
			<version>${log4j.version}</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.apache.logging.log4j</groupId>
			<artifactId>log4j-api</artifactId>
			<version>${log4j.version}</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.apache.logging.log4j</groupId>
			<artifactId>log4j-core</artifactId>
			<version>${log4j.version}</version>
			<scope>runtime</scope>
		</dependency>
	</dependencies>
</project>
```
```java
package com.codedocsy;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.atomic.AtomicInteger;


public class DataStreamJob {
    public static void main(String[] args) throws Exception {
        AtomicInteger parallelism=new AtomicInteger(0);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        System.out.println("默认执行的并发度"+env.getParallelism());
        env.fromSequence(1, 10)
                .filter(new FilterFunction<Long>() {
                    @Override
                    public boolean filter(Long input) throws Exception {
                        System.out.println("输入的数据:" + input);
                        parallelism.incrementAndGet();
                        System.out.println("执行的线程:" + Thread.currentThread().getId());
                        // 每秒一个数据
                        Thread.sleep(1000);
                        return true;
                    }
                });
        System.out.println("execute调用之前总并发数:" + parallelism.get());
        env.execute("Flink Java API Skeleton");
        System.out.println("execute调用之后的总并发数:" + parallelism.get());
    }
}

```
默认执行的并发度8
execute调用之前总并发数:0
输入的数据:1
执行的线程:94
输入的数据:5
执行的线程:98
输入的数据:6
执行的线程:95
输入的数据:8
输入的数据:10
执行的线程:102
输入的数据:3
执行的线程:100
输入的数据:7
执行的线程:99
执行的线程:97
输入的数据:9
执行的线程:101
输入的数据:2
执行的线程:94
输入的数据:4
执行的线程:100
execute调用之后的总并发数:0

明白了，你是在 本地单 JVM 运行 Flink，并希望用 AtomicInteger 在主线程获取“算子执行完毕后的总并发数”。问题是：即使在本地，Flink 的算子是异步调度的，filter 内的代码在 Task thread pool 中执行，而 env.execute() 返回之前，任务可能还没开始或者还没结束主线程打印，所以直接打印仍然是 0。

要实现你想要的效果，可以用 同步机制，让主线程等待所有 Task 完成，再打印 parallelism。最简单的方式是使用 CountDownLatch 或 Future + Executor 来阻塞主线程直到所有数据处理完成

```
public class DataStreamJob {
    public static void main(String[] args) throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        System.out.println("默认执行的并发度" + env.getParallelism());
        env.fromSequence(1, 10)
                .filter(new FilterFunction<Long>() {
                    @Override
                    public boolean filter(Long input) throws Exception {
                        int count = counter.incrementAndGet();
                        System.out.println("输入的数据: " + input +
                                ", 当前执行计数: " + count +
                                ", 执行线程ID: " + Thread.currentThread().getId());
                        // 模拟耗时处理
                        Thread.sleep(500);
                        return true;
                    }
                }).print();
        env.execute("Flink Java API Skeleton");
        Thread.sleep(10000);
        System.out.println("执行的总次数:" + counter.get());
    }
}
```
默认执行的并发度8
输入的数据: 9, 当前执行计数: 1, 执行线程ID: 102
输入的数据: 6, 当前执行计数: 1, 执行线程ID: 95
输入的数据: 7, 当前执行计数: 1, 执行线程ID: 98
输入的数据: 3, 当前执行计数: 1, 执行线程ID: 101
输入的数据: 1, 当前执行计数: 1, 执行线程ID: 99
输入的数据: 5, 当前执行计数: 1, 执行线程ID: 94
输入的数据: 10, 当前执行计数: 1, 执行线程ID: 97
输入的数据: 8, 当前执行计数: 1, 执行线程ID: 100
输入的数据: 4, 当前执行计数: 2, 执行线程ID: 101
输入的数据: 2, 当前执行计数: 2, 执行线程ID: 99
执行的总次数:0
这是 Flink 并行执行 + Java 对象在多个 Task 实例中的行为 导致的。具体原因如下：

1️⃣ Flink 并行执行模型

fromSequence(1, 10) 会被 Flink 拆成多个 并行子任务（Task） 来执行。

每个子任务都是 独立的对象实例，每个 FilterFunction 在每个 Task 内都有自己的 AtomicInteger。

所以你看到的 counter.incrementAndGet() 在每个 Task 内都是从 0 开始的，因此每个元素都是 1。

线程 ID 不同也是因为不同 Task 可能在不同线程上执行。

2️⃣ 为什么不会累加到总数

AtomicInteger 只是单个 JVM 对象的线程安全计数器。

在 Flink 中，每个并行 Task 是独立序列化的，它们的 counter 并不共享。

因此你看到的是每个 Task 内部的计数，而不是全局计数。

```java
public class DataStreamJob2 {
    public static void main(String[] args) throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        System.out.println("默认执行的并发度" + env.getParallelism());
        env.fromSequence(1, 10)
                .filter(new FilterFunction<Long>() {
                    @Override
                    public boolean filter(Long input) throws Exception {
                        int count = counter.incrementAndGet();
                        System.out.println("输入的数据: " + input
                                + ",counter对象:" + System.identityHashCode(counter) +
                                ", 当前执行计数: " + count +
                                ", 执行线程ID: " + Thread.currentThread().getId());
                        // 模拟耗时处理
                        Thread.sleep(500);
                        return true;
                    }
                });
        env.execute("Flink Java API Skeleton");
        Thread.sleep(10000);
        System.out.println("执行的总次数:" + counter.get());
    }
}
```

```java
默认执行的并发度8
输入的数据: 10,counter对象:1327754742, 当前执行计数: 1, 执行线程ID: 101
输入的数据: 3,counter对象:610930655, 当前执行计数: 1, 执行线程ID: 94
输入的数据: 1,counter对象:362050357, 当前执行计数: 1, 执行线程ID: 102
输入的数据: 6,counter对象:426800492, 当前执行计数: 1, 执行线程ID: 100
输入的数据: 8,counter对象:2091525756, 当前执行计数: 1, 执行线程ID: 99
输入的数据: 7,counter对象:1012522145, 当前执行计数: 1, 执行线程ID: 98
输入的数据: 5,counter对象:1141356083, 当前执行计数: 1, 执行线程ID: 97
输入的数据: 9,counter对象:618227821, 当前执行计数: 1, 执行线程ID: 95
输入的数据: 2,counter对象:362050357, 当前执行计数: 2, 执行线程ID: 102
输入的数据: 4,counter对象:610930655, 当前执行计数: 2, 执行线程ID: 94
执行的总次数:0
```
同一个线程加1了。

```java
package com.codedocsy;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.atomic.AtomicInteger;


public class DataStreamJob2 {
    public static void main(String[] args) throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        System.out.println("默认执行的并发度" + env.getParallelism());
        env.setParallelism(1);
        env.fromSequence(1, 10)
                .filter(new FilterFunction<Long>() {
                    @Override
                    public boolean filter(Long input) throws Exception {
                        int count = counter.incrementAndGet();
                        System.out.println("输入的数据: " + input
                                + ",counter对象:" + System.identityHashCode(counter) +
                                ", 当前执行计数: " + count +
                                ", 执行线程ID: " + Thread.currentThread().getId());
                        // 模拟耗时处理
                        Thread.sleep(500);
                        return true;
                    }
                });
        env.execute("Flink Java API Skeleton");
        Thread.sleep(10000);
        System.out.println("执行的总次数:" + counter.get());
    }
}
```

```java
输入的数据: 1,counter对象:1776165161, 当前执行计数: 1, 执行线程ID: 87
输入的数据: 2,counter对象:1776165161, 当前执行计数: 2, 执行线程ID: 87
输入的数据: 3,counter对象:1776165161, 当前执行计数: 3, 执行线程ID: 87
输入的数据: 4,counter对象:1776165161, 当前执行计数: 4, 执行线程ID: 87
输入的数据: 5,counter对象:1776165161, 当前执行计数: 5, 执行线程ID: 87
输入的数据: 6,counter对象:1776165161, 当前执行计数: 6, 执行线程ID: 87
输入的数据: 7,counter对象:1776165161, 当前执行计数: 7, 执行线程ID: 87
输入的数据: 8,counter对象:1776165161, 当前执行计数: 8, 执行线程ID: 87
输入的数据: 9,counter对象:1776165161, 当前执行计数: 9, 执行线程ID: 87
输入的数据: 10,counter对象:1776165161, 当前执行计数: 10, 执行线程ID: 87
```

 # 模拟实现
 