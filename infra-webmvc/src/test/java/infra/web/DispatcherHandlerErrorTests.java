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

package infra.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.web.context.support.RequestScope;
import infra.web.annotation.ExceptionHandler;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.result.AsyncReturnValueHandler;
import infra.web.mock.MockHttpContext;
import infra.web.multipart.MultipartRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Regression tests for dispatch failures outside the Exception hierarchy.
 */
class DispatcherHandlerErrorTests {

  @ParameterizedTest
  @EnumSource(FailureStage.class)
  void errorIsResolvedAndResourcesAreReleasedOnce(FailureStage stage) throws Exception {
    var error = new AssertionError("boom");
    var observed = new AtomicReference<Throwable>();
    var listeners = new AtomicInteger();
    var callbacks = new AtomicInteger();
    var destroyed = new AtomicInteger();
    try (var application = new AnnotationConfigApplicationContext()) {
      if (stage == FailureStage.FILTER) {
        application.registerBean("filter", Filter.class, () -> (request, chain) -> { throw error; });
      }
      application.refresh();
      var dispatcher = new DispatcherHandler(application);
      HttpRequestHandler handler = request -> { throw error; };
      var renderingHandler = new FailingReturnValueHandler(error);
      dispatcher.setHandlerMapping(request -> stage == FailureStage.RENDERING ? renderingHandler : handler);
      dispatcher.setExceptionHandler((request, failure, selected) -> {
        observed.set(failure);
        return HttpRequestHandler.NONE_RETURN_VALUE;
      });
      dispatcher.addRequestCompletedActions((request, failure) -> listeners.incrementAndGet());
      dispatcher.start();

      var multipart = mock(MultipartRequest.class);
      var request = new TrackingContext(multipart);
      request.registerCallback(HttpContext.Lifecycle.COMPLETED, "test", callbacks::incrementAndGet);
      HttpContextHolder.set(request);
      try {
        RequestScope.instance.registerDestructionCallback("bean", destroyed::incrementAndGet);
      }
      finally {
        HttpContextHolder.cleanup();
      }

      Throwable escaped = catchThrowable(() -> {
        if (stage == FailureStage.ASYNC || stage == FailureStage.ASYNC_HANDLER) {
          dispatcher.handleConcurrentResult(request, renderingHandler,
                  stage == FailureStage.ASYNC ? error : "result");
        }
        else {
          dispatcher.handleRequest(request);
        }
      });

      assertThat(escaped).isNull();
      assertThat(observed.get()).isSameAs(error);
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(listeners).hasValue(1);
      assertThat(callbacks).hasValue(1);
      assertThat(destroyed).hasValue(1);
      assertThat(request.transportCompletions).isEqualTo(1);
      verify(multipart).cleanup();
      dispatcher.handleConcurrentResult(request, null, error);
      assertThat(listeners).hasValue(1);
      assertThat(callbacks).hasValue(1);
    }
  }

  @Test
  void annotationExceptionHandlerMatchesOriginalError() throws Exception {
    var error = new AssertionError("boom");
    try (var application = new AnnotationConfigApplicationContext()) {
      application.refresh();
      var controller = new ErrorController(error);
      var dispatcher = new DispatcherHandler(application);
      var resolver = new ExceptionHandlerAnnotationExceptionHandler();
      resolver.setApplicationContext(application);
      resolver.afterPropertiesSet();
      dispatcher.setExceptionHandler(resolver);
      dispatcher.setHandlerMapping(request -> new HandlerMethod(controller, "handle"));
      dispatcher.start();
      var request = new MockHttpContext(application);

      dispatcher.handleRequest(request);

      assertThat(controller.observed).isSameAs(error);
      assertThat(request.isRequestCompleted()).isTrue();
    }
  }

  @ParameterizedTest
  @EnumSource(FailureStage.class)
  void unresolvedErrorSendsOneErrorResponseAndCompletes(FailureStage stage) throws Exception {
    var error = new AssertionError("boom");
    var observed = new AtomicReference<Throwable>();
    try (var application = new AnnotationConfigApplicationContext()) {
      if (stage == FailureStage.FILTER) {
        application.registerBean("filter", Filter.class, () -> (request, chain) -> { throw error; });
      }
      application.refresh();
      var dispatcher = new DispatcherHandler(application);
      var renderingHandler = new FailingReturnValueHandler(error);
      dispatcher.setHandlerMapping(request -> stage == FailureStage.RENDERING ? renderingHandler
              : (HttpRequestHandler) http -> { throw error; });
      dispatcher.setExceptionHandler((request, failure, selected) -> null);
      dispatcher.addRequestCompletedActions((request, failure) -> observed.set(failure));
      dispatcher.start();
      var sends = new AtomicInteger();
      var request = new MockHttpContext() {
        @Override
        public void sendError(int status, @Nullable String message) throws java.io.IOException {
          sends.incrementAndGet();
          super.sendError(status, message);
        }
      };

      if (stage == FailureStage.ASYNC || stage == FailureStage.ASYNC_HANDLER) {
        dispatcher.handleConcurrentResult(request, renderingHandler,
                stage == FailureStage.ASYNC ? error : "result");
      }
      else {
        dispatcher.handleRequest(request);
      }

      assertThat(observed.get()).isSameAs(error);
      assertThat(request.getStatus()).isEqualTo(500);
      assertThat(sends).hasValue(1);
      assertThat(request.isRequestCompleted()).isTrue();
    }
  }

