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

package cn.taketoday.framework.web.reactive.error;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.framework.web.error.ErrorAttributeOptions;
import cn.taketoday.framework.web.error.ErrorAttributeOptions.Include;
import cn.taketoday.framework.web.servlet.error.DefaultErrorAttributes;
import cn.taketoday.framework.web.servlet.error.ErrorAttributes;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.ServerCodecConfigurer;
import cn.taketoday.mock.http.server.reactive.MockServerHttpRequest;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.MapBindingResult;
import cn.taketoday.validation.ObjectError;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ResponseStatusException;
import cn.taketoday.web.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefaultErrorAttributes}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
//class DefaultErrorAttributesTests {
//
//  private static final ResponseStatusException NOT_FOUND = new ResponseStatusException(HttpStatus.NOT_FOUND);
//
//  private DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();
//
//  private final List<HttpMessageReader<?>> readers = ServerCodecConfigurer.create().getReaders();
//
//  @Test
//  void missingExceptionAttribute() {
//    RequestContext exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
//    ServerRequest request = ServerRequest.create(exchange, this.readers);
//    assertThatIllegalStateException()
//            .isThrownBy(() -> this.errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults()))
//            .withMessageContaining("Missing exception attribute in ServerWebExchange");
//  }
//
//  @Test
//  void includeTimeStamp() {
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, NOT_FOUND),
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes.get("timestamp")).isInstanceOf(Date.class);
//  }
//
//  @Test
//  void defaultStatusCode() {
//    Error error = new OutOfMemoryError("Test error");
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, error),
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes.get("error")).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
//    assertThat(attributes.get("status")).isEqualTo(500);
//  }
//
//  @Test
//  void annotatedResponseStatusCode() {
//    Exception error = new CustomException();
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, error),
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes.get("error")).isEqualTo(HttpStatus.I_AM_A_TEAPOT.getReasonPhrase());
//    assertThat(attributes).doesNotContainKey("message");
//    assertThat(attributes.get("status")).isEqualTo(HttpStatus.I_AM_A_TEAPOT.value());
//  }
//
//  @Test
//  void annotatedResponseStatusCodeWithExceptionMessage() {
//    Exception error = new CustomException("Test Message");
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, error),
//            ErrorAttributeOptions.of(Include.MESSAGE));
//    assertThat(attributes.get("error")).isEqualTo(HttpStatus.I_AM_A_TEAPOT.getReasonPhrase());
//    assertThat(attributes.get("message")).isEqualTo("Test Message");
//    assertThat(attributes.get("status")).isEqualTo(HttpStatus.I_AM_A_TEAPOT.value());
//  }
//
//  @Test
//  void annotatedResponseStatusCodeWithCustomReasonPhrase() {
//    Exception error = new Custom2Exception();
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, error),
//            ErrorAttributeOptions.of(Include.MESSAGE));
//    assertThat(attributes.get("error")).isEqualTo(HttpStatus.I_AM_A_TEAPOT.getReasonPhrase());
//    assertThat(attributes.get("status")).isEqualTo(HttpStatus.I_AM_A_TEAPOT.value());
//    assertThat(attributes.get("message")).isEqualTo("Nope!");
//  }
//
//  @Test
//  void includeStatusCode() {
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, NOT_FOUND),
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes.get("error")).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
//    assertThat(attributes.get("status")).isEqualTo(404);
//  }
//
//  @Test
//  void getError() {
//    Error error = new OutOfMemoryError("Test error");
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    ServerRequest serverRequest = buildServerRequest(request, error);
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(serverRequest,
//            ErrorAttributeOptions.of(Include.MESSAGE));
//    assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
//    assertThat(attributes.get("exception")).isNull();
//    assertThat(attributes.get("message")).isEqualTo("Test error");
//  }
//
//  @Test
//  void excludeMessage() {
//    Error error = new OutOfMemoryError("Test error");
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    ServerRequest serverRequest = buildServerRequest(request, error);
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(serverRequest,
//            ErrorAttributeOptions.defaults());
//    assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
//    assertThat(attributes).doesNotContainKey("message");
//  }
//
//  @Test
//  void includeException() {
//    RuntimeException error = new RuntimeException("Test");
//    this.errorAttributes = new DefaultErrorAttributes();
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    ServerRequest serverRequest = buildServerRequest(request, error);
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(serverRequest,
//            ErrorAttributeOptions.of(Include.EXCEPTION, Include.MESSAGE));
//    assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
//    assertThat(serverRequest.attribute(ErrorAttributes.ERROR_ATTRIBUTE)).containsSame(error);
//    assertThat(attributes.get("exception")).isEqualTo(RuntimeException.class.getName());
//    assertThat(attributes.get("message")).isEqualTo("Test");
//  }
//
//  @Test
//  void processResponseStatusException() {
//    RuntimeException nested = new RuntimeException("Test");
//    ResponseStatusException error = new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid request", nested);
//    this.errorAttributes = new DefaultErrorAttributes();
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    ServerRequest serverRequest = buildServerRequest(request, error);
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(serverRequest,
//            ErrorAttributeOptions.of(Include.EXCEPTION, Include.MESSAGE));
//    assertThat(attributes.get("status")).isEqualTo(400);
//    assertThat(attributes.get("message")).isEqualTo("invalid request");
//    assertThat(attributes.get("exception")).isEqualTo(RuntimeException.class.getName());
//    assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
//    assertThat(serverRequest.attribute(ErrorAttributes.ERROR_ATTRIBUTE)).containsSame(error);
//  }
//
//  @Test
//  void processResponseStatusExceptionWithNoNestedCause() {
//    ResponseStatusException error = new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE,
//            "could not process request");
//    this.errorAttributes = new DefaultErrorAttributes();
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    ServerRequest serverRequest = buildServerRequest(request, error);
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(serverRequest,
//            ErrorAttributeOptions.of(Include.EXCEPTION, Include.MESSAGE));
//    assertThat(attributes.get("status")).isEqualTo(406);
//    assertThat(attributes.get("message")).isEqualTo("could not process request");
//    assertThat(attributes.get("exception")).isEqualTo(ResponseStatusException.class.getName());
//    assertThat(this.errorAttributes.getError(serverRequest)).isSameAs(error);
//    assertThat(serverRequest.attribute(ErrorAttributes.ERROR_ATTRIBUTE)).containsSame(error);
//  }
//
//  @Test
//  void notIncludeTrace() {
//    RuntimeException ex = new RuntimeException("Test");
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, ex),
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes.get("trace")).isNull();
//  }
//
//  @Test
//  void includeTrace() {
//    RuntimeException ex = new RuntimeException("Test");
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, ex),
//            ErrorAttributeOptions.of(Include.STACK_TRACE));
//    assertThat(attributes.get("trace").toString()).startsWith("java.lang");
//  }
//
//  @Test
//  void includePath() {
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, NOT_FOUND),
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes.get("path")).isEqualTo("/test");
//  }
//
//  @Test
//  void includeLogPrefix() {
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    ServerRequest serverRequest = buildServerRequest(request, NOT_FOUND);
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(serverRequest,
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes.get("requestId")).isEqualTo(serverRequest.exchange().getRequest().getId());
//  }
//
//  @Test
//  void extractBindingResultErrors() throws Exception {
//    Method method = getClass().getDeclaredMethod("method", String.class);
//    MethodParameter stringParam = new MethodParameter(method, 0);
//    BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
//    bindingResult.addError(new ObjectError("c", "d"));
//    Exception ex = new WebExchangeBindException(stringParam, bindingResult);
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, ex),
//            ErrorAttributeOptions.of(Include.MESSAGE, Include.BINDING_ERRORS));
//    assertThat(attributes.get("message")).asString()
//            .startsWith("Validation failed for argument at index 0 in method: "
//                    + "int cn.taketoday.framework.web.reactive.error.DefaultErrorAttributesTests"
//                    + ".method(java.lang.String), with 1 error(s)");
//    assertThat(attributes.get("errors")).isEqualTo(bindingResult.getAllErrors());
//  }
//
//  @Test
//  void extractBindingResultErrorsExcludeMessageAndErrors() throws Exception {
//    Method method = getClass().getDeclaredMethod("method", String.class);
//    MethodParameter stringParam = new MethodParameter(method, 0);
//    BindingResult bindingResult = new MapBindingResult(Collections.singletonMap("a", "b"), "objectName");
//    bindingResult.addError(new ObjectError("c", "d"));
//    Exception ex = new WebExchangeBindException(stringParam, bindingResult);
//    MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
//    Map<String, Object> attributes = this.errorAttributes.getErrorAttributes(buildServerRequest(request, ex),
//            ErrorAttributeOptions.defaults());
//    assertThat(attributes).doesNotContainKey("message");
//    assertThat(attributes).doesNotContainKey("errors");
//  }
//
//  private RequestContext buildServerRequest(MockServerHttpRequest request, Throwable error) {
//    RequestContext exchange = MockServerWebExchange.from(request);
//    this.errorAttributes.storeErrorInformation(error, exchange);
//    return new (exchange, this.readers);
//  }
//
//  int method(String firstParam) {
//    return 42;
//  }
//
//  @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
//  static class CustomException extends RuntimeException {
//
//    CustomException() {
//    }
//
//    CustomException(String message) {
//      super(message);
//    }
//
//  }
//
//  @ResponseStatus(value = HttpStatus.I_AM_A_TEAPOT, reason = "Nope!")
//  static class Custom2Exception extends RuntimeException {
//
//  }
//
//}
