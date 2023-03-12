package io.github.changebooks.redis;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 延迟任务，时间轮
 *
 * @author changebooks@qq.com
 */
public final class TimeoutScheduler {
    /**
     * 名称
     */
    private static final String POOL_NAME = "TIMEOUT-SCHEDULER";

    /**
     * 线程工厂
     */
    private static final ThreadFactory THREAD_FACTORY = new DefaultThreadFactory(POOL_NAME);

    /**
     * 刻度时长
     */
    private static final int TICK_DURATION = 100;

    /**
     * 刻度时间的单位
     */
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    /**
     * 总刻度数
     */
    private static final int TICKS_PER_WHEEL = 512;

    /**
     * 检测内存泄漏？
     */
    private static final boolean LEAK_DETECTION = false;

    private static final Timer TIMER =
            new HashedWheelTimer(THREAD_FACTORY, TICK_DURATION, TIME_UNIT, TICKS_PER_WHEEL, LEAK_DETECTION);

    private TimeoutScheduler() {
    }

    /**
     * 新增延迟任务
     *
     * @param task  延迟任务
     * @param delay 延迟时间
     * @param unit  延迟时间的单位
     * @return 任务句柄
     */
    public static Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        return TIMER.newTimeout(task, delay, unit);
    }

    /**
     * 取消未执行的延迟任务
     *
     * @return 被取消的任务句柄
     */
    public static Set<Timeout> stop() {
        return TIMER.stop();
    }

}
