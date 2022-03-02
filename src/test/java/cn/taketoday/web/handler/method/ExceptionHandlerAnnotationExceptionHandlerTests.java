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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Locale;

import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.Order;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.handler.HandlerMethodReturnValueHandler;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockHttpServletResponse;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.NestedServletException;
import cn.taketoday.web.view.Model;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 22:45
 */
class ExceptionHandlerAnnotationExceptionHandlerTests {

  private static int RESOLVER_COUNT;

  private static int HANDLER_COUNT;

  private ExceptionHandlerAnnotationExceptionHandler handler;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @BeforeAll
  public static void setupOnce() {
    ExceptionHandlerAnnotationExceptionHandler resolver = new ExceptionHandlerAnnotationExceptionHandler();
    resolver.afterPropertiesSet();
    RESOLVER_COUNT = resolver.getArgumentResolvers().getResolvers().size();
    HANDLER_COUNT = resolver.getReturnValueHandlers().getHandlers().size();
  }

  @BeforeEach
  public void setup() throws Exception {
    this.handler = new ExceptionHandlerAnnotationExceptionHandler();
    this.handler.setWarnLogCategory(this.handler.getClass().getName());
    this.request = new MockHttpServletRequest("GET", "/");
    this.request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
    this.response = new MockHttpServletResponse();
  }

