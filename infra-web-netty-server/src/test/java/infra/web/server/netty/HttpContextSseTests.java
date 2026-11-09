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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
 * Tests for chunked response framing.
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
  void explicitChunkedTransferEncodingUsesChunkedFraming() throws Exception {
    HttpContext context = createContext();
    context.setContentType(MediaType.TEXT_EVENT_STREAM);
    context.setHeader(HttpHeaderNames.TRANSFER_ENCODING.toString(), "chunked");
    context.getOutputStream().write("data: hello\n\n".getBytes(StandardCharsets.UTF_8));

    context.completeRequestInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRANSFER_ENCODING)).isEqualTo("chunked");
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void explicitChunkedTransferEncodingRemovesContentLength() throws Exception {
    HttpContext context = createContext();
    context.setContentType(MediaType.TEXT_EVENT_STREAM);
    context.setHeader(HttpHeaderNames.TRANSFER_ENCODING.toString(), "chunked");
    context.setContentLength(100);
    context.getOutputStream().write("data: hello\n\n".getBytes(StandardCharsets.UTF_8));

    context.completeRequestInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRANSFER_ENCODING)).isEqualTo("chunked");
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void explicitChunkedFramingIsNotLimitedToEventStream() throws Exception {
    HttpContext context = createContext();
    context.setContentType(MediaType.APPLICATION_JSON);
    context.setHeader(HttpHeaderNames.TRANSFER_ENCODING.toString(), "chunked");
    context.getOutputStream().write("{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8));

    context.completeRequestInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRANSFER_ENCODING)).isEqualTo("chunked");
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void eventStreamContentTypeAloneUsesContentLength() throws Exception {
    byte[] body = "data: hello\n\n".getBytes(StandardCharsets.UTF_8);
    HttpContext context = createContext();
    context.setContentType(MediaType.TEXT_EVENT_STREAM);
    context.getOutputStream().write(body);

    context.completeRequestInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)).isFalse();
    assertThat(response.headers().get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(String.valueOf(body.length));
  }

  private HttpContext createContext() {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/events");
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler((request1, message) -> { })
            .build();
    return new HttpContext(channel, request, config, mock(ApplicationContext.class),
            mock(DispatcherHandler.class), mock(HttpTrafficHandler.class));
  }

}
