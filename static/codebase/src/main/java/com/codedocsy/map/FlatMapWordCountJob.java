package com.codedocsy.map;


import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.util.Collector;


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