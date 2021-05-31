package com.zjw.rpc

import com.zjw.exception.RPCException

/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */

private[zjw] trait RpcEnvFactory {

  def create(config: RpcEnvConfig): RpcEnv
}

private[zjw] trait RpcEndpoint {

  val rpcEnv: RpcEnv

  final def self: RpcEndpointRef = {
    require(rpcEnv != null, "rpcEnv has not been initialized")
    rpcEnv.endpointRef(this)
  }

  def receive: PartialFunction[Any, Unit] = {
    case _ => throw new RPCException(self + " does not implement 'receive'")
  }

  def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case _ => context.sendFailure(new RPCException(self + " won't reply anything"))
  }

  def onError(cause: Throwable): Unit = {
    // By default, throw e and let RpcEnv handle it
    throw cause
  }

  def onConnected(remoteAddress: RpcAddress): Unit = {
    // By default, do nothing.
  }

  def onDisconnected(remoteAddress: RpcAddress): Unit = {
    // By default, do nothing.
  }

  def onNetworkError(cause: Throwable, remoteAddress: RpcAddress): Unit = {
    // By default, do nothing.
  }

  def onStart(): Unit = {
    // By default, do nothing.
  }

  def onStop(): Unit = {
    // By default, do nothing.
  }

  final def stop(): Unit = {
    val _self = self
    if (_self != null) {
      rpcEnv.stop(_self)
    }
  }
}

private[zjw] trait ThreadSafeRpcEndpoint extends RpcEndpoint
