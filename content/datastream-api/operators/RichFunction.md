## FlatMap
```java

/**
 * @param <T> Type of the input elements.
 * @param <O> Type of the returned elements.
 */
@Public
@FunctionalInterface
public interface FlatMapFunction<T, O> extends Function, Serializable {
    void flatMap(T value, Collector<O> out) throws Exception;
}
```
注意，O是Collector的类型，不要传递Collector<O>
如果非要传，请参考复杂类型传递这一个章节。

```java
public class FlatMapWordCountJob {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> socketTextStream = env.socketTextStream("localhost", 9999);
        socketTextStream.flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String input, Collector<String> out) throws Exception {
                        String[] words = input.split("\\s+");
                        for (String word : words) {
                            if (!word.isEmpty()) {
                                out.collect(word);
                            }
                        }
                    }
                })
                .addSink(new SinkFunction<String>() {
                    @Override
                    public void invoke(String value, Context context) {
                        System.out.println(value);
                    }
                });
        env.execute();
    }
}
```


非常好的问题！你问的是：

```java
.flatMap((String line, Collector<String> out) -> { ... })
```

传给下一个算子的，是 **单个 `String`**，还是 **`Collector<String>`**？

---

### ✅ 简短答案：

> **传给下一个算子的是你通过 `out.collect(...)` 发出去的每一个“单个 `String`”**  
> `Collector<String>` 只是一个**工具**，用来向下游发送数据，它本身**不会被传递**

---

### 🔍 详细解释

我们来看这个 `flatMap` 的签名：

```java
.flatMap((String line, Collector<String> out) -> { ... })
```

这是 Flink 的 `FlatMapFunction<T, O>` 的 lambda 写法，等价于：

```java
new FlatMapFunction<String, String>() {
    @Override
    public void flatMap(String line, Collector<String> out) {
        // 你可以多次调用 out.collect(...) 发送多个元素
        out.collect("word1");
        out.collect("word2");
    }
}
```
通过 `nc -lk 9999`来快速开启。
hello flink  hello world!

```java
hello
flink
hello
world!
```


#### 📦 数据流向：

```
上游数据： "hello world"
          ↓
flatMap(line = "hello world", out)
  → split → "hello", "world"
  → out.collect("hello")   → 这会发送一个 String 到下游
  → out.collect("world")   → 这也会发送一个 String 到下游
          ↓
下游算子收到： "hello"  → 被处理
              "world"  → 被处理
```

---

### ✅ 关键点总结

| 问题 | 回答 |
|------|------|
| `Collector<String>` 会被传给下一个算子吗？ | ❌ 不会！它只是一个**输出工具**，类似 `System.out` |
| 下一个算子收到的是什么？ | ✅ 每一次 `out.collect(element)` 发送的那个 **单个元素**（如 `"hello"`） |
| 一个输入可以发多个输出吗？ | ✅ 可以！这正是 `flatMap` 的意义：1条输入 → 0条、1条 或 多条输出 |
| `Collector` 是什么类型？ | 它是一个接口，Flink 内部实现它来收集你发送的数据 |

---

### ✅ 举个例子

```java
stream.flatMap((String line, Collector<String> out) -> {
    String[] words = line.split("\\s+");
    for (String word : words) {
        out.collect(word.toUpperCase());  // 发送单个 String
    }
}).map(word -> "Processed: " + word);
```

输入：
```
"hello world"
```

`flatMap` 执行过程：
- `out.collect("HELLO")` → 下游收到 `"HELLO"`
- `out.collect("WORLD")` → 下游收到 `"WORLD"`

下游 `map` 收到：
- `"HELLO"` → 输出 `"Processed: HELLO"`
- `"WORLD"` → 输出 `"Processed: WORLD"`

---

### ✅ 类比理解

你可以把 `Collector<T> out` 想象成一个 **快递员**：

- 你（`flatMap`）拿到一个包裹（`line`）
- 拆开，得到多个物品（`words`）
- 对每个物品调用 `out.collect(item)`，相当于叫快递员把这件东西送给下一站
- 快递员（`Collector`）不会自己变成货物被送走，他只是**送货的人**

---

### ✅ 总结一句话：

> 🔥 `Collector<String>` 是“发送数据的工具”，  
> ✅ 真正传给下一个算子的是你用 `out.collect(...)` 发出的 **每一个单个 `String` 元素**

这就是 `flatMap` 的核心机制：**一对多映射**。

--- 

如果你想进一步了解：
- `Collector` 的内部实现
- `collect()` 是如何异步传输的
- 如何自定义输出收集器

也可以继续问我！



