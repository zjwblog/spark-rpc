package com.zjw.client;

public class ChunkFetchFailureException extends RuntimeException {
  public ChunkFetchFailureException(String errorMsg, Throwable cause) {
    super(errorMsg, cause);
  }

  public ChunkFetchFailureException(String errorMsg) {
    super(errorMsg);
  }
}
