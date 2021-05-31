package com.zjw.client;

import java.nio.ByteBuffer;

/**
 * Created by zjwblog<co.zjwblog@gmail.com> on 2021/5/28
 */
public interface RpcResponseCallback {
  void onSuccess(ByteBuffer response);

  void onFailure(Throwable e);
}
