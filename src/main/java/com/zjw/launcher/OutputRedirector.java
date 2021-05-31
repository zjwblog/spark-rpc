package com.zjw.launcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Redirects lines read from a given input stream to a j.u.l.Logger (at INFO level).
 */
class OutputRedirector {

  private final BufferedReader reader;
  private final Logger sink;
  private final Thread thread;
  private final ChildProcAppHandle callback;

  private volatile boolean active;

  OutputRedirector(InputStream in, String loggerName, ThreadFactory tf) {
    this(in, loggerName, tf, null);
  }

  OutputRedirector(
      InputStream in,
      String loggerName,
      ThreadFactory tf,
      ChildProcAppHandle callback) {
    this.active = true;
    this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    this.thread = tf.newThread(this::redirect);
    this.sink = Logger.getLogger(loggerName);
    this.callback = callback;
    thread.start();
  }

  private void redirect() {
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        if (active) {
          sink.info(line.replaceFirst("\\s*$", ""));
        }
      }
    } catch (IOException e) {
      sink.log(Level.FINE, "Error reading child process output.", e);
    } finally {
      if (callback != null) {
        callback.monitorChild();
      }
    }
  }

  /**
   * This method just stops the output of the process from showing up in the local logs.
   * The child's output will still be read (and, thus, the redirect thread will still be
   * alive) to avoid the child process hanging because of lack of output buffer.
   */
  void stop() {
    active = false;
  }

  boolean isAlive() {
    return thread.isAlive();
  }

}
