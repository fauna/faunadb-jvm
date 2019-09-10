package com.faunadb.common.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SocketUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;

/**
 * HTTP client built on top of <a href="https://netty.io/4.1/api/index.html">Netty</a>.
 *
 */
public class HttpClient extends AbstractReferenceCounted implements AutoCloseable {

  private static final int WORKER_QUIET_PERIOD = 2_000;
  private static final int WORKER_TIMEOUT = 15_000;
  private static final int MAX_CONTENT_LENGTH = 5 * 1000 * 1000;

  private final int port;
  private final String host;
  private final int connectionTimeout;
  private final int requestTimeout;
  private final boolean secured;
  private final SslContext sslContext;

  private Bootstrap bootstrap;
  private ConcurrentLinkedQueue<Channel> pool = new ConcurrentLinkedQueue<>();
  private EventLoopGroup worker;

  /**
   * @param endpoint the base endpoint URL for this client requests
   */
  public HttpClient(URL endpoint) {
    this(endpoint, -1, -1);
  }

  /**
   *
   * @param endpoint the base endpoint URL for this client requests
   * @param connectionTimeout timeout in milliseconds, <code>-1</code> to ignore it
   * @param requestTimeout timeout in milliseconds, <code>-1</code> to ignore it
   */
  public HttpClient(URL endpoint, int connectionTimeout, int requestTimeout) {
    this.host = extractHost(endpoint);
    this.secured = endpoint.getProtocol().equalsIgnoreCase("https");
    this.port = extractPort(endpoint);
    this.sslContext = initSslContext();

    this.connectionTimeout = connectionTimeout;
    this.requestTimeout = requestTimeout;

    initBoot();
  }

