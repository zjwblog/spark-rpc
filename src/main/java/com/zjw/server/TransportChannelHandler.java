package com.zjw.server;

import com.zjw.client.TransportClient;
import com.zjw.client.TransportResponseHandler;
import com.zjw.protocol.RequestMessage;
import com.zjw.protocol.ResponseMessage;
import com.zjw.util.NettyUtils2;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransportChannelHandler extends ChannelInboundHandlerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(TransportChannelHandler.class);

  private final TransportClient client;
  private final TransportResponseHandler responseHandler;
  private final TransportRequestHandler requestHandler;
  private final long requestTimeoutNs;
  private final boolean closeIdleConnections;

  public TransportChannelHandler(
      TransportClient client,
      TransportResponseHandler responseHandler,
      TransportRequestHandler requestHandler,
      long requestTimeoutMs,
      boolean closeIdleConnections) {
    this.client = client;
    this.responseHandler = responseHandler;
    this.requestHandler = requestHandler;
    this.requestTimeoutNs = requestTimeoutMs * 1000L * 1000;
    this.closeIdleConnections = closeIdleConnections;
  }

  public TransportClient getClient() {
    return client;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    logger.warn("Exception in connection from " + NettyUtils2.getRemoteAddress(ctx.channel()),
      cause);
    requestHandler.exceptionCaught(cause);
    responseHandler.exceptionCaught(cause);
    ctx.close();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    try {
      requestHandler.channelActive();
    } catch (RuntimeException e) {
      logger.error("Exception from request handler while channel is active", e);
    }
    try {
      responseHandler.channelActive();
    } catch (RuntimeException e) {
      logger.error("Exception from response handler while channel is active", e);
    }
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    try {
      requestHandler.channelInactive();
    } catch (RuntimeException e) {
      logger.error("Exception from request handler while channel is inactive", e);
    }
    try {
      responseHandler.channelInactive();
    } catch (RuntimeException e) {
      logger.error("Exception from response handler while channel is inactive", e);
    }
    super.channelInactive(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object request) throws Exception {
    if (request instanceof RequestMessage) {
      requestHandler.handle((RequestMessage) request);
    } else if (request instanceof ResponseMessage) {
      responseHandler.handle((ResponseMessage) request);
    } else {
      ctx.fireChannelRead(request);
    }
  }

  /** Triggered based on events from an {@link io.netty.handler.timeout.IdleStateHandler}. */
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      // See class comment for timeout semantics. In addition to ensuring we only timeout while
      // there are outstanding requests, we also do a secondary consistency check to ensure
      // there's no race between the idle timeout and incrementing the numOutstandingRequests
      // (see SPARK-7003).
      //
      // To avoid a race between TransportClientFactory.createClient() and this code which could
      // result in an inactive client being returned, this needs to run in a synchronized block.
      synchronized (this) {
        boolean isActuallyOverdue =
          System.nanoTime() - responseHandler.getTimeOfLastRequestNs() > requestTimeoutNs;
        if (e.state() == IdleState.ALL_IDLE && isActuallyOverdue) {
          if (responseHandler.numOutstandingRequests() > 0) {
            String address = NettyUtils2.getRemoteAddress(ctx.channel());
            logger.error("Connection to {} has been quiet for {} ms while there are outstanding " +
              "requests. Assuming connection is dead; please adjust spark.network.timeout if " +
              "this is wrong.", address, requestTimeoutNs / 1000 / 1000);
            client.timeOut();
            ctx.close();
          } else if (closeIdleConnections) {
            // While CloseIdleConnections is enable, we also close idle connection
            client.timeOut();
            ctx.close();
          }
        }
      }
    }
    ctx.fireUserEventTriggered(evt);
  }

  public TransportResponseHandler getResponseHandler() {
    return responseHandler;
  }

}
