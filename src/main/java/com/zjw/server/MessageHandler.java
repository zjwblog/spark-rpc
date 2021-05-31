package com.zjw.server;

import com.zjw.protocol.Message;

public abstract class MessageHandler<T extends Message> {
  /** Handles the receipt of a single message. */
  public abstract void handle(T message) throws Exception;

  /** Invoked when the channel this MessageHandler is on is active. */
  public abstract void channelActive();

  /** Invoked when an exception was caught on the Channel. */
  public abstract void exceptionCaught(Throwable cause);

  /** Invoked when the channel this MessageHandler is on is inactive. */
  public abstract void channelInactive();
}
