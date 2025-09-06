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

package infra.web.handler.result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import infra.core.MethodParameter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import infra.web.BindingContext;
import infra.web.ResolvableMethod;
import infra.web.async.DeferredResult;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 23:24
 */
class DeferredResultReturnValueHandlerTests {

  private DeferredResultReturnValueHandler handler;

  private HttpMockRequestImpl request;

  private MockRequestContext webRequest;

  @BeforeEach
  public void setup() throws Exception {
    this.handler = new DeferredResultReturnValueHandler();
    this.request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    this.webRequest = new MockRequestContext(null, this.request, response);

    this.request.setAsyncSupported(true);
  }

  @Test
  public void supportsReturnType() throws Exception {
    assertThat(this.handler.supportsHandlerMethod(
            ResolvableMethod.on(TestController.class).resolveHandlerMethod(DeferredResult.class, String.class))).isTrue();

    assertThat(this.handler.supportsHandlerMethod(
            ResolvableMethod.on(TestController.class).resolveHandlerMethod(Future.class, String.class))).isTrue();

    assertThat(this.handler.supportsHandlerMethod(
            ResolvableMethod.on(TestController.class).resolveHandlerMethod(CompletableFuture.class, String.class))).isTrue();
  }

  @Test
  public void doesNotSupportReturnType() throws Exception {
    assertThat(this.handler.supportsHandlerMethod(ResolvableMethod.on(TestController.class).resolveHandlerMethod(String.class))).isFalse();
  }

  @Test
  public void deferredResult() throws Exception {
    DeferredResult<String> result = new DeferredResult<>();
    IllegalStateException ex = new IllegalStateException();
    testHandle(result, DeferredResult.class, () -> result.setErrorResult(ex), ex);
  }

  @Test
  public void listenableFuture() throws Exception {
    Promise<String> future = Future.forPromise(Runnable::run);
    testHandle(future, Future.class,
            () -> future.trySuccess("foo"), "foo");
  }

  @Test
  public void completableFuture() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    testHandle(future, CompletableFuture.class, () -> future.complete("foo"), "foo");
  }

  @Test
  public void deferredResultWithError() throws Exception {
    DeferredResult<String> result = new DeferredResult<>();
    testHandle(result, DeferredResult.class, () -> result.setResult("foo"), "foo");
  }

  @Test
  public void listenableFutureWithError() throws Exception {
    Promise<String> future = Future.forPromise(Runnable::run);
    IllegalStateException ex = new IllegalStateException();
    testHandle(future, Future.class,
            () -> future.tryFailure(ex), ex);
  }

  @Test
  public void completableFutureWithError() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    IllegalStateException ex = new IllegalStateException();
    testHandle(future, CompletableFuture.class, () -> future.completeExceptionally(ex), ex);
  }

  private void testHandle(Object returnValue, Class<?> asyncType,
          Runnable setResultTask, Object expectedValue) throws Exception {

    BindingContext mavContainer = new BindingContext();
    webRequest.setBinding(mavContainer);
    MethodParameter returnType = ResolvableMethod.on(TestController.class).resolveReturnType(asyncType, String.class);
    this.handler.handleReturnValue(webRequest, returnType, returnValue);

    assertThat(this.request.isAsyncStarted()).isTrue();
    assertThat(webRequest.asyncManager().hasConcurrentResult()).isFalse();

    setResultTask.run();

    assertThat(webRequest.asyncManager().hasConcurrentResult()).isTrue();
    assertThat(webRequest.asyncManager().getConcurrentResult()).isEqualTo(expectedValue);
  }

  @SuppressWarnings("unused")
  static class TestController {

    String handleString() { return null; }

    DeferredResult<String> handleDeferredResult() { return null; }

    Future<String> handleListenableFuture() { return null; }

    CompletableFuture<String> handleCompletableFuture() { return null; }
  }

}