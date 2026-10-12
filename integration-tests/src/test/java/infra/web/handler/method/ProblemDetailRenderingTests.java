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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.support.StaticApplicationContext;
import infra.http.HttpStatus;
import infra.http.HttpRequest;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.web.ErrorResponse;
import infra.web.HttpRequestMethodNotSupportedException;
import infra.web.bind.MissingRequestParameterException;
import infra.web.handler.ResponseEntityExceptionHandler;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.ResponseBody;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandlerTests.FailingProblemController;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandlerTests.RethrowingProblemController;
import infra.web.bind.resolver.HttpEntityMethodProcessor;
import infra.web.bind.resolver.RequestResponseBodyMethodProcessor;
import infra.web.util.UriComponentsBuilder;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/** Adapted problem-detail rendering scenarios from the upstream exception resolver tests. */
class ProblemDetailRenderingTests {

  @Test
  void jsonOnlyAdviceBypassesHtmlView() throws Exception {
    try (var application = new AnnotationConfigApplicationContext(JsonOnlyAdvice.class)) {
      var resolver = resolver(application);
      resolver.setErrorResponseViewResolver((request, detail, error) -> {
        throw new AssertionError("JSON-only advice must not resolve an HTML view");
      });
      var request = new MockRequest("GET", "/orders/42");
      request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
      var context = new MockHttpContext(application, request, new MockResponse());
      context.setMatchingMetadata(new infra.web.HandlerMatchingMetadata(context));
      assertThat(resolver.handleException(context, new IllegalStateException(), null))
              .isInstanceOf(ProblemDetail.class);
    }
  }

