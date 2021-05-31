package com.zjw.util;

import com.zjw.client.TransportClient;
import com.zjw.client.TransportClientBootstrap;
import com.zjw.client.TransportClientFactory;
import com.zjw.client.TransportResponseHandler;
import com.zjw.protocol.MessageDecoder;
import com.zjw.protocol.MessageEncoder;
import com.zjw.server.RpcHandler;
import com.zjw.server.TransportChannelHandler;
import com.zjw.server.TransportRequestHandler;
import com.zjw.server.TransportServer;
import com.zjw.server.TransportServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportContext {

  private static final Logger logger = LoggerFactory.getLogger(TransportContext.class);

  private final TransportConf conf;
  private final RpcHandler rpcHandler;
  private final boolean closeIdleConnections;

  /**
   * Force to create MessageEncoder and MessageDecoder so that we can make sure they will be created
   * before switching the current context class loader to ExecutorClassLoader.
   * <p>
   * Netty's MessageToMessageEncoder uses Javassist to generate a matcher class and the
   * implementation calls "Class.forName" to check if this calls is already generated. If the
   * following two objects are created in "ExecutorClassLoader.findClass", it will cause
   * "ClassCircularityError". This is because loading this Netty generated class will call
   * "ExecutorClassLoader.findClass" to search this class, and "ExecutorClassLoader" will try to use
   * RPC to load it and cause to load the non-exist matcher class again. JVM will report
   * `ClassCircularityError` to prevent such infinite recursion. (See SPARK-17714)
   */
  private static final MessageEncoder ENCODER = MessageEncoder.INSTANCE;
  private static final MessageDecoder DECODER = MessageDecoder.INSTANCE;

  public TransportContext(TransportConf conf, RpcHandler rpcHandler) {
    this(conf, rpcHandler, false);
  }

  public TransportContext(
      TransportConf conf,
      RpcHandler rpcHandler,
      boolean closeIdleConnections) {
    this.conf = conf;
    this.rpcHandler = rpcHandler;
    this.closeIdleConnections = closeIdleConnections;
  }

  /**
   * Initializes a ClientFactory which runs the given TransportClientBootstraps prior to returning a
   * new Client. Bootstraps will be executed synchronously, and must run successfully in order to
   * create a Client.
   */
  public TransportClientFactory createClientFactory(List<TransportClientBootstrap> bootstraps) {
    return new TransportClientFactory(this, bootstraps);
  }

  public TransportClientFactory createClientFactory() {
    return createClientFactory(new ArrayList<>());
  }

  /**
   * Create a server which will attempt to bind to a specific port.
   */
  public TransportServer createServer(int port, List<TransportServerBootstrap> bootstraps) {
    return new TransportServer(this, null, port, rpcHandler, bootstraps);
  }

  /**
   * Create a server which will attempt to bind to a specific host and port.
   */
  public TransportServer createServer(
      String host, int port, List<TransportServerBootstrap> bootstraps) {
    return new TransportServer(this, host, port, rpcHandler, bootstraps);
  }

  /**
   * Creates a new server, binding to any available ephemeral port.
   */
  public TransportServer createServer(List<TransportServerBootstrap> bootstraps) {
    return createServer(0, bootstraps);
  }

  public TransportServer createServer() {
    return createServer(0, new ArrayList<>());
  }

  public TransportChannelHandler initializePipeline(SocketChannel channel) {
    return initializePipeline(channel, rpcHandler);
  }

  public TransportChannelHandler initializePipeline(
      SocketChannel channel,
      RpcHandler channelRpcHandler) {
    try {
      TransportChannelHandler channelHandler = createChannelHandler(channel, channelRpcHandler);
      channel.pipeline()
          .addLast("encoder", ENCODER)
          .addLast(TransportFrameDecoder.HANDLER_NAME, NettyUtils2.createFrameDecoder())
          .addLast("decoder", DECODER)
          .addLast("idleStateHandler",
              new IdleStateHandler(0, 0, conf.connectionTimeoutMs() / 1000))
          // NOTE: Chunks are currently guaranteed to be returned in the order of request, but this
          // would require more logic to guarantee if this were not part of the same event loop.
          .addLast("handler", channelHandler);
      return channelHandler;
    } catch (RuntimeException e) {
      logger.error("Error while initializing Netty pipeline", e);
      throw e;
    }
  }

  /**
   * Creates the server- and client-side handler which is used to handle both RequestMessages and
   * ResponseMessages. The channel is expected to have been successfully created, though certain
   * properties (such as the remoteAddress()) may not be available yet.
   */
  private TransportChannelHandler createChannelHandler(Channel channel, RpcHandler rpcHandler) {
    TransportResponseHandler responseHandler = new TransportResponseHandler(channel);
    TransportClient client = new TransportClient(channel, responseHandler);
    TransportRequestHandler requestHandler = new TransportRequestHandler(channel, client,
        rpcHandler, conf.maxChunksBeingTransferred());
    return new TransportChannelHandler(client, responseHandler, requestHandler,
        conf.connectionTimeoutMs(), closeIdleConnections);
  }

  public TransportConf getConf() {
    return conf;
  }
}
