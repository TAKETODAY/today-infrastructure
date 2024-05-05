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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Locale;

import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.support.StaticMessageSource;
import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.MethodNotAllowedException;
import cn.taketoday.web.ResponseStatusException;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 21:49
 */
class ResponseStatusExceptionHandlerTests {

  private final ResponseStatusExceptionHandler exceptionHandler = new ResponseStatusExceptionHandler();

  private final HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "");

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @BeforeEach
  public void setup() {
    exceptionHandler.setWarnLogCategory(exceptionHandler.getClass().getName());
  }

  @Test
  public void statusCode() throws Exception {
    StatusCodeException ex = new StatusCodeException();

    Object mav = handleException(ex);
    assertResolved(mav, 400, null);
  }

  @Test
  public void statusCodeFromComposedResponseStatus() throws Exception {
    StatusCodeFromComposedResponseStatusException ex = new StatusCodeFromComposedResponseStatusException();
    Object mav = handleException(ex);
    assertResolved(mav, 400, null);
  }

  @Test
  public void statusCodeAndReason() throws Exception {
    StatusCodeAndReasonException ex = new StatusCodeAndReasonException();
    Object mav = handleException(ex);
    assertResolved(mav, 410, "You suck!");
  }

  @Test
  public void statusCodeAndReasonMessage() throws Exception {
    Locale locale = Locale.CHINESE;
    LocaleContextHolder.setLocale(locale);
    try {
      StaticMessageSource messageSource = new StaticMessageSource();
      messageSource.addMessage("gone.reason", locale, "Gone reason message");
      exceptionHandler.setMessageSource(messageSource);

      StatusCodeAndReasonMessageException ex = new StatusCodeAndReasonMessageException();
      handleException(ex);
      assertThat(response.getErrorMessage()).as("Invalid status reason").isEqualTo("Gone reason message");
    }
    finally {
      LocaleContextHolder.resetLocaleContext();
    }
  }

  @Test
  public void notAnnotated() throws Exception {
    Exception ex = new Exception();
    Object object = handleException(ex);
    Object mav = handleException(ex);
    assertThat(mav).as("Object returned").isNull();
  }

  @Nullable
  private Object handleException(Exception ex) throws Exception {
    ServletRequestContext context = new ServletRequestContext(null, request, response);
    try {
      return exceptionHandler.handleException(context, ex, null);
    }
    finally {
      context.flush();
    }
  }

  @Test // SPR-12903
  public void nestedException() throws Exception {
    Exception cause = new StatusCodeAndReasonMessageException();
    TypeMismatchException ex = new TypeMismatchException("value", ITestBean.class, cause);
    Object mav = handleException(ex);
    assertResolved(mav, 410, "gone.reason");
  }

  @Test
  public void responseStatusException() throws Exception {
    ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);
    Object mav = handleException(ex);
    assertResolved(mav, 400, null);
  }

  @Test  // SPR-15524
  public void responseStatusExceptionWithReason() throws Exception {
    ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "The reason");
    Object mav = handleException(ex);
    assertResolved(mav, 400, "The reason");
  }

  @Test
  void responseStatusExceptionWithHeaders() throws Exception {
    Exception ex = new MethodNotAllowedException(
            HttpMethod.GET, Arrays.asList(HttpMethod.POST, HttpMethod.PUT));

    Object mav = handleException(ex);

    assertResolved(mav, 405, "Request method 'GET' is not supported.");
    assertThat(response.getHeader(HttpHeaders.ALLOW)).isEqualTo("POST,PUT");
  }

  private void assertResolved(Object mav, int status, String reason) {
    assertThat(mav).isEqualTo(HandlerExceptionHandler.NONE_RETURN_VALUE);
//    assertThat(mav != null && mav.isEmpty()).as("No Empty Object returned").isTrue();
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(response.getErrorMessage()).isEqualTo(reason);
    assertThat(response.isCommitted()).isTrue();
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @SuppressWarnings("serial")
  private static class StatusCodeException extends Exception {
  }

  @ResponseStatus(code = HttpStatus.GONE, reason = "You suck!")
  @SuppressWarnings("serial")
  private static class StatusCodeAndReasonException extends Exception {
  }

  @ResponseStatus(code = HttpStatus.GONE, reason = "gone.reason")
  @SuppressWarnings("serial")
  private static class StatusCodeAndReasonMessageException extends Exception {
  }

  @ResponseStatus
  @Retention(RetentionPolicy.RUNTIME)
  @SuppressWarnings("unused")
  @interface ComposedResponseStatus {

    @AliasFor(annotation = ResponseStatus.class, attribute = "code")
    HttpStatus responseStatus() default HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @ComposedResponseStatus(responseStatus = HttpStatus.BAD_REQUEST)
  @SuppressWarnings("serial")
  private static class StatusCodeFromComposedResponseStatusException extends Exception {
  }

}