  @ParameterizedTest
  @ValueSource(classes = { RethrowingProblemController.class, FailingProblemController.class })
  void localHandlerFailureWritesOriginalExceptionAsProblemDetail(Class<?> controllerType) throws Exception {
    try (var application = new StaticApplicationContext()) {
      application.refresh();
      var request = new MockRequest("GET", "/orders/42");
      request.addHeader("Accept", "application/json");
      var response = new MockResponse();
      var context = new MockHttpContext(application, request, response);
      var handler = new HandlerMethod(controllerType.getDeclaredConstructor().newInstance(), "handle");
      Object value = resolver(application).handleException(context, new IllegalStateException("Secret internal state"), handler);
      new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null)
              .handleReturnValue(context, null, value);
      context.flush();
      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      assertThat(response.getContentAsString()).contains("\"status\":500").doesNotContain("Secret");
    }
  }

  @ParameterizedTest
  @ValueSource(classes = { InstanceAdvice.class, ResponseEntityAdvice.class, ResponseBodyAdvice.class })
  void applicationAdviceValuesAreResolvedAsViews(Class<?> adviceType) throws Exception {
    try (var application = new AnnotationConfigApplicationContext(adviceType)) {
      var resolver = resolver(application);
      var response = new MockResponse();
      var request = new MockRequest("GET", "/orders/42");
      request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
      var context = new MockHttpContext(application, request, response);
      resolver.setErrorResponseViewResolver((current, detail, error) -> {
        if (adviceType == InstanceAdvice.class) {
          assertThat(detail.getInstance()).isEqualTo(URI.create("/orders/42/errors/7f3c2a91"));
        }
        if (adviceType == ResponseBodyAdvice.class) {
          assertThat(error).isNull();
        }
        return new ModelAndView("error/" + detail.getStatus(), Map.of("problem", detail));
      });
      var value = (ModelAndView) resolver.handleException(context, new IllegalStateException(), null);
      context.flush();
      int status = adviceType == ResponseEntityAdvice.class ? 422 :
              adviceType == ResponseBodyAdvice.class ? 400 : 409;
      assertThat(value.getViewName()).isEqualTo("error/" + status);
      assertThat(value.getStatus()).isEqualTo(HttpStatus.valueOf(status));
      assertThat(response.getStatus()).isEqualTo(status);
      if (adviceType == ResponseEntityAdvice.class) {
        assertThat(response.getHeader("X-Error")).isEqualTo("true");
      }
    }
  }

  @Test
  void responseEntityExceptionHandlerCoexists() throws Exception {
    try (var application = new AnnotationConfigApplicationContext(ProblemDetailsAdvice.class)) {
      var response = new MockResponse();
      var context = new MockHttpContext(application, new MockRequest("GET", "/orders/42"), response);
      Object value = resolver(application).handleException(context,
              new MissingRequestParameterException("id", "int"), null);
      new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null)
              .handleReturnValue(context, null, value);
      context.flush();
      assertThat(response.getStatus()).isEqualTo(400);
      assertThat(response.getContentAsString()).contains("Required parameter 'id' is not present.");
    }
  }

  @Test
  void responseEntityExceptionHandlerThatRethrowsWrappedException() throws Exception {
    try (var application = new AnnotationConfigApplicationContext(ProblemDetailsAdvice.class)) {
      var response = new MockResponse();
      var context = new MockHttpContext(application, new MockRequest("GET", "/orders/42"), response);
      Object value = resolver(application).handleException(context,
              new IllegalStateException(new MissingRequestParameterException("id", "int")), null);
      assertThat(value).isNotNull();
      new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null)
              .handleReturnValue(context, null, value);
      context.flush();
      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }
  }

  @Test
  void viewResolverReturnsNullWithoutRepeatingInterceptors() throws Exception {
    try (var application = new StaticApplicationContext()) {
      application.refresh();
      var resolver = resolver(application);
      var invocations = new AtomicInteger();
      List<ErrorResponse.Interceptor> interceptors = List.of((detail, error) -> {
        invocations.incrementAndGet();
        detail.setProperty("traceId", "123");
      });
      resolver.setErrorResponseInterceptors(interceptors);
      resolver.setErrorResponseViewResolver((request, detail, error) -> null);
      var request = new MockRequest("GET", "/orders/42");
      request.addHeader("Accept", "text/html");
      var response = new MockResponse();
      var context = new MockHttpContext(application, request, response);
      Object value = resolver.handleException(context, new IllegalStateException(), null);
      var processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()),
              null, List.of(), null, interceptors);
      processor.handleReturnValue(context, null, value);
      context.flush();
      assertThat(invocations).hasValue(1);
      assertThat(response.getContentAsString()).contains("\"traceId\":\"123\"");
    }
  }

  @Test
  void errorResponseInterceptorsWithRequest() throws Exception {
    try (var application = new StaticApplicationContext()) {
      application.refresh();
      var request = new MockRequest("GET", "/orders/42?trace=on");
      var response = new MockResponse();
      var context = new MockHttpContext(application, request, response);
      ErrorResponse.Interceptor interceptor = new ErrorResponse.Interceptor() {
        @Override
        public void handleError(ProblemDetail detail, ErrorResponse error) {
          throw new AssertionError("Request-aware overload must be invoked");
        }

        @Override
        public void handleError(ProblemDetail detail, ErrorResponse error, infra.http.HttpRequest current) {
          assertThat(current).isSameAs(context);
          detail.setProperty("trace", UriComponentsBuilder.forURI(current.getURI()).build().getQueryParams().getFirst("trace"));
        }
      };
      var processor = new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()),
              null, List.of(), null, List.of(
                      (detail, error) -> detail.setProperty("errorResponse", error != null), interceptor));
      processor.handleReturnValue(context, null, new infra.web.ErrorResponseException(HttpStatus.BAD_REQUEST, null));
      context.flush();
      assertThat(response.getContentAsString()).contains("\"trace\":\"on\"", "\"errorResponse\":true");
    }
  }

  @Test
  void responseBodyInterceptorsReadQueryParameters() throws Exception {
    try (var application = new StaticApplicationContext()) {
      application.refresh();
      var request = new MockRequest("GET", "/orders/42?trace=on");
      var response = new MockResponse();
      var context = new MockHttpContext(application, request, response);
      ErrorResponse.Interceptor interceptor = new ErrorResponse.Interceptor() {
        @Override
        public void handleError(ProblemDetail detail, ErrorResponse error) {
        }

        @Override
        public void handleError(ProblemDetail detail, ErrorResponse error, HttpRequest current) {
          detail.setProperty("trace", UriComponentsBuilder.forURI(current.getURI()).build().getQueryParams().getFirst("trace"));
        }
      };
      var processor = new RequestResponseBodyMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()),
              null, List.of(), List.of(
                      (detail, error) -> detail.setProperty("errorResponse", error != null), interceptor));
      processor.handleReturnValue(context, null, ProblemDetail.forStatus(HttpStatus.BAD_REQUEST));
      context.flush();
      assertThat(response.getContentAsString()).contains("\"trace\":\"on\"", "\"errorResponse\":false");
    }
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = { "*/*", "application/json", "text/html;q=0.5,application/json" })
  void jsonFallbackIsActuallyWritten(String accept) throws Exception {
    try (var application = new StaticApplicationContext()) {
      application.refresh();
      var resolver = resolver(application);
      resolver.setErrorResponseViewResolver((request, detail, error) -> new ModelAndView("error"));
      var request = new MockRequest("GET", "/orders/42");
      if (accept != null) {
        request.addHeader("Accept", accept);
      }
      var response = new MockResponse();
      var context = new MockHttpContext(application, request, response);
      Object value = resolver.handleException(context, new IllegalStateException("Secret internal state"), null);
      assertThat(value).isInstanceOf(ErrorResponse.class);
      new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null)
              .handleReturnValue(context, null, value);
      context.flush();
      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      assertThat(response.getContentAsString()).contains("\"status\":500").doesNotContain("Secret");
    }
  }

  @Test
  void localizedUnknownExceptionIsActuallyWritten() throws Exception {
    try (var application = new StaticApplicationContext()) {
      application.addMessage(ErrorResponse.getDefaultDetailMessageCode(IllegalStateException.class, null),
              Locale.FRENCH, "Erreur interne");
      application.refresh();
      var request = new MockRequest();
      request.addPreferredLocale(Locale.FRENCH);
      var response = new MockResponse();
      var context = new MockHttpContext(application, request, response);
      Object value = resolver(application).handleException(context, new IllegalStateException(), null);
      new HttpEntityMethodProcessor(List.of(new JacksonJsonHttpMessageConverter()), null)
              .handleReturnValue(context, null, value);
      context.flush();
      assertThat(response.getContentAsString()).contains("Erreur interne");
    }
  }

  @Test
  void adviceWithExplicitContentTypeDoesNotResolveView() throws Exception {
    try (var application = new AnnotationConfigApplicationContext(ContentTypeAdvice.class)) {
      var resolver = resolver(application);
      resolver.setErrorResponseViewResolver((request, detail, error) -> {
        throw new AssertionError("Explicit content type must bypass views");
      });
      var request = new MockRequest();
      request.addHeader("Accept", "text/html");
      Object value = resolver.handleException(new MockHttpContext(application, request, new MockResponse()),
              new IllegalStateException(), null);
      assertThat(value).isInstanceOf(ResponseEntity.class);
    }
  }

  @Test
  void adviceHasPrecedenceOverFallback() throws Exception {
    try (var application = new AnnotationConfigApplicationContext(ContentTypeAdvice.class)) {
      Object value = resolver(application).handleException(
              new MockHttpContext(application, new MockRequest(), new MockResponse()),
              new IllegalStateException(), null);
      assertThat(((ResponseEntity<?>) value).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
  }

  @Test
  void interceptorsRunBeforeViewResolutionAndHeadersArePreserved() throws Exception {
    try (var application = new StaticApplicationContext()) {
      application.refresh();
      var resolver = resolver(application);
      var invocations = new AtomicInteger();
      resolver.setErrorResponseInterceptors(List.of((detail, error) -> {
        invocations.incrementAndGet();
        detail.setInstance(URI.create("/orders/42/errors/unique"));
        detail.setProperty("traceId", "123");
      }));
      resolver.setErrorResponseViewResolver((request, detail, error) -> {
        assertThat(detail.getInstance()).isEqualTo(URI.create("/orders/42/errors/unique"));
        assertThat(detail.getProperties()).containsEntry("traceId", "123");
        return new ModelAndView("error/405", Map.of("problem", detail));
      });
      var request = new MockRequest();
      request.addHeader("Accept", "*/*;q=0.8,text/html");
      var response = new MockResponse();
      var context = new MockHttpContext(application, request, response);
      Object value = resolver.handleException(context,
              new HttpRequestMethodNotSupportedException("POST", List.of("GET")), null);
      context.flush();
      assertThat(value).isInstanceOf(ModelAndView.class);
      assertThat(response.getStatus()).isEqualTo(405);
      assertThat(response.getHeader("Allow")).isEqualTo("GET");
      assertThat(invocations).hasValue(1);
      assertThat(response.getContentAsString()).isEmpty();
    }
  }

  private ExceptionHandlerAnnotationExceptionHandler resolver(infra.context.ApplicationContext application) {
    var resolver = new ExceptionHandlerAnnotationExceptionHandler();
    resolver.setApplicationContext(application);
    resolver.setRenderUnhandledExceptionsAsProblemDetails(true);
    resolver.afterPropertiesSet();
    return resolver;
  }

  @ControllerAdvice
  static class JsonOnlyAdvice {

    @ExceptionHandler(produces = MediaType.APPLICATION_JSON_VALUE)
    public ProblemDetail handle(IllegalStateException exception) {
      return ProblemDetail.forStatus(HttpStatus.CONFLICT);
    }
  }

  @ControllerAdvice
  static class InstanceAdvice {

    @ExceptionHandler
    public ProblemDetail handle(IllegalStateException exception) {
      ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Order 42 is already cancelled");
      detail.setType(URI.create("https://example.org/problems/order-already-cancelled"));
      detail.setTitle("Order already cancelled");
      detail.setInstance(URI.create("/orders/42/errors/7f3c2a91"));
      return detail;
    }
  }

  @ControllerAdvice
  static class ResponseEntityAdvice {

    @ExceptionHandler
    public ResponseEntity<ProblemDetail> handle(IllegalStateException exception) {
      return ResponseEntity.status(422).header("X-Error", "true")
              .body(ProblemDetail.forStatus(HttpStatus.valueOf(422)));
    }
  }

  @ControllerAdvice
  static class ResponseBodyAdvice {

    @ExceptionHandler
    @ResponseBody
    public Object handle(IllegalStateException exception) {
      return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    }
  }

  @ControllerAdvice
  static class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {
  }

  @ControllerAdvice
  static class ContentTypeAdvice {
    @ExceptionHandler
    public ResponseEntity<ProblemDetail> handle(IllegalStateException exception) {
      return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON)
              .body(ProblemDetail.forStatus(HttpStatus.CONFLICT));
    }
  }
}
