/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.context.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.ReflectionTestUtils;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 19:02
 */
class WebAsyncManagerTests {

  private WebAsyncManager asyncManager;

  private AsyncWebRequest asyncWebRequest;

  private MockHttpServletRequest servletRequest = new MockHttpServletRequest();

  private ServletRequestContext request = new ServletRequestContext(null, servletRequest, null);

  @BeforeEach
  public void setup() {
    this.asyncManager = request.getAsyncManager();
    this.asyncManager.setTaskExecutor(new SyncTaskExecutor());
    this.asyncWebRequest = mock(AsyncWebRequest.class);
    this.asyncManager.setAsyncRequest(this.asyncWebRequest);

    BeanWrapper.forDirectFieldAccess(request)
            .setPropertyValue("asyncWebRequest", asyncWebRequest);

    reset(this.asyncWebRequest);
  }

  @Test
  public void startAsyncProcessingWithoutAsyncWebRequest() throws Exception {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    ServletRequestContext request = new ServletRequestContext(null, servletRequest, null);
    WebAsyncManager manager = request.getAsyncManager();

    ReflectionTestUtils.setField(manager, "asyncRequest", null);

    assertThatIllegalStateException()
            .isThrownBy(() -> manager.startCallableProcessing(new StubCallable(1)))
            .withMessage("AsyncWebRequest is required");

    assertThatIllegalStateException()
            .isThrownBy(() -> manager.startDeferredResultProcessing(new DeferredResult<String>()))
            .withMessage("AsyncWebRequest is required");
  }

  @Test
  public void isConcurrentHandlingStarted() {
    given(this.asyncWebRequest.isAsyncStarted()).willReturn(false);

    assertThat(this.request.isConcurrentHandlingStarted()).isFalse();

    reset(this.asyncWebRequest);
    given(this.asyncWebRequest.isAsyncStarted()).willReturn(true);
    request.getAsyncWebRequest();
    assertThat(this.request.isConcurrentHandlingStarted()).isTrue();
  }

  @Test
  public void setAsyncRequestAfterAsyncStarted() {
    this.asyncWebRequest.startAsync();
    assertThatIllegalArgumentException().isThrownBy(() -> this.asyncManager.setAsyncRequest(null));
  }

  @Test
  public void startCallableProcessing() throws Exception {
    int concurrentResult = 21;
    Callable<Object> task = new StubCallable(concurrentResult);

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);

    setupDefaultAsyncScenario();

    this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
    this.asyncManager.startCallableProcessing(task);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(concurrentResult);

