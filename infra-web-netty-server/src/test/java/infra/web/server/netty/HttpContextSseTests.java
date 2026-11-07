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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.web.DispatcherHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for SSE response framing.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class HttpContextSseTests {

  private final EmbeddedChannel channel = new EmbeddedChannel();

  @AfterEach
  void tearDown() {
    channel.finishAndReleaseAll();
  }

  @Test
  void contentTypeWithCharsetUsesChunkedFraming() throws Exception {
    HttpContext context = createContext();
    context.setContentType("text/event-stream;charset=UTF-8");
    context.getOutputStream().write("data: hello\n\n".getBytes(StandardCharsets.UTF_8));

    context.requestCompletedInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRANSFER_ENCODING)).isEqualTo("chunked");
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void contentLengthIsRemovedForEventStream() throws Exception {
    HttpContext context = createContext();
    context.setContentType(MediaType.TEXT_EVENT_STREAM);
    context.setContentLength(100);
    context.getOutputStream().write("data: hello\n\n".getBytes(StandardCharsets.UTF_8));

    context.requestCompletedInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRANSFER_ENCODING)).isEqualTo("chunked");
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void lookalikeContentTypeIsNotEventStream() {
    HttpContext context = createContext();
    context.setContentType("text/event-streaming");

    context.requestCompletedInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)).isFalse();
    assertThat(response.headers().get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("0");
  }

  @Test
  void contentTypeWithWhitespaceBeforeParametersUsesChunkedFraming() throws Exception {
    HttpContext context = createContext();
    context.setContentType("text/event-stream ; charset=UTF-8");
    context.getOutputStream().write("data: hello\n\n".getBytes(StandardCharsets.UTF_8));

    context.requestCompletedInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRANSFER_ENCODING)).isEqualTo("chunked");
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void http10EventStreamDoesNotUseChunkedFraming() throws Exception {
    HttpContext context = createContext(HttpVersion.HTTP_1_0, HttpMethod.GET);
    context.setContentType("text/event-stream");
    context.getOutputStream().write("data: hello\n\n".getBytes(StandardCharsets.UTF_8));

    context.requestCompletedInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)).isFalse();
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
          "text/event-stream",
          "TEXT/EVENT-STREAM",
          "Text/Event-Stream",
          "text/event-stream;charset=UTF-8",
          "text/event-stream; charset=UTF-8",
          "text/event-stream ; charset=UTF-8",
          "text/event-stream\t;charset=UTF-8",
          "text/event-stream;"
  })
  void isEventStreamAcceptsEventStreamMediaType(String contentType) {
    assertThat(NettyHttpContext.isEventStream(contentType)).isTrue();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {
          "",
          "text/event-streaming",
          "text/event-stream-foo",
          "text/event-streamx",
          "text/event",
          "text/plain",
          "application/json",
          " text/event-stream",
          "text/event-stream,text/plain"
  })
  void isEventStreamRejectsOtherValues(@Nullable String contentType) {
    assertThat(NettyHttpContext.isEventStream(contentType)).isFalse();
  }

  private HttpContext createContext() {
    return createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
  }

  private HttpContext createContext(HttpVersion version, HttpMethod method) {
    var request = new DefaultHttpRequest(version, method, "/events");
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler((request1, message) -> { })
            .build();
    return new HttpContext(channel, request, config, mock(ApplicationContext.class),
            mock(DispatcherHandler.class), mock(HttpTrafficHandler.class));
  }

}
