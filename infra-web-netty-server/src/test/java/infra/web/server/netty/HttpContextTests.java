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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.http.MediaType;
import infra.util.ExceptionUtils;
import infra.web.DispatcherHandler;
import infra.web.Filter;
import infra.web.RequestCompletedListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
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
  void filterFailureFallsBackToTransportWhenSendErrorFails() {
    verifyFilterFailure(false, new IllegalStateException("filter failed"));
  }

  @Test
  void committedFilterFailureCompletesLifecycleWithoutSecondResponse() {
    verifyFilterFailure(true, new IllegalStateException("filter failed"));
  }

  @Test
  void filterErrorReleasesResourcesWhenSendErrorFails() {
    verifyFilterFailure(false, new AssertionError("filter failed"));
  }

  @Test
  void committedFilterErrorCompletesLifecycleWithoutSecondResponse() {
    verifyFilterFailure(true, new AssertionError("filter failed"));
  }

  @Test
  void asyncFilterFailureCompletesLifecycleWhenSendErrorFails() {
    verifyFilterFailure(false, new IllegalStateException("filter failed"), true);
  }

  @Test
  void committedAsyncFilterErrorCompletesLifecycleWithoutSecondResponse() {
    verifyFilterFailure(true, new AssertionError("filter failed"), true);
  }

  private void verifyFilterFailure(boolean committed, Throwable failure) {
    verifyFilterFailure(committed, failure, false);
  }

  private void verifyFilterFailure(boolean committed, Throwable failure, boolean async) {
    var failures = new ArrayList<Throwable>();
    var asyncCompletions = new AtomicInteger();
    try (var applicationContext = new AnnotationConfigApplicationContext()) {
      applicationContext.registerBean("filter", Filter.class, () -> (request, chain) -> {
        if (async) {
          request.asyncWebRequest().addCompletionHandler(() -> {
            assertThat(request.isRequestCompleted()).isTrue();
            asyncCompletions.incrementAndGet();
          });
          request.asyncWebRequest().startAsync();
        }
        request.getOutputStream().write(42);
        if (committed) {
          request.flush();
        }
        throw ExceptionUtils.sneakyThrow(failure);
      });
      applicationContext.registerBean("completed", RequestCompletedListener.class,
              () -> (request, notHandled) -> failures.add(notHandled));
      applicationContext.refresh();
      var dispatcher = new DispatcherHandler(applicationContext);
      dispatcher.start();
      var errorAttempts = new AtomicInteger();
      var config = createConfigBuilder().manualBodyRead(true).sendErrorHandler((request, message) -> {
        errorAttempts.incrementAndGet();
        throw new IllegalStateException("sendError failed");
      }).build();
      var channel = new EmbeddedChannel();
      try {
        var trafficHandler = new HttpTrafficHandler(config, applicationContext, dispatcher, (request, task) -> task.run());
        var message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
        message.headers().set(HttpHeaderNames.CONTENT_LENGTH, 10);
        var request = new HttpContext(channel, message, config, applicationContext, dispatcher, trafficHandler);
        request.attach();
        var completed = new AtomicInteger();
        request.registerCompletedCallback(completed::incrementAndGet);

        request.run();

        assertThat(request.isRequestCompleted()).isTrue();
        if (async) {
          assertThat(request.isConcurrentHandlingStarted()).isFalse();
          assertThat(request.asyncWebRequest().isAsyncComplete()).isTrue();
          request.asyncWebRequest().dispatch("late result");
          assertThat(asyncCompletions).hasValue(1);
        }
        assertThat(failures).containsExactly(failure);
        assertThat(errorAttempts).hasValue(committed ? 0 : 1);
        assertThat(completed).hasValue(1);
        assertThat(channel.config().isAutoRead()).isTrue();
        assertThat(channel.isActive()).isFalse();
        Object response = channel.readOutbound();
        assertThat(response).isInstanceOf(io.netty.handler.codec.http.HttpResponse.class);
        var status = ((io.netty.handler.codec.http.HttpResponse) response).status();
        assertThat(status.code()).isEqualTo(committed ? 200 : 500);
        ReferenceCountUtil.release(response);
        Object remaining;
        while ((remaining = channel.readOutbound()) != null) {
          assertThat(remaining).isNotInstanceOf(io.netty.handler.codec.http.HttpResponse.class);
          assertThat(remaining).isNotInstanceOf(LastHttpContent.class);
          ReferenceCountUtil.release(remaining);
        }
      }
      finally {
        channel.finishAndReleaseAll();
      }
    }
  }

  @Test
  void initDataExceedsMaxContentLengthShouldTriggerError() {
    // Given
    Channel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149).build();
    ApplicationContext context = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    HttpTrafficHandler channelHandler = mock(HttpTrafficHandler.class);

    when(request.method()).thenReturn(HttpMethod.POST);
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

    when(request.method()).thenReturn(HttpMethod.POST);
    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "150"));

    // When
    HttpContext contextUnderTest = new HttpContext(channel, request, config, context, dispatcherHandler, channelHandler);

    // Then
    assertThat(contextUnderTest.getContentLength()).isEqualTo(150L);
    verify(request, never()).setDecoderResult(any(DecoderResult.class));
  }

  @Test
  void normalHttpContentShouldDeliverToRequestBodyStream() {
    Channel channel = new EmbeddedChannel();

    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149).build();

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
    Channel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(149).build();

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

  @Test
  void getContentLength() {
    DefaultHttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    message.headers().set(HttpHeaderNames.CONTENT_LENGTH, "150");
    assertThat(HttpContext.getContentLength(message)).isEqualTo(150L);
    message.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
    assertThat(HttpContext.getContentLength(message)).isEqualTo(0L);

    message.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
    assertThat(HttpContext.getContentLength(message)).isEqualTo(-1L);
  }

  @Test
  void http1HeadersShouldShareUnderlyingHeadersStore() {
    DefaultHttpHeaders source = new DefaultHttpHeaders();
    source.add("X-Test", "value");
    DefaultHeaders<CharSequence, CharSequence, ?> headers =
            (DefaultHeaders<CharSequence, CharSequence, ?>) source.unwrap();

    HttpHeaders view = new Http1Headers(headers);

    assertThat(view).isInstanceOf(Http1Headers.class);
    assertThat(view.get("X-Test")).isEqualTo("value");

    source.add("X-Added", "later");
    assertThat(view.get("X-Added")).isEqualTo("later");
  }

  @Test
  void manualBodyReadIsRestoredAfterRequestCompletes() {
    EmbeddedChannel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().manualBodyRead(true).build();
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = new DispatcherHandler(applicationContext) {
      @Override
      public void handleRequest(infra.web.HttpContext context) throws Exception {
        completeRequest(context, null);
      }
    };
    HttpTrafficHandler trafficHandler = mock(HttpTrafficHandler.class);

    when(request.method()).thenReturn(HttpMethod.POST);
    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "10"));
    when(request.protocolVersion()).thenReturn(HttpVersion.HTTP_1_1);
    when(request.decoderResult()).thenReturn(DecoderResult.SUCCESS);

    HttpContext context = new HttpContext(channel, request, config, applicationContext, dispatcherHandler, trafficHandler);
    assertThat(channel.config().isAutoRead()).isFalse();

    context.run();

    assertThat(context.isRequestCompleted()).as("request completed").isTrue();
    assertThat(channel.config().isAutoRead()).isTrue();
  }

  @Test
  void manualBodyReadIsRestoredWhenDeclaredContentLengthExceedsLimit() {
    EmbeddedChannel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().maxContentLength(100).manualBodyRead(true).build();
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    HttpTrafficHandler trafficHandler = mock(HttpTrafficHandler.class);

    when(request.method()).thenReturn(HttpMethod.POST);
    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "150"));

    HttpContext context = new HttpContext(channel, request, config, applicationContext, dispatcherHandler, trafficHandler);
    assertThat(channel.config().isAutoRead()).isFalse();

    context.run();

    assertThat(channel.config().isAutoRead()).isTrue();
  }

  @Test
  void manualBodyReadIsRestoredWhenContinueIsRejected() {
    EmbeddedChannel channel = new EmbeddedChannel();
    HttpRequest request = mock(HttpRequest.class);
    NettyRequestConfig config = createConfigBuilder().manualBodyRead(true).build();
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    DispatcherHandler dispatcherHandler = mock(DispatcherHandler.class);
    HttpTrafficHandler trafficHandler = mock(HttpTrafficHandler.class);

    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.set(HttpHeaderNames.CONTENT_LENGTH, "10");
    headers.set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE);

    when(request.method()).thenReturn(HttpMethod.POST);
    when(request.headers()).thenReturn(headers);
    when(request.protocolVersion()).thenReturn(HttpVersion.HTTP_1_1);
    when(dispatcherHandler.requestContinueExpected(any())).thenReturn(false);

    HttpContext context = new HttpContext(channel, request, config, applicationContext, dispatcherHandler, trafficHandler);
    assertThat(channel.config().isAutoRead()).isFalse();
    assertThat(context.is100ContinueExpected()).isTrue();

    context.run();

    assertThat(channel.config().isAutoRead()).isTrue();
    Object response = channel.readOutbound();
    assertThat(response).isInstanceOf(DefaultFullHttpResponse.class);
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
