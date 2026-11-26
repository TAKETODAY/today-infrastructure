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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import infra.context.ApplicationContext;
import infra.web.DispatcherHandler;
import infra.web.async.DeferredResult;
import infra.web.multipart.MultipartParser;
import infra.web.server.error.SendErrorHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:14
 */
class NettyWebAsyncRequestTests {

  private final EmbeddedChannel channel = new EmbeddedChannel();

  @AfterEach
  void releaseChannel() {
    channel.finishAndReleaseAll();
  }

  @Test
  void positiveTimeoutRunsOnEventLoopAtDeadline() {
    channel.freezeTime();
    var async = new NettyWebAsyncRequest(newRequest());
    var timeouts = new AtomicInteger();
    async.addTimeoutHandler(timeouts::incrementAndGet);
    async.setTimeout(Duration.ofMillis(20));
    async.startAsync();

    channel.advanceTimeBy(19, TimeUnit.MILLISECONDS);
    channel.runScheduledPendingTasks();
    assertThat(timeouts).hasValue(0);
    channel.advanceTimeBy(1, TimeUnit.MILLISECONDS);
    channel.runScheduledPendingTasks();
    assertThat(timeouts).hasValue(1);
  }

  @Test
  void zeroTimeoutDoesNotScheduleAnEvent() {
    var async = new NettyWebAsyncRequest(newRequest());
    async.setTimeout(Duration.ZERO);
    async.startAsync();

    assertThat(async.isAsyncStarted()).isTrue();
    assertThat(channel.runScheduledPendingTasks()).isEqualTo(-1);
  }

  @Test
  void failedDispatchCancelsTimeoutAndCompletesAfterReportingError() throws Exception {
    channel.freezeTime();
    var dispatcher = mock(DispatcherHandler.class);
    var request = newRequest(dispatcher);
    var async = request.asyncRequest();
    var handler = new Object();
    var failure = new IllegalStateException("dispatch failed");
    var error = new AtomicReference<Throwable>();
    var completions = new AtomicInteger();
    willThrow(failure).given(dispatcher).handleConcurrentResult(request, handler, "result");
    async.addErrorHandler(error::set);
    async.addCompletionHandler(() -> {
      assertThat(error.get()).isSameAs(failure);
      completions.incrementAndGet();
    });
    async.setTimeout(Duration.ofSeconds(1));
    var result = new DeferredResult<String>();
    request.asyncManager().startDeferredResultProcessing(result, handler);
    assertThat(channel.runScheduledPendingTasks()).isPositive();

    assertThat(result.setResult("result")).isTrue();

    assertThat(completions).hasValue(1);
    assertThat(async.isAsyncComplete()).isTrue();
    assertThat(channel.runScheduledPendingTasks()).isEqualTo(-1);
    FullHttpResponse response = channel.readOutbound();
    try {
      assertThat(response.status()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
    finally {
      response.release();
    }
  }

  private ServerHttpExchange newRequest() {
    return newRequest(mock(DispatcherHandler.class));
  }

  private ServerHttpExchange newRequest(DispatcherHandler dispatcher) {
    var context = mock(ApplicationContext.class);
    var config = NettyRequestConfig.forBuilder(false)
            .sendErrorHandler(mock(SendErrorHandler.class)).multipartParser(mock(MultipartParser.class)).build();
    var handler = new HttpTrafficHandler(config, context, dispatcher, (request, task) -> task.run());
    return new ServerHttpExchange(channel, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"),
            config, context, dispatcher, handler);
  }

  @Test
  void dispatchIgnoresLateResultsAndTimeouts() throws Exception {
    NettyServerHttpExchange request = mock(NettyServerHttpExchange.class);
    var asyncRequest = new NettyWebAsyncRequest(request);
    var completions = new AtomicInteger();
    var timeouts = new AtomicInteger();
    asyncRequest.addCompletionHandler(completions::incrementAndGet);
    asyncRequest.addTimeoutHandler(timeouts::incrementAndGet);
    asyncRequest.startAsync();

    asyncRequest.dispatch("first result");
    asyncRequest.dispatch("late result");
    asyncRequest.dispatch(new AssertionError("late filter failure"));
    asyncRequest.checkTimeout();

    assertThat(asyncRequest.isAsyncStarted()).isFalse();
    assertThat(asyncRequest.isAsyncComplete()).isTrue();
    assertThat(completions).hasValue(1);
    assertThat(timeouts).hasValue(0);
    assertThatIllegalStateException().isThrownBy(asyncRequest::startAsync);
    verify(request, times(1)).dispatchConcurrentResult("first result");
    verifyNoMoreInteractions(request);
  }

  @Test
  void constructorShouldInitializeFields() {
    NettyServerHttpExchange request = mock(NettyServerHttpExchange.class);
    NettyWebAsyncRequest asyncRequest = new NettyWebAsyncRequest(request);

    assertThat(asyncRequest.isAsyncStarted()).isFalse();
  }

  @Test
  void startAsyncShouldSetAsyncStartedFlag() {
    NettyServerHttpExchange request = mock(NettyServerHttpExchange.class);
    NettyWebAsyncRequest asyncRequest = new NettyWebAsyncRequest(request);

    asyncRequest.startAsync();

    assertThat(asyncRequest.isAsyncStarted()).isTrue();
  }

  @Test
  void isAsyncStartedShouldReturnFalseByDefault() {
    NettyServerHttpExchange request = mock(NettyServerHttpExchange.class);
    NettyWebAsyncRequest asyncRequest = new NettyWebAsyncRequest(request);

    assertThat(asyncRequest.isAsyncStarted()).isFalse();
  }

}
