
package com.codedocsy.helloworld;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.atomic.AtomicInteger;


public class DataStreamJob2 {
    public static void main(String[] args) throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        System.out.println("默认执行的并发度" + env.getParallelism());
        env.setParallelism(1);
        env.fromSequence(1, 10)
                .filter(new FilterFunction<Long>() {
                    @Override
                    public boolean filter(Long input) throws Exception {
                        int count = counter.incrementAndGet();
                        System.out.println("输入的数据: " + input
                                + ",counter对象:" + System.identityHashCode(counter) +
                                ", 当前执行计数: " + count +
                                ", 执行线程ID: " + Thread.currentThread().getId());
                        // 模拟耗时处理
                        Thread.sleep(500);
                        return true;
                    }
                });
        env.execute("Flink Java API Skeleton");
        Thread.sleep(10000);
        System.out.println("执行的总次数:" + counter.get());
    }
}
