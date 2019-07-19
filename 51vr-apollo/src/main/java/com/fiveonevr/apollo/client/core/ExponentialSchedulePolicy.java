package com.fiveonevr.apollo.client.core;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/15.
 */


//指数级别增长策略
public class ExponentialSchedulePolicy implements SchedulePolicy{

    private final long delayTimeLowerBound;
    private final long delayTimeUpperBound;
    private long lastDelayTime;

    public ExponentialSchedulePolicy(long delayTimeLowerBound, long delayTimeUpperBound) {
        this.delayTimeLowerBound = delayTimeLowerBound;
        this.delayTimeUpperBound = delayTimeUpperBound;
    }

    @Override
    public long fail() {
        long delayTime = this.lastDelayTime;
        //说明第一次进来，或者前一次执行成功，将delayTime置为下限数字
        if (delayTime == 0) {
            delayTime = this.delayTimeLowerBound;
        } else {
            delayTime = Math.min(this.lastDelayTime << 1, this.delayTimeUpperBound);
        }
        this.lastDelayTime = delayTime;
        return lastDelayTime;
    }


    @Override
    public void success() {
        this.lastDelayTime = 0;
    }
}
