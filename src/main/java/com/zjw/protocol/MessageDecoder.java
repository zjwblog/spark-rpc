package com.zjw.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public final class MessageDecoder extends MessageToMessageDecoder<ByteBuf> {

  private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

  public static final MessageDecoder INSTANCE = new MessageDecoder();

  private MessageDecoder() {}

  @Override
  public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    Message.Type msgType = Message.Type.decode(in);
    Message decoded = decode(msgType, in);
    assert decoded.type() == msgType;
    logger.trace("Received message {}: {}", msgType, decoded);
    out.add(decoded);
  }

  private Message decode(Message.Type msgType, ByteBuf in) {
    switch (msgType) {
      case ChunkFetchRequest:
        return ChunkFetchRequest.decode(in);

      case ChunkFetchSuccess:
        return ChunkFetchSuccess.decode(in);

      case ChunkFetchFailure:
        return ChunkFetchFailure.decode(in);

      case RpcRequest:
        return RpcRequest.decode(in);

      case RpcResponse:
        return RpcResponse.decode(in);

      case RpcFailure:
        return RpcFailure.decode(in);

      case OneWayMessage:
        return OneWayMessage.decode(in);

      case StreamRequest:
        return StreamRequest.decode(in);

      case StreamResponse:
        return StreamResponse.decode(in);

      case StreamFailure:
        return StreamFailure.decode(in);

      default:
        throw new IllegalArgumentException("Unexpected message type: " + msgType);
    }
  }
}
