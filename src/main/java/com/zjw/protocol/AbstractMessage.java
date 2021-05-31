package com.zjw.protocol;

import com.google.common.base.Objects;
import com.zjw.buffer.ManagedBuffer;

public abstract class AbstractMessage implements Message {
  private final ManagedBuffer body;
  private final boolean isBodyInFrame;

  protected AbstractMessage() {
    this(null, false);
  }

  protected AbstractMessage(ManagedBuffer body, boolean isBodyInFrame) {
    this.body = body;
    this.isBodyInFrame = isBodyInFrame;
  }

  @Override
  public ManagedBuffer body() {
    return body;
  }

  @Override
  public boolean isBodyInFrame() {
    return isBodyInFrame;
  }

  protected boolean equals(AbstractMessage other) {
    return isBodyInFrame == other.isBodyInFrame && Objects.equal(body, other.body);
  }

}
