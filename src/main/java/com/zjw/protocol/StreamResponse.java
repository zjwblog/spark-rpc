package com.zjw.protocol;

import com.google.common.base.Objects;
import com.zjw.buffer.ManagedBuffer;
import io.netty.buffer.ByteBuf;

public final class StreamResponse extends AbstractResponseMessage {
  public final String streamId;
  public final long byteCount;

  public StreamResponse(String streamId, long byteCount, ManagedBuffer buffer) {
    super(buffer, false);
    this.streamId = streamId;
    this.byteCount = byteCount;
  }

  @Override
  public Type type() { return Type.StreamResponse; }

  @Override
  public int encodedLength() {
    return 8 + Encoders.Strings.encodedLength(streamId);
  }

  /** Encoding does NOT include 'buffer' itself. See {@link MessageEncoder}. */
  @Override
  public void encode(ByteBuf buf) {
    Encoders.Strings.encode(buf, streamId);
    buf.writeLong(byteCount);
  }

  @Override
  public ResponseMessage createFailureResponse(String error) {
    return new StreamFailure(streamId, error);
  }

  public static StreamResponse decode(ByteBuf buf) {
    String streamId = Encoders.Strings.decode(buf);
    long byteCount = buf.readLong();
    return new StreamResponse(streamId, byteCount, null);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(byteCount, streamId, body());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof StreamResponse) {
      StreamResponse o = (StreamResponse) other;
      return byteCount == o.byteCount && streamId.equals(o.streamId);
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("streamId", streamId)
      .add("byteCount", byteCount)
      .add("body", body())
      .toString();
  }

}
