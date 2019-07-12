package com.fiveonevr.apollo.client.build;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface Injector {


  <T> T getInstance(Class<T> clazz);


  <T> T getInstance(Class<T> clazz, String name);
}
