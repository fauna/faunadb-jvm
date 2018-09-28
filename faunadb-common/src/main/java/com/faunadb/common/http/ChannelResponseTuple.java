package com.faunadb.common.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.concurrent.CompletableFuture;

class ChannelResponseTuple {

  public final Channel channel;
  public final CompletableFuture<FullHttpResponse> responseFuture;

  ChannelResponseTuple(Channel channel, CompletableFuture<FullHttpResponse> responseFuture) {
    this.channel = channel;
    this.responseFuture = responseFuture;
  }

}
