package com.fiveonevr.apollo.client.core;

/**
 * Schedule policy
 * @author Jason Song(song_s@ctrip.com)
 */
public interface SchedulePolicy {
  long fail();

  void success();
}
