/*
 * Copyright 2012-present the original author or authors.
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

package infra.webmvc.error;

import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import infra.core.MethodParameter;
import infra.http.HttpStatus;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.ReflectionUtils;
import infra.validation.BindException;
import infra.validation.BindingResult;
import infra.validation.MapBindingResult;
import infra.validation.ObjectError;
import infra.web.RequestContext;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.mock.MockRequestContext;
import infra.web.server.ResponseStatusException;
import infra.web.util.WebUtils;
import infra.webmvc.error.ErrorAttributeOptions.Include;

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

  private final HttpMockRequestImpl request = new HttpMockRequestImpl();

  private final RequestContext webRequest = new MockRequestContext(
          null, this.request, new MockHttpResponseImpl());

  @Test
  void includeTimeStamp() {
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes.get("timestamp")).isInstanceOf(Instant.class);
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
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    var attributes = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(errorAttributes.getError(webRequest)).isSameAs(ex);
    assertThat(webRequest.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE)).isSameAs(ex);

    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void mockErrorWithMessage() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
    assertThat(this.webRequest.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
            .isSameAs(ex);
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void mockErrorWithoutMessage() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(ex);
    assertThat(this.webRequest.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
            .isSameAs(ex);
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes).doesNotContainKey("message");
  }

  @Test
  void mockMessageWithMessage() {
    this.request.setAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE, "Test");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void mockMessageWithoutMessage() {
    this.request.setAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE, "Test");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes).doesNotContainKey("message");
  }

  @Test
  void nullExceptionMessage() {
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new RuntimeException());
    this.request.setAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE, "Test");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void nullExceptionMessageAndMockMessage() {
    this.request.setAttribute("infra.mock.api.error.exception", new RuntimeException());
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).doesNotContainKey("exception");
    assertThat(attributes.get("message")).isEqualTo("No message available");
  }

  @Test
  void getError() {
    Assertions.setMaxStackTraceElementsDisplayed(1000);
    Error error = new OutOfMemoryError("Test error");
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, error);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(this.errorAttributes.getError(this.webRequest)).isSameAs(error);
    assertThat(this.webRequest.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE))
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
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
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
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    Map<String, Object> attributes = errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.EXCEPTION, Include.MESSAGE));
    assertThat(attributes.get("exception")).isEqualTo(RuntimeException.class.getName());
    assertThat(attributes.get("message")).isEqualTo("Test");
  }

  @Test
  void withStackTraceAttribute() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.STACK_TRACE));
    assertThat(attributes.get("trace").toString()).startsWith("java.lang");
  }

  @Test
  void withoutStackTraceAttribute() {
    RuntimeException ex = new RuntimeException("Test");
    this.request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes).doesNotContainKey("trace");
  }

  @Test
  void shouldIncludePathByDefault() {
    request.setRequestURI("path");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.defaults());
    assertThat(attributes.get("path")).isEqualTo("path");
  }

  @Test
  void shouldIncludePath() {
    request.setRequestURI("path");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of(Include.PATH));
    assertThat(attributes).containsEntry("path", "path");
  }

  @Test
  void shouldExcludePath() {
    this.request.setAttribute("infra.mock.api.error.request_uri", "path");
    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(this.webRequest,
            ErrorAttributeOptions.of());
    assertThat(attributes).doesNotContainEntry("path", "path");
  }

  @Test
  void whenGetMessageIsOverriddenThenMessageAttributeContainsValueReturnedFromIt() {
    Map<String, Object> attributes = new DefaultErrorAttributes() {

      @Override
      protected String getMessage(RequestContext request, @Nullable Throwable error) {
        return "custom message";
      }

    }.getErrorAttributes(this.webRequest, ErrorAttributeOptions.of(Include.MESSAGE));
    assertThat(attributes).containsEntry("message", "custom message");
  }

  @Test
  void extractBindingResultErrorsThatCausedAResponseStatusException() throws Exception {
    BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
    bindingResult.addError(new ObjectError("c", "d"));
    Exception ex = new BindException(bindingResult);
    ResponseStatusException invalid = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid", ex);
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, invalid);

    Map<String, Object> attributes = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));

    assertThat(attributes.get("message")).isEqualTo("Invalid");
    assertThat(attributes).containsEntry("errors", bindingResult.getAllErrors());
  }

}
