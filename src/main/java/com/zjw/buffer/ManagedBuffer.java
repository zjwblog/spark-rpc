package com.zjw.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */
public abstract class ManagedBuffer {

  /**
   * Number of bytes of the data. If this buffer will decrypt for all of the views into the data,
   * this is the size of the decrypted data.
   */
  public abstract long size();

  /**
   * Exposes this buffer's data as an NIO ByteBuffer. Changing the position and limit of the
   * returned ByteBuffer should not affect the content of this buffer.
   */
  // TODO: Deprecate this, usage may require expensive memory mapping or allocation.
  public abstract ByteBuffer nioByteBuffer() throws IOException;

  /**
   * Exposes this buffer's data as an InputStream. The underlying implementation does not
   * necessarily check for the length of bytes read, so the caller is responsible for making sure
   * it does not go over the limit.
   */
  public abstract InputStream createInputStream() throws IOException;

  /**
   * Increment the reference count by one if applicable.
   */
  public abstract ManagedBuffer retain();

  /**
   * If applicable, decrement the reference count by one and deallocates the buffer if the
   * reference count reaches zero.
   */
  public abstract ManagedBuffer release();

  /**
   * Convert the buffer into an Netty object, used to write the data out. The return value is either
   * a {@link io.netty.buffer.ByteBuf} or a {@link io.netty.channel.FileRegion}.
   *
   * If this method returns a ByteBuf, then that buffer's reference count will be incremented and
   * the caller will be responsible for releasing this new reference.
   */
  public abstract Object convertToNetty() throws IOException;
}
