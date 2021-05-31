package com.zjw.protocol;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;

public final class StreamRequest extends AbstractMessage implements RequestMessage {
   public final String streamId;

   public StreamRequest(String streamId) {
     this.streamId = streamId;
   }

  @Override
  public Type type() { return Type.StreamRequest; }

  @Override
  public int encodedLength() {
    return Encoders.Strings.encodedLength(streamId);
  }

  @Override
  public void encode(ByteBuf buf) {
    Encoders.Strings.encode(buf, streamId);
  }

  public static StreamRequest decode(ByteBuf buf) {
    String streamId = Encoders.Strings.decode(buf);
    return new StreamRequest(streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(streamId);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof StreamRequest) {
      StreamRequest o = (StreamRequest) other;
      return streamId.equals(o.streamId);
    }
    return false;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("streamId", streamId)
      .toString();
  }

}
