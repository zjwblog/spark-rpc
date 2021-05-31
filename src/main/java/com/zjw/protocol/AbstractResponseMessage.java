package com.zjw.protocol;

import com.zjw.buffer.ManagedBuffer;

public abstract class AbstractResponseMessage extends AbstractMessage implements ResponseMessage {

  protected AbstractResponseMessage(ManagedBuffer body, boolean isBodyInFrame) {
    super(body, isBodyInFrame);
  }

  public abstract ResponseMessage createFailureResponse(String error);
}
