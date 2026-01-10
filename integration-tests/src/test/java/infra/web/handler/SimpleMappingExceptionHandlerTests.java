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

package infra.web.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Properties;

import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.ExceptionUtils;
import infra.web.mock.MockRequestContext;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/3 9:42
 */
class SimpleMappingExceptionHandlerTests {

  private SimpleMappingExceptionHandler exceptionHandler;
  private HttpMockRequestImpl request;
  private MockHttpResponseImpl response;
  private Object handler1;
  private Object handler2;
  private Exception genericException;

  @BeforeEach
  public void setUp() throws Exception {
    exceptionHandler = new SimpleMappingExceptionHandler();
    handler1 = new String();
    handler2 = new Object();
    request = new HttpMockRequestImpl();
    response = new MockHttpResponseImpl();
    request.setMethod("GET");
    genericException = new Exception();
  }

  @Test
  public void setOrder() {
    exceptionHandler.setOrder(2);
    assertThat(exceptionHandler.getOrder()).isEqualTo(2);
  }

  @Test
  public void defaultErrorView() {
    exceptionHandler.setDefaultErrorView("default-view");
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("default-view");
    assertThat(mav.getModel().get(SimpleMappingExceptionHandler.DEFAULT_EXCEPTION_ATTRIBUTE)).isEqualTo(genericException);
  }

  @Test
  public void defaultErrorViewDifferentHandler() {
    exceptionHandler.setDefaultErrorView("default-view");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    ModelAndView mav = handleException(handler2, genericException);
    assertThat(mav).isNull();
  }

  @Test
  public void defaultErrorViewDifferentHandlerClass() {
    exceptionHandler.setDefaultErrorView("default-view");
    exceptionHandler.setMappedHandlerClasses(String.class);
    ModelAndView mav = handleException(handler2, genericException);
    assertThat(mav).isNull();
  }

  @Test
  public void nullExceptionAttribute() {
    exceptionHandler.setDefaultErrorView("default-view");
    exceptionHandler.setExceptionAttribute(null);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("default-view");
    assertThat(mav.getModel().get(SimpleMappingExceptionHandler.DEFAULT_EXCEPTION_ATTRIBUTE)).isNull();
  }

  private ModelAndView handleException(Object handler, Exception ex) {
    try {
      Object ret = this.exceptionHandler.handleException(new MockRequestContext(null, request, response), ex, handler);
      if (ret instanceof ModelAndView mav) {
        return mav;
      }
      else if (ret == null) {
        return null;
      }
      else if (ret instanceof String viewName) {
        return new ModelAndView(viewName);
      }
      return null;
//      return new ModelAndView(ret);
    }
    catch (Exception e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  @Test
  public void nullExceptionMappings() {
    exceptionHandler.setExceptionMappings(null);
    exceptionHandler.setDefaultErrorView("default-view");
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("default-view");
  }

  @Test
  public void noDefaultStatusCode() {
    exceptionHandler.setDefaultErrorView("default-view");
    handleException(handler1, genericException);
    assertThat(response.getStatus()).isEqualTo(HttpMockResponse.SC_OK);
  }

  @Test
  public void setDefaultStatusCode() {
    exceptionHandler.setDefaultErrorView("default-view");
    exceptionHandler.setDefaultStatusCode(HttpMockResponse.SC_BAD_REQUEST);
    handleException(handler1, genericException);
    assertThat(response.getStatus()).isEqualTo(HttpMockResponse.SC_BAD_REQUEST);
  }

  @Test
  public void noDefaultStatusCodeInInclude() {
    exceptionHandler.setDefaultErrorView("default-view");
    exceptionHandler.setDefaultStatusCode(HttpMockResponse.SC_BAD_REQUEST);
    handleException(handler1, genericException);
    assertThat(response.getStatus()).isEqualTo(HttpMockResponse.SC_BAD_REQUEST);
  }

  @Test
  public void specificStatusCode() {
    exceptionHandler.setDefaultErrorView("default-view");
    exceptionHandler.setDefaultStatusCode(HttpMockResponse.SC_BAD_REQUEST);
    Properties statusCodes = new Properties();
    statusCodes.setProperty("default-view", "406");
    exceptionHandler.setStatusCodes(statusCodes);
    handleException(handler1, genericException);
    assertThat(response.getStatus()).isEqualTo(HttpMockResponse.SC_NOT_ACCEPTABLE);
  }

  @Test
  public void simpleExceptionMapping() {
    Properties props = new Properties();
    props.setProperty("Exception", "error");
    exceptionHandler.setWarnLogCategory("HANDLER_EXCEPTION");
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("error");
  }

  @Test
  public void exactExceptionMappingWithHandlerSpecified() {
    Properties props = new Properties();
    props.setProperty("java.lang.Exception", "error");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("error");
  }

  @Test
  public void exactExceptionMappingWithHandlerClassSpecified() {
    Properties props = new Properties();
    props.setProperty("java.lang.Exception", "error");
    exceptionHandler.setMappedHandlerClasses(String.class);
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("error");
  }

  @Test
  public void exactExceptionMappingWithHandlerInterfaceSpecified() {
    Properties props = new Properties();
    props.setProperty("java.lang.Exception", "error");
    exceptionHandler.setMappedHandlerClasses(Comparable.class);
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("error");
  }

  @Test
  public void simpleExceptionMappingWithHandlerSpecifiedButWrongHandler() {
    Properties props = new Properties();
    props.setProperty("Exception", "error");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler2, genericException);
    assertThat(mav).isNull();
  }

  @Test
  public void simpleExceptionMappingWithHandlerClassSpecifiedButWrongHandler() {
    Properties props = new Properties();
    props.setProperty("Exception", "error");
    exceptionHandler.setMappedHandlerClasses(String.class);
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler2, genericException);
    assertThat(mav).isNull();
  }

