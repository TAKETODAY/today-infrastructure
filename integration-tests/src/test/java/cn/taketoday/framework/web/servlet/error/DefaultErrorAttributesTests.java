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

package cn.taketoday.framework.web.servlet.error;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.framework.web.error.ErrorAttributeOptions;
import cn.taketoday.framework.web.error.ErrorAttributeOptions.Include;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.MapBindingResult;
import cn.taketoday.validation.ObjectError;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.servlet.ServletRequestContext;
import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultErrorAttributes}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Scott Frederick
 */
class DefaultErrorAttributesTests {

  private final DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  private final RequestContext webRequest = new ServletRequestContext(
          null, this.request, new MockHttpServletResponse());

  @Test
  void includeTimeStamp() {
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes.get("timestamp")).isInstanceOf(Date.class);
  }

  @Test
  void specificStatusCode() {
    this.request.setAttribute("jakarta.servlet.error.status_code", 404);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes.get("error")).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
    assertThat(attributes.get("status")).isEqualTo(404);
  }

  @Test
  void missingStatusCode() {
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes.get("error")).isEqualTo("None");
    assertThat(attributes.get("status")).isEqualTo(999);
  }

  @Test
  void mvcError() {
    RuntimeException ex = new RuntimeException("Test");
    Object modelAndView = this.errorAttributes.handleException(webRequest, ex, null);
    this.request.setAttribute("jakarta.servlet.error.exception", new RuntimeException("Ignored"));
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
    assertThat(this.webRequest.getAttribute(ErrorAttributes.ERROR_ATTRIBUTE))
            .isSameAs(ex);
    assertThat(modelAndView).isNull();
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void servletErrorWithMessage() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute("jakarta.servlet.error.exception", ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
    assertThat(this.webRequest.getAttribute(ErrorAttributes.ERROR_ATTRIBUTE))
            .isSameAs(ex);
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void servletErrorWithoutMessage() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute("jakarta.servlet.error.exception", ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
    assertThat(this.webRequest.getAttribute(ErrorAttributes.ERROR_ATTRIBUTE))
            .isSameAs(ex);
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes).doesNotContainKey("message");
  }

  @Test
  void servletMessageWithMessage() {
    this.request.setAttribute("jakarta.servlet.error.message", "Test");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void servletMessageWithoutMessage() {
    this.request.setAttribute("jakarta.servlet.error.message", "Test");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes).doesNotContainKey("message");
  }

  @Test
  void nullExceptionMessage() {
    this.request.setAttribute("jakarta.servlet.error.exception", new RuntimeException());
    this.request.setAttribute("jakarta.servlet.error.message", "Test");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void nullExceptionMessageAndServletMessage() {
    this.request.setAttribute("jakarta.servlet.error.exception", new RuntimeException());
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("No message available");
  }

  @Test
  void unwrapServletException() {
    RuntimeException ex = new RuntimeException("Test");
    ServletException wrapped = new ServletException(new ServletException(ex));
    this.request.setAttribute("jakarta.servlet.error.exception", wrapped);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(wrapped);
    assertThat(this.webRequest.getAttribute(ErrorAttributes.ERROR_ATTRIBUTE))
            .isSameAs(wrapped);
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void getError() {
    Error error = new OutOfMemoryError("Test error");
    this.request.setAttribute("jakarta.servlet.error.exception", error);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(error);
    assertThat(this.webRequest.getAttribute(ErrorAttributes.ERROR_ATTRIBUTE))
            .isSameAs(error);
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test error");
  }

  @Test
  void withBindingErrors() {
    BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
    bindingResult.addError(new ObjectError("c", "d"));
    Exception ex = new BindException(bindingResult);
    testBindingResult(bindingResult, ex, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
  }

  @Test
  void withoutBindingErrors() {
    BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
    bindingResult.addError(new ObjectError("c", "d"));
    Exception ex = new BindException(bindingResult);
    testBindingResult(bindingResult, ex, ErrorAttributeOptions.defaults());
  }

  @Test
  void withMethodArgumentNotValidExceptionBindingErrors() {
    Method method = ReflectionUtils.findMethod(String.class, "substring", int.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
    bindingResult.addError(new ObjectError("c", "d"));
    Exception ex = new MethodArgumentNotValidException(parameter, bindingResult);
    testBindingResult(bindingResult, ex, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
  }

  private void testBindingResult(BindingResult bindingResult, Exception ex, ErrorAttributeOptions options) {
    this.request.setAttribute("jakarta.servlet.error.exception", ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest, options);
    if (options.isIncluded(Include.MESSAGE)) {
      assertThat(attributes.get("message"))
              .isEqualTo("Validation failed for object='objectName'. Error count: 1");
    }
    else {
      assertThat(attributes).doesNotContainKey("message");
    }
    if (options.isIncluded(Include.BINDING_ERRORS)) {
      assertThat(attributes.get("errors")).isEqualTo(bindingResult.getAllErrors());
    }
    else {
      assertThat(attributes).doesNotContainKey("errors");
    }
  }

  @Test
  void withExceptionAttribute() {
    DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute("jakarta.servlet.error.exception", ex);
    Map<String, Object> attributes = errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.EXCEPTION, Include.MESSAGE));
    assertThat(attributes.get("exception")).isEqualTo(RuntimeException.class.getName());
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void withStackTraceAttribute() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute("jakarta.servlet.error.exception", ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.STACK_TRACE));
    assertThat(attributes.get("trace").toString()).startsWith("java.lang");
  }

  @Test
  void withoutStackTraceAttribute() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute("jakarta.servlet.error.exception", ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes).doesNotContainKey("trace");
  }

  @Test
  void path() {
    this.request.setAttribute("jakarta.servlet.error.request_uri", "path");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes.get("path")).isEqualTo("path");
  }

  @Test
  void whenGetMessageIsOverriddenThenMessageAttributeContainsValueReturnedFromIt() {
    Map<String, Object> attributes = new DefaultErrorAttributes() {

      @Override
      protected String getMessage(RequestContext webRequest, Throwable error) {
        return "custom message";
      }

    }.getErrorAttributes(this.webRequest, ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).containsEntry("message", "custom message");
  }

}