  @Test
  void filterErrorAfterCompletionDoesNotResolveOrCompleteAgain() throws Exception {
    var error = new AssertionError("post-processing");
    var listeners = new AtomicInteger();
    try (var application = new AnnotationConfigApplicationContext()) {
      application.registerBean("filter", Filter.class, () -> (request, chain) -> {
        chain.doFilter(request);
        throw error;
      });
      application.refresh();
      var dispatcher = new DispatcherHandler(application);
      dispatcher.setHandlerMapping(request -> (HttpRequestHandler) http -> HttpRequestHandler.NONE_RETURN_VALUE);
      dispatcher.setExceptionHandler((request, failure, selected) -> {
        throw new AssertionError("Must not resolve completed request");
      });
      dispatcher.addRequestCompletedActions((request, failure) -> listeners.incrementAndGet());
      dispatcher.start();
      var request = new MockHttpContext();

      assertThat(catchThrowable(() -> dispatcher.handleRequest(request)))
              .isSameAs(error);
      assertThat(listeners).hasValue(1);
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(request.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void completionErrorIsSuppressedOntoOriginalFailure() {
    var failure = new IllegalStateException("dispatch");
    var cleanupError = new AssertionError("cleanup");
    try (var application = new AnnotationConfigApplicationContext()) {
      application.refresh();
      var dispatcher = new DispatcherHandler(application);
      var request = new MockHttpContext() {
        @Override
        public boolean isCommitted() {
          return true;
        }

        @Override
        protected void completeRequestInternal(@Nullable Throwable notHandled) {
          throw cleanupError;
        }
      };
      var callbacks = new AtomicInteger();
      request.registerCallback(HttpContext.Lifecycle.COMPLETED, "test", callbacks::incrementAndGet);

      assertThat(catchThrowable(() -> dispatcher.completeRequest(request, failure)))
              .isSameAs(failure).hasSuppressedException(cleanupError);
      assertThat(request.isRequestCompleted()).isTrue();
      assertThat(callbacks).hasValue(1);
    }
  }

  @Test
  void completionErrorWithoutOriginalFailurePropagatesUnchanged() {
    var cleanupError = new AssertionError("cleanup");
    try (var application = new AnnotationConfigApplicationContext()) {
      application.refresh();
      var dispatcher = new DispatcherHandler(application);
      var request = new MockHttpContext() {
        @Override
        protected void completeRequestInternal(@Nullable Throwable failure) {
          throw cleanupError;
        }
      };
      assertThat(catchThrowable(() -> dispatcher.completeRequest(request, null))).isSameAs(cleanupError);
      assertThat(request.isRequestCompleted()).isTrue();
    }
  }

  enum FailureStage {HANDLER, FILTER, ASYNC, ASYNC_HANDLER, RENDERING}

  static class ErrorController {

    private final AssertionError error;

    @Nullable AssertionError observed;

    ErrorController(AssertionError error) {
      this.error = error;
    }

    public void handle() {
      throw error;
    }

    @ExceptionHandler
    public Object onError(AssertionError error) {
      this.observed = error;
      return HttpRequestHandler.NONE_RETURN_VALUE;
    }
  }

  static class FailingReturnValueHandler implements HttpRequestHandler, ReturnValueHandler, AsyncReturnValueHandler {

    private final AssertionError error;

    FailingReturnValueHandler(AssertionError error) {
      this.error = error;
    }

    @Override
    public Object handleRequest(HttpContext request) {
      return "result";
    }

    @Override
    public boolean supportsHandler(Object handler) {
      return true;
    }

    @Override
    public void handleReturnValue(HttpContext context, @Nullable Object handler, @Nullable Object value) {
      throw error;
    }

    @Override
    public void handleAsyncReturnValue(HttpContext context, @Nullable Object value) {
      throw error;
    }
  }

  static class TrackingContext extends MockHttpContext {

    int transportCompletions;

    TrackingContext(MultipartRequest multipart) {
      this.multipartRequest = multipart;
    }

    @Override
    protected void completeRequestInternal(@Nullable Throwable failure) {
      transportCompletions++;
    }
  }
}
