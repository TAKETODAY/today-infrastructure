/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.context.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.mock.web.MockAsyncContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.async.AsyncRequestTimeoutException;
import cn.taketoday.web.async.CallableProcessingInterceptor;
import cn.taketoday.web.async.DeferredResult;
import cn.taketoday.web.async.DeferredResultProcessingInterceptor;
import cn.taketoday.web.async.WebAsyncManager;
import cn.taketoday.web.async.WebAsyncTask;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.StandardServletAsyncWebRequest;
import cn.taketoday.mock.api.AsyncEvent;

import static cn.taketoday.web.async.CallableProcessingInterceptor.RESULT_NONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * {@link WebAsyncManager} tests where container-triggered timeout/completion
 * events are simulated.
 *
 * @author Rossen Stoyanchev
 */
public class WebAsyncManagerTimeoutTests {

  private static final AsyncEvent ASYNC_EVENT = null;

  private WebAsyncManager asyncManager;

  private StandardServletAsyncWebRequest asyncWebRequest;

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  private ServletRequestContext request;

  @BeforeEach
  public void setup() {
    this.servletRequest = new MockHttpServletRequest("GET", "/test");
    this.servletRequest.setAsyncSupported(true);
    this.servletResponse = new MockHttpServletResponse();
    this.asyncWebRequest = new StandardServletAsyncWebRequest(servletRequest, servletResponse);

    AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
    request = new ServletRequestContext(null, servletRequest, servletResponse);
    this.asyncManager = request.getAsyncManager();
    this.asyncManager.setTaskExecutor(executor);
    this.asyncManager.setAsyncRequest(this.asyncWebRequest);
  }

  @Test
  public void startCallableProcessingTimeoutAndComplete() throws Exception {
    StubCallable callable = new StubCallable();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    given(interceptor.handleTimeout(this.request, callable)).willReturn(RESULT_NONE);

    this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
    this.asyncManager.startCallableProcessing(callable);

    this.asyncWebRequest.onTimeout(ASYNC_EVENT);
    this.asyncWebRequest.onComplete(ASYNC_EVENT);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult().getClass()).isEqualTo(AsyncRequestTimeoutException.class);

    verify(interceptor).beforeConcurrentHandling(this.request, callable);
    verify(interceptor).afterCompletion(this.request, callable);
  }

  @Test
  public void startCallableProcessingTimeoutAndResumeThroughCallback() throws Exception {

    StubCallable callable = new StubCallable();
    WebAsyncTask<Object> webAsyncTask = new WebAsyncTask<>(callable);
    webAsyncTask.onTimeout(() -> 7);

    this.asyncManager.startCallableProcessing(webAsyncTask);

    this.asyncWebRequest.onTimeout(ASYNC_EVENT);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(7);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startCallableProcessingTimeoutAndResumeThroughInterceptor() throws Exception {

    StubCallable callable = new StubCallable();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    given(interceptor.handleTimeout(this.request, callable)).willReturn(22);

    this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
    this.asyncManager.startCallableProcessing(callable);

    this.asyncWebRequest.onTimeout(ASYNC_EVENT);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(22);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

    verify(interceptor).beforeConcurrentHandling(this.request, callable);
  }

  @Test
  public void startCallableProcessingAfterTimeoutException() throws Exception {

    StubCallable callable = new StubCallable();
    Exception exception = new Exception();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    given(interceptor.handleTimeout(this.request, callable)).willThrow(exception);

    this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
    this.asyncManager.startCallableProcessing(callable);

    this.asyncWebRequest.onTimeout(ASYNC_EVENT);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

    verify(interceptor).beforeConcurrentHandling(this.request, callable);
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void startCallableProcessingTimeoutAndCheckThreadInterrupted() throws Exception {

    StubCallable callable = new StubCallable();
    Future future = mock(Future.class);

    AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
    given(executor.submit(any(Runnable.class))).willReturn(future);

    this.asyncManager.setTaskExecutor(executor);
    this.asyncManager.startCallableProcessing(callable);

    this.asyncWebRequest.onTimeout(ASYNC_EVENT);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();

    verify(future).cancel(true);
    verifyNoMoreInteractions(future);
  }

  @Test
  public void startDeferredResultProcessingTimeoutAndComplete() throws Exception {

    DeferredResult<Integer> deferredResult = new DeferredResult<>();

    DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
    given(interceptor.handleTimeout(this.request, deferredResult)).willReturn(true);

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    this.asyncWebRequest.onTimeout(ASYNC_EVENT);
    this.asyncWebRequest.onComplete(ASYNC_EVENT);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult().getClass()).isEqualTo(AsyncRequestTimeoutException.class);

    verify(interceptor).beforeConcurrentHandling(this.request, deferredResult);
    verify(interceptor).preProcess(this.request, deferredResult);
    verify(interceptor).afterCompletion(this.request, deferredResult);
  }

  @Test
  public void startDeferredResultProcessingTimeoutAndResumeWithDefaultResult() throws Exception {

    DeferredResult<Integer> deferredResult = new DeferredResult<>(null, 23);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    AsyncEvent event = null;
    this.asyncWebRequest.onTimeout(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(23);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startDeferredResultProcessingTimeoutAndResumeThroughCallback() throws Exception {

    final DeferredResult<Integer> deferredResult = new DeferredResult<>();
    deferredResult.onTimeout(() -> deferredResult.setResult(23));

    this.asyncManager.startDeferredResultProcessing(deferredResult);

    AsyncEvent event = null;
    this.asyncWebRequest.onTimeout(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(23);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startDeferredResultProcessingTimeoutAndResumeThroughInterceptor() throws Exception {

    DeferredResult<Integer> deferredResult = new DeferredResult<>();

    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
      @Override
      public <T> boolean handleTimeout(RequestContext request, DeferredResult<T> result) throws Exception {
        result.setErrorResult(23);
        return true;
      }
    };

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    AsyncEvent event = null;
    this.asyncWebRequest.onTimeout(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(23);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startDeferredResultProcessingAfterTimeoutException() throws Exception {

    DeferredResult<Integer> deferredResult = new DeferredResult<>();
    final Exception exception = new Exception();

    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
      @Override
      public <T> boolean handleTimeout(RequestContext request, DeferredResult<T> result) throws Exception {
        throw exception;
      }
    };

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    AsyncEvent event = null;
    this.asyncWebRequest.onTimeout(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  private final class StubCallable implements Callable<Object> {
    @Override
    public Object call() throws Exception {
      return 21;
    }
  }

}
