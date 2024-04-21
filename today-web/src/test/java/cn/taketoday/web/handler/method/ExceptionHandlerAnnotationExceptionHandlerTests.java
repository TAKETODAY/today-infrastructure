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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.util.Locale;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.ApplicationObjectSupport;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.ui.Model;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;
import cn.taketoday.web.servlet.MockServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 22:45
 */
class ExceptionHandlerAnnotationExceptionHandlerTests {

  private ExceptionHandlerAnnotationExceptionHandler handler;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @EnableWebMvc
  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

  @BeforeEach
  public void setup() throws Exception {
    this.handler = new ExceptionHandlerAnnotationExceptionHandler();
    this.handler.setWarnLogCategory(this.handler.getClass().getName());

    this.request = new MockHttpServletRequest("GET", "/");
    this.response = new MockHttpServletResponse();
  }

  @Test
  void nullHandler() throws Exception {
    Object handler = null;
    this.handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));

    this.handler.afterPropertiesSet();
    Object mav = this.handler.handleException(new MockServletRequestContext(this.request, this.response), null, handler);
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
    assertThat(mav.getViewName()).isNull();

//    assertThat(mav.getViewName()).isEqualTo("errorView");
//    assertThat(mav.getModel().get("detail")).isEqualTo("Bad argument");
  }

  private ModelAndView handleException(Exception ex, HandlerMethod handlerMethod) throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.refresh();
    return handleException(context, ex, handlerMethod);
  }

  private ModelAndView handleException(
          ApplicationContext context, Exception ex, HandlerMethod handlerMethod) throws Exception {
    ResolvableParameterFactory factory = new ResolvableParameterFactory();
    ActionMappingAnnotationHandler handler = new ActionMappingAnnotationHandler(
            handlerMethod, factory.createArray(handlerMethod.getMethod()), handlerMethod.getBeanType()) {
      @Override
      public Object getHandlerObject() {
        return handlerMethod.getBean();
      }
    };

    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);

    MockServletRequestContext context1 = new MockServletRequestContext(context, request, response);
    context1.setBinding(new BindingContext());
    Object ret = this.handler.handleException(context1, ex, handler);
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
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
  }

  @Test
    // gh-26317
  void handleExceptionResponseBodyMatchingCauseLevel2() throws Exception {
    Exception ex = new Exception(new Exception(new IllegalArgumentException()));
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
    this.handler.afterPropertiesSet();
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
  }

  @Test
  void handleExceptionResponseWriter() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseWriterController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
    this.handler.afterPropertiesSet();
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
  }

  @Test
    // SPR-13546
  void handleExceptionModelAtArgument() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ModelArgumentController(), "handle");
    handler.setApplicationContext(new AnnotationConfigApplicationContext(Config.class));
    this.handler.afterPropertiesSet();
    ResolvableParameterFactory factory = new ResolvableParameterFactory();

    ActionMappingAnnotationHandler handler = new ActionMappingAnnotationHandler(
            handlerMethod, factory.createArray(handlerMethod.getMethod()), handlerMethod.getBeanType()) {
      @Override
      public Object getHandlerObject() {
        return handlerMethod.getBean();
      }
    };

    MockServletRequestContext context = new MockServletRequestContext(null, request, response);
    context.setBinding(new BindingContext());

    Object ret = this.handler.handleException(context, ex, handler);

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
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("AnotherTestExceptionResolver: IllegalAccessException");
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
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("TestExceptionResolver: IllegalStateException");
  }

  @Test
    // gh-26317
  void handleExceptionGlobalHandlerOrderedMatchingCauseLevel2() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    Exception ex = new Exception(new Exception(new IllegalStateException()));
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("TestExceptionResolver: IllegalStateException");
  }

  @Test
    // SPR-12605
  void handleExceptionWithHandlerMethodArg() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    ArrayIndexOutOfBoundsException ex = new ArrayIndexOutOfBoundsException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("HandlerMethod: handle");
  }

  @Test
  void handleExceptionWithAssertionError() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    AssertionError err = new AssertionError("argh");
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = handleException(new ServletException("Handler dispatch failed", err), handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo(err.toString());
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
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo(rootCause.toString());
  }

  @Test
    //gh-27156
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
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
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
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
  }

  @Test
  void handleExceptionControllerAdviceNoHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    Object mav = this.handler.handleException(new MockServletRequestContext(this.request, this.response), ex, null);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(this.response.getContentAsString()).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
  }

  @Test
    // SPR-16496
  void handleExceptionControllerAdviceAgainstProxy() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, MyControllerAdviceConfig.class);
    handler.setApplicationContext(ctx);
    handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    HandlerMethod handlerMethod = new HandlerMethod(new ProxyFactory(new ResponseBodyController()).getProxy(), "handle");
    ModelAndView mav = handleException(ctx, ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
  }

  @Test
    // gh-22619
  void handleExceptionViaMappedHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class, MyControllerAdviceConfig.class);
    this.handler.setMappedHandlerClasses(HttpRequestHandler.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    Object mav = this.handler.handleException(
            new MockServletRequestContext(ctx, this.request, this.response), ex, handler);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(this.response.getContentAsString()).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
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

}
