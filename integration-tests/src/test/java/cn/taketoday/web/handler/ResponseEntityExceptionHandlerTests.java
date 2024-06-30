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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.context.support.StaticMessageSource;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.mock.web.MockMockConfig;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.validation.MapBindingResult;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ResponseStatusException;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.async.AsyncRequestTimeoutException;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingPathVariableException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.resolver.MissingRequestPartException;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import cn.taketoday.web.mock.MockDispatcher;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;
import cn.taketoday.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 15:49
 */
class ResponseEntityExceptionHandlerTests {

  private final ResponseEntityExceptionHandler exceptionHandler = new ApplicationExceptionHandler();

  private SimpleHandlerExceptionHandler defaultExceptionResolver = new SimpleHandlerExceptionHandler();

  private HttpMockRequest mockRequest = new HttpMockRequestImpl("GET", "/");

  private MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

  private RequestContext request = new MockRequestContext(null, this.mockRequest, this.mockResponse);

  @SuppressWarnings("unchecked")
  @Test
  void supportsAllDefaultHandlerExceptionResolverExceptionTypes() throws Exception {

    ExceptionHandler annotation = ResponseEntityExceptionHandler.class
            .getMethod("handleException", Exception.class, RequestContext.class)
            .getAnnotation(ExceptionHandler.class);

    Arrays.stream(SimpleHandlerExceptionHandler.class.getDeclaredMethods())
            .filter(method -> method.getName().startsWith("handle") && (method.getParameterCount() == 4))
            .filter(method -> !method.getName().equals("handleErrorResponse"))
            .map(method -> method.getParameterTypes()[0])
            .forEach(exceptionType -> assertThat(annotation.value())
                    .as("@ExceptionHandler is missing declaration for " + exceptionType.getName())
                    .contains((Class<Exception>) exceptionType));
  }