  private void initBoot() {
    worker = initWorker();

    bootstrap = new Bootstrap();
    bootstrap.group(worker);
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

    if (connectionTimeout > 0) {
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout);
    }
  }

  private NioEventLoopGroup initWorker() {
    DefaultThreadFactory defaultThreadFactory = new DefaultThreadFactory("fauna-http-client", true);
    return new NioEventLoopGroup(0, defaultThreadFactory);
  }

  private SslContext initSslContext() {
    if (!secured) {
      return null;
    }

    SslContextBuilder builder = SslContextBuilder.forClient();
    builder.sslProvider(SslProvider.JDK);
    builder.trustManager(InsecureTrustManagerFactory.INSTANCE);

    try {
      return builder.build();
    } catch (SSLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Implemented for {@link AutoCloseable}. Releases the client.
   */
  @Override
  public void close() {
    release();
  }

  /**
   * Implemented for {@link AbstractReferenceCounted}.
   *
   * @returns ReferenceCounted
   */
  @Override
  public ReferenceCounted touch(Object hint) {
    return this;
  }

  /**
   * Frees any resources held by the client. Also closes the underlying worker
   *
   * @throws IOException
   */
  @Override
  protected void deallocate() {
    if (!isClosed()) {
      if (worker != null) {
        worker.shutdownGracefully(WORKER_QUIET_PERIOD, WORKER_TIMEOUT, TimeUnit.MILLISECONDS);
      }
    }
    silentlyClose();
  }

  private void silentlyClose() {
    for (Channel channel : pool) {
      try {
        if (channel.isActive()) {
          channel.close();
        }
      } catch (Throwable t) {}
    }

    pool.clear();
  }

  /**
   * Verifies if the client stills accepting new requests
   *
   * @return
   * @see #close()
   */
  public boolean isClosed() {
    return worker == null || worker.isShuttingDown() || worker.isTerminated();
  }

  /**
   * Sends a {@link FullHttpRequest} that will be processed asynchronously
   *
   * @param req {@link FullHttpRequest}
   * @return {@link CompletableFuture} containing the asynchronous computation of the
   * {@link FullHttpResponse}
   * @throws IllegalStateException if the the client is already closed {@link #isClosed()}
   */
  public CompletableFuture<FullHttpResponse> sendRequest(FullHttpRequest req) {
    if (isClosed()) {
      throw new IllegalStateException("Client already closed");
    }

    ensureHeaders(req);

    CompletableFuture<ChannelResponseTuple> channel = getChannel();

    return channel.thenCompose(channelResponseTuple ->
            writeTo(req, channelResponseTuple.channel).thenCompose(length -> channelResponseTuple.responseFuture)
    );
  }

  private CompletableFuture<ChannelResponseTuple> getChannel() {
    CompletableFuture<ChannelResponseTuple> ret = null;

    do {
      Channel channel = pool.poll();

      if (channel != null && channel.isActive()) {
        CompletableFuture<FullHttpResponse> responseFuture = new CompletableFuture<>();
        HttpResponseHandler handler = new HttpResponseHandler(responseFuture);

        responseFuture.whenComplete((a, b) -> pool.offer(channel));

        channel.pipeline().replace(HttpResponseHandler.class, "response-handler", handler);
        ret = new CompletableFuture<>().completedFuture(new ChannelResponseTuple(channel, responseFuture));

      } else if (channel == null) {
        CompletableFuture<FullHttpResponse> responseFuture = new CompletableFuture<>();
        HttpResponseHandler handler = new HttpResponseHandler(responseFuture);

        CompletableFuture<Channel> connect = connect(ch -> ch.pipeline().addLast(handler));

        ret = connect.thenApply(ch -> {
          responseFuture.whenComplete((a, b) -> pool.offer(ch));
          return new ChannelResponseTuple(ch, responseFuture);
        });
      }

    }
    while (ret == null);

    return ret;
  }

  private void ensureHeaders(FullHttpRequest req) {
    req.headers().set(HttpHeaderNames.USER_AGENT, "Fauna Netty Http Client");
    req.headers().set(HttpHeaderNames.HOST, host);

    if (!req.headers().contains(HttpHeaderNames.CONTENT_LENGTH) && requestContainsPayload(req)) {
      req.headers().set(HttpHeaderNames.CONTENT_LENGTH, req.content().readableBytes());
    }
  }

  private boolean requestContainsPayload(FullHttpRequest req) {
    return (POST.equals(req.method()) || PUT.equals(req.method()) || PATCH.equals(req.method())) &&
            req.content().readableBytes() > 0;
  }

  private CompletableFuture<Integer> writeTo(FullHttpRequest req, Channel ch) {
    int length = req.content().readableBytes();
    ChannelFuture channelFuture = ch.writeAndFlush(req);
    return toFuture(channelFuture).thenApply(ign -> length);
  }

  private CompletableFuture<Channel> connect(Consumer<SocketChannel> socketChannelHandler) {
    SocketAddress socketAddress = SocketUtils.socketAddress(host, port);
    return connect(socketAddress, socketChannelHandler);
  }

  private String extractHost(URL endpoint) {
    if (endpoint.getHost() == null) {
      throw new IllegalArgumentException("Invalid endpoint: no host provided");
    }

    return endpoint.getHost();
  }

  private int extractPort(URL endpoint) {
    int port = endpoint.getPort();

    if (port > 0) {
      return port;
    }

    return secured ? 443 : 80;
  }

  private CompletableFuture<Channel> connect(SocketAddress socketAddress, Consumer<SocketChannel> socketChannelHandler) {
    Bootstrap cloned = bootstrap.clone();
    cloned.handler(new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        if (sslContext != null) {
          p.addFirst("sslHandler", sslHandler(sslContext, ch));
        }

        if (requestTimeout > 0) {
          p.addLast("timeout handler", new HttpClientTimeoutHandler(requestTimeout));
        }

        p.addLast("codec", new HttpClientCodec());
        p.addLast("inflator", new HttpContentDecompressor());
        p.addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));

        socketChannelHandler.accept(ch);
      }
    });

    ChannelFuture cf = cloned.connect(socketAddress);
    CompletableFuture<Channel> completableFuture = toFuture(cf);
    return completableFuture;
  }

  private SslHandler sslHandler(SslContext ctx, SocketChannel ch) {
    SSLEngine sslEngine = ctx.newEngine(ch.alloc(), host, port);
    SslHandler sslHandler = new SslHandler(sslEngine);
    return sslHandler;
  }

  private CompletableFuture<Channel> toFuture(ChannelFuture cf) {
    CompletableFuture<Channel> completableFuture = new CompletableFuture<>();

    cf.addListener((ChannelFutureListener) future -> {
      if (future.isSuccess())
        completableFuture.complete(future.channel());
      else if (future.isCancelled())
        completableFuture.completeExceptionally(new CancellationException());
      else
        completableFuture.completeExceptionally(future.cause());
    });

    return completableFuture;
  }

}
