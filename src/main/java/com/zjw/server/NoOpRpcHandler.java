package com.zjw.server;

import com.zjw.client.RpcResponseCallback;
import com.zjw.client.TransportClient;
import java.nio.ByteBuffer;


/** An RpcHandler suitable for a client-only TransportContext, which cannot receive RPCs. */
public class NoOpRpcHandler extends RpcHandler {
  private final StreamManager streamManager;

  public NoOpRpcHandler() {
    streamManager = new OneForOneStreamManager();
  }

  @Override
  public void receive(TransportClient client, ByteBuffer message, RpcResponseCallback callback) {
    throw new UnsupportedOperationException("Cannot handle messages");
  }

  @Override
  public StreamManager getStreamManager() { return streamManager; }
}
