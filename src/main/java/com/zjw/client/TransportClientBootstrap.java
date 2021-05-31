package com.zjw.client;

import io.netty.channel.Channel;

public interface TransportClientBootstrap {
  /** Performs the bootstrapping operation, throwing an exception on failure. */
  void doBootstrap(TransportClient client, Channel channel) throws RuntimeException;
}
