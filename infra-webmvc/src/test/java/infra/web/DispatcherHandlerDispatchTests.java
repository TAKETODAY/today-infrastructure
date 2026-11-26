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

package infra.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import infra.context.support.GenericApplicationContext;
import infra.core.Ordered;
import infra.web.accept.ApiVersionStrategy;
import infra.web.handler.AsyncHandler;
import infra.web.handler.HandlerNotFoundException;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.ReturnValueHandlerNotFoundException;
import infra.web.mock.MockHttpContext;
import infra.web.server.RequestContinueExpectedResolver;
import infra.web.view.ViewRef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.withSettings;

class DispatcherHandlerDispatchTests {

  @Test
  void publishesResolvedApiVersionBeforeMappingAndNotifiesDeprecationStrategy() throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      MockHttpContext request = new MockHttpContext(application);
      ApiVersionStrategy strategy = mock(ApiVersionStrategy.class);
      given(strategy.resolveParseAndValidateVersion(request)).willAnswer(invocation -> 2);
      application.getBeanFactory().registerSingleton("apiVersionStrategy", strategy);
      application.refresh();
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      HttpRequestHandler handler = mock(HttpRequestHandler.class);
      given(handler.handleRequest(request)).willReturn(HttpRequestHandler.NONE_RETURN_VALUE);
      HandlerMapping mapping = mock(HandlerMapping.class);
      given(mapping.getHandler(request)).willAnswer(invocation -> {
        assertThat(request.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE)).isEqualTo(2);
        return handler;
      });
      dispatcher.setHandlerMapping(mapping);
      dispatcher.start();

      dispatcher.handleRequest(request);

      assertThat(dispatcher.getApiVersionStrategy()).isSameAs(strategy);
      then(strategy).should().handleDeprecations(2, handler, request);
      then(handler).should().handleRequest(request);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void routesUnmappedRequestToConfiguredNotFoundPath(boolean throwException) throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      NotFoundHandler fallback = mock(NotFoundHandler.class);
      application.getBeanFactory().registerSingleton("notFoundHandler", fallback);
      application.refresh();
      MockHttpContext request = new MockHttpContext(application);
      request.request.setMethod("GET");
      request.request.setRequestURI("/missing");
      given(fallback.handleNotFound(request)).willReturn(HttpRequestHandler.NONE_RETURN_VALUE);
      HandlerExceptionHandler exceptionHandler = mock(HandlerExceptionHandler.class);
      given(exceptionHandler.handleException(any(), any(HandlerNotFoundException.class), isNull()))
              .willReturn(HandlerExceptionHandler.NONE_RETURN_VALUE);
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      dispatcher.setThrowExceptionIfNoHandlerFound(throwException);
      dispatcher.setExceptionHandler(exceptionHandler);
      dispatcher.start();

      dispatcher.handleRequest(request);

      if (throwException) {
        then(exceptionHandler).should().handleException(any(), any(HandlerNotFoundException.class), isNull());
        then(fallback).shouldHaveNoInteractions();
      }
      else {
        then(fallback).should().handleNotFound(request);
        then(exceptionHandler).shouldHaveNoInteractions();
      }
      assertThat(request.isRequestCompleted()).isTrue();
    }
  }

  @Test
  void continuesPastUndecidedExpectContinueResolver() {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      MockHttpContext request = new MockHttpContext(application);
      MockHttpContext undecided = new MockHttpContext(application);
      RequestContinueExpectedResolver first = mock(RequestContinueExpectedResolver.class,
              withSettings().extraInterfaces(Ordered.class));
      RequestContinueExpectedResolver second = mock(RequestContinueExpectedResolver.class,
              withSettings().extraInterfaces(Ordered.class));
      given(((Ordered) first).getOrder()).willReturn(-1);
      given(((Ordered) second).getOrder()).willReturn(0);
      given(first.shouldContinue(any())).willReturn(null);
      given(second.shouldContinue(undecided)).willReturn(null);
      given(second.shouldContinue(request)).willReturn(false);
      application.getBeanFactory().registerSingleton("firstResolver", first);
      application.getBeanFactory().registerSingleton("secondResolver", second);
      application.refresh();
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      dispatcher.start();

      assertThat(dispatcher.requestContinueExpected(request)).isFalse();
      assertThat(dispatcher.requestContinueExpected(undecided)).isNull();
      then(first).should().shouldContinue(request);
      then(second).should().shouldContinue(request);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void translatesNullResultToDefaultView(boolean rendererAvailable) throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      application.getBeanFactory().registerSingleton("viewTranslator", (RequestToViewNameTranslator) request -> "landing");
      application.refresh();
      MockHttpContext request = new MockHttpContext(application);
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      dispatcher.setHandlerMapping(context -> (HttpRequestHandler) http -> null);
      ReturnValueHandler renderer = mock(ReturnValueHandler.class);
      ReturnValueHandlerManager manager = mock(ReturnValueHandlerManager.class);
      if (rendererAvailable) {
        given(manager.selectHandler(isNull(), any(ViewRef.class))).willReturn(renderer);
      }
      dispatcher.setReturnValueHandler(manager);
      dispatcher.setExceptionHandler((context, failure, handler) -> null);
      dispatcher.start();

      dispatcher.handleRequest(request);

      if (rendererAvailable) {
        var view = org.mockito.ArgumentCaptor.forClass(ViewRef.class);
        then(renderer).should().handleReturnValue(any(), isNull(), view.capture());
        assertThat(view.getValue().getViewName()).isEqualTo("landing");
      }
      else {
        assertThat(request.getStatus()).isEqualTo(500);
      }
      assertThat(request.isRequestCompleted()).isTrue();
    }
  }

  @Test
  void reportsUnsupportedSendResultWithoutCompletingRequest() {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      MockHttpContext request = new MockHttpContext(application);
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      dispatcher.setReturnValueHandler(mock(ReturnValueHandlerManager.class));

      assertThatThrownBy(() -> dispatcher.send(request, "unsupported"))
              .isInstanceOfSatisfying(ReturnValueHandlerNotFoundException.class,
                      failure -> assertThat(failure.getReturnValue()).isEqualTo("unsupported"));
      assertThat(request.isRequestCompleted()).isFalse();
    }
  }

  @Test
  void asyncResultUsesWrappedReturnValueHandler() throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      MockHttpContext request = new MockHttpContext(application);
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      AsyncHandler handler = mock(AsyncHandler.class);
      ReturnValueHandler renderer = mock(ReturnValueHandler.class);
      given(handler.wrapConcurrentResult("async result")).willReturn(renderer);

      dispatcher.handleConcurrentResult(request, handler, "async result");

      then(renderer).should().handleReturnValue(request, renderer, "async result");
      assertThat(request.isRequestCompleted()).isTrue();
    }
  }

  @Test
  void asyncFailureAfterCommitCompletesAndPropagatesOriginalFailure() throws Exception {
    try (GenericApplicationContext application = new GenericApplicationContext()) {
      MockHttpContext request = new MockHttpContext(application);
      request.response.setCommitted(true);
      DispatcherHandler dispatcher = new DispatcherHandler(application);
      HandlerExceptionHandler resolver = mock(HandlerExceptionHandler.class);
      dispatcher.setExceptionHandler(resolver);
      Throwable failure = new IllegalStateException("async failure");

      assertThatThrownBy(() -> dispatcher.handleConcurrentResult(request, null, failure)).isSameAs(failure);
      then(resolver).should(never()).handleException(any(), any(), any());
      assertThat(request.isRequestCompleted()).isTrue();
    }
  }

}