  @Test
  void httpRequestMethodNotSupported() {
    ResponseEntity<Object> entity =
            testException(new HttpRequestMethodNotSupportedException("GET", List.of("POST", "DELETE")));

    assertThat(entity.getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("POST,DELETE");
  }

  @Test
  void httpMediaTypeNotSupported() {
    ResponseEntity<Object> entity = testException(new HttpMediaTypeNotSupportedException(
            MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML)));

    assertThat(entity.getHeaders().getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/atom+xml, application/xml");
    assertThat(entity.getHeaders().getAcceptPatch()).isEmpty();
  }

  @Test
  void patchHttpMediaTypeNotSupported() {
    this.mockRequest = new HttpMockRequestImpl("PATCH", "/");
    this.request = new MockRequestContext(null, this.mockRequest, this.mockResponse);

    ResponseEntity<Object> entity = testException(
            new HttpMediaTypeNotSupportedException(
                    MediaType.APPLICATION_JSON,
                    List.of(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML),
                    HttpMethod.PATCH));

    HttpHeaders headers = entity.getHeaders();
    assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/atom+xml, application/xml");
    assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("application/atom+xml, application/xml");
    assertThat(headers.getFirst(HttpHeaders.ACCEPT_PATCH)).isEqualTo("application/atom+xml, application/xml");
  }

  @Test
  void httpMediaTypeNotAcceptable() {
    testException(new HttpMediaTypeNotAcceptableException(""));
  }

  @Test
  void missingPathVariable() throws NoSuchMethodException {
    testException(new MissingPathVariableException("param",
            new MethodParameter(getClass().getDeclaredMethod("handle", String.class), 0)));
  }

  @Test
  void missingServletRequestParameter() {
    testException(new MissingRequestParameterException("param", "type"));
  }

  @Test
  void servletRequestBindingException() {
    testException(new RequestBindingException("message"));
  }

  @Test
  void errorResponseProblemDetailViaMessageSource() {
    try {
      Locale locale = Locale.UK;
      LocaleContextHolder.setLocale(locale);

      String type = "https://example.com/probs/unsupported-content";
      String title = "Media type is not valid or not supported";
      Class<HttpMediaTypeNotSupportedException> exceptionType = HttpMediaTypeNotSupportedException.class;

      StaticMessageSource source = new StaticMessageSource();
      source.addMessage(ErrorResponse.getDefaultTypeMessageCode(exceptionType), locale, type);
      source.addMessage(ErrorResponse.getDefaultTitleMessageCode(exceptionType), locale, title);
      source.addMessage(ErrorResponse.getDefaultDetailMessageCode(exceptionType, null), locale,
              "Content-Type {0} not supported. Supported: {1}");

      this.exceptionHandler.setMessageSource(source);

      ResponseEntity<?> entity = testException(new HttpMediaTypeNotSupportedException(
              MediaType.APPLICATION_JSON, List.of(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML)));

      ProblemDetail problem = (ProblemDetail) entity.getBody();
      assertThat(problem).isNotNull();
      assertThat(problem.getType()).isEqualTo(URI.create(type));
      assertThat(problem.getTitle()).isEqualTo(title);
      assertThat(problem.getDetail()).isEqualTo(
              "Content-Type application/json not supported. Supported: [application/atom+xml, application/xml]");
    }
    finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test // gh-30300
  public void reasonAsDetailShouldBeUpdatedViaMessageSource() {

    Locale locale = Locale.UK;
    LocaleContextHolder.setLocale(locale);

    String reason = "bad.request";
    String message = "Breaking Bad Request";
    try {
      StaticMessageSource messageSource = new StaticMessageSource();
      messageSource.addMessage(reason, locale, message);

      this.exceptionHandler.setMessageSource(messageSource);

      ResponseEntity<?> entity = testException(new ResponseStatusException(HttpStatus.BAD_REQUEST, reason));

      ProblemDetail body = (ProblemDetail) entity.getBody();
      assertThat(body.getDetail()).isEqualTo(message);
    }
    finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  void conversionNotSupported() {
    testException(new ConversionNotSupportedException(new Object(), Object.class, null));
  }

  @Test
  void typeMismatch() {
    testException(new TypeMismatchException("foo", String.class));
  }

  @Test
  void typeMismatchWithProblemDetailViaMessageSource() {
    Locale locale = Locale.UK;
    LocaleContextHolder.setLocale(locale);

    try {
      StaticMessageSource messageSource = new StaticMessageSource();
      messageSource.addMessage(
              ErrorResponse.getDefaultDetailMessageCode(TypeMismatchException.class, null), locale,
              "Failed to set {0} to value: {1}");

      this.exceptionHandler.setMessageSource(messageSource);

      ResponseEntity<?> entity = testException(
              new TypeMismatchException(new PropertyChangeEvent(this, "name", "John", "James"), String.class));

      ProblemDetail body = (ProblemDetail) entity.getBody();
      assertThat(body.getDetail()).isEqualTo("Failed to set name to value: James");
    }
    finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  public void httpMessageNotReadable() {
    testException(new HttpMessageNotReadableException("message", null));
  }

  @Test
  void httpMessageNotWritable() {
    testException(new HttpMessageNotWritableException(""));
  }

  @Test
  void methodArgumentNotValid() throws Exception {
    testException(new MethodArgumentNotValidException(
            new MethodParameter(getClass().getDeclaredMethod("handle", String.class), 0),
            new MapBindingResult(Collections.emptyMap(), "name")));
  }

  @Test
  void missingRequestPart() {
    testException(new MissingRequestPartException("partName"));
  }

  @Test
  void noHandlerFoundException() {
    HttpHeaders requestHeaders = HttpHeaders.forWritable();
    requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // gh-29626

    ResponseEntity<Object> responseEntity =
            testException(new HandlerNotFoundException("GET", "/resource", requestHeaders));

    assertThat(responseEntity.getHeaders()).isEmpty();
  }

//  @Test
//  void noResourceFoundException() {
//    testException(new NoResourceFoundException(HttpMethod.GET, "/resource"));
//  }

  @Test
  void asyncRequestTimeoutException() {
    testException(new AsyncRequestTimeoutException());
  }

  @Test
  void maxUploadSizeExceededException() {
    testException(new MaxUploadSizeExceededException(1000));
  }

  @Test
  void controllerAdvice() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("exceptionHandlerAnnotationExceptionHandler", ExceptionHandlerAnnotationExceptionHandler.class);

    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);

    ctx.setMockContext(new MockContextImpl());
    ctx.refresh();

    ReturnValueHandlerManager manager = ctx.getBean(ReturnValueHandlerManager.class);
    manager.registerDefaultHandlers();

    ExceptionHandlerAnnotationExceptionHandler resolver = new ExceptionHandlerAnnotationExceptionHandler();
    resolver.setApplicationContext(ctx);
    resolver.afterPropertiesSet();

    RequestBindingException ex = new RequestBindingException("message");

    manager.handleReturnValue(request, null, resolver.handleException(request, ex, null));

    assertThat(this.mockResponse.getStatus()).isEqualTo(400);
    assertThat(this.mockResponse.getContentAsString()).isEqualTo("error content");
    assertThat(this.mockResponse.getHeader("someHeader")).isEqualTo("someHeaderValue");
  }

  @Test
  void controllerAdviceWithNestedException() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);
    ctx.setMockContext(new MockContextImpl());
    ctx.refresh();

    ExceptionHandlerAnnotationExceptionHandler resolver = new ExceptionHandlerAnnotationExceptionHandler();
    resolver.setApplicationContext(ctx);
    resolver.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException(new RequestBindingException("message"));
    MockRequestContext context = new MockRequestContext(null, this.mockRequest, this.mockResponse);
    assertThat(resolver.handleException(context, ex, null)).isNotNull().isInstanceOf(ResponseEntity.class);
  }

