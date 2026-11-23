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

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.context.ApplicationContext;
import infra.web.DispatcherHandler;
import infra.web.server.HttpRequestExecutor;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpTrafficHandler} error handling.
 */
class HttpTrafficHandlerTests {

  @Test
  void releasesFramesWithoutARequestAndForwardsUnrecognizedMessages() {
    var channel = channelWithTrafficHandler();
    try {
      var content = new DefaultHttpContent(Unpooled.buffer().writeByte(1));
      var data = new DefaultHttp2DataFrame(Unpooled.buffer().writeByte(2));
      var webSocket = new BinaryWebSocketFrame(Unpooled.buffer().writeByte(3));
      assertThat(channel.writeInbound(content, data, webSocket,
              new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().add("checksum", "abc"), true),
              new DefaultHttp2ResetFrame(Http2Error.CANCEL))).isFalse();
      assertThat(content.refCnt()).isZero();
      assertThat(data.refCnt()).isZero();
      assertThat(webSocket.refCnt()).isZero();

      var windowUpdate = new DefaultHttp2WindowUpdateFrame(1);
      var other = new Object();
      assertThat(channel.writeInbound(windowUpdate, other)).isTrue();
      assertThat(channel.<Object>readInbound()).isSameAs(windowUpdate);
      assertThat(channel.<Object>readInbound()).isSameAs(other);
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void trailingHeadersCompleteAnAttachedRequestBody() throws IOException {
    var channel = new EmbeddedStreamChannel(newTrafficHandler((context, command) -> { }));
    try {
      channel.writeInbound(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().method("POST").path("/")));
      var chunk = new DefaultHttp2DataFrame(Unpooled.buffer().writeByte(42));
      channel.writeInbound(chunk, new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().add("checksum", "abc"), true));

      try (var body = NettyHttpContext.lookup(channel).getInputStream()) {
        assertThat(body.read()).isEqualTo(42);
        assertThat(body.read()).isEqualTo(-1);
      }
      assertThat(chunk.refCnt()).isZero();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void channelCloseDetachesHttp1RequestAndDiscardsUnreadBody() {
    var channel = channelWithTrafficHandler();
    try {
      var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
      request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 1);
      var content = new DefaultLastHttpContent(Unpooled.buffer().writeByte(42));
      channel.writeInbound(request, content);
      assertThat(content.refCnt()).isOne();

      channel.close().syncUninterruptibly();

      assertThat(NettyHttpContext.lookup(channel)).isNull();
      assertThat(content.refCnt()).isZero();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void streamResetDetachesTheRequestAndFailsItsBodyReader() throws IOException {
    var channel = new EmbeddedStreamChannel(newTrafficHandler((context, command) -> { }));
    try {
      channel.writeInbound(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().method("POST").path("/")));
      try (var body = NettyHttpContext.lookup(channel).getInputStream()) {
        channel.writeInbound(new DefaultHttp2ResetFrame(Http2Error.CANCEL));

        assertThat(NettyHttpContext.lookup(channel)).isNull();
        assertThatIOException().isThrownBy(body::read).withMessage("HTTP/2 stream reset: 8");
      }
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  private static EmbeddedChannel channelWithTrafficHandler() {
    return new EmbeddedChannel(newTrafficHandler((context, command) -> { }));
  }

  @Test
  void exceptionBeforeHttp2CommitEndsTheStreamWithAnErrorResponse() {
    var channel = new EmbeddedStreamChannel(newTrafficHandler((context, command) -> {
      throw new IllegalStateException("dispatch failed");
    }));
    try {
      channel.writeInbound(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().method("GET").path("/"), true));

      Http2HeadersFrame response = channel.readOutbound();
      assertThat(response.headers().status().toString()).isEqualTo("500");
      assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isZero();
      assertThat(response.isEndStream()).isTrue();
      assertThat(channel.isOpen()).isFalse();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  private static HttpTrafficHandler newTrafficHandler(HttpRequestExecutor executor) {
    var config = NettyRequestConfig.forBuilder(false).multipartParser(mock()).sendErrorHandler(mock()).build();
    return new HttpTrafficHandler(config, mock(), mock(), executor);
  }

  @Test
  void exceptionAfterCommitClosesChannel() {
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler(mock())
            .build();
    HttpRequestExecutor executor = (context, command) -> {
      ((NettyHttpContext) context).writeHeaders();
      throw new IllegalStateException("boom");
    };
    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(
            config, mock(ApplicationContext.class), mock(DispatcherHandler.class), executor);

    EmbeddedChannel channel = new EmbeddedChannel(trafficHandler);
    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

    channel.writeInbound(request);

    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  void exceptionBeforeCommitWritesErrorResponse() {
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler(mock())
            .build();
    HttpRequestExecutor executor = (context, command) -> {
      throw new IllegalStateException("boom");
    };
    HttpTrafficHandler trafficHandler = new HttpTrafficHandler(
            config, mock(ApplicationContext.class), mock(DispatcherHandler.class), executor);

    EmbeddedChannel channel = new EmbeddedChannel(trafficHandler);
    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");

    channel.writeInbound(request);

    assertThat(channel.outboundMessages()).hasSize(1);
  }

  private static final class EmbeddedStreamChannel extends EmbeddedChannel implements Http2StreamChannel {

    private final Http2FrameStream stream = mock();

    EmbeddedStreamChannel(HttpTrafficHandler handler) {
      super(handler);
    }

    @Override
    public Http2FrameStream stream() {
      return stream;
    }

  }

}
