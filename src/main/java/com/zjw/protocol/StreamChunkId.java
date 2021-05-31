package com.zjw.protocol;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;

public final class StreamChunkId implements Encodable {
  public final long streamId;
  public final int chunkIndex;

  public StreamChunkId(long streamId, int chunkIndex) {
    this.streamId = streamId;
    this.chunkIndex = chunkIndex;
  }

  @Override
  public int encodedLength() {
    return 8 + 4;
  }

  public void encode(ByteBuf buffer) {
    buffer.writeLong(streamId);
    buffer.writeInt(chunkIndex);
  }

  public static StreamChunkId decode(ByteBuf buffer) {
    assert buffer.readableBytes() >= 8 + 4;
    long streamId = buffer.readLong();
    int chunkIndex = buffer.readInt();
    return new StreamChunkId(streamId, chunkIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(streamId, chunkIndex);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof StreamChunkId) {
      StreamChunkId o = (StreamChunkId) other;
      return streamId == o.streamId && chunkIndex == o.chunkIndex;
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("streamId", streamId)
      .add("chunkIndex", chunkIndex)
      .toString();
  }
}
