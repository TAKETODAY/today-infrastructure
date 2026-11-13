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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

/**
 * Minimal HTTP/1.1 client used by the Netty server end-to-end tests. It keeps a
 * single connection open so tests can verify keep-alive reuse and reads chunked
 * responses, including trailing fields, through the regular Netty codec.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http1TestClient implements AutoCloseable {

  private static final long TIMEOUT_SECONDS = 10;

  private final EventLoopGroup group = new NioEventLoopGroup(1);

  private final Channel channel;

  private final BlockingQueue<CompletableFuture<Response>> pending = new LinkedBlockingQueue<>();

  private Http1TestClient(int port) throws InterruptedException {
    this.channel = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast(new HttpClientCodec())
                        .addLast(new ResponseHandler());
              }
            })
            .connect("localhost", port)
            .sync()
            .channel();
  }

  static Http1TestClient connect(int port) throws Exception {
    return new Http1TestClient(port);
  }

  Response get(String path) throws Exception {
    return request(HttpMethod.GET, path);
  }

  Response head(String path) throws Exception {
    return request(HttpMethod.HEAD, path);
  }

  private Response request(HttpMethod method, String path) throws Exception {
    CompletableFuture<Response> future = new CompletableFuture<>();
    pending.add(future);

    DefaultFullHttpRequest request = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, method, path);
    request.headers().set(HttpHeaderNames.HOST, "localhost");
    HttpUtil.setKeepAlive(request, true);

    channel.writeAndFlush(request);
    return await(future);
  }

  boolean isActive() {
    return channel.isActive();
  }

  private static Response await(CompletableFuture<Response> future)
          throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Override
  public void close() {
    channel.close().awaitUninterruptibly();
    group.shutdownGracefully(0, 1, TimeUnit.SECONDS).awaitUninterruptibly();
  }

  record Response(int status, Map<String, String> headers,
          Map<String, String> trailers, byte[] body) {

    String bodyAsString() {
      return new String(body, StandardCharsets.UTF_8);
    }

  }

  private final class ResponseHandler extends ChannelInboundHandlerAdapter {

    private @Nullable ResponseBuilder current;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      try {
        if (msg instanceof HttpResponse response) {
          current = new ResponseBuilder(response.status().code());
          copyHeaders(response, current.headers);
          if (msg instanceof LastHttpContent last) {
            append(last.content(), current);
            complete(last);
          }
        }
        else if (msg instanceof HttpContent content) {
          if (current != null) {
            append(content.content(), current);
            if (msg instanceof LastHttpContent last) {
              complete(last);
            }
          }
        }
        else {
          ctx.fireChannelRead(msg);
        }
      }
      finally {
        if (msg instanceof HttpContent || msg instanceof HttpResponse) {
          ReferenceCountUtil.release(msg);
        }
      }
    }

    private void complete(LastHttpContent last) {
      ResponseBuilder builder = current;
      if (builder == null) {
        return;
      }
      current = null;

      for (Map.Entry<String, String> entry : last.trailingHeaders()) {
        builder.trailers.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
      }

      CompletableFuture<Response> future = pending.poll();
      if (future != null) {
        future.complete(builder.build());
      }
    }

    private static void append(ByteBuf buffer, ResponseBuilder builder) {
      byte[] bytes = new byte[buffer.readableBytes()];
      buffer.getBytes(buffer.readerIndex(), bytes);
      builder.body.writeBytes(bytes);
    }

    private static void copyHeaders(HttpResponse response, Map<String, String> target) {
      for (Map.Entry<String, String> entry : response.headers()) {
        target.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      CompletableFuture<Response> future;
      while ((future = pending.poll()) != null) {
        future.completeExceptionally(new IllegalStateException("Connection closed before the response completed"));
      }
      ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      CompletableFuture<Response> future;
      while ((future = pending.poll()) != null) {
        future.completeExceptionally(cause);
      }
      ctx.close();
    }

  }

  private static final class ResponseBuilder {

    private final int status;

    private final Map<String, String> headers = new LinkedHashMap<>();

    private final Map<String, String> trailers = new LinkedHashMap<>();

    private final ByteArrayOutputStream body = new ByteArrayOutputStream();

    private ResponseBuilder(int status) {
      this.status = status;
    }

    private Response build() {
      return new Response(status, Map.copyOf(headers), Map.copyOf(trailers), body.toByteArray());
    }

  }

}
