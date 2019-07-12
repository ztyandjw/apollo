package com.fiveonevr.apollo.client.build;


public interface Ordered {

  int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;
  int LOWEST_PRECEDENCE = Integer.MAX_VALUE;
  int getOrder();
}
