package com.codedocsy.keyedstream;


import com.codedocsy.common.CountItem;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.util.Collector;


public class KeyedProcessFunction01 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> socketTextStream = env.socketTextStream("localhost", 9999);
        socketTextStream.flatMap(new FlatMapFunction<String, CountItem>() {
                    @Override
                    public void flatMap(String input, Collector<CountItem> out) {
                        System.out.println("flatMap: " + input + ", 执行线程ID: " + Thread.currentThread().getId());
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
                           public String getKey(CountItem input) {
                               return input.getKey();
                           }
                       }
                )
                .process(new KeyedProcessFunction<String, CountItem, CountItem>() {
                    @Override
                    public void processElement(CountItem value, KeyedProcessFunction<String, CountItem, CountItem>.Context ctx, Collector<CountItem> out) {
                        System.out.println("process Element value: " + value);
                        out.collect(value);
                    }
                })
                .addSink(new SinkFunction<CountItem>() {
                    @Override
                    public void invoke(CountItem value, Context context) {
                        System.out.println("SinkFunction : " + value);
                    }
                });
        env.execute();
    }
}