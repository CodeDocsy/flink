package com.codedocsy.keyedstream;


import com.codedocsy.common.CountItem;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.util.Collector;


public class KeyedProcessFunction02 {
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
                           public String getKey(CountItem input) {
                               return input.getKey();
                           }
                       }
                )
                .process(new KeyedProcessFunction<String, CountItem, CountItem>() {
                    @Override
                    public void processElement(CountItem value, KeyedProcessFunction<String, CountItem, CountItem>.Context ctx, Collector<CountItem> out) {
                        System.out.println("process Element value: " + value);
                        ctx.timerService().registerProcessingTimeTimer(ctx.timerService().currentProcessingTime() + 5000);
                        out.collect(value);
                    }


                    /**
                     * 注意这里的Collector 依旧是向下游发送数据的
                     * @param timestamp
                     * @param ctx
                     * @param out
                     */
                    @Override
                    public void onTimer(long timestamp, KeyedProcessFunction<String, CountItem, CountItem>.OnTimerContext ctx, Collector<CountItem> out) {
                        System.out.println("onTimer timestamp: " + timestamp);
                        out.collect(new CountItem("通过onTimer生成的Item", 1));
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