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

package infra.web.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.beans.ConversionNotSupportedException;
import infra.beans.TypeMismatchException;
import infra.beans.testfixture.beans.TestBean;
import infra.core.MethodParameter;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.lang.Nullable;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.validation.BeanPropertyBindingResult;
import infra.web.HandlerExceptionHandler;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.HttpRequestMethodNotSupportedException;
import infra.web.RequestContext;
import infra.web.async.AsyncRequestTimeoutException;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.bind.MissingPathVariableException;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.RequestBindingException;
import infra.web.bind.resolver.MissingRequestPartException;
import infra.web.mock.MockRequestContext;
import infra.web.multipart.MaxUploadSizeExceededException;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/5/16 15:45
 */
class SimpleHandlerExceptionHandlerTests {

  private final SimpleHandlerExceptionHandler exceptionResolver = new SimpleHandlerExceptionHandler();

  private final HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "/");

  private final MockHttpResponseImpl response = new MockHttpResponseImpl();

  private final MockRequestContext context = new MockRequestContext(request, response);

  @BeforeEach
  void setup() {
    exceptionResolver.setWarnLogCategory(exceptionResolver.getClass().getName());
  }

  @Test
  void handleHttpRequestMethodNotSupported() throws Exception {
    HttpRequestMethodNotSupportedException ex =
            new HttpRequestMethodNotSupportedException("GET", Arrays.asList("POST", "PUT"));
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(405);
    assertThat(context.responseHeaders().getAllow()).as("Invalid Allow header").isEqualTo(Set.of(HttpMethod.POST, HttpMethod.PUT));
  }

  @Test
  void handleHttpMediaTypeNotSupported() throws Exception {
    HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(new MediaType("text", "plain"),
            Collections.singletonList(new MediaType("application", "pdf")));
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(415);
    assertThat(context.responseHeaders().getAccept()).as("Invalid Accept header").isEqualTo(List.of(MediaType.valueOf("application/pdf")));
  }

  @Test
  void patchHttpMediaTypeNotSupported() throws Exception {
    HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(new MediaType("text", "plain"),
            Collections.singletonList(new MediaType("application", "pdf")), HttpMethod.PATCH);
    HttpMockRequestImpl request = new HttpMockRequestImpl("PATCH", "/");
    MockRequestContext context1 = new MockRequestContext(request, response);
    Object mav = exceptionResolver.handleException(context1, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(415);
    assertThat(context1.responseHeaders().getAcceptPatch()).as("Invalid Accept header").isEqualTo(List.of(MediaType.valueOf("application/pdf")));
  }

  @Test
  void handleMissingPathVariable() throws Exception {
    Method method = getClass().getMethod("handle", String.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    MissingPathVariableException ex = new MissingPathVariableException("foo", parameter);
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(500);
    assertThat(response.getErrorMessage()).isEqualTo("Required path variable 'foo' is not present.");
  }

  @Test
  void handleMissingRequestParameter() throws Exception {
    MissingRequestParameterException ex = new MissingRequestParameterException("foo", "bar");
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
    assertThat(response.getErrorMessage()).isEqualTo("Required parameter 'foo' is not present.");
  }

  @Test
  void handleRequestBindingException() throws Exception {
    String message = "Missing required value - header, cookie, or pathvar";
    RequestBindingException ex = new RequestBindingException(message);
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
  }

  @Test
  void handleTypeMismatch() throws Exception {
    TypeMismatchException ex = new TypeMismatchException("foo", String.class);
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
  }

  @Test
  public void handleHttpMessageNotReadable() throws Exception {
    HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foo", null);
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
  }

  @Test
  void handleHttpMessageNotWritable() throws Exception {
    HttpMessageNotWritableException ex = new HttpMessageNotWritableException("foo");
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(500);
  }

  @Test
  void handleMethodArgumentNotValid() throws Exception {
    BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new TestBean(), "testBean");
    errors.rejectValue("name", "invalid");
    MethodParameter parameter = new MethodParameter(this.getClass().getMethod("handle", String.class), 0);
    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, errors);
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
  }

  @Test
  void handleMissingRequestPartException() throws Exception {
    MissingRequestPartException ex = new MissingRequestPartException("name");
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(400);
    assertThat(response.getErrorMessage()).contains("part");
    assertThat(response.getErrorMessage()).contains("name");
    assertThat(response.getErrorMessage()).contains("not present");
  }

  @Test
  void handleNoHandlerFoundException() throws Exception {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "/resource");
    HandlerNotFoundException ex = new HandlerNotFoundException(mockRequest.getMethod(), mockRequest.getRequestURI(), HttpHeaders.empty());
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(404);
  }

//  @Test
//  void handleNoResourceFoundException() {
//    NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/resource");
//    ModelAndView mav = exceptionResolver.handleException(context,  ex, null);
//    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
//    assertThat(mav.isEmpty()).as("No Empty ModelAndView returned").isTrue();
//    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(404);
//  }

  @Test
  void handleConversionNotSupportedException() throws Exception {
    ConversionNotSupportedException ex =
            new ConversionNotSupportedException(new Object(), String.class, new Exception());
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(500);

  }

  @Test
  public void handleAsyncRequestTimeoutException() throws Exception {
    Exception ex = new AsyncRequestTimeoutException();
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(503);
  }

  @Test
  void handleMaxUploadSizeExceededException() throws Exception {
    MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1000);
    Object mav = exceptionResolver.handleException(context, ex, null);
    assertThat(mav).as("No ModelAndView returned").isNotNull().isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
    assertThat(response.getStatus()).as("Invalid status code").isEqualTo(413);
    assertThat(response.getErrorMessage()).isEqualTo("Maximum upload size exceeded");
  }

  @Test
  void customModelAndView() throws Exception {
    ModelAndView expected = new ModelAndView();

    HandlerExceptionHandler resolver = new SimpleHandlerExceptionHandler() {

      @Override
      protected ModelAndView handleHttpRequestMethodNotSupported(
              HttpRequestMethodNotSupportedException ex, RequestContext requestContext, @Nullable Object handler) {
        return expected;
      }
    };

    HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET");

    Object actual = resolver.handleException(context, ex, null);
    assertThat(actual).isSameAs(expected);
  }

  @SuppressWarnings("unused")
  public void handle(String arg) {
  }

}