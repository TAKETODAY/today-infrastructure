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

package cn.taketoday.web.handler;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.http.server.ServletServerHttpRequest;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.MapBindingResult;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingPathVariableException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.resolver.MissingRequestPartException;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.context.async.AsyncRequestTimeoutException;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.support.StaticWebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletConfig;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 15:49
 */
class ResponseEntityExceptionHandlerTests {

  private ResponseEntityExceptionHandler exceptionHandlerSupport = new ApplicationExceptionHandler();

  private SimpleHandlerExceptionHandler defaultExceptionResolver = new SimpleHandlerExceptionHandler();

  private MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");

  private MockHttpServletResponse servletResponse = new MockHttpServletResponse();

  private RequestContext request = new ServletRequestContext(null, this.servletRequest, this.servletResponse);

  @SuppressWarnings("unchecked")
  @Test
  public void supportsAllDefaultHandlerExceptionResolverExceptionTypes() throws Exception {

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
  public void httpRequestMethodNotSupported() {
    List<String> supported = Arrays.asList("POST", "DELETE");
    Exception ex = new HttpRequestMethodNotSupportedException("GET", supported);

    ResponseEntity<Object> responseEntity = testException(ex);
    assertThat(responseEntity.getHeaders().getAllow()).isEqualTo(Set.of(HttpMethod.POST, HttpMethod.DELETE));
  }

  @Test
  public void handleHttpMediaTypeNotSupported() {
    List<MediaType> acceptable = Arrays.asList(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML);
    Exception ex = new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_JSON, acceptable);

    ResponseEntity<Object> responseEntity = testException(ex);
    assertThat(responseEntity.getHeaders().getAccept()).isEqualTo(acceptable);
    assertThat(responseEntity.getHeaders().getAcceptPatch()).isEmpty();
  }

  @Test
  public void patchHttpMediaTypeNotSupported() {
    this.servletRequest = new MockHttpServletRequest("PATCH", "/");
    this.request = new ServletRequestContext(null, this.servletRequest, this.servletResponse);

    List<MediaType> acceptable = Arrays.asList(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML);
    Exception ex = new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_JSON, acceptable, HttpMethod.PATCH);

