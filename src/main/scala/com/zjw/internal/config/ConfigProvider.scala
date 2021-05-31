package com.zjw.internal.config

/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */

import java.util.{Map => JMap}

import com.zjw.conf.RpcConf

/**
 * A source of configuration values.
 */
private[zjw] trait ConfigProvider {

  def get(key: String): Option[String]

}

private[zjw] class EnvProvider extends ConfigProvider {

  override def get(key: String): Option[String] = sys.env.get(key)

}

private[zjw] class SystemProvider extends ConfigProvider {

  override def get(key: String): Option[String] = sys.props.get(key)

}

private[zjw] class MapProvider(conf: JMap[String, String]) extends ConfigProvider {

  override def get(key: String): Option[String] = Option(conf.get(key))

}

/**
 * A config provider that only reads Spark config keys.
 */
private[zjw] class SparkConfigProvider(conf: JMap[String, String]) extends ConfigProvider {

  override def get(key: String): Option[String] = {
    if (key.startsWith("spark.")) {
      Option(conf.get(key)).orElse(RpcConf.getDeprecatedConfig(key, conf))
    } else {
      None
    }
  }

}
