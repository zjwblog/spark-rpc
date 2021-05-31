package com.zjw.rpc.netty

import com.zjw.rpc.{RpcCallContext, RpcEndpoint, RpcEnv}


private[netty] class RpcEndpointVerifier(override val rpcEnv: RpcEnv, dispatcher: Dispatcher)
  extends RpcEndpoint {

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {
    case RpcEndpointVerifier.CheckExistence(name) => context.reply(dispatcher.verify(name))
  }
}

private[netty] object RpcEndpointVerifier {
  val NAME = "endpoint-verifier"

  case class CheckExistence(name: String)
}