    ResponseEntity<Object> responseEntity = testException(ex);
    assertThat(responseEntity.getHeaders().getAccept()).isEqualTo(acceptable);
    assertThat(responseEntity.getHeaders().getAcceptPatch()).isEqualTo(acceptable);
  }

  @Test
  public void httpMediaTypeNotAcceptable() {
    Exception ex = new HttpMediaTypeNotAcceptableException("");
    testException(ex);
  }

  @Test
  public void missingPathVariable() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("handle", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    Exception ex = new MissingPathVariableException("param", parameter);
    testException(ex);
  }

  @Test
  public void missingServletRequestParameter() {
    Exception ex = new MissingRequestParameterException("param", "cn/taketoday/core/testfixture/type");
    testException(ex);
  }

  @Test
  public void servletRequestBindingException() {
    Exception ex = new RequestBindingException("message");
    testException(ex);
  }

  @Test
  public void conversionNotSupported() {
    Exception ex = new ConversionNotSupportedException(new Object(), Object.class, null);
    testException(ex);
  }

  @Test
  public void typeMismatch() {
    Exception ex = new TypeMismatchException("foo", String.class);
    testException(ex);
  }

  @Test
  public void httpMessageNotReadable() {
    Exception ex = new HttpMessageNotReadableException("message");
    testException(ex);
  }

  @Test
  public void httpMessageNotWritable() {
    Exception ex = new HttpMessageNotWritableException("");
    Assertions.setMaxStackTraceElementsDisplayed(100);
    testException(ex);
  }

  @Test
  public void methodArgumentNotValid() throws Exception {
    Exception ex = new MethodArgumentNotValidException(
            new MethodParameter(getClass().getDeclaredMethod("handle", String.class), 0),
            new MapBindingResult(Collections.emptyMap(), "name"));
    testException(ex);
  }

  @Test
  public void missingServletRequestPart() {
    Exception ex = new MissingRequestPartException("partName");
    testException(ex);
  }

  @Test
  public void bindException() {
    Exception ex = new BindException(new Object(), "name");
    testException(ex);
  }

  @Test
  public void noHandlerFoundException() {
    ServletServerHttpRequest req = new ServletServerHttpRequest(
            new MockHttpServletRequest("GET", "/resource"));
    Exception ex = new HandlerNotFoundException(req.getMethod().toString(),
            req.getServletRequest().getRequestURI(), req.getHeaders());
    testException(ex);
  }

  @Test
  public void asyncRequestTimeoutException() {
    testException(new AsyncRequestTimeoutException());
  }

  @Test
  public void controllerAdvice() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("exceptionHandlerAnnotationExceptionHandler", ExceptionHandlerAnnotationExceptionHandler.class);

    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);

    ctx.setServletContext(new MockServletContext());
    ctx.refresh();

    ReturnValueHandlerManager manager = ctx.getBean(ReturnValueHandlerManager.class);
    manager.registerDefaultHandlers();

    ExceptionHandlerAnnotationExceptionHandler resolver = new ExceptionHandlerAnnotationExceptionHandler();
    resolver.setApplicationContext(ctx);
    resolver.afterPropertiesSet();

    RequestBindingException ex = new RequestBindingException("message");
    assertThat(resolver.handleException(request, ex, null)).isNotNull();

    assertThat(this.servletResponse.getStatus()).isEqualTo(400);
    assertThat(this.servletResponse.getContentAsString()).isEqualTo("error content");
    assertThat(request.responseHeaders().getFirst("someHeader")).isEqualTo("someHeaderValue");
  }

  @Test
  public void controllerAdviceWithNestedException() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);
    ctx.setServletContext(new MockServletContext());
    ctx.refresh();

    ExceptionHandlerAnnotationExceptionHandler resolver = new ExceptionHandlerAnnotationExceptionHandler();
    resolver.setApplicationContext(ctx);
    resolver.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException(new RequestBindingException("message"));
    assertThat(resolver.handleException(new ServletRequestContext(null, this.servletRequest, this.servletResponse), ex, null)).isNull();
  }

  @Test
  public void controllerAdviceWithinDispatcherServlet() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("controller", ExceptionThrowingController.class);
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("exceptionHandlerAnnotationExceptionHandler", ExceptionHandlerAnnotationExceptionHandler.class);

    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);
    ctx.setServletContext(new MockServletContext());

    ctx.refresh();

    ReturnValueHandlerManager manager = ctx.getBean(ReturnValueHandlerManager.class);
    manager.registerDefaultHandlers();

    DispatcherServlet servlet = new DispatcherServlet(ctx);
    servlet.init(new MockServletConfig());
    servlet.service(this.servletRequest, this.servletResponse);

    assertThat(this.servletResponse.getStatus()).isEqualTo(400);
    assertThat(this.servletResponse.getContentAsString()).isEqualTo("error content");
    assertThat(this.servletResponse.getHeader("someHeader")).isEqualTo("someHeaderValue");
  }

  @Test
  public void controllerAdviceWithNestedExceptionWithinDispatcherServlet() throws Exception {
    StaticWebApplicationContext ctx = new StaticWebApplicationContext();
    ctx.registerSingleton("controller", NestedExceptionThrowingController.class);
    ctx.registerSingleton("exceptionHandler", ApplicationExceptionHandler.class);
    ctx.registerSingleton("parameterResolvingRegistry", ParameterResolvingRegistry.class);
    ctx.registerSingleton("returnValueHandlerManager", ReturnValueHandlerManager.class);
    ctx.setServletContext(new MockServletContext());

    ctx.refresh();

    DispatcherServlet servlet = new DispatcherServlet(ctx);
    servlet.init(new MockServletConfig());
    try {
      servlet.service(this.servletRequest, this.servletResponse);
    }
    catch (ServletException ex) {
      boolean condition1 = ex.getCause() instanceof IllegalStateException;
      assertThat(condition1).isTrue();
      boolean condition = ex.getCause().getCause() instanceof RequestBindingException;
      assertThat(condition).isTrue();
    }
  }

  private ResponseEntity<Object> testException(Exception ex) {
    try {
      ResponseEntity<Object> responseEntity = this.exceptionHandlerSupport.handleException(ex, this.request);

      // SPR-9653
      if (HttpStatus.INTERNAL_SERVER_ERROR.equals(responseEntity.getStatusCode())) {
        assertThat(this.servletRequest.getAttribute("jakarta.servlet.error.exception")).isSameAs(ex);
      }

      defaultExceptionResolver.handleException(this.request, ex, null);

      assertThat(responseEntity.getStatusCode().value()).isEqualTo(this.servletResponse.getStatus());

      return responseEntity;
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
    public void handleRequest() throws Exception {
      throw new IllegalStateException(new RequestBindingException("message"));
    }
  }

  @ControllerAdvice
  private static class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleRequestBindingException(
            RequestBindingException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

      headers = HttpHeaders.create();
      headers.set("someHeader", "someHeaderValue");
      return handleExceptionInternal(ex, "error content", headers, status, request);
    }
  }

  @SuppressWarnings("unused")
  void handle(String arg) {
  }

}
