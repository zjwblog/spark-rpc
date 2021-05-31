package com.zjw.protocol;

import com.google.common.base.Objects;
import com.zjw.buffer.ManagedBuffer;
import com.zjw.buffer.NettyManagedBuffer;
import io.netty.buffer.ByteBuf;

public final class ChunkFetchSuccess extends AbstractResponseMessage {
  public final StreamChunkId streamChunkId;

  public ChunkFetchSuccess(StreamChunkId streamChunkId, ManagedBuffer buffer) {
    super(buffer, true);
    this.streamChunkId = streamChunkId;
  }

  @Override
  public Type type() { return Type.ChunkFetchSuccess; }

  @Override
  public int encodedLength() {
    return streamChunkId.encodedLength();
  }

  @Override
  public void encode(ByteBuf buf) {
    streamChunkId.encode(buf);
  }

  @Override
  public ResponseMessage createFailureResponse(String error) {
    return new ChunkFetchFailure(streamChunkId, error);
  }

  /** Decoding uses the given ByteBuf as our data, and will retain() it. */
  public static ChunkFetchSuccess decode(ByteBuf buf) {
    StreamChunkId streamChunkId = StreamChunkId.decode(buf);
    buf.retain();
    NettyManagedBuffer managedBuf = new NettyManagedBuffer(buf.duplicate());
    return new ChunkFetchSuccess(streamChunkId, managedBuf);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(streamChunkId, body());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof ChunkFetchSuccess) {
      ChunkFetchSuccess o = (ChunkFetchSuccess) other;
      return streamChunkId.equals(o.streamChunkId) && super.equals(o);
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("streamChunkId", streamChunkId)
      .add("buffer", body())
      .toString();
  }
}
