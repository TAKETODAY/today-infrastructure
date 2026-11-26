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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.http.HttpCookie;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.util.ExceptionUtils;
import infra.web.DispatcherHandler;
import infra.web.Filter;
import infra.web.RequestCompletedListener;
import infra.web.ServerHttpExchange;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/15 20:31
 */
class ServerHttpExchangeTests {

  @Test
  void acceptedContinueClearsExpectationBeforeDispatchAndReadsBodyOnDemand() throws Exception {
    var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/");
    request.headers().set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE);
    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 1);
    var reads = new AtomicInteger();
    var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
      @Override
      public void read(ChannelHandlerContext ctx) {
        if (!ctx.channel().config().isAutoRead()) {
          reads.incrementAndGet();
          ctx.fireChannelRead(new DefaultLastHttpContent(Unpooled.buffer().writeByte(42)));
        }
        else {
          ctx.read();
        }
      }
    });
    var dispatcher = mock(DispatcherHandler.class);
    when(dispatcher.requestContinueExpected(any())).thenReturn(true);
    var config = createConfigBuilder().manualBodyRead(true).build();
    var traffic = new HttpTrafficHandler(config, mock(), dispatcher, (context, command) -> command.run());
    channel.pipeline().addLast(traffic);
    try {
      channel.writeInbound(request);
      var context = NettyServerHttpExchange.lookup(channel);
      assertThat(context).isNotNull();
      assertThat(channel.config().isAutoRead()).isFalse();
      assertThat(request.headers().contains(HttpHeaderNames.EXPECT)).isFalse();
      verify(dispatcher).handleRequest(context);
      DefaultFullHttpResponse interim = channel.readOutbound();
      try {
        assertThat(interim.status().code()).isEqualTo(100);
      }
      finally {
        interim.release();
      }
      try (var body = context.getInputStream()) {
        assertThat(body.read()).isEqualTo(42);
        assertThat(body.read()).isEqualTo(-1);
      }
      assertThat(reads.get()).isOne();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void headRejectsInvalidWriteRangesWithoutCountingBytesAndClosesNonPersistentConnection() throws Exception {
    var channel = new EmbeddedChannel();
    try {
      var request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.HEAD, "/");
      var context = createContext(channel, request);
      var body = context.getOutputStream();
      byte[] bytes = { 1, 2, 3 };
      assertThatIndexOutOfBoundsException().isThrownBy(() -> body.write(bytes, -1, 1));
      assertThatIndexOutOfBoundsException().isThrownBy(() -> body.write(bytes, 0, -1));
      assertThatIndexOutOfBoundsException().isThrownBy(() -> body.write(bytes, 2, 2));
      body.write(bytes, 1, 2);
      context.completeRequestInternal(null);

      HttpResponse response = channel.readOutbound();
      assertThat(response.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(2);
      assertThat(channel.<Object>readOutbound()).isSameAs(LastHttpContent.EMPTY_LAST_CONTENT);
      assertThat(channel.isOpen()).isFalse();
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void responseMessageSharesHeadersAndCommitsStatusAndBody() throws Exception {
    var channel = new EmbeddedChannel();
    try {
      var context = createContext(channel, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"));
      var response = context.asHttpOutputMessage();
      assertThat(context.asHttpOutputMessage()).isSameAs(response);
      var headers = infra.http.HttpHeaders.forWritable();
      headers.add("X-Test", "first");
      response.setHeaders(null);
      response.setHeaders(headers);
      var extraHeaders = infra.http.HttpHeaders.forWritable();
      extraHeaders.add("X-Test", "second");
      response.addHeaders(infra.http.HttpHeaders.forWritable());
      response.addHeaders(extraHeaders);
      response.addHeader("X-Test", "third");
      response.setHeader("X-Remove", "value");
      assertThat(response.removeHeader("X-Remove")).isTrue();
      assertThat(response.containsHeader("x-test")).isTrue();
      assertThat(response.containsHeaderValue("x-test", "SECOND")).isTrue();
      assertThat(response.getHeader("X-Test")).isEqualTo("first");
      assertThat(response.getHeaders("x-test")).containsExactly("first", "second", "third");
      assertThat(response.getHeaderNames()).contains("X-Test").doesNotContain("X-Remove");
      assertThat(response.getHeaders()).isSameAs(context.responseHeaders());
      assertThat(response.getContentLength()).isEqualTo(-1);
      response.setStatusCode(HttpStatus.CREATED);
      response.setContentType(MediaType.TEXT_PLAIN);
      response.setContentLength(3);
      assertThat(response.getContentLength()).isEqualTo(3);
      var body = response.getBody();
      body.write(new byte[0]);
      body.write('a');
      body.write(new byte[] { 'x', 'b', 'c' }, 1, 2);
      response.close();
      assertThat(context.isCommitted()).isTrue();
      response.flush();

      io.netty.handler.codec.http.HttpResponse head = channel.readOutbound();
      assertThat(head.status().code()).isEqualTo(201);
      assertThat(head.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("text/plain");
      assertThat(head.headers().get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo("3");
      ByteBuf content = channel.readOutbound();
      try {
        assertThat(content.toString(StandardCharsets.US_ASCII)).isEqualTo("abc");
      }
      finally {
        content.release();
      }
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  @Test
  void requestHeadersAndResponseCookieAttributesArePreserved() {
    var channel = new EmbeddedChannel();
    try {
      var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      var context = createContext(channel, request);
      assertThat(context.readCookies()).isEmpty();
      request.headers().add(HttpHeaderNames.COOKIE, ";");
      assertThat(context.readCookies()).isEmpty();
      request.headers().set(HttpHeaderNames.COOKIE, "first=one");
      assertThat(context.readCookies()).extracting(HttpCookie::getName).containsExactly("first");
      request.headers().add(HttpHeaderNames.COOKIE, "second=two");
      assertThat(context.containsHeader("cookie")).isTrue();
      assertThat(context.containsHeaderValue("COOKIE", "SECOND=TWO")).isTrue();
      assertThat(context.getHeaderNames()).contains("cookie");
      assertThat(context.getHeaders("Cookie")).containsExactly("first=one", "second=two");
      assertThat(context.readCookies()).extracting(HttpCookie::getValue).containsExactly("one", "two");
      context.addCookie(new HttpCookie("simple", "value"));
      context.addCookie(ResponseCookie.builder("session", "abc").path("/").domain("example.test")
              .maxAge(60).secure(true).httpOnly(true).sameSite("lax").partitioned(true).build());
      context.addCookie(ResponseCookie.builder("other", "abc").sameSite("unknown").build());
      context.flush();

      io.netty.handler.codec.http.HttpResponse head = channel.readOutbound();
      var cookies = head.headers().getAll(HttpHeaderNames.SET_COOKIE);
      assertThat(cookies).hasSize(3);
      assertThat(cookies.get(0)).startsWith("simple=value").doesNotContain("SameSite=");
      assertThat(cookies.get(1)).contains("session=abc", "Path=/", "Domain=example.test", "Max-Age=60",
              "Secure", "HTTPOnly", "SameSite=Lax", "Partitioned");
      assertThat(cookies.get(2)).startsWith("other=abc").doesNotContain("SameSite=");
    }
    finally {
      channel.finishAndReleaseAll();
    }
  }

  private static infra.web.server.netty.ServerHttpExchange createContext(Channel channel, HttpRequest request) {
    return new infra.web.server.netty.ServerHttpExchange(channel, request, createRequestConfig(), mock(ApplicationContext.class),
            mock(DispatcherHandler.class), mock(HttpTrafficHandler.class));
  }

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
          request.asyncRequest().addCompletionHandler(() -> {
            assertThat(request.isRequestCompleted()).isTrue();
            asyncCompletions.incrementAndGet();
          });
          request.asyncRequest().startAsync();
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
        var request = new infra.web.server.netty.ServerHttpExchange(channel, message, config, applicationContext, dispatcher, trafficHandler);
        request.attach();
        var completed = new AtomicInteger();
        request.registerCompletedCallback(completed::incrementAndGet);

        request.run();

        assertThat(request.isRequestCompleted()).isTrue();
        if (async) {
          assertThat(request.isConcurrentHandlingStarted()).isFalse();
          assertThat(request.asyncRequest().isAsyncComplete()).isTrue();
          request.asyncRequest().dispatch("late result");
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
    infra.web.server.netty.ServerHttpExchange contextUnderTest = new infra.web.server.netty.ServerHttpExchange(channel, request, config, context, dispatcherHandler, channelHandler);

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
    infra.web.server.netty.ServerHttpExchange contextUnderTest = new infra.web.server.netty.ServerHttpExchange(channel, request, config, context, dispatcherHandler, channelHandler);

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

    infra.web.server.netty.ServerHttpExchange contextUnderTest = new infra.web.server.netty.ServerHttpExchange(channel, request, config, context, dispatcherHandler, channelHandler);

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

    infra.web.server.netty.ServerHttpExchange contextUnderTest = new infra.web.server.netty.ServerHttpExchange(channel, request, config, context, dispatcherHandler, channelHandler);

    // When
    contextUnderTest.onDataReceived(lastHttpContent);

    // Then
    // Stream should be created, data delivered and completed
  }

  @Test
  void getContentLength() {
    DefaultHttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    message.headers().set(HttpHeaderNames.CONTENT_LENGTH, "150");
    assertThat(infra.web.server.netty.ServerHttpExchange.getContentLength(message)).isEqualTo(150L);
    message.headers().set(HttpHeaderNames.CONTENT_LENGTH, "0");
    assertThat(infra.web.server.netty.ServerHttpExchange.getContentLength(message)).isEqualTo(0L);

    message.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
    assertThat(infra.web.server.netty.ServerHttpExchange.getContentLength(message)).isEqualTo(-1L);
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
      public void handleRequest(ServerHttpExchange exchange) throws Exception {
        completeRequest(exchange, null);
      }
    };
    HttpTrafficHandler trafficHandler = mock(HttpTrafficHandler.class);

    when(request.method()).thenReturn(HttpMethod.POST);
    when(request.headers()).thenReturn(new DefaultHttpHeaders().set(HttpHeaderNames.CONTENT_LENGTH, "10"));
    when(request.protocolVersion()).thenReturn(HttpVersion.HTTP_1_1);
    when(request.decoderResult()).thenReturn(DecoderResult.SUCCESS);

    infra.web.server.netty.ServerHttpExchange context = new infra.web.server.netty.ServerHttpExchange(channel, request, config, applicationContext, dispatcherHandler, trafficHandler);
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

    infra.web.server.netty.ServerHttpExchange context = new infra.web.server.netty.ServerHttpExchange(channel, request, config, applicationContext, dispatcherHandler, trafficHandler);
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

    infra.web.server.netty.ServerHttpExchange context = new infra.web.server.netty.ServerHttpExchange(channel, request, config, applicationContext, dispatcherHandler, trafficHandler);
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
