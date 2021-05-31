package com.zjw.server;

import static com.zjw.util.NettyUtils2.getRemoteAddress;

import com.google.common.base.Throwables;
import com.zjw.buffer.ManagedBuffer;
import com.zjw.buffer.NioManagedBuffer;
import com.zjw.client.RpcResponseCallback;
import com.zjw.client.TransportClient;
import com.zjw.protocol.ChunkFetchFailure;
import com.zjw.protocol.ChunkFetchRequest;
import com.zjw.protocol.ChunkFetchSuccess;
import com.zjw.protocol.Encodable;
import com.zjw.protocol.OneWayMessage;
import com.zjw.protocol.RequestMessage;
import com.zjw.protocol.RpcFailure;
import com.zjw.protocol.RpcRequest;
import com.zjw.protocol.RpcResponse;
import com.zjw.protocol.StreamFailure;
import com.zjw.protocol.StreamRequest;
import com.zjw.protocol.StreamResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler that processes requests from clients and writes chunk data back. Each handler is
 * attached to a single Netty channel, and keeps track of which streams have been fetched via this
 * channel, in order to clean them up if the channel is terminated (see #channelUnregistered).
 * <p>
 * The messages should have been processed by the pipeline setup by {@link TransportServer}.
 */
public class TransportRequestHandler extends MessageHandler<RequestMessage> {

  private static final Logger logger = LoggerFactory.getLogger(TransportRequestHandler.class);

  /**
   * The Netty channel that this handler is associated with.
   */
  private final Channel channel;

  /**
   * Client on the same channel allowing us to talk back to the requester.
   */
  private final TransportClient reverseClient;

  /**
   * Handles all RPC messages.
   */
  private final RpcHandler rpcHandler;

  /**
   * Returns each chunk part of a stream.
   */
  private final StreamManager streamManager;

  /**
   * The max number of chunks being transferred and not finished yet.
   */
  private final long maxChunksBeingTransferred;

  public TransportRequestHandler(
      Channel channel,
      TransportClient reverseClient,
      RpcHandler rpcHandler,
      Long maxChunksBeingTransferred) {
    this.channel = channel;
    this.reverseClient = reverseClient;
    this.rpcHandler = rpcHandler;
    this.streamManager = rpcHandler.getStreamManager();
    this.maxChunksBeingTransferred = maxChunksBeingTransferred;
  }

  @Override
  public void exceptionCaught(Throwable cause) {
    rpcHandler.exceptionCaught(cause, reverseClient);
  }

  @Override
  public void channelActive() {
    rpcHandler.channelActive(reverseClient);
  }

  @Override
  public void channelInactive() {
    if (streamManager != null) {
      try {
        streamManager.connectionTerminated(channel);
      } catch (RuntimeException e) {
        logger.error("StreamManager connectionTerminated() callback failed.", e);
      }
    }
    rpcHandler.channelInactive(reverseClient);
  }

  @Override
  public void handle(RequestMessage request) {
    if (request instanceof ChunkFetchRequest) {
      processFetchRequest((ChunkFetchRequest) request);
    } else if (request instanceof RpcRequest) {
      processRpcRequest((RpcRequest) request);
    } else if (request instanceof OneWayMessage) {
      processOneWayMessage((OneWayMessage) request);
    } else if (request instanceof StreamRequest) {
      processStreamRequest((StreamRequest) request);
    } else {
      throw new IllegalArgumentException("Unknown request type: " + request);
    }
  }

  private void processFetchRequest(final ChunkFetchRequest req) {
    if (logger.isTraceEnabled()) {
      logger.trace("Received req from {} to fetch block {}", getRemoteAddress(channel),
          req.streamChunkId);
    }
    long chunksBeingTransferred = streamManager.chunksBeingTransferred();
    if (chunksBeingTransferred >= maxChunksBeingTransferred) {
      logger.warn("The number of chunks being transferred {} is above {}, close the connection.",
          chunksBeingTransferred, maxChunksBeingTransferred);
      channel.close();
      return;
    }
    ManagedBuffer buf;
    try {
      streamManager.checkAuthorization(reverseClient, req.streamChunkId.streamId);
      buf = streamManager.getChunk(req.streamChunkId.streamId, req.streamChunkId.chunkIndex);
    } catch (Exception e) {
      logger.error(String.format("Error opening block %s for request from %s",
          req.streamChunkId, getRemoteAddress(channel)), e);
      respond(new ChunkFetchFailure(req.streamChunkId, Throwables.getStackTraceAsString(e)));
      return;
    }

    streamManager.chunkBeingSent(req.streamChunkId.streamId);
    respond(new ChunkFetchSuccess(req.streamChunkId, buf)).addListener(future -> {
      streamManager.chunkSent(req.streamChunkId.streamId);
    });
  }

  private void processStreamRequest(final StreamRequest req) {
    if (logger.isTraceEnabled()) {
      logger.trace("Received req from {} to fetch stream {}", getRemoteAddress(channel),
          req.streamId);
    }

    long chunksBeingTransferred = streamManager.chunksBeingTransferred();
    if (chunksBeingTransferred >= maxChunksBeingTransferred) {
      logger.warn("The number of chunks being transferred {} is above {}, close the connection.",
          chunksBeingTransferred, maxChunksBeingTransferred);
      channel.close();
      return;
    }
    ManagedBuffer buf;
    try {
      buf = streamManager.openStream(req.streamId);
    } catch (Exception e) {
      logger.error(String.format(
          "Error opening stream %s for request from %s", req.streamId, getRemoteAddress(channel)),
          e);
      respond(new StreamFailure(req.streamId, Throwables.getStackTraceAsString(e)));
      return;
    }

    if (buf != null) {
      streamManager.streamBeingSent(req.streamId);
      respond(new StreamResponse(req.streamId, buf.size(), buf)).addListener(future -> {
        streamManager.streamSent(req.streamId);
      });
    } else {
      respond(new StreamFailure(req.streamId, String.format(
          "Stream '%s' was not found.", req.streamId)));
    }
  }

  private void processRpcRequest(final RpcRequest req) {
    try {
      rpcHandler.receive(reverseClient, req.body().nioByteBuffer(), new RpcResponseCallback() {
        @Override
        public void onSuccess(ByteBuffer response) {
          respond(new RpcResponse(req.requestId, new NioManagedBuffer(response)));
        }

        @Override
        public void onFailure(Throwable e) {
          respond(new RpcFailure(req.requestId, Throwables.getStackTraceAsString(e)));
        }
      });
    } catch (Exception e) {
      logger.error("Error while invoking RpcHandler#receive() on RPC id " + req.requestId, e);
      respond(new RpcFailure(req.requestId, Throwables.getStackTraceAsString(e)));
    } finally {
      req.body().release();
    }
  }

  private void processOneWayMessage(OneWayMessage req) {
    try {
      rpcHandler.receive(reverseClient, req.body().nioByteBuffer());
    } catch (Exception e) {
      logger.error("Error while invoking RpcHandler#receive() for one-way message.", e);
    } finally {
      req.body().release();
    }
  }

  /**
   * Responds to a single message with some Encodable object. If a failure occurs while sending, it
   * will be logged and the channel closed.
   */
  private ChannelFuture respond(Encodable result) {
    SocketAddress remoteAddress = channel.remoteAddress();
    return channel.writeAndFlush(result).addListener(future -> {
      if (future.isSuccess()) {
        logger.trace("Sent result {} to client {}", result, remoteAddress);
      } else {
        logger.error(String.format("Error sending result %s to %s; closing connection",
            result, remoteAddress), future.cause());
        channel.close();
      }
    });
  }
}
