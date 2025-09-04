package com.codedocsy.keyedstream;

import com.codedocsy.window.StreamWordCountWindowJob;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;


public class KeyedStream01 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> socketTextStream = env.socketTextStream("localhost", 9999);
        System.out.println("默认执行的并发度" + env.getParallelism());
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
                .keyBy(new KeySelector<String, String>() {
                    @Override
                    public String getKey(String input) throws Exception {
                        System.out.println("key: " + input + ", 执行线程ID: " + Thread.currentThread().getId());
                        Thread.sleep(500);
                        return input;
                    }
                }).print();
        env.execute();
    }
}