  @Test
  void nullHandler() {
    Object handler = null;
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handler, null);
    assertThat(mav).as("Exception can be resolved only if there is a HandlerMethod").isNull();
  }

  @Test
  void setCustomArgumentResolvers() {
    HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
    this.handler.setCustomArgumentResolvers(Collections.singletonList(resolver));
    this.handler.afterPropertiesSet();

    assertThat(this.handler.getArgumentResolvers().getResolvers().contains(resolver)).isTrue();
    assertMethodProcessorCount(RESOLVER_COUNT + 1, HANDLER_COUNT);
  }

  @Test
  void setArgumentResolvers() {
    HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
    this.handler.setArgumentResolvers(Collections.singletonList(resolver));
    this.handler.afterPropertiesSet();

    assertMethodProcessorCount(1, HANDLER_COUNT);
  }

  @Test
  void setCustomReturnValueHandlers() {
    HandlerMethodReturnValueHandler handler = new ViewNameMethodReturnValueHandler();
    this.handler.setCustomReturnValueHandlers(Collections.singletonList(handler));
    this.handler.afterPropertiesSet();

    assertThat(this.handler.getReturnValueHandlers().getHandlers().contains(handler)).isTrue();
    assertMethodProcessorCount(RESOLVER_COUNT, HANDLER_COUNT + 1);
  }

  @Test
  void setResponseBodyAdvice() {
    this.handler.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));
    assertThat(this.handler).extracting("responseBodyAdvice").asList().hasSize(1);
    this.handler.setResponseBodyAdvice(Collections.singletonList(new CustomResponseBodyAdvice()));
    assertThat(this.handler).extracting("responseBodyAdvice").asList().hasSize(2);
  }

  @Test
  void setReturnValueHandlers() {
    HandlerMethodReturnValueHandler handler = new ModelMethodProcessor();
    this.handler.setReturnValueHandlers(Collections.singletonList(handler));
    this.handler.afterPropertiesSet();

    assertMethodProcessorCount(RESOLVER_COUNT, 1);
  }

  @Test
  void resolveNoExceptionHandlerForException() throws NoSuchMethodException {
    Exception npe = new NullPointerException();
    HandlerMethod handlerMethod = new HandlerMethod(new IoExceptionController(), "handle");
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, npe);

    assertThat(mav).as("NPE should not have been handled").isNull();
  }

  @Test
  void handleExceptionModelAndView() throws NoSuchMethodException {
    IllegalArgumentException ex = new IllegalArgumentException("Bad argument");
    HandlerMethod handlerMethod = new HandlerMethod(new ModelAndViewController(), "handle");
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isFalse();
    assertThat(mav.getViewName()).isEqualTo("errorView");
    assertThat(mav.getModel().get("detail")).isEqualTo("Bad argument");
  }

  @Test
  void handleExceptionResponseBody() throws UnsupportedEncodingException, NoSuchMethodException {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
  }

  @Test
    // gh-26317
  void handleExceptionResponseBodyMatchingCauseLevel2() throws UnsupportedEncodingException, NoSuchMethodException {
    Exception ex = new Exception(new Exception(new IllegalArgumentException()));
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
  }

  @Test
  void handleExceptionResponseWriter() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseWriterController(), "handle");
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("IllegalArgumentException");
  }

  @Test
    // SPR-13546
  void handleExceptionModelAtArgument() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new ModelArgumentController(), "handle");
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).isNotNull();
    assertThat(mav.getModelMap().size()).isEqualTo(1);
    assertThat(mav.getModelMap().get("exceptionClassName")).isEqualTo("IllegalArgumentException");
  }

  @Test
    // SPR-14651
  void resolveRedirectAttributesAtArgument() throws Exception {
    IllegalArgumentException ex = new IllegalArgumentException();
    HandlerMethod handlerMethod = new HandlerMethod(new RedirectAttributesController(), "handle");
    this.handler.afterPropertiesSet();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).isNotNull();
    assertThat(mav.getViewName()).isEqualTo("redirect:/");
    FlashMap flashMap = (FlashMap) this.request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE);
    assertThat((Object) flashMap).as("output FlashMap should exist").isNotNull();
    assertThat(flashMap.get("exceptionClassName")).isEqualTo("IllegalArgumentException");
  }

  @Test
  void handleExceptionGlobalHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalAccessException ex = new IllegalAccessException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    Object mav = handleException(ex, handlerMethod);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("AnotherTestExceptionResolver: IllegalAccessException");
  }

  private Object handleException(IllegalAccessException ex, HandlerMethod handlerMethod) {
    Object mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);
    return mav;
  }

  @Test
  void handleExceptionGlobalHandlerOrdered() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

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
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

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
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

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
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod,
            new NestedServletException("Handler dispatch failed", err));

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
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

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
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
    assertThat(this.response.getErrorMessage()).isEqualTo("Gateway Timeout");
    assertThat(this.response.getContentAsString()).isEqualTo("");
  }

  @Test
  void handleExceptionControllerAdviceHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    HandlerMethod handlerMethod = new HandlerMethod(new ResponseBodyController(), "handle");
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

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
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

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
    ModelAndView mav = this.handler.handleException(this.request, this.response, null, ex);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
  }

  @Test
    // SPR-16496
  void handleExceptionControllerAdviceAgainstProxy() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    HandlerMethod handlerMethod = new HandlerMethod(new ProxyFactory(new ResponseBodyController()).getProxy(), "handle");
    ModelAndView mav = this.handler.handleException(this.request, this.response, handlerMethod, ex);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("BasePackageTestExceptionResolver: IllegalStateException");
  }

  @Test
    // gh-22619
  void handleExceptionViaMappedHandler() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyControllerAdviceConfig.class);
    this.handler.setMappedHandlerClasses(HttpRequestHandler.class);
    this.handler.setApplicationContext(ctx);
    this.handler.afterPropertiesSet();

    IllegalStateException ex = new IllegalStateException();
    ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
    ModelAndView mav = this.handler.handleException(this.request, this.response, handler, ex);

    assertThat(mav).as("Exception was not handled").isNotNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(this.response.getContentAsString()).isEqualTo("DefaultTestExceptionResolver: IllegalStateException");
  }

  private void assertMethodProcessorCount(int resolverCount, int handlerCount) {
    assertThat(this.handler.getArgumentResolvers().getResolvers().size()).isEqualTo(resolverCount);
    assertThat(this.handler.getReturnValueHandlers().getHandlers().size()).isEqualTo(handlerCount);
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
  static class ResponseBodyController extends WebApplicationContextSupport implements ResponseBodyInterface {

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

  @RestControllerAdvice(assignableTypes = WebApplicationContextSupport.class)
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
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
      return false;
    }

    @Nullable
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
            MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, RequestContext context) {
      return null;
    }
  }

}
