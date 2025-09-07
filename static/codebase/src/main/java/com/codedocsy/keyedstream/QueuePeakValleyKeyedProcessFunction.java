package com.codedocsy.keyedstream;

import com.codedocsy.common.QueueCheckResult;
import com.codedocsy.common.RideEvent;
import lombok.*;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.util.Map;

/**
 * 1.每一个波峰波谷结束，数据会被清除
 */
@Data
@NoArgsConstructor
public class QueuePeakValleyKeyedProcessFunction extends KeyedProcessFunction<String, RideEvent, QueueCheckResult> {
    public QueuePeakValleyKeyedProcessFunction(int peakWindowSize, int valleyWindowSize,
                                               Map<Integer, Integer> minPassCountConfig) {
        this.peakWindowSize = peakWindowSize;
        this.valleyWindowSize = valleyWindowSize;
        this.minPassCountConfig = minPassCountConfig;
    }

    /**
     * 连续多少帧才被认为是峰的候选。如果没有连续N次，会直接被忽略
     * 计算方式：（Available Seats/预估正常情况下每秒的通过人数）
     */
    private int peakWindowSize = 3;

    /**
     * 连续多少帧才被认为是谷的候选，才会被当做真正的波峰波谷，如果没有连续N次，会直接被忽略
     * 计算方式：（每轮发车之间的预估时间间隔/每一帧的间隔）
     */
    private int valleyWindowSize = 10;

    /**
     * 时段->最小通过人数。否则被认为是噪声（当做波谷处理）可能每个时间段不一样，第一个Integer表示时间,
     * 默认是0
     * 0表示：00:00::00-00：59：59，依次类推
     */
    private Map<Integer, Integer> minPassCountConfig;

    /**
     * 上一帧的是波峰还是波谷
     */
    private ValueState<Integer> previousFrameStatus;

    /**
     * 每个高峰开始时间
     */
    private ValueState<Long> peakStartTime;

    /**
     * 每个高峰结束的时间，代表车启动，用这个时间做车启动时间
     */
    private ValueState<Long> peakEndTime;


    /**
     * 高峰时段累加的人数
     */
    private ValueState<Integer> cumulativeHeadcount;

    /**
     * 当前连续上升帧数
     */
    private ValueState<Integer> consecutivePeakFrameState;

    /**
     * 当前连续下降帧数
     */
    private ValueState<Long> consecutiveValleyFrameState;

    @Override
    public void open(OpenContext openContext) throws Exception {
        peakStartTime = getRuntimeContext().getState(new ValueStateDescriptor<>("peakStartTime", Types.LONG));
        peakEndTime = getRuntimeContext().getState(new ValueStateDescriptor<>("peakEndTime", Types.LONG));
        cumulativeHeadcount = getRuntimeContext().getState(new ValueStateDescriptor<>("cumulativeHeadcount", Types.INT));
        consecutivePeakFrameState = getRuntimeContext().getState(new ValueStateDescriptor<>("consecutivePeakFrameState", Types.INT));
        consecutiveValleyFrameState = getRuntimeContext().getState(new ValueStateDescriptor<>("consecutiveValleyFrameState", Types.LONG));
        previousFrameStatus = getRuntimeContext().getState(new ValueStateDescriptor<>("previousFrameStatus", Types.INT));
    }

    @Override
    public void processElement(RideEvent current, KeyedProcessFunction<String, RideEvent, QueueCheckResult>.Context ctx, Collector<QueueCheckResult> out) throws Exception {
        int passCount = current.getPassCount();
        Integer consecutivePeakFrameSize = consecutivePeakFrameState.value() == null ? 0 : consecutivePeakFrameState.value();
        Long consecutiveValleyFrameSize = consecutiveValleyFrameState.value() == null ? 0 : consecutiveValleyFrameState.value();
        Integer acc = cumulativeHeadcount.value() == null ? 0 : cumulativeHeadcount.value();

        // ============ 峰值检测 ============
        FrameStatusEnum currentFrameStatusEnum = getCurrentFrameStatus(passCount);
        if (getCurrentFrameStatus(passCount) == FrameStatusEnum.Peak) {
            consecutivePeakFrameSize++;
            acc += passCount;
            cumulativeHeadcount.update(acc);
            consecutivePeakFrameState.update(consecutivePeakFrameSize);
        } else {
            consecutiveValleyFrameSize++;
            consecutiveValleyFrameState.update(consecutiveValleyFrameSize);
        }
        //如果走完完整的一趟发车，全部清零，准备下一趟
        if (consecutivePeakFrameSize >= peakWindowSize &&
                consecutiveValleyFrameSize >= valleyWindowSize) {
            // 清理状态，准备下一趟
            QueueCheckResult queueCheckResult = new QueueCheckResult();
            queueCheckResult.setCount(cumulativeHeadcount.value());
            queueCheckResult.setStartTime(peakStartTime.value());
            queueCheckResult.setEndTime(peakEndTime.value());
            peakStartTime.clear();
            peakEndTime.clear();
            cumulativeHeadcount.clear();
            consecutiveValleyFrameState.clear();
            consecutivePeakFrameState.clear();
        }
        previousFrameStatus.update(currentFrameStatusEnum.value);
    }


    /**
     * 判断是当前帧数据属于波峰还是波谷
     *
     * @return
     */
    private FrameStatusEnum getCurrentFrameStatus(Integer passCount) throws IOException {
        int previousFrameStatusInt = previousFrameStatus.value() == null ? -1 : 1;
        Long consecutiveValleyFrameSize = consecutiveValleyFrameState.value() == null ? 0 : consecutiveValleyFrameState.value();
        if (passCount >= getMinCountForTimeSlot()) {
            if (previousFrameStatusInt == FrameStatusEnum.Peak.value)
                return FrameStatusEnum.Peak;
            //前面没有波谷或者前面是波谷
            if (consecutiveValleyFrameSize == 0 || consecutiveValleyFrameSize >= valleyWindowSize) {
                return FrameStatusEnum.Peak;
            }
        }
        return FrameStatusEnum.Valley;
    }

    private int getMinCountForTimeSlot() {
        return 1;
    }

    @AllArgsConstructor
    public enum FrameStatusEnum {
        Peak(1), Valley(-1);
        @Getter
        private int value;
    }
}

