package com.fiveonevr.apollo.client.internals;

import com.fiveonevr.apollo.client.spi.ConfigFactory;



//将namespace与configfactory进行注册
public interface ConfigRegistry {

  void register(String namespace, ConfigFactory factory);

  ConfigFactory getFactory(String namespace);
}
