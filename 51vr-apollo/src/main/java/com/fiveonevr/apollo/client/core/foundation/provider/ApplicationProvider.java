package com.fiveonevr.apollo.client.core.foundation.provider;

import java.io.InputStream;


public interface ApplicationProvider extends Provider {

  String getAppId();


  boolean isAppIdSet();


  void initialize(InputStream in);
}
