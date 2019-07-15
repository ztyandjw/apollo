package com.fiveonevr.apollo.client.core;

/**
 * @author T1m Zhang(49244143@qq.com) 2019/7/15.
 */

public interface SchedulePolicy {
    long fail();
    void success();
}
