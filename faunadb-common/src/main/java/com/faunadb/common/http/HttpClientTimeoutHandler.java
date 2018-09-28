package com.faunadb.common.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutException;

import java.util.concurrent.TimeUnit;

public class HttpClientTimeoutHandler extends ChannelDuplexHandler {

  private volatile boolean waiting = false;

  private final IdleStateHandler trigger;

  public HttpClientTimeoutHandler(int responseTimeout) {
    trigger = new IdleStateHandler(responseTimeout, 0, 0, TimeUnit.MILLISECONDS);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().addBefore(ctx.name(), "idle trigger", trigger);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().remove(trigger);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof FullHttpMessage || msg instanceof LastHttpContent)
      waiting = true;

    ctx.write(msg, promise);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof FullHttpRequest || msg instanceof LastHttpContent)
      waiting = false;

    ctx.fireChannelRead(msg);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object e) throws Exception {
    if (e instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) e;

      if (event.state() == IdleState.READER_IDLE && waiting)
        throw ReadTimeoutException.INSTANCE;
    }
  }

}