  @Test
  void controllerAdviceWithinDispatcherServlet() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("controller", ExceptionThrowingController.class);
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("exceptionHandlerAnnotationExceptionHandler", ExceptionHandlerAnnotationExceptionHandler.class);

    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);
    ctx.setMockContext(new MockContextImpl());

    ctx.refresh();

    ReturnValueHandlerManager manager = ctx.getBean(ReturnValueHandlerManager.class);
    manager.registerDefaultHandlers();

    MockDispatcher servlet = new MockDispatcher(ctx);
    servlet.init(new MockMockConfig());
    servlet.service(this.mockRequest, this.mockResponse);

    assertThat(this.mockResponse.getStatus()).isEqualTo(400);
    assertThat(this.mockResponse.getContentAsString()).isEqualTo("error content");
    assertThat(this.mockResponse.getHeader("someHeader")).isEqualTo("someHeaderValue");
  }

  @Test
  void controllerAdviceWithNestedExceptionWithinDispatcherServlet() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("controller", NestedExceptionThrowingController.class);
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);
    ctx.setMockContext(new MockContextImpl());

    ctx.refresh();

    MockDispatcher servlet = new MockDispatcher(ctx);
    servlet.init(new MockMockConfig());
    try {
      servlet.service(this.mockRequest, this.mockResponse);
    }
    catch (MockException ex) {
      boolean condition1 = ex.getCause() instanceof IllegalStateException;
      assertThat(condition1).isTrue();
      boolean condition = ex.getCause().getCause() instanceof RequestBindingException;
      assertThat(condition).isTrue();
    }
  }

  private ResponseEntity<Object> testException(Exception ex) {
    try {
      ResponseEntity<Object> entity = this.exceptionHandler.handleException(ex, this.request);
      assertThat(entity).isNotNull();

      defaultExceptionResolver.handleException(this.request, ex, null);
      assertThat(entity.getStatusCode().value()).isEqualTo(this.mockResponse.getStatus());

      return entity;
    }
    catch (Exception ex2) {
      throw new IllegalStateException("handleException threw exception", ex2);
    }
  }

  @Controller
  private static class ExceptionThrowingController {

    @RequestMapping("/")
    public void handleRequest() throws Exception {
      throw new RequestBindingException("message");
    }
  }

  @Controller
  private static class NestedExceptionThrowingController {

    @RequestMapping("/")
    public void handleRequest() {
      throw new IllegalStateException(new RequestBindingException("message"));
    }
  }

  @ControllerAdvice
  private static class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleRequestBindingException(
            RequestBindingException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

      headers = HttpHeaders.forWritable();
      headers.set("someHeader", "someHeaderValue");
      return handleExceptionInternal(ex, "error content", headers, status, request);
    }
  }

  @SuppressWarnings("unused")
  void handle(String arg) {
  }

}
