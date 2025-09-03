package com.codedocsy.map;


import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.util.Collector;


public class FlatMapWordCountJob2 {
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
                    public void flatMap(String input, Collector<CountItem> out) throws Exception {
                        String[] words = input.split("\\s+");
                        for (String word : words) {
                            if (!word.isEmpty()) {
                                out.collect(new CountItem(word, 1));
                            }
                        }
                    }
                })
                .addSink(new SinkFunction<CountItem>() {
                    @Override
                    public void invoke(CountItem value, Context context) {
                        System.out.println(value);
                    }
                });
        env.execute();
    }
}