/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * Minimal HTTP/2 client used by the Netty server end-to-end tests. It speaks HTTP/2
 * over cleartext with prior knowledge, or over TLS with ALPN, and drives the stream
 * level frame API directly so tests do not depend on the HTTP/1.x object model.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http2TestClient implements AutoCloseable {

  private static final long TIMEOUT_SECONDS = 10;

  private final EventLoopGroup group = new NioEventLoopGroup(1);

  private final String host;

  private final int port;

  private final boolean secure;

  private final Channel connection;

  private Http2TestClient(String host, int port, boolean secure) throws InterruptedException {
    this.host = host;
    this.port = port;
    this.secure = secure;
    this.connection = connect();
  }

  static Http2TestClient cleartext(int port) throws Exception {
    return new Http2TestClient("localhost", port, false);
  }

  static Http2TestClient secure(int port) throws Exception {
    return new Http2TestClient("localhost", port, true);
  }

  private Channel connect() throws InterruptedException {
    Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) {
                if (secure) {
                  ch.pipeline().addLast(createSslContext().newHandler(ch.alloc(), host, port));
                }
                ch.pipeline()
                        .addLast(Http2FrameCodecBuilder.forClient().build())
                        .addLast(new Http2MultiplexHandler(new Http2StreamChannelInitializer()));
              }
            });
    return bootstrap.connect(host, port).sync().channel();
  }

  private SslContext createSslContext() {
    try {
      return SslContextBuilder.forClient()
              .trustManager(InsecureTrustManagerFactory.INSTANCE)
              .applicationProtocolConfig(new ApplicationProtocolConfig(
                      ApplicationProtocolConfig.Protocol.ALPN,
                      ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                      ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                      ApplicationProtocolNames.HTTP_2,
                      ApplicationProtocolNames.HTTP_1_1))
              .build();
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to create client SSL context", ex);
    }
  }

  Response get(String path) throws Exception {
    return request("GET", path, List.of(), null);
  }

  Response post(String path, byte[] body) throws Exception {
    List<byte[]> chunks = new ArrayList<>();
    chunks.add(body);
    return request("POST", path, chunks, null);
  }

  Response postChunked(String path, List<byte[]> chunks) throws Exception {
    return request("POST", path, chunks, null);
  }

  Response postWithTrailers(String path, byte[] body, Map<String, String> trailers) throws Exception {
    Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(connection);
    CompletableFuture<Response> future = new CompletableFuture<>();
    bootstrap.handler(new ResponseHandler(future));
    Http2StreamChannel stream = bootstrap.open().sync().getNow();

    Http2Headers headers = new DefaultHttp2Headers()
            .method("POST")
            .path(path)
            .scheme(secure ? "https" : "http")
            .authority(host + ":" + port);
    stream.write(new DefaultHttp2HeadersFrame(headers, false));
    stream.write(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(body), false));

    Http2Headers trailingHeaders = new DefaultHttp2Headers();
    for (Map.Entry<String, String> entry : trailers.entrySet()) {
      trailingHeaders.add(entry.getKey(), entry.getValue());
    }
    stream.write(new DefaultHttp2HeadersFrame(trailingHeaders, true));
    stream.flush();
    return await(future);
  }

  Response request(String method, String path, List<byte[]> chunks,
          @Nullable Map<String, String> extraHeaders) throws Exception {
    Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(connection);
    CompletableFuture<Response> future = new CompletableFuture<>();
    bootstrap.handler(new ResponseHandler(future));
    Http2StreamChannel stream = bootstrap.open().sync().getNow();

    Http2Headers headers = new DefaultHttp2Headers()
            .method(method)
            .path(path)
            .scheme(secure ? "https" : "http")
            .authority(host + ":" + port);
    if (extraHeaders != null) {
      for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
        headers.add(entry.getKey(), entry.getValue());
      }
    }

    boolean empty = chunks.isEmpty();
    stream.write(new DefaultHttp2HeadersFrame(headers, empty));
    for (int i = 0; i < chunks.size(); i++) {
      boolean last = i == chunks.size() - 1;
      stream.write(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(chunks.get(i)), last));
    }
    stream.flush();
    return await(future);
  }

  /**
   * Open a stream, send the request headers with an {@code expect: 100-continue}
   * header, wait for the informational response and only then send the body.
   */
  ContinueResponse expectContinue(String path, byte[] body) throws Exception {
    Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(connection);
    CompletableFuture<Response> future = new CompletableFuture<>();
    CompletableFuture<Integer> continueFuture = new CompletableFuture<>();
    bootstrap.handler(new ResponseHandler(future, continueFuture));
    Http2StreamChannel stream = bootstrap.open().sync().getNow();

    Http2Headers headers = new DefaultHttp2Headers()
            .method("POST")
            .path(path)
            .scheme(secure ? "https" : "http")
            .authority(host + ":" + port)
            .add("expect", "100-continue");
    stream.write(new DefaultHttp2HeadersFrame(headers, false));
    stream.flush();

    int status = continueFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    if (status == 100) {
      stream.write(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(body), true));
      stream.flush();
    }
    return new ContinueResponse(status, await(future));
  }

  /**
   * Send a reset frame for a fresh stream and return immediately. Used to verify
   * that a reset stream does not tear down the connection.
   */
  void reset(String path) throws Exception {
    Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(connection);
    bootstrap.handler(new ChannelInboundHandlerAdapter());
    Http2StreamChannel stream = bootstrap.open().sync().getNow();
    Http2Headers headers = new DefaultHttp2Headers()
            .method("POST")
            .path(path)
            .scheme(secure ? "https" : "http")
            .authority(host + ":" + port);
    stream.write(new DefaultHttp2HeadersFrame(headers, false));
    stream.write(new DefaultHttp2ResetFrame(Http2Error.CANCEL));
    stream.flush();
  }

  boolean isActive() {
    return connection.isActive();
  }

  private static Response await(CompletableFuture<Response> future)
          throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Override
  public void close() {
    connection.close().awaitUninterruptibly();
    group.shutdownGracefully(0, 1, TimeUnit.SECONDS).awaitUninterruptibly();
  }

  record Response(int status, Map<String, String> headers, byte[] body) {

    String bodyAsString() {
      return new String(body, StandardCharsets.UTF_8);
    }

  }

  record ContinueResponse(int firstStatus, Response response) {

  }

  private static final class Http2StreamChannelInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel ch) {
      // handlers are installed per stream via Http2StreamChannelBootstrap
    }

  }

  private static final class ResponseHandler extends ChannelInboundHandlerAdapter {

    private final CompletableFuture<Response> future;

    private final @Nullable CompletableFuture<Integer> continueFuture;

    private final Map<String, String> headers = new LinkedHashMap<>();

    private ByteBuf body = Unpooled.buffer();

    private int status = -1;

    private boolean completed;

    ResponseHandler(CompletableFuture<Response> future) {
      this(future, null);
    }

    ResponseHandler(CompletableFuture<Response> future, @Nullable CompletableFuture<Integer> continueFuture) {
      this.future = future;
      this.continueFuture = continueFuture;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (msg instanceof Http2HeadersFrame frame) {
        int code = Integer.parseInt(frame.headers().status().toString());
        if (code == 100) {
          if (continueFuture != null) {
            continueFuture.complete(code);
          }
          return;
        }
        status = code;
        for (Map.Entry<CharSequence, CharSequence> entry : frame.headers()) {
          headers.put(entry.getKey().toString(), entry.getValue().toString());
        }
        if (continueFuture != null && !continueFuture.isDone()) {
          continueFuture.complete(code);
        }
        if (frame.isEndStream()) {
          complete();
        }
      }
      else if (msg instanceof Http2DataFrame frame) {
        try {
          body.writeBytes(frame.content());
          if (frame.isEndStream()) {
            complete();
          }
        }
        finally {
          frame.release();
        }
      }
      else {
        ctx.fireChannelRead(msg);
      }
    }

    private void complete() {
      if (completed) {
        return;
      }
      completed = true;
      byte[] bytes = new byte[body.readableBytes()];
      body.readBytes(bytes);
      body.release();
      body = Unpooled.EMPTY_BUFFER;
      future.complete(new Response(status, Map.copyOf(headers), bytes));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      if (!future.isDone()) {
        future.completeExceptionally(cause);
      }
      ctx.close();
    }

  }

}
