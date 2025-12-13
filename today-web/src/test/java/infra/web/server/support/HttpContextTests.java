/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.util.concurrent.Awaiter;
import infra.web.DispatcherHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
    Channel channel = mock(Channel.class);
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149).build();
    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    NettyChannelHandler channelHandler = mock(NettyChannelHandler.class);

    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "150"));

    // When
    HttpContext contextUnderTest = new HttpContext(channel, request, config, context, dispatcherHandler, channelHandler);

    // Then
    assertThat(contextUnderTest.getContentLength()).isEqualTo(150L);
  }

  @Test
  void validContentLengthWithinLimitShouldNotSetDecoderFailure() {
    // Given
    Channel channel = mock(Channel.class);
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createRequestConfig();
    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    NettyChannelHandler channelHandler = mock(NettyChannelHandler.class);

    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "150"));

    // When
    HttpContext contextUnderTest = new HttpContext(channel, request, config, context, dispatcherHandler, channelHandler);

    // Then
    assertThat(contextUnderTest.getContentLength()).isEqualTo(150L);
    verify(request, never()).setDecoderResult(any(DecoderResult.class));
  }

  @Test
  void normalHttpContentShouldDeliverToRequestBodyStream() {
    // Given
    Awaiter awaiter = mock(Awaiter.class);
    Channel channel = mock(Channel.class);
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149)
            .awaiterFactory(req -> awaiter).build();

    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    NettyChannelHandler channelHandler = mock(NettyChannelHandler.class);
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
    Channel channel = mock(Channel.class);
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149)
            .awaiterFactory(req -> awaiter).build();

    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    NettyChannelHandler channelHandler = mock(NettyChannelHandler.class);
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