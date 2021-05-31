package com.zjw.rpc

import com.zjw.util.Utils


/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */
private[zjw] case class RpcAddress(host: String, port: Int) {

  def hostPort: String = host + ":" + port

  /** Returns a string in the form of "spark://host:port". */
  def toSparkURL: String = "spark://" + hostPort

  override def toString: String = hostPort
}


private[zjw] object RpcAddress {

  /** Return the [[RpcAddress]] represented by `uri`. */
  def fromURIString(uri: String): RpcAddress = {
    val uriObj = new java.net.URI(uri)
    RpcAddress(uriObj.getHost, uriObj.getPort)
  }

  /** Returns the [[RpcAddress]] encoded in the form of "spark://host:port" */
  def fromSparkURL(sparkUrl: String): RpcAddress = {
    val (host, port) = Utils.extractHostPortFromSparkUrl(sparkUrl)
    RpcAddress(host, port)
  }
}
