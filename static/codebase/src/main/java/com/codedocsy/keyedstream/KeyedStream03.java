package com.codedocsy.keyedstream;


import com.codedocsy.common.SensorReading;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.temporal.ChronoUnit;


public class KeyedStream03 {


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> socketTextStream = env.socketTextStream("localhost", 9999);
        socketTextStream.map(new MapFunction<String, SensorReading>() {
                    @Override
                    public SensorReading map(String line) throws Exception {
                        String[] parts = line.split(",");
                        String sensorId = parts[0];
                        Integer temp = Integer.parseInt(parts[1]);
                        return new SensorReading(sensorId, temp);
                    }
                })
                .keyBy((KeySelector<SensorReading, String>) input -> input.getSensorId()
                )
                .process(new KeyedProcessFunction<String, SensorReading, String>() {
                    private ValueState<Integer> countState; // 连续高温次数
                    @Override
                    public void processElement(SensorReading value, KeyedProcessFunction<String, SensorReading, String>.Context ctx,
                                               Collector<String> out) throws Exception {

                    }
                })
                .addSink(new SinkFunction<String>() {
                    @Override
                    public void invoke(String value, Context context) {

                    }
                });
        env.execute();
    }
}