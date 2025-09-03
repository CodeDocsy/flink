```java

```




public class StreamWordCountWindowJob {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> socketTextStream = env.socketTextStream("localhost", 9999);

        socketTextStream.keyBy().window();
        socketTextStream.windowAll()
        env.execute();
    }

}

可以看到 如果没有keyby，则只有windowAll() 有keyby则有window

主要是为了区分不同。


这是一个非常好的问题！你问的是：

> **为什么 `keyBy()` 之后必须先接 `window()`，而不能是 `keyBy → map → window`？**

换句话说，你可能会想：

```java
stream.keyBy(...)         // 得到 KeyedStream
       .map(...)          // 想先 map 一下
       .window(...)       // 再开窗口？ ❌ 编译错误！
```

但你会发现，**`.window(...)` 方法根本不在 `map` 之后可用**。这是因为在 Flink 的 API 设计中，**窗口（Window）必须作用于 `KeyedStream` 上，而不是普通的 `DataStream`**。

---

## ✅ 核心原因：Flink 窗口机制的设计原则

### 🔑 只有 `KeyedStream` 才能调用 `.window(...)`
- `keyBy()` 返回的是 `KeyedStream<K, T>`
- 只有这个 `KeyedStream` 才有 `.window(...)` 方法
- 一旦你对 `KeyedStream` 调用了 `map()`、`flatMap()` 等操作，它就会变成普通的 `DataStream`
- 而普通的 `DataStream` **没有 `.window()` 方法**

```java
DataStream<String> stream = ...;

KeyedStream<String, String> keyed = stream.keyBy(x -> x);  // ✅ OK

KeyedStream<String, String> 有 .window() 方法

DataStream<String> mapped = keyed.map(x -> x + "!");      // ❌ 变成了 DataStream

mapped.window(...)  // ❌ 编译错误！DataStream 没有 window() 方法
```

---

## 🧱 为什么这样设计？

Flink 的窗口是 **基于 key 的状态管理机制**，它要保证：

> **相同 key 的数据进入同一个窗口算子，进行独立聚合**

比如：
- 单词 `"hello"` 的所有数据归到 `"hello"` 的窗口中
- 单词 `"world"` 的归到 `"world"` 的窗口中
- 它们在物理上可能运行在不同 subtask 上，各自维护自己的窗口状态

所以窗口必须在 `keyBy` 之后、且在任何会破坏 key 分组的操作之前定义。

---

## ✅ 正确顺序：`keyBy → window → aggregate`

```java
stream
  .keyBy(word -> word)                           // → KeyedStream
  .window(TumblingProcessingTimeWindows.of(...))  // ✅ 窗口作用于 KeyedStream
  .reduce((a, b) -> new CountItem(a.key, a.count + b.count));
```

或者：

```java
  .sum(1)  // 如果是 Tuple 类型
```

---

## ✅ 如果你想在开窗前做 map，怎么办？

### ✔️ 方法一：在 `keyBy` 前 `map`

只要保证 `keyBy` 的输入是你想要的类型即可：

```java
socketTextStream
  .flatMap((line, out) -> { ... })           // 分词
  .map(word -> new CountItem(word, 1))       // 先 map 成目标对象
  .keyBy(item -> item.getKey())              // 再 keyBy
  .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
  .reduce((a, b) -> new CountItem(a.getKey(), a.getCount() + b.getCount()))
  .print();
```

✅ 这是完全合法的。

---

### ✔️ 方法二：在窗口聚合后使用 `map`

如果你需要在聚合后转换格式：

```java
  .window(...)
  .reduce(...)                // 聚合出结果
  .map(item -> {              // 最后再 map 处理展示逻辑
      item.setCount(item.getCount() * 2);
      return item;
  })
```

---

## ❌ 错误写法示例（常见误区）

```java
stream
  .keyBy(...)
  .map(...)           // ❌ 这一步之后就不是 KeyedStream 了
  .window(...)        // ❌ 编译失败！没有这个方法
```

---

## 📊 总结：Flink 流处理链式调用限制

| 操作 | 输入类型 | 输出类型 | 能否后续开窗 |
|------|----------|----------|-------------|
| `keyBy()` | `DataStream<T>` | `KeyedStream<K, T>` | ✅ 可以 `.window()` |
| `map()` | `KeyedStream` | `DataStream<R>` | ❌ 不能再 `.window()` |
| `window()` | `KeyedStream` | `WindowedStream` | ✅ 接 `reduce`/`aggregate` |
| `reduce()` | `WindowedStream` | `DataStream<R>` | ✅ 后续可 map/print |

---

## ✅ 正确结构模板

```java
env.addSource(...)
   .flatMap()           // 预处理
   .map()               // 转换为 POJO 或 Tuple
   .keyBy(...)          // 分组（必须在这之后开窗）
   .window(...)         // 定义窗口（必须紧跟 keyBy 后）
   .reduce()/.sum()/.aggregate()  // 聚合
   .map()               // 后处理（可选）
   .print();
```

---

### ✅ 类比理解

你可以把 `keyBy().window()` 看作一个 **原子组合操作**，就像 SQL 中的：

```sql
GROUP BY key, 
TUMBLE(ts, INTERVAL '10' SECOND)
```

你不能说：

```sql
SELECT ...
FROM ...
GROUP BY key
SELECT count(*) * 2 AS cnt   -- 先算表达式？
TUMBLE(...)                  -- 再分组窗口？ ❌ 不合法
```

