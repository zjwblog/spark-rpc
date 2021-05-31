package com.zjw.client;

import com.zjw.util.TransportFrameDecoder;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

class StreamInterceptor implements TransportFrameDecoder.Interceptor {

  private final TransportResponseHandler handler;
  private final String streamId;
  private final long byteCount;
  private final StreamCallback callback;
  private long bytesRead;

  StreamInterceptor(
      TransportResponseHandler handler,
      String streamId,
      long byteCount,
      StreamCallback callback) {
    this.handler = handler;
    this.streamId = streamId;
    this.byteCount = byteCount;
    this.callback = callback;
    this.bytesRead = 0;
  }

  @Override
  public void exceptionCaught(Throwable cause) throws Exception {
    handler.deactivateStream();
    callback.onFailure(streamId, cause);
  }

  @Override
  public void channelInactive() throws Exception {
    handler.deactivateStream();
    callback.onFailure(streamId, new ClosedChannelException());
  }

  @Override
  public boolean handle(ByteBuf buf) throws Exception {
    int toRead = (int) Math.min(buf.readableBytes(), byteCount - bytesRead);
    ByteBuffer nioBuffer = buf.readSlice(toRead).nioBuffer();

    int available = nioBuffer.remaining();
    callback.onData(streamId, nioBuffer);
    bytesRead += available;
    if (bytesRead > byteCount) {
      RuntimeException re = new IllegalStateException(String.format(
        "Read too many bytes? Expected %d, but read %d.", byteCount, bytesRead));
      callback.onFailure(streamId, re);
      handler.deactivateStream();
      throw re;
    } else if (bytesRead == byteCount) {
      handler.deactivateStream();
      callback.onComplete(streamId);
    }

    return bytesRead != byteCount;
  }

}
