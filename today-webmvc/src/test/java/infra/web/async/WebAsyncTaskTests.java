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

package infra.web.async;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import infra.beans.factory.BeanFactory;
import infra.core.task.AsyncTaskExecutor;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 15:44
 */
class WebAsyncTaskTests {

  @Test
  void constructorWithCallableOnly() {
    Callable<String> callable = () -> "test";
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(callable);

    assertThat(webAsyncTask.getCallable()).isSameAs(callable);
    assertThat(webAsyncTask.getTimeout()).isNull();
    assertThat(webAsyncTask.getExecutor()).isNull();
  }

  @Test
  void constructorWithTimeoutAndCallable() {
    Callable<String> callable = () -> "test";
    Long timeout = 5000L;
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(timeout, callable);

    assertThat(webAsyncTask.getCallable()).isSameAs(callable);
    assertThat(webAsyncTask.getTimeout()).isEqualTo(timeout);
    assertThat(webAsyncTask.getExecutor()).isNull();
  }

  @Test
  void constructorWithTimeoutExecutorNameAndCallable() {
    Callable<String> callable = () -> "test";
    Long timeout = 5000L;
    String executorName = "testExecutor";
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(timeout, executorName, callable);

    assertThat(webAsyncTask.getCallable()).isSameAs(callable);
    assertThat(webAsyncTask.getTimeout()).isEqualTo(timeout);
    assertThat(webAsyncTask).extracting("executorName").isEqualTo(executorName);
  }

  @Test
  void constructorWithTimeoutExecutorAndCallable() {
    Callable<String> callable = () -> "test";
    Long timeout = 5000L;
    AsyncTaskExecutor executor = Runnable::run;
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(timeout, executor, callable);

    assertThat(webAsyncTask.getCallable()).isSameAs(callable);
    assertThat(webAsyncTask.getTimeout()).isEqualTo(timeout);
    assertThat(webAsyncTask.getExecutor()).isSameAs(executor);
  }

  @Test
  void constructorWithNullCallableThrowsException() {
    assertThatThrownBy(() -> new WebAsyncTask<>((Callable<String>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Callable is required");
  }

  @Test
  void constructorWithNullExecutorNameThrowsException() {
    Callable<String> callable = () -> "test";
    assertThatThrownBy(() -> new WebAsyncTask<>(5000L, (String) null, callable))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Executor name is required");
  }

  @Test
  void constructorWithNullExecutorThrowsException() {
    Callable<String> callable = () -> "test";
    assertThatThrownBy(() -> new WebAsyncTask<>(5000L, (AsyncTaskExecutor) null, callable))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Executor is required");
  }

  @Test
  void setBeanFactoryStoresBeanFactory() {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(null, "testExecutor", () -> "test");
    BeanFactory beanFactory = mock(BeanFactory.class);

    webAsyncTask.setBeanFactory(beanFactory);

    // Accessing private field indirectly through getExecutor when executorName is set
    when(beanFactory.getBean("testExecutor", AsyncTaskExecutor.class)).thenReturn(Runnable::run);

    assertThat(webAsyncTask.getExecutor()).isNotNull();
  }

  @Test
  void getExecutorReturnsDirectExecutor() {
    AsyncTaskExecutor executor = Runnable::run;
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(5000L, executor, () -> "test");

    assertThat(webAsyncTask.getExecutor()).isSameAs(executor);
  }

  @Test
  void getExecutorReturnsExecutorFromBeanFactory() {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(null, "testExecutor", () -> "test");
    BeanFactory beanFactory = mock(BeanFactory.class);
    AsyncTaskExecutor executor = Runnable::run;
    webAsyncTask.setBeanFactory(beanFactory);

    when(beanFactory.getBean("testExecutor", AsyncTaskExecutor.class)).thenReturn(executor);

    assertThat(webAsyncTask.getExecutor()).isSameAs(executor);
  }

  @Test
  void getExecutorReturnsNullWhenNoExecutorSet() {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");

    assertThat(webAsyncTask.getExecutor()).isNull();
  }

  @Test
  void getExecutorThrowsExceptionWhenBeanFactoryNotSet() {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(null, "testExecutor", () -> "test");

    assertThatThrownBy(webAsyncTask::getExecutor)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("BeanFactory is required to look up an executor bean by name");
  }

  @Test
  void onTimeoutSetsCallback() {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");
    Callable<String> timeoutCallback = () -> "timeout";

    webAsyncTask.onTimeout(timeoutCallback);

    assertThat(webAsyncTask).extracting("timeoutCallback").isSameAs(timeoutCallback);
  }

  @Test
  void onErrorSetsCallback() {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");
    Callable<String> errorCallback = () -> "error";

    webAsyncTask.onError(errorCallback);

    assertThat(webAsyncTask).extracting("errorCallback").isSameAs(errorCallback);
  }

  @Test
  void onCompletionSetsCallback() {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");
    Runnable completionCallback = () -> { };

    webAsyncTask.onCompletion(completionCallback);

    assertThat(webAsyncTask).extracting("completionCallback").isSameAs(completionCallback);
  }

  @Test
  void createInterceptorHandlesTimeout() throws Exception {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");
    Callable<String> timeoutCallback = () -> "timeout";
    webAsyncTask.onTimeout(timeoutCallback);

    CallableProcessingInterceptor interceptor = webAsyncTask.createInterceptor();
    Object result = interceptor.handleTimeout(new MockRequestContext(), () -> "test");

    assertThat(result).isEqualTo("timeout");
  }

  @Test
  void createInterceptorHandlesError() throws Exception {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");
    Callable<String> errorCallback = () -> "error";
    webAsyncTask.onError(errorCallback);

    CallableProcessingInterceptor interceptor = webAsyncTask.createInterceptor();
    Exception testException = new Exception("test");
    Object result = interceptor.handleError(new MockRequestContext(), () -> "test", testException);

    assertThat(result).isEqualTo("error");
  }

  @Test
  void createInterceptorHandlesCompletion() throws Exception {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");
    boolean[] completionCalled = { false };
    webAsyncTask.onCompletion(() -> completionCalled[0] = true);

    CallableProcessingInterceptor interceptor = webAsyncTask.createInterceptor();
    interceptor.afterCompletion(new MockRequestContext(), () -> "test");

    assertThat(completionCalled[0]).isTrue();
  }

  @Test
  void createInterceptorReturnsResultNoneWhenNoCallbacks() throws Exception {
    WebAsyncTask<String> webAsyncTask = new WebAsyncTask<>(() -> "test");

    CallableProcessingInterceptor interceptor = webAsyncTask.createInterceptor();
    Object timeoutResult = interceptor.handleTimeout(new MockRequestContext(), () -> "test");
    Object errorResult = interceptor.handleError(new MockRequestContext(), () -> "test", new Exception());

    assertThat(timeoutResult).isEqualTo(CallableProcessingInterceptor.RESULT_NONE);
    assertThat(errorResult).isEqualTo(CallableProcessingInterceptor.RESULT_NONE);
  }

}