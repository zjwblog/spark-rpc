package com.zjw.server;

import com.zjw.buffer.ManagedBuffer;
import com.zjw.client.TransportClient;
import io.netty.channel.Channel;

/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */
public abstract class StreamManager {
  /**
   * Called in response to a fetchChunk() request. The returned buffer will be passed as-is to the
   * client. A single stream will be associated with a single TCP connection, so this method
   * will not be called in parallel for a particular stream.
   *
   * Chunks may be requested in any order, and requests may be repeated, but it is not required
   * that implementations support this behavior.
   *
   * The returned ManagedBuffer will be release()'d after being written to the network.
   *
   * @param streamId id of a stream that has been previously registered with the StreamManager.
   * @param chunkIndex 0-indexed chunk of the stream that's requested
   */
  public abstract ManagedBuffer getChunk(long streamId, int chunkIndex);

  /**
   * Called in response to a stream() request. The returned data is streamed to the client
   * through a single TCP connection.
   *
   * Note the <code>streamId</code> argument is not related to the similarly named argument in the
   * {@link #getChunk(long, int)} method.
   *
   * @param streamId id of a stream that has been previously registered with the StreamManager.
   * @return A managed buffer for the stream, or null if the stream was not found.
   */
  public ManagedBuffer openStream(String streamId) {
    throw new UnsupportedOperationException();
  }

  /**
   * Indicates that the given channel has been terminated. After this occurs, we are guaranteed not
   * to read from the associated streams again, so any state can be cleaned up.
   */
  public void connectionTerminated(Channel channel) { }

  /**
   * Verify that the client is authorized to read from the given stream.
   *
   * @throws SecurityException If client is not authorized.
   */
  public void checkAuthorization(TransportClient client, long streamId) { }

  /**
   * Return the number of chunks being transferred and not finished yet in this StreamManager.
   */
  public long chunksBeingTransferred() {
    return 0;
  }

  /**
   * Called when start sending a chunk.
   */
  public void chunkBeingSent(long streamId) { }

  /**
   * Called when start sending a stream.
   */
  public void streamBeingSent(String streamId) { }

  /**
   * Called when a chunk is successfully sent.
   */
  public void chunkSent(long streamId) { }

  /**
   * Called when a stream is successfully sent.
   */
  public void streamSent(String streamId) { }

}