顺序必须是：**先分组 → 再聚合 → 再计算**

---

### ✅ 结论

> ❓ 为什么 `keyBy` 后必须接 `window`，不能 `map`？
>
> ✅ 因为 `.window()` 只能在 `KeyedStream` 上调用，而 `.map()` 会把 `KeyedStream` 变成普通 `DataStream`，从而丢失窗口能力。

📌 所以你要么：
- 在 `keyBy` 前 `map`
- 或在 `window + 聚合` 后再 `map`

这就是 Flink 类型系统和 API 设计的严谨性体现。

如有需要，我也可以画个流程图帮助理解。

```java
package com.codedocsy.window;


import com.codedocsy.map.FlatMapWordCountJob2;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.temporal.ChronoUnit;


public class StreamWordCountWindowJob {

    public static class CountItem {
        public CountItem(String key, Integer count) {
            this.key=key;
            this.count=count;
        }

        private String key;
        private Integer count;

        public void setKey(String key) {
            this.key = key;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public String getKey() {
            return key;
        }

        public Integer getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "CountItem{" +
                    "key='" + key + '\'' +
                    ", count=" + count +
                    '}';
        }
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> socketTextStream = env.socketTextStream("localhost", 9999);
        socketTextStream.flatMap(new FlatMapFunction<String, CountItem>() {
                    @Override
                    public void flatMap(String input, Collector<CountItem> out) {
                        String[] words = input.split("\\s+");
                        for (String word : words) {
                            if (!word.isEmpty()) {
                                out.collect(new CountItem(word, 1));
                            }
                        }
                    }
                })
                .keyBy(new KeySelector<CountItem, String>() {
                           @Override
                           public String getKey(CountItem s) throws Exception {
                               return s.getKey();
                           }
                       }

                )
                .window(TumblingProcessingTimeWindows.of(Duration.of(10, ChronoUnit.SECONDS)))

                .reduce(new ReduceFunction<CountItem>() {
                    @Override
                    public CountItem reduce(CountItem a, CountItem b) {
                        return new CountItem(a.getKey(), a.getCount() + b.getCount());
                    }
                })
                .addSink(new SinkFunction<CountItem>() {
                    @Override
                    public void invoke(CountItem value, Context context) {
                        System.out.println(value);
                    }
                })
        ;
        env.execute();
    }
}
```
```java
hello flink  hello world!
hello flink  hello world!
hello flink  hello world!
hello flink  hello world
```
输出:
```
CountItem{key='world!', count=4}
CountItem{key='hello', count=8}
CountItem{key='flink', count=4}
```
当一个窗口满时，自动执行接下的算子。
但是，当我们停止输入时，不会有任何输出。

那么，现在需要只要窗口时间到了就输出，怎么做？

在 Flink 里：

窗口触发计算 的前提是 窗口里至少有一条元素。

所以如果 10 秒内 完全没有数据进入，窗口是不会“空触发”的。

## KeyedProcessFunction



## WindowFunction

```java
    // ------------------------------------------------------------------------
    //  Operations on the keyed windows
    // ------------------------------------------------------------------------

    /**
     * Applies a reduce function to the window. The window function is called for each evaluation of
     * the window for each key individually. The output of the reduce function is interpreted as a
     * regular non-windowed stream.
     *
     * <p>This window will try and incrementally aggregate data as much as the window policies
     * permit. For example, tumbling time windows can aggregate the data, meaning that only one
     * element per key is stored. Sliding time windows will aggregate on the granularity of the
     * slide interval, so a few elements are stored per key (one per slide interval). Custom windows
     * may not be able to incrementally aggregate, or may need to store extra values in an
     * aggregation tree.
     *
     * @param function The reduce function.
     * @return The data stream that is the result of applying the reduce function to the window.
     */
    @SuppressWarnings("unchecked")
    public SingleOutputStreamOperator<T> reduce(ReduceFunction<T> function) {
        if (function instanceof RichFunction) {
            throw new UnsupportedOperationException(
                    "ReduceFunction of reduce can not be a RichFunction. "
                            + "Please use reduce(ReduceFunction, WindowFunction) instead.");
        }

        // clean the closure
        function = input.getExecutionEnvironment().clean(function);
        return reduce(function, new PassThroughWindowFunction<>());
    }

    /**
     * Applies the given window function to each window. The window function is called for each
     * evaluation of the window for each key individually. The output of the window function is
     * interpreted as a regular non-windowed stream.
     *
     * <p>Arriving data is incrementally aggregated using the given reducer.
     *
     * @param reduceFunction The reduce function that is used for incremental aggregation.
     * @param function The window function.
     * @return The data stream that is the result of applying the window function to the window.
     */
    public <R> SingleOutputStreamOperator<R> reduce(
            ReduceFunction<T> reduceFunction, WindowFunction<T, R, K, W> function) {

        TypeInformation<T> inType = input.getType();
        TypeInformation<R> resultType = getWindowFunctionReturnType(function, inType);
        return reduce(reduceFunction, function, resultType);
    }
```
WindowFunction积累一个窗口内所有的数据。

注意，要看源码，接收哪些实现。

## Reference
1.[Windows](https://nightlies.apache.org/flink/flink-docs-release-2.1/docs/dev/datastream/operators/windows/)


