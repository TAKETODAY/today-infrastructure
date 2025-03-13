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

package infra.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.util.Locale;

import infra.aop.framework.ProxyFactory;
import infra.beans.FatalBeanException;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.support.ApplicationObjectSupport;
import infra.context.support.StaticApplicationContext;
import infra.core.MethodParameter;
import infra.core.annotation.Order;
import infra.core.i18n.LocaleContextHolder;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.lang.Nullable;
import infra.mock.api.MockException;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.stereotype.Controller;
import infra.ui.Model;
import infra.ui.ModelMap;
import infra.util.ClassUtils;
import infra.web.BindingContext;
import infra.web.HttpRequestHandler;
import infra.web.RedirectModel;
import infra.web.RequestContext;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.ResponseBody;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestControllerAdvice;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.handler.function.HandlerFunction;
import infra.web.handler.function.ServerResponse;
import infra.web.mock.MockRequestContext;
import infra.web.resource.ResourceHttpRequestHandler;
import infra.web.util.WebUtils;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 22:45
 */
class ExceptionHandlerAnnotationExceptionHandlerTests {

  private ExceptionHandlerAnnotationExceptionHandler handler;

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  @EnableWebMvc
  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

  @BeforeEach
  public void setup() throws Exception {
    this.handler = new ExceptionHandlerAnnotationExceptionHandler();
    this.handler.setWarnLogCategory(this.handler.getClass().getName());

    this.request = new HttpMockRequestImpl("GET", "/");
    this.response = new MockHttpResponseImpl();
  }

