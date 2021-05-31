package com.zjw.server;

import io.netty.channel.Channel;

public interface TransportServerBootstrap {
  /**
   * Customizes the channel to include new features, if needed.
   *
   * @param channel The connected channel opened by the client.
   * @param rpcHandler The RPC handler for the server.
   * @return The RPC handler to use for the channel.
   */
  RpcHandler doBootstrap(Channel channel, RpcHandler rpcHandler);
}