    verifyDefaultAsyncScenario();
    verify(interceptor).beforeConcurrentHandling(this.request, task);
    verify(interceptor).preProcess(this.request, task);
    verify(interceptor).postProcess(this.request, task, concurrentResult);
  }

  @Test
  public void startCallableProcessingCallableException() throws Exception {
    Exception concurrentResult = new Exception();
    Callable<Object> task = new StubCallable(concurrentResult);

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);

    setupDefaultAsyncScenario();

    this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
    this.asyncManager.startCallableProcessing(task);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(concurrentResult);

    verifyDefaultAsyncScenario();
    verify(interceptor).beforeConcurrentHandling(this.request, task);
    verify(interceptor).preProcess(this.request, task);
    verify(interceptor).postProcess(this.request, task, concurrentResult);
  }

  @Test
  public void startCallableProcessingBeforeConcurrentHandlingException() throws Exception {
    Callable<Object> task = new StubCallable(21);
    Exception exception = new Exception();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    willThrow(exception).given(interceptor).beforeConcurrentHandling(this.request, task);

    this.asyncManager.registerCallableInterceptor("interceptor", interceptor);

    assertThatException()
            .isThrownBy(() -> this.asyncManager.startCallableProcessing(task))
            .isEqualTo(exception);

    assertThat(this.asyncManager.hasConcurrentResult()).isFalse();

    verify(this.asyncWebRequest).addTimeoutHandler(notNull());
    verify(this.asyncWebRequest).addErrorHandler(notNull());
    verify(this.asyncWebRequest).addCompletionHandler(notNull());
  }

  @Test
  public void startCallableProcessingPreProcessException() throws Exception {
    Callable<Object> task = new StubCallable(21);
    Exception exception = new Exception();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    willThrow(exception).given(interceptor).preProcess(this.request, task);

    setupDefaultAsyncScenario();

    this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
    this.asyncManager.startCallableProcessing(task);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);

    verifyDefaultAsyncScenario();
    verify(interceptor).beforeConcurrentHandling(this.request, task);
  }

  @Test
  public void startCallableProcessingPostProcessException() throws Exception {
    Callable<Object> task = new StubCallable(21);
    Exception exception = new Exception();

    CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
    willThrow(exception).given(interceptor).postProcess(this.request, task, 21);

    setupDefaultAsyncScenario();

    this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
    this.asyncManager.startCallableProcessing(task);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);

    verifyDefaultAsyncScenario();
    verify(interceptor).beforeConcurrentHandling(this.request, task);
    verify(interceptor).preProcess(this.request, task);
  }

  @Test
  public void startCallableProcessingPostProcessContinueAfterException() throws Exception {
    Callable<Object> task = new StubCallable(21);
    Exception exception = new Exception();

    CallableProcessingInterceptor interceptor1 = mock(CallableProcessingInterceptor.class);
    CallableProcessingInterceptor interceptor2 = mock(CallableProcessingInterceptor.class);
    willThrow(exception).given(interceptor2).postProcess(this.request, task, 21);

    setupDefaultAsyncScenario();

    this.asyncManager.registerCallableInterceptors(interceptor1, interceptor2);
    this.asyncManager.startCallableProcessing(task);

    assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);

    verifyDefaultAsyncScenario();
    verify(interceptor1).beforeConcurrentHandling(this.request, task);
    verify(interceptor1).preProcess(this.request, task);
    verify(interceptor1).postProcess(this.request, task, 21);
    verify(interceptor2).beforeConcurrentHandling(this.request, task);
    verify(interceptor2).preProcess(this.request, task);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void startCallableProcessingWithAsyncTask() throws Exception {
    AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
//    given(this.request.unwrapRequest(HttpServletRequest.class)).willReturn(this.servletRequest);

    WebAsyncTask<Object> asyncTask = new WebAsyncTask<>(1000L, executor, mock(Callable.class));
    this.asyncManager.startCallableProcessing(asyncTask);

    verify(executor).submit((Runnable) notNull());
    verify(this.asyncWebRequest).setTimeout(1000L);
    verify(this.asyncWebRequest).addTimeoutHandler(any(Runnable.class));
    verify(this.asyncWebRequest).addErrorHandler(any(Consumer.class));
    verify(this.asyncWebRequest).addCompletionHandler(any(Runnable.class));
    verify(this.asyncWebRequest).startAsync();
  }

  @Test
  public void startCallableProcessingNullInput() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.asyncManager.startCallableProcessing((Callable<?>) null))
            .withMessage("Callable is required");
  }

  @Test
  public void startDeferredResultProcessing() throws Exception {
    DeferredResult<String> deferredResult = new DeferredResult<>(1000L);
    String concurrentResult = "abc";

    DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);

    setupDefaultAsyncScenario();

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    deferredResult.setResult(concurrentResult);

    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(concurrentResult);
    verifyDefaultAsyncScenario();
    verify(interceptor).beforeConcurrentHandling(this.request, deferredResult);
    verify(interceptor).preProcess(this.request, deferredResult);
    verify(interceptor).postProcess(request, deferredResult, concurrentResult);
    verify(this.asyncWebRequest).setTimeout(1000L);
  }

  @Test
  public void startDeferredResultProcessingBeforeConcurrentHandlingException() throws Exception {
    DeferredResult<Integer> deferredResult = new DeferredResult<>();
    Exception exception = new Exception();

    DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
    willThrow(exception).given(interceptor).beforeConcurrentHandling(this.request, deferredResult);

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);

    assertThatException()
            .isThrownBy(() -> this.asyncManager.startDeferredResultProcessing(deferredResult))
            .isEqualTo(exception);

    assertThat(this.asyncManager.hasConcurrentResult()).isFalse();

    verify(this.asyncWebRequest).addTimeoutHandler(notNull());
    verify(this.asyncWebRequest).addErrorHandler(notNull());
    verify(this.asyncWebRequest).addCompletionHandler(notNull());
  }

  @Test
  public void startDeferredResultProcessingPreProcessException() throws Exception {
    DeferredResult<Integer> deferredResult = new DeferredResult<>();
    Exception exception = new Exception();

    DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
    willThrow(exception).given(interceptor).preProcess(this.request, deferredResult);

    setupDefaultAsyncScenario();

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    deferredResult.setResult(25);

    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
    verifyDefaultAsyncScenario();
    verify(interceptor).beforeConcurrentHandling(this.request, deferredResult);
  }

  @Test
  public void startDeferredResultProcessingPostProcessException() throws Exception {
    DeferredResult<Integer> deferredResult = new DeferredResult<>();
    Exception exception = new Exception();

    DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
    willThrow(exception).given(interceptor).postProcess(this.request, deferredResult, 25);

    setupDefaultAsyncScenario();

    this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
    this.asyncManager.startDeferredResultProcessing(deferredResult);

    deferredResult.setResult(25);

    assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
    verifyDefaultAsyncScenario();
    verify(interceptor).beforeConcurrentHandling(this.request, deferredResult);
    verify(interceptor).preProcess(this.request, deferredResult);
  }

  @Test
  public void startDeferredResultProcessingNullInput() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.asyncManager.startDeferredResultProcessing(null))
            .withMessage("DeferredResult is required");
  }

  private void setupDefaultAsyncScenario() {
//    given(this.request.unwrapRequest(HttpServletRequest.class)).willReturn(this.servletRequest);
    given(this.asyncWebRequest.isAsyncComplete()).willReturn(false);
  }

  private void verifyDefaultAsyncScenario() {
    verify(this.asyncWebRequest).addTimeoutHandler(notNull());
    verify(this.asyncWebRequest).addErrorHandler(notNull());
    verify(this.asyncWebRequest).addCompletionHandler(notNull());
    verify(this.asyncWebRequest).startAsync();
    verify(this.asyncWebRequest).dispatch(notNull());
  }

  private final class StubCallable implements Callable<Object> {

    private Object value;

    public StubCallable(Object value) {
      this.value = value;
    }

    @Override
    public Object call() throws Exception {
      if (this.value instanceof Exception) {
        throw ((Exception) this.value);
      }
      return this.value;
    }
  }

  @SuppressWarnings("serial")
  private static class SyncTaskExecutor extends SimpleAsyncTaskExecutor {

    @Override
    @SuppressWarnings("deprecation")
    public void execute(Runnable task, long startTimeout) {
      task.run();
    }
  }

}