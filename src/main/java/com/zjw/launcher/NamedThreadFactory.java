package com.zjw.launcher;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

class NamedThreadFactory implements ThreadFactory {

  private final String nameFormat;
  private final AtomicLong threadIds;

  NamedThreadFactory(String nameFormat) {
    this.nameFormat = nameFormat;
    this.threadIds = new AtomicLong();
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r, String.format(nameFormat, threadIds.incrementAndGet()));
    t.setDaemon(true);
    return t;
  }

}