  @Test
  void nullHandler() throws Exception {
    Object handler = null;
    this.handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));

    this.handler.afterPropertiesSet();
    Object mav = this.handler.handleException(new MockRequestContext(this.request, this.response), null, handler);
    assertThat(mav).as("Exception can be resolved only if there is a HandlerMethod").isNull();
  }

  @Test
  void resolveNoExceptionHandlerForException() throws Exception {
    Exception npe = new NullPointerException();
    HandlerMethod handlerMethod = new HandlerMethod(new IoExceptionController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));

    this.handler.afterPropertiesSet();
    ModelAndView mav = handleException(npe, handlerMethod);
    assertThat(mav).as("NPE should not have been handled").isNull();
  }

  @Test
  void handleExceptionModelAndView() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException("Bad argument");
    HandlerMethod handlerMethod = new HandlerMethod(new ModelAndViewController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
    this.handler.afterPropertiesSet();

    ModelAndView mav = handleException(ex, handlerMethod);// ViewRenderingException
    assertThat(mav).isNotNull();
    assertThat(mav.getViewName()).isEqualTo("errorView");
    assertThat(mav.getModel().get("detail")).isEqualTo("Bad argument");
  }

  private ModelAndView handleException(Exception ex, HandlerMethod handlerMethod) throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.refresh();
    return handleException(context, ex, handlerMethod);
  }

  private ModelAndView handleException(ApplicationContext context, Exception ex, HandlerMethod handlerMethod) throws Exception {
    MockRequestContext context1 = new MockRequestContext(context, request, response);
    context1.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);

    context1.setBinding(new BindingContext());
    Object ret = this.handler.handleException(context1, ex, handlerMethod);
    if (ret instanceof ModelAndView mav) {
      return mav;
    }
    else if (ret == null) {
      return null;
    }
    else if (ret instanceof String viewName) {
      return new ModelAndView(viewName);
    }
    return new ModelAndView();
  }

  @Test
  void handleExceptionResponseBody() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));

    this.handler.afterPropertiesSet();
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("IllegalArgumentException");
  }

  @Test
  void handleExceptionResponseBodyMatchingCauseLevel2() throws Exception {
    Exception ex = new Exception(new Exception(new IllegalArgumentException()));
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
    this.handler.afterPropertiesSet();
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("IllegalArgumentException");
  }

  @Test
  void handleExceptionResponseWriter() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseWriterController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
    this.handler.afterPropertiesSet();
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).isNull();
    assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
  }

  @Test
  void handleExceptionModelAtArgument() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ModelArgumentController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
    this.handler.afterPropertiesSet();
    ResolvableParameterFactory factory = new ResolvableParameterFactory();

    MockRequestContext context = new MockRequestContext(null, request, response);
    context.setBinding(new BindingContext());

    Object ret = this.handler.handleException(context, ex, handlerMethod);

    ModelMap model = context.getBinding().getModel();
    assertThat(model.size()).isEqualTo(1);
    assertThat(model.getAttribute("exceptionClassName")).isEqualTo("IllegalArgumentException");
  }

  @Test
  void handleExceptionGlobalHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalAccessException ex = new IllegalAccessException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("AnotherTestExceptionResolver: IllegalAccessException");
  }

  @Test
  void handleExceptionGlobalHandlerOrdered() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("TestExceptionResolver: IllegalStateException");
  }

  @Test
  void handleExceptionGlobalHandlerOrderedMatchingCauseLevel2() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    Exception ex = new Exception(new Exception(new IllegalStateException()));
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("TestExceptionResolver: IllegalStateException");
  }

  @Test
  void handleExceptionWithHandlerMethodArg() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("HandlerMethod: handle");
  }

  @Test
  void handleExceptionWithAssertionError() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    AssertionError err = new AssertionError("argh");
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(new MockException("Handler dispatch failed", err), handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo(err.toString());
  }

  @Test
  void handleExceptionWithAssertionErrorAsRootCause() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    AssertionError rootCause = new AssertionError("argh");
    FatalBeanException cause = new FatalBeanException("wrapped", rootCause);
    Exception ex = new Exception(cause);  // gh-26317
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo(rootCause.toString());
  }

  @Test
  void handleExceptionWithReasonResolvedByMessageSource() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    StaticApplicationContext context = new StaticApplicationContext(ctx);
    Locale locale = Locale.ENGLISH;
    context.addMessage("gateway.timeout", locale, "Gateway Timeout");
    context.refresh();
    LocaleContextHolder.setLocale(locale);
    this.handler.setApplicationContext(context);
    this.handler.afterPropertiesSet();

    SocketTimeoutException ex = new SocketTimeoutException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(context, ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
    assertThat(this.response.getErrorMessage()).isEqualTo("Gateway Timeout");
    assertThat(this.response.getContentAsString()).isEqualTo("");
  }

  @Test
  void handleExceptionControllerAdviceHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, MyControllerAdviceConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
  }

  @Test
    // gh-26317
  void handleExceptionControllerAdviceHandlerMatchingCauseLevel2() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    Exception ex = new Exception(new IllegalStateException());
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
  }

  @Test
  void handleExceptionControllerAdviceNoHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    Object mav = this.handler.handleException(new MockRequestContext(this.request, this.response), ex, null);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
  }

  @Test
  void handleExceptionControllerAdviceAgainstProxy() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, MyControllerAdviceConfig.class);
    handler.setApplicationContext(ctx);
    handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    HandlerMethod handlerMethod = new HandlerMethod(new ProxyFactory(new ResponseBodyController()).getProxy(), "handle");
    ModelAndView mav = handleException(ctx, ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
  }

  @Test
  void handleExceptionViaMappedHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, MyControllerAdviceConfig.class);
    this.handler.setMappedHandlerClasses(HttpRequestHandler.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    Object mav = this.handler.handleException(
            new MockRequestContext(ctx, this.request, this.response), ex, handler);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
  }

  @Test
  void resolveExceptionJsonMediaType() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
    handler.setApplicationContext(ctx);
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new MediaTypeController(), "handle");
    this.handler.afterPropertiesSet();
    this.request.addHeader("Accept", "application/json");

    Object mav = this.handler.handleException(
            new MockRequestContext(ctx, this.request, this.response), ex, handlerMethod);

    assertThat(mav).isEqualTo("jsonBody");
  }

  @Test
  void resolveExceptionHtmlMediaType() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
    handler.setApplicationContext(ctx);

    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new MediaTypeController(), "handle");
    this.handler.afterPropertiesSet();
    this.request.addHeader("Accept", "text/html");

    Object mav = this.handler.handleException(
            new MockRequestContext(ctx, this.request, this.response), ex, handlerMethod);

    assertThat(mav).isNotNull();
    assertThat(mav).isEqualTo("htmlView");
  }

  @Test
  void resolveExceptionDefaultMediaType() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
    handler.setApplicationContext(ctx);
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new MediaTypeController(), "handle");
    this.handler.afterPropertiesSet();
    this.request.addHeader("Accept", "*/*");

    Object mav = this.handler.handleException(
            new MockRequestContext(ctx, this.request, this.response), ex, handlerMethod);

    assertThat(mav).isNotNull();
    assertThat(mav).isEqualTo("htmlView");
  }

  @Test
  void resolveExceptionGlobalHandlerForHandlerFunction() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    handler.setApplicationContext(ctx);
    handler.afterPropertiesSet();

    IllegalAccessException ex = new IllegalAccessException();
    HandlerFunction<ServerResponse> handlerFunction = req -> {
      throw new IllegalAccessException();
    };

    Object mav = this.handler.handleException(new MockRequestContext(ctx, this.request, this.response), ex, handlerFunction);
    assertThat(mav).isEqualTo("AnotherTestExceptionResolver: IllegalAccessException");
  }

  @Controller
  static class ModelAndViewController {

    public void handle() { }

    @ExceptionHandler
    public ModelAndView handle(Exception ex) throws IOException {
      return new ModelAndView("errorView", "detail", ex.getMessage());
    }
  }

  @Controller
  static class ResponseWriterController {

    public void handle() { }

    @ExceptionHandler
    public void handleException(Exception ex, Writer writer) throws IOException {
      writer.write(ClassUtils.getShortName(ex.getClass()));
    }
  }

  interface ResponseBodyInterface {

    void handle();

    @ExceptionHandler
    @ResponseBody
    String handleException(IllegalArgumentException ex);
  }

  @Controller
  static class ResponseBodyController extends ApplicationObjectSupport implements ResponseBodyInterface {

    @Override
    public void handle() { }

    @Override
    @ExceptionHandler
    @ResponseBody
    public String handleException(IllegalArgumentException ex) {
      return ClassUtils.getShortName(ex.getClass());
    }
  }

  @Controller
  static class IoExceptionController {

    public void handle() { }

    @ExceptionHandler(value = IOException.class)
    public void handleException() {
    }
  }

  @Controller
  static class ModelArgumentController {

    public void handle() { }

    @ExceptionHandler
    public void handleException(Exception ex, Model model) {
      model.setAttribute("exceptionClassName", ClassUtils.getShortName(ex.getClass()));
    }
  }

  @Controller
  static class RedirectAttributesController {

    public void handle() { }

    @ExceptionHandler
    public String handleException(Exception ex, RedirectModel redirectAttributes) {
      redirectAttributes.setAttribute("exceptionClassName", ClassUtils.getShortName(ex.getClass()));
      return "redirect:/";
    }
  }

  @RestControllerAdvice
  @Order(1)
  static class TestExceptionResolver {

    @ExceptionHandler
    public String handleException(IllegalStateException ex) {
      return "TestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
    }

    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    public String handleWithHandlerMethod(HandlerMethod handlerMethod) {
      return "HandlerMethod: " + handlerMethod.getMethod().getName();
    }

    @ExceptionHandler(AssertionError.class)
    public String handleAssertionError(Error err) {
      return err.toString();
    }
  }

  @RestControllerAdvice
  @Order(2)
  static class AnotherTestExceptionResolver {

    @ExceptionHandler({ IllegalStateException.class, IllegalAccessException.class })
    public String handleException(Exception ex) {
      return "AnotherTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
    }
  }

  @RestControllerAdvice
  @Order(3)
  static class ResponseStatusTestExceptionResolver {

    @ExceptionHandler(SocketTimeoutException.class)
    @ResponseStatus(code = HttpStatus.GATEWAY_TIMEOUT, reason = "gateway.timeout")
    public void handleException(SocketTimeoutException ex) {

    }
  }

  @EnableWebMvc
  @Configuration
  static class MyConfig {

    @Bean
    public TestExceptionResolver testExceptionResolver() {
      return new TestExceptionResolver();
    }

    @Bean
    public AnotherTestExceptionResolver anotherTestExceptionResolver() {
      return new AnotherTestExceptionResolver();
    }

    @Bean
    public ResponseStatusTestExceptionResolver responseStatusTestExceptionResolver() {
      return new ResponseStatusTestExceptionResolver();
    }
  }

  @RestControllerAdvice("java.lang")
  @Order(1)
  static class NotCalledTestExceptionResolver {

    @ExceptionHandler
    public String handleException(IllegalStateException ex) {
      return "NotCalledTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
    }
  }

  @RestControllerAdvice(assignableTypes = ApplicationObjectSupport.class)
  @Order(2)
  static class BasePackageTestExceptionResolver {

    @ExceptionHandler
    public String handleException(IllegalStateException ex) {
      return "BasePackageTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
    }
  }

  @RestControllerAdvice
  @Order(3)
  static class DefaultTestExceptionResolver {

    @ExceptionHandler
    public String handleException(IllegalStateException ex) {
      return "DefaultTestExceptionResolver: " + ClassUtils.getShortName(ex.getClass());
    }
  }

  @EnableWebMvc
  @Configuration
  static class MyControllerAdviceConfig {

    @Bean
    public NotCalledTestExceptionResolver notCalledTestExceptionResolver() {
      return new NotCalledTestExceptionResolver();
    }

    @Bean
    public BasePackageTestExceptionResolver basePackageTestExceptionResolver() {
      return new BasePackageTestExceptionResolver();
    }

    @Bean
    public DefaultTestExceptionResolver defaultTestExceptionResolver() {
      return new DefaultTestExceptionResolver();
    }
  }

  static class CustomResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(@Nullable Object body, @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
      return false;
    }

    @Nullable
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
            MethodParameter returnType, MediaType contentType,
            HttpMessageConverter<?> converter, RequestContext context) {
      return null;
    }
  }

  @Controller
  static class MediaTypeController {

    public void handle() { }

    @ExceptionHandler(exception = IllegalArgumentException.class, produces = "application/json")
    @ResponseBody
    public String handleExceptionJson() {
      return "jsonBody";
    }

    @ExceptionHandler(exception = IllegalArgumentException.class, produces = { "text/html", "*/*" })
    public String handleExceptionHtml() {
      return "htmlView";
    }
  }

}
