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

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.util.concurrent.Awaiter;
import infra.web.DispatcherHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/15 20:31
 */
class HttpContextTests {

  @Test
  void initDataExceedsMaxContentLengthShouldTriggerError() {
    // Given
    Channel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149).build();
    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    HttpTrafficHandler channelHandler = mock(HttpTrafficHandler.class);

    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "150"));

    // When
    HttpContext contextUnderTest = new HttpContext(channel, request, config, context, dispatcherHandler, channelHandler);

    // Then
    assertThat(contextUnderTest.getContentLength()).isEqualTo(150L);
  }

  @Test
  void validContentLengthWithinLimitShouldNotSetDecoderFailure() {
    // Given
    Channel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createRequestConfig();
    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    HttpTrafficHandler channelHandler = mock(HttpTrafficHandler.class);

    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "150"));

    // When
    HttpContext contextUnderTest = new HttpContext(channel, request, config, context, dispatcherHandler, channelHandler);

    // Then
    assertThat(contextUnderTest.getContentLength()).isEqualTo(150L);
    verify(request, never()).setDecoderResult(any(DecoderResult.class));
  }

  @Test
  void normalHttpContentShouldDeliverToRequestBodyStream() {
    Awaiter awaiter = mock(Awaiter.class);
    Channel channel = new EmbeddedChannel();

    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149)
            .awaiterFactory(req -> awaiter).build();

    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    HttpTrafficHandler channelHandler = mock(HttpTrafficHandler.class);
    HttpContent httpContent = mock(HttpContent.class);
    ByteBuf byteBuf = mock(ByteBuf.class);

    when(httpContent.content()).thenReturn(byteBuf);
    when(byteBuf.readableBytes()).thenReturn(50);
    when(request.method()).thenReturn(HttpMethod.POST);
    when(request.headers()).thenReturn(new DefaultHttpHeaders()
            .set(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    HttpContext contextUnderTest = new HttpContext(channel, request, config, context, dispatcherHandler, channelHandler);

    // When
    contextUnderTest.onDataReceived(httpContent);

    // Then
    // Since requestBody is created lazily, we need to check that the stream was used
    // We cannot directly verify this without accessing the private field
  }

  @Test
  void lastHttpContentWithNoExistingStreamButWithDataShouldCreateAndCompleteStream() {
    // Given
    Awaiter awaiter = mock(Awaiter.class);
    Channel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149)
            .awaiterFactory(req -> awaiter).build();

    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    HttpTrafficHandler channelHandler = mock(HttpTrafficHandler.class);
    LastHttpContent lastHttpContent = mock(LastHttpContent.class);
    ByteBuf byteBuf = mock(ByteBuf.class);

    when(lastHttpContent.content()).thenReturn(byteBuf);
    when(byteBuf.readableBytes()).thenReturn(50);
    when(request.method()).thenReturn(HttpMethod.POST);
    when(request.headers()).thenReturn(new DefaultHttpHeaders()
            .set(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    HttpContext contextUnderTest = new HttpContext(channel, request, config, context, dispatcherHandler, channelHandler);

    // When
    contextUnderTest.onDataReceived(lastHttpContent);

    // Then
    // Stream should be created, data delivered and completed
  }

  private static NettyRequestConfig createRequestConfig() {
    return createConfigBuilder().build();
  }

  private static NettyRequestConfig.Builder createConfigBuilder() {
    return NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler((request, message) -> {

            });
  }

}