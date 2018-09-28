package com.faunadb.common.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static io.netty.util.CharsetUtil.UTF_8;
import static java.lang.String.format;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final CompletableFuture<FullHttpResponse> responseFuture;

  HttpResponseHandler(CompletableFuture<FullHttpResponse> responseFuture) {
    super(false);
    this.responseFuture = responseFuture;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
    debugResponse(response);
    responseFuture.complete(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    ctx.close();
    responseFuture.completeExceptionally(cause);
  }

  private void debugResponse(FullHttpResponse httpResponse) {
    if (!log.isDebugEnabled())
      return;

    log.debug(format("> STATUS CODE: %d", httpResponse.status().code()));

    for (CharSequence name : httpResponse.headers().names())
      for (CharSequence value : httpResponse.headers().getAll(name))
        log.debug(format("> HEADER  %s : %s", name, value));

    log.debug(format("> CONTENT: %s", httpResponse.content().toString(UTF_8)));
  }

}
