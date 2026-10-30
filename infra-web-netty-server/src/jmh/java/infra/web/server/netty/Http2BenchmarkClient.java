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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;

/**
 * Minimal cleartext HTTP/2 client for benchmarks. Kept independent from test
 * sources so the JMH source set can compile on its own.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http2BenchmarkClient implements AutoCloseable {

  private static final long TIMEOUT_SECONDS = 10;

  private final EventLoopGroup group = new NioEventLoopGroup(1);

  private final String host = "localhost";

  private final int port;

  private final Channel connection;

  Http2BenchmarkClient(int port) throws InterruptedException {
    this.port = port;
    Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                        .addLast(Http2FrameCodecBuilder.forClient().build())
                        .addLast(new Http2MultiplexHandler(new ChannelInitializer<Channel>() {
                          @Override
                          protected void initChannel(Channel ch) {
                          }
                        }));
              }
            });
    this.connection = bootstrap.connect(host, port).sync().channel();
  }

  int get(String path) throws Exception {
    Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(connection);
    CompletableFuture<Integer> future = new CompletableFuture<>();
    bootstrap.handler(new StatusHandler(future));
    Http2StreamChannel stream = bootstrap.open().sync().getNow();

    Http2Headers headers = new DefaultHttp2Headers()
            .method("GET")
            .path(path)
            .scheme("http")
            .authority(host + ":" + port);
    stream.write(new DefaultHttp2HeadersFrame(headers, true));
    stream.flush();
    return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  @Override
  public void close() {
    connection.close().awaitUninterruptibly();
    group.shutdownGracefully(0, 1, TimeUnit.SECONDS).awaitUninterruptibly();
  }

  private static final class StatusHandler extends ChannelInboundHandlerAdapter {

    private final CompletableFuture<Integer> future;

    private int status;

    private StatusHandler(CompletableFuture<Integer> future) {
      this.future = future;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (msg instanceof Http2HeadersFrame frame) {
        status = Integer.parseInt(frame.headers().status().toString());
        if (frame.isEndStream()) {
          future.complete(status);
        }
      }
      else if (msg instanceof Http2DataFrame frame) {
        try {
          if (frame.isEndStream()) {
            future.complete(status);
          }
        }
        finally {
          frame.release();
        }
      }
      else {
        ctx.fireChannelRead(Unpooled.EMPTY_BUFFER);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      future.completeExceptionally(cause);
      ctx.close();
    }

  }

}
