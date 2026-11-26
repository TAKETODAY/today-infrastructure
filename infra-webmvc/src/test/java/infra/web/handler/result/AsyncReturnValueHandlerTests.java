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

package infra.web.handler.result;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import infra.beans.factory.BeanFactory;
import infra.core.task.AsyncTaskExecutor;
import infra.http.ResponseEntity;
import infra.web.ServerHttpExchange;
import infra.web.async.DeferredResult;
import infra.web.async.WebAsyncManager;
import infra.web.async.WebAsyncTask;
import infra.web.handler.StreamingResponseBody;
import infra.web.handler.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class AsyncReturnValueHandlerTests {

  private final ServerHttpExchange context = mock(ServerHttpExchange.class);

  private final WebAsyncManager manager = mock(WebAsyncManager.class);

  private final HandlerMethod method = mock(HandlerMethod.class);

  @Test
  void callableHandlerPassesTheTaskAndAdaptsAResumedValue() throws Exception {
    given(context.asyncManager()).willReturn(manager);
    var handler = new CallableMethodReturnValueHandler();
    Callable<String> task = () -> "result";
    handler.handleReturnValue(context, method, task);
    then(manager).should().startCallableProcessing(task, method);

    handler.handleReturnValue(context, method, "resumed");
    ArgumentCaptor<Callable<?>> captor = ArgumentCaptor.captor();
    then(manager).should(times(2)).startCallableProcessing(captor.capture(), eq(method));
    assertThat(captor.getValue().call()).isEqualTo("resumed");
  }

  @Test
  void asyncTaskHandlerSuppliesTheFactoryForNamedExecutors() throws Exception {
    given(context.asyncManager()).willReturn(manager);
    BeanFactory factory = mock(BeanFactory.class);
    AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
    given(factory.getBean("sampleExecutor", AsyncTaskExecutor.class)).willReturn(executor);
    var handler = new AsyncTaskMethodReturnValueHandler(factory);
    WebAsyncTask<String> task = new WebAsyncTask<>(null, "sampleExecutor", () -> "result");

    handler.handleReturnValue(context, method, task);
    then(manager).should().startCallableProcessing(task, method);
    assertThat(task.getExecutor()).isSameAs(executor);

    handler.handleReturnValue(context, method, "resumed");
    ArgumentCaptor<Callable<?>> captor = ArgumentCaptor.captor();
    then(manager).should().startCallableProcessing(captor.capture(), eq(method));
    assertThat(captor.getValue().call()).isEqualTo("resumed");
  }

  @Test
  void deferredHandlerAdaptsResumedValuesAndUnwrapsCompletionFailures() throws Exception {
    given(context.asyncManager()).willReturn(manager);
    var handler = new DeferredResultReturnValueHandler();
    IllegalArgumentException failure = new IllegalArgumentException("failed");
    handler.handleReturnValue(context, method, "resumed");
    handler.handleReturnValue(context, method, CompletableFuture.failedFuture(new CompletionException(failure)));

    ArgumentCaptor<DeferredResult<?>> captor = ArgumentCaptor.captor();
    then(manager).should(times(2)).startDeferredResultProcessing(captor.capture(), eq(method));
    assertThat(captor.getAllValues()).extracting(DeferredResult::getResult).containsExactly("resumed", failure);
  }

  @Test
  void streamingHandlerMatchesDirectAndWrappedBodyTypes() throws Exception {
    var handler = new StreamingResponseBodyReturnValueHandler();
    assertThat(handler.supportsHandlerMethod(handlerMethod("stream"))).isTrue();
    assertThat(handler.supportsHandlerMethod(handlerMethod("streamEntity"))).isTrue();
    assertThat(handler.supportsHandlerMethod(handlerMethod("textEntity"))).isFalse();
  }

  @Test
  void streamingHandlerIgnoresNullAndRejectsUnrelatedValues() throws Exception {
    var handler = new StreamingResponseBodyReturnValueHandler();
    handler.handleReturnValue(context, null, null);
    assertThatIllegalArgumentException().isThrownBy(() -> handler.handleReturnValue(context, null, "unexpected"))
            .withMessage("StreamingResponseBody expected");
    then(context).shouldHaveNoInteractions();
  }

  @Test
  void streamingHandlerAdaptsResumedValueForHandlerMethod() throws Exception {
    given(context.asyncManager()).willReturn(manager);
    var handler = new StreamingResponseBodyReturnValueHandler();

    handler.handleReturnValue(context, method, "resumed");

    ArgumentCaptor<Callable<?>> captor = ArgumentCaptor.captor();
    then(manager).should().startCallableProcessing(captor.capture(), eq(method));
    assertThat(captor.getValue().call()).isEqualTo("resumed");
  }

  StreamingResponseBody stream() {
    return output -> { };
  }

  ResponseEntity<StreamingResponseBody> streamEntity() {
    return ResponseEntity.ok(stream());
  }

  ResponseEntity<String> textEntity() {
    return ResponseEntity.ok("sample");
  }

  private HandlerMethod handlerMethod(String name) throws NoSuchMethodException {
    return new HandlerMethod(this, AsyncReturnValueHandlerTests.class.getDeclaredMethod(name));
  }

  @Test
  void deferredHandlerIgnoresNullAndRejectsUnexpectedValues() throws Exception {
    var handler = new DeferredResultReturnValueHandler();
    handler.handleReturnValue(context, null, null);
    assertThatIllegalStateException().isThrownBy(() -> handler.handleReturnValue(context, null, "unexpected"))
            .withMessageContaining("unexpected");
    then(context).shouldHaveNoInteractions();
  }

}
