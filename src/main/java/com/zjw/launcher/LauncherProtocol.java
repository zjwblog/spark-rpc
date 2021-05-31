package com.zjw.launcher;

import java.io.Serializable;

/**
 * Message definitions for the launcher communication protocol. These messages must remain
 * backwards-compatible, so that the launcher can talk to older versions of Spark that support
 * the protocol.
 */
final class LauncherProtocol {

  /** Environment variable where the server port is stored. */
  static final String ENV_LAUNCHER_PORT = "_SPARK_LAUNCHER_PORT";

  /** Environment variable where the secret for connecting back to the server is stored. */
  static final String ENV_LAUNCHER_SECRET = "_SPARK_LAUNCHER_SECRET";

  /** Spark conf key used to propagate the server port for in-process launches. */
  static final String CONF_LAUNCHER_PORT = "spark.launcher.port";

  /** Spark conf key used to propagate the app secret for in-process launches. */
  static final String CONF_LAUNCHER_SECRET = "spark.launcher.secret";

  static class Message implements Serializable {

  }

  /**
   * Hello message, sent from client to server.
   */
  static class Hello extends Message {

    final String secret;
    final String sparkVersion;

    Hello(String secret, String version) {
      this.secret = secret;
      this.sparkVersion = version;
    }

  }

  /**
   * SetAppId message, sent from client to server.
   */
  static class SetAppId extends Message {

    final String appId;

    SetAppId(String appId) {
      this.appId = appId;
    }

  }

  /**
   * SetState message, sent from client to server.
   */
  static class SetState extends Message {

    final SparkAppHandle.State state;

    SetState(SparkAppHandle.State state) {
      this.state = state;
    }

  }

  /**
   * Stop message, send from server to client to stop the application.
   */
  static class Stop extends Message {

  }

}
