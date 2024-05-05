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
import cn.taketoday.web.RequestContext;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link WebAsyncManager} tests where container-triggered error/completion
 * events are simulated.
 *
 * @author Violeta Georgieva
 * @since 4.0
 */
public class WebAsyncManagerErrorTests {

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

    AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
    request = new ServletRequestContext(null, servletRequest, servletResponse);

    this.asyncWebRequest = (StandardServletAsyncWebRequest) request.getAsyncWebRequest();
    this.asyncManager = request.getAsyncManager();
    this.asyncManager.setTaskExecutor(executor);
    this.asyncManager.setAsyncRequest(this.asyncWebRequest);

  }

  @Test
  public void startCallableProcessingErrorAndComplete() throws Exception {
    StubCallable callable = new StubCallable();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    Exception e = new Exception();
    given(interceptor.handleError(this.request, callable, e)).willReturn(RESULT_NONE);

    this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
    this.asyncManager.startCallableProcessing(callable);

    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);
    this.asyncWebRequest.onComplete(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);

    verify(interceptor).beforeConcurrentHandling(this.request, callable);
    verify(interceptor).afterCompletion(this.request, callable);
  }

  @Test
  public void startCallableProcessingErrorAndResumeThroughCallback() throws Exception {

    StubCallable callable = new StubCallable();
    WebAsyncTask<Object> webAsyncTask = new WebAsyncTask<>(callable);
    webAsyncTask.onError(() -> 7);

    this.asyncManager.startCallableProcessing(webAsyncTask);

    Exception e = new Exception();
    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(7);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startCallableProcessingErrorAndResumeThroughInterceptor() throws Exception {

    StubCallable callable = new StubCallable();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    Exception e = new Exception();
    given(interceptor.handleError(this.request, callable, e)).willReturn(22);

    this.asyncManager.registerCallableInterceptor("errorInterceptor", interceptor);
    this.asyncManager.startCallableProcessing(callable);

    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(22);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

    verify(interceptor).beforeConcurrentHandling(this.request, callable);
  }

  @Test
  public void startCallableProcessingAfterException() throws Exception {

    StubCallable callable = new StubCallable();
    Exception exception = new Exception();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    Exception e = new Exception();
    given(interceptor.handleError(this.request, callable, e)).willThrow(exception);

    this.asyncManager.registerCallableInterceptor("errorInterceptor", interceptor);
    this.asyncManager.startCallableProcessing(callable);

    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

    verify(interceptor).beforeConcurrentHandling(this.request, callable);
  }

  @Test
  public void startDeferredResultProcessingErrorAndComplete() throws Exception {

    DeferredResult<Integer> deferredResult = new DeferredResult<>();

    DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
    Exception e = new Exception();
    given(interceptor.handleError(this.request, deferredResult, e)).willReturn(true);

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);
    this.asyncWebRequest.onComplete(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);

    verify(interceptor).beforeConcurrentHandling(this.request, deferredResult);
    verify(interceptor).preProcess(this.request, deferredResult);
    verify(interceptor).afterCompletion(this.request, deferredResult);
  }

  @Test
  public void startDeferredResultProcessingErrorAndResumeWithDefaultResult() throws Exception {

    Exception e = new Exception();
    DeferredResult<Throwable> deferredResult = new DeferredResult<>(null, e);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startDeferredResultProcessingErrorAndResumeThroughCallback() throws Exception {

    final DeferredResult<Throwable> deferredResult = new DeferredResult<>();
    deferredResult.onError(t -> deferredResult.setResult(t));

    this.asyncManager.startDeferredResultProcessing(deferredResult);

    Exception e = new Exception();
    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startDeferredResultProcessingErrorAndResumeThroughInterceptor() throws Exception {

    DeferredResult<Integer> deferredResult = new DeferredResult<>();

    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
      @Override
      public <T> boolean handleError(RequestContext request, DeferredResult<T> result, Throwable t)
              throws Exception {
        result.setErrorResult(t);
        return true;
      }
    };

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    Exception e = new Exception();
    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  @Test
  public void startDeferredResultProcessingAfterException() throws Exception {

    DeferredResult<Integer> deferredResult = new DeferredResult<>();
    final Exception exception = new Exception();

    DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
      @Override
      public <T> boolean handleError(
              RequestContext request, DeferredResult<T> deferredResult, Throwable t) throws Exception {
        throw exception;
      }
    };

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    Exception e = new Exception();
    AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
    this.asyncWebRequest.onError(event);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
    assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  private final class StubCallable implements Callable<Object> {
    @Override
    public Object call() throws Exception {
      return 21;
    }
  }

}
