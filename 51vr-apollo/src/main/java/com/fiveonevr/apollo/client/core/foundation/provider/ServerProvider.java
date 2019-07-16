package com.fiveonevr.apollo.client.core.foundation.provider;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provider for server related properties
 */
public interface ServerProvider extends Provider {

  String getEnvType();


  boolean isEnvTypeSet();


  String getDataCenter();


  boolean isDataCenterSet();


  void initialize(InputStream in) throws IOException;
}