  @Test
  public void simpleExceptionMappingWithExclusion() {
    Properties props = new Properties();
    props.setProperty("Exception", "error");
    exceptionHandler.setExceptionMappings(props);
    exceptionHandler.setExcludedExceptions(IllegalArgumentException.class);
    ModelAndView mav = handleException(handler1, new IllegalArgumentException());
    assertThat(mav).isNull();
  }

  @Test
  public void missingExceptionInMapping() {
    Properties props = new Properties();
    props.setProperty("SomeFooThrowable", "error");
    exceptionHandler.setWarnLogCategory("HANDLER_EXCEPTION");
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav).isNull();
  }

  @Test
  public void twoMappings() {
    Properties props = new Properties();
    props.setProperty("java.lang.Exception", "error");
    props.setProperty("AnotherException", "another-error");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("error");
  }

  @Test
  public void twoMappingsOneShortOneLong() {
    Properties props = new Properties();
    props.setProperty("Exception", "error");
    props.setProperty("AnotherException", "another-error");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, genericException);
    assertThat(mav.getViewName()).isEqualTo("error");
  }

  @Test
  public void twoMappingsOneShortOneLongThrowOddException() {
    Exception oddException = new SomeOddException();
    Properties props = new Properties();
    props.setProperty("Exception", "error");
    props.setProperty("SomeOddException", "another-error");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, oddException);
    assertThat(mav.getViewName()).isEqualTo("another-error");
  }

  @Test
  public void twoMappingsThrowOddExceptionUseLongExceptionMapping() {
    Exception oddException = new SomeOddException();
    Properties props = new Properties();
    props.setProperty("java.lang.Exception", "error");
    props.setProperty("SomeOddException", "another-error");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, oddException);
    assertThat(mav.getViewName()).isEqualTo("another-error");
  }

  @Test
  public void threeMappings() {
    Exception oddException = new AnotherOddException();
    Properties props = new Properties();
    props.setProperty("java.lang.Exception", "error");
    props.setProperty("SomeOddException", "another-error");
    props.setProperty("AnotherOddException", "another-some-error");
    exceptionHandler.setMappedHandlers(Collections.singleton(handler1));
    exceptionHandler.setExceptionMappings(props);
    ModelAndView mav = handleException(handler1, oddException);
    assertThat(mav.getViewName()).isEqualTo("another-some-error");
  }

  @SuppressWarnings("serial")
  private static class SomeOddException extends Exception {

  }

  @SuppressWarnings("serial")
  private static class AnotherOddException extends Exception {

  }

}
