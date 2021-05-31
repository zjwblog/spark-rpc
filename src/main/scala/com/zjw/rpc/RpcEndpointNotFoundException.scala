package com.zjw.rpc

import com.zjw.exception.RPCException

private[zjw] class RpcEndpointNotFoundException(uri: String)
  extends RPCException(s"Cannot find endpoint: $uri")
