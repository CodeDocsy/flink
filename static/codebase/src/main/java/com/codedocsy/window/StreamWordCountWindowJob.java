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