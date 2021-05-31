package com.zjw.exception

/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */
class RPCException(message: String, cause: Throwable)
  extends Exception(message, cause) {

  def this(message: String) = this(message, null)
}
