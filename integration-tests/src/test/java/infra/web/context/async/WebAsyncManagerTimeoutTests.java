/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.context.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import infra.core.task.AsyncTaskExecutor;
import infra.mock.api.AsyncEvent;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockAsyncContext;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.async.AsyncRequestTimeoutException;
import infra.web.async.CallableProcessingInterceptor;
import infra.web.async.DeferredResult;
import infra.web.async.DeferredResultProcessingInterceptor;
import infra.web.async.WebAsyncManager;
import infra.web.async.WebAsyncTask;
import infra.web.mock.MockRequestContext;
import infra.web.mock.StandardMockAsyncWebRequest;

import static infra.web.async.CallableProcessingInterceptor.RESULT_NONE;
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

  private StandardMockAsyncWebRequest asyncWebRequest;

  private HttpMockRequestImpl mockRequest;

  private MockHttpResponseImpl mockResponse;

  private MockRequestContext request;

  @BeforeEach
  public void setup() {
    this.mockRequest = new HttpMockRequestImpl("GET", "/test");
    this.mockRequest.setAsyncSupported(true);
    this.mockResponse = new MockHttpResponseImpl();
    this.asyncWebRequest = new StandardMockAsyncWebRequest(mockRequest, mockResponse);

    AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
    request = new MockRequestContext(null, mockRequest, mockResponse);
    this.asyncManager = request.asyncManager();
    this.asyncManager.setTaskExecutor(executor);
    this.request.setAsyncRequest(this.asyncWebRequest);
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
    assertThat(((MockAsyncContext) this.mockRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
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
    assertThat(((MockAsyncContext) this.mockRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

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
    assertThat(((MockAsyncContext) this.mockRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

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
    assertThat(((MockAsyncContext) this.mockRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
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
    assertThat(((MockAsyncContext) this.mockRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
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
    assertThat(((MockAsyncContext) this.mockRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
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
    assertThat(((MockAsyncContext) this.mockRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
  }

  private final class StubCallable implements Callable<Object> {
    @Override
    public Object call() throws Exception {
      return 21;
    }
  }

}
