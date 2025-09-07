package com.codedocsy.keyedstream;


import com.codedocsy.common.SensorReading;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.legacy.SinkFunction;
import org.apache.flink.util.Collector;

import java.io.IOException;


public class KeyedProcessFunction03 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> socketTextStream = env.socketTextStream("localhost", 9999);
        socketTextStream.map((MapFunction<String, SensorReading>) line -> {
                    String[] parts = line.split(",");
                    String sensorId = parts[0];
                    Integer temp = Integer.parseInt(parts[1]);
                    return new SensorReading(sensorId, temp);
                })
                .keyBy((KeySelector<SensorReading, String>) input -> input.getSensorId()
                )
                .process(new KeyedProcessFunction<String, SensorReading, String>() {
                    // 连续高温次数
                    private ValueState<Integer> countState;

                    @Override
                    public void open(OpenContext openContext) throws Exception {
                        countState = getRuntimeContext().getState(
                                new ValueStateDescriptor<>("highTempCount", Integer.class, 0));
                    }

                    @Override
                    public void processElement(SensorReading value, KeyedProcessFunction<String, SensorReading, String>.Context ctx,
                                               Collector<String> out) throws IOException {
                        Integer count = countState.value();
                        if (value.getTemperature() > 50) {
                            count += 1;
                            if (count >= 5) {
                                out.collect("告警! 传感器 " + value.getSensorId() + " 连续 " + count + " 次温度超过50度");
                                // 告警后重置计数
                                count = 0;
                            }
                        } else {
                            //温度未超过阈值，重置计数
                            count = 0;
                        }
                        countState.update(count);
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