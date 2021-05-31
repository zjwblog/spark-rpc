package com.zjw.launcher;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handle implementation for monitoring apps started as a child process.
 */
class ChildProcAppHandle extends AbstractAppHandle {

  private static final Logger LOG = Logger.getLogger(ChildProcAppHandle.class.getName());

  private volatile Process childProc;
  private OutputRedirector redirector;

  ChildProcAppHandle(LauncherServer server) {
    super(server);
  }

  @Override
  public synchronized void disconnect() {
    try {
      super.disconnect();
    } finally {
      if (redirector != null) {
        redirector.stop();
      }
    }
  }

  @Override
  public synchronized void kill() {
    if (!isDisposed()) {
      setState(State.KILLED);
      disconnect();
      if (childProc != null) {
        if (childProc.isAlive()) {
          childProc.destroyForcibly();
        }
        childProc = null;
      }
    }
  }

  void setChildProc(Process childProc, String loggerName, InputStream logStream) {
    this.childProc = childProc;
    if (logStream != null) {
      this.redirector = new OutputRedirector(logStream, loggerName,
        SparkLauncher.REDIRECTOR_FACTORY, this);
    } else {
      // If there is no log redirection, spawn a thread that will wait for the child process
      // to finish.
      SparkLauncher.REDIRECTOR_FACTORY.newThread(this::monitorChild).start();
    }
  }

  /**
   * Wait for the child process to exit and update the handle's state if necessary, according to
   * the exit code.
   */
  void monitorChild() {
    Process proc = childProc;
    if (proc == null) {
      // Process may have already been disposed of, e.g. by calling kill().
      return;
    }

    while (proc.isAlive()) {
      try {
        proc.waitFor();
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Exception waiting for child process to exit.", e);
      }
    }

    synchronized (this) {
      if (isDisposed()) {
        return;
      }

      int ec;
      try {
        ec = proc.exitValue();
      } catch (Exception e) {
        LOG.log(Level.WARNING, "Exception getting child process exit code, assuming failure.", e);
        ec = 1;
      }

      if (ec != 0) {
        State currState = getState();
        // Override state with failure if the current state is not final, or is success.
        if (!currState.isFinal() || currState == State.FINISHED) {
          setState(State.FAILED, true);
        }
      }

      dispose();
    }
  }

}
