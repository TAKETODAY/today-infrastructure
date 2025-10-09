/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Locale;

import infra.context.support.StaticMessageSource;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 16:18
 */
class ErrorResponseTests {

  @Test
  void getDefaultTypeMessageCodeReturnsExpectedFormat() {
    String code = ErrorResponse.getDefaultTypeMessageCode(TestException.class);
    assertThat(code).isEqualTo("problemDetail.type.infra.web.ErrorResponseTests$TestException");
  }

  @Test
  void getDefaultTitleMessageCodeReturnsExpectedFormat() {
    String code = ErrorResponse.getDefaultTitleMessageCode(TestException.class);
    assertThat(code).isEqualTo("problemDetail.title.infra.web.ErrorResponseTests$TestException");
  }

  @Test
  void getDefaultDetailMessageCodeReturnsExpectedFormat() {
    String code = ErrorResponse.getDefaultDetailMessageCode(TestException.class, null);
    assertThat(code).isEqualTo("problemDetail.infra.web.ErrorResponseTests$TestException");
  }

  @Test
  void getDefaultDetailMessageCodeWithSuffixReturnsExpectedFormat() {
    String code = ErrorResponse.getDefaultDetailMessageCode(TestException.class, "validation");
    assertThat(code).isEqualTo("problemDetail.infra.web.ErrorResponseTests$TestException.validation");
  }

  @Test
  void createReturnsErrorResponseWithGivenParameters() {
    Throwable ex = new RuntimeException("test");
    HttpStatusCode status = HttpStatus.BAD_REQUEST;
    String detail = "Test detail";

    ErrorResponse errorResponse = ErrorResponse.create(ex, status, detail);

    assertThat(errorResponse.getStatusCode()).isEqualTo(status);
    assertThat(errorResponse.getBody().getDetail()).isEqualTo(detail);
    assertThat(errorResponse.getBody().getStatus()).isEqualTo(status.value());
  }

  @Test
  void builderWithThrowableStatusCodeAndDetailCreatesCorrectInstance() {
    Throwable ex = new IllegalArgumentException("invalid argument");
    HttpStatusCode status = HttpStatus.BAD_REQUEST;
    String detail = "Invalid request parameter";

    ErrorResponse.Builder builder = ErrorResponse.builder(ex, status, detail);
    ErrorResponse errorResponse = builder.build();

    assertThat(errorResponse.getStatusCode()).isEqualTo(status);
    assertThat(errorResponse.getBody().getDetail()).isEqualTo(detail);
    assertThat(errorResponse.getBody().getStatus()).isEqualTo(status.value());
  }

  @Test
  void builderWithCustomProblemDetailCreatesCorrectInstance() {
    Throwable ex = new IllegalStateException("state error");
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource conflict");

    ErrorResponse.Builder builder = ErrorResponse.builder(ex, problemDetail);
    ErrorResponse errorResponse = builder.build();

    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(errorResponse.getBody()).isSameAs(problemDetail);
  }

  @Test
  void builderHeaderMethodsAddHeadersCorrectly() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .header("X-Custom-Header", "custom-value")
            .header("X-Multi-Header", "value1", "value2")
            .build();

    HttpHeaders headers = errorResponse.getHeaders();
    assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("custom-value");
    assertThat(headers.get("X-Multi-Header")).containsExactly("value1", "value2");
  }

  @Test
  void builderHeadersConsumerManipulatesHeaders() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .headers(httpHeaders -> httpHeaders.add("X-Test", "test-value"))
            .build();

    assertThat(errorResponse.getHeaders().getFirst("X-Test")).isEqualTo("test-value");
  }

  @Test
  void builderTypeSetTitleAndInstanceSetProblemDetailFields() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    URI type = URI.create("https://example.com/problem-type");
    URI instance = URI.create("https://example.com/problem-instance");

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .type(type)
            .title("Bad Request Title")
            .instance(instance)
            .build();

    ProblemDetail body = errorResponse.getBody();
    assertThat(body.getType()).isEqualTo(type);
    assertThat(body.getTitle()).isEqualTo("Bad Request Title");
    assertThat(body.getInstance()).isEqualTo(instance);
  }

  @Test
  void builderDetailMessageCodesSetCorrectly() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .typeMessageCode("custom.type.code")
            .titleMessageCode("custom.title.code")
            .detailMessageCode("custom.detail.code")
            .build();

    assertThat(errorResponse.getTypeMessageCode()).isEqualTo("custom.type.code");
    assertThat(errorResponse.getTitleMessageCode()).isEqualTo("custom.title.code");
    assertThat(errorResponse.getDetailMessageCode()).isEqualTo("custom.detail.code");
  }

  @Test
  void builderDetailMessageArgumentsSetCorrectly() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    Object[] args = { "arg1", "arg2" };

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .detailMessageArguments(args)
            .build();

    assertThat(errorResponse.getDetailMessageArguments()).containsExactly(args);
  }

  @Test
  void builderPropertyAddsDynamicProperty() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .property("customKey", "customValue")
            .build();

    ProblemDetail body = errorResponse.getBody();
    assertThat(body.getProperties()).containsEntry("customKey", "customValue");
  }

  @Test
  void builderBuildWithMessageSourceResolvesTitleAndDetail() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    StaticMessageSource messageSource = new StaticMessageSource();
    Locale locale = Locale.ENGLISH;
    messageSource.addMessage("problemDetail.title.java.lang.RuntimeException", locale, "Runtime Error");
    messageSource.addMessage("problemDetail.java.lang.RuntimeException", locale, "Runtime exception occurred");

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .build(messageSource, locale);

    ProblemDetail body = errorResponse.getBody();
    assertThat(body.getTitle()).isEqualTo("Runtime Error");
    assertThat(body.getDetail()).isEqualTo("Runtime exception occurred");
  }

  @Test
  void getDefaultTypeMessageCode() {
    String code = ErrorResponse.getDefaultTypeMessageCode(RuntimeException.class);
    assertThat(code).isEqualTo("problemDetail.type.java.lang.RuntimeException");
  }

  @Test
  void getDefaultTitleMessageCode() {
    String code = ErrorResponse.getDefaultTitleMessageCode(IllegalArgumentException.class);
    assertThat(code).isEqualTo("problemDetail.title.java.lang.IllegalArgumentException");
  }

  @Test
  void getDefaultDetailMessageCodeWithoutSuffix() {
    String code = ErrorResponse.getDefaultDetailMessageCode(IllegalStateException.class, null);
    assertThat(code).isEqualTo("problemDetail.java.lang.IllegalStateException");
  }

  @Test
  void getDefaultDetailMessageCodeWithSuffix() {
    String code = ErrorResponse.getDefaultDetailMessageCode(IllegalStateException.class, "validation");
    assertThat(code).isEqualTo("problemDetail.java.lang.IllegalStateException.validation");
  }

  @Test
  void createFactoryMethod() {
    Throwable ex = new RuntimeException("test");
    HttpStatusCode status = HttpStatus.BAD_REQUEST;
    String detail = "Test detail";

    ErrorResponse errorResponse = ErrorResponse.create(ex, status, detail);

    assertThat(errorResponse.getStatusCode()).isEqualTo(status);
    assertThat(errorResponse.getBody().getDetail()).isEqualTo(detail);
    assertThat(errorResponse.getBody().getStatus()).isEqualTo(status.value());
  }

  @Test
  void builderWithStatusCodeAndDetail() {
    Throwable ex = new IllegalArgumentException("invalid argument");
    HttpStatusCode status = HttpStatus.BAD_REQUEST;
    String detail = "Invalid request";

    ErrorResponse errorResponse = ErrorResponse.builder(ex, status, detail).build();

    assertThat(errorResponse.getStatusCode()).isEqualTo(status);
    assertThat(errorResponse.getBody().getDetail()).isEqualTo(detail);
    assertThat(errorResponse.getBody().getStatus()).isEqualTo(status.value());
  }

  @Test
  void builderWithProblemDetail() {
    Throwable ex = new IllegalStateException("state error");
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Conflict detail");

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail).build();

    assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(errorResponse.getBody()).isSameAs(problemDetail);
    assertThat(errorResponse.getBody().getDetail()).isEqualTo("Conflict detail");
  }

  @Test
  void builderHeader() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .header("X-Custom-Header", "custom-value")
            .header("X-Multi-Value", "value1", "value2")
            .build();

    HttpHeaders headers = errorResponse.getHeaders();
    assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("custom-value");
    assertThat(headers.get("X-Multi-Value")).containsExactly("value1", "value2");
  }

  @Test
  void builderHeadersConsumer() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .headers(httpHeaders -> httpHeaders.add("X-Consumer-Header", "consumer-value"))
            .build();

    assertThat(errorResponse.getHeaders().getFirst("X-Consumer-Header")).isEqualTo("consumer-value");
  }

  @Test
  void builderHeadersMap() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    HttpHeaders existingHeaders = HttpHeaders.forWritable();
    existingHeaders.add("X-Existing-Header", "existing-value");

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .headers(existingHeaders)
            .build();

    assertThat(errorResponse.getHeaders().getFirst("X-Existing-Header")).isEqualTo("existing-value");
  }

  @Test
  void builderTypeTitleAndInstance() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    URI type = URI.create("https://example.com/problem");
    URI instance = URI.create("https://example.com/problem/1");

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .type(type)
            .title("Custom Title")
            .instance(instance)
            .build();

    ProblemDetail body = errorResponse.getBody();
    assertThat(body.getType()).isEqualTo(type);
    assertThat(body.getTitle()).isEqualTo("Custom Title");
    assertThat(body.getInstance()).isEqualTo(instance);
  }

  @Test
  void builderTypeMessageCode() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .typeMessageCode("custom.type.message.code")
            .build();

    assertThat(errorResponse.getTypeMessageCode()).isEqualTo("custom.type.message.code");
  }

  @Test
  void builderTitleMessageCode() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .titleMessageCode("custom.title.message.code")
            .build();

    assertThat(errorResponse.getTitleMessageCode()).isEqualTo("custom.title.message.code");
  }

  @Test
  void builderDetailAndDetailMessageCode() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .detail("Custom detail")
            .detailMessageCode("custom.detail.message.code")
            .build();

    assertThat(errorResponse.getBody().getDetail()).isEqualTo("Custom detail");
    assertThat(errorResponse.getDetailMessageCode()).isEqualTo("custom.detail.message.code");
  }

  @Test
  void builderDetailMessageArguments() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    Object[] arguments = { "arg1", "arg2" };
    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .detailMessageArguments(arguments)
            .build();

    assertThat(errorResponse.getDetailMessageArguments()).containsExactly(arguments);
  }

  @Test
  void builderProperty() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .property("customProperty", "customValue")
            .build();

    assertThat(errorResponse.getBody().getProperties()).containsEntry("customProperty", "customValue");
  }

  @Test
  void builderBuildWithMessageSource() {
    Throwable ex = new RuntimeException();
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    StaticMessageSource messageSource = new StaticMessageSource();
    Locale locale = Locale.ENGLISH;
    messageSource.addMessage("problemDetail.title.java.lang.RuntimeException", locale, "Runtime Error Title");
    messageSource.addMessage("problemDetail.java.lang.RuntimeException", locale, "Runtime Error Detail");

    ErrorResponse errorResponse = ErrorResponse.builder(ex, problemDetail)
            .build(messageSource, locale);

    ProblemDetail body = errorResponse.getBody();
    assertThat(body.getTitle()).isEqualTo("Runtime Error Title");
    assertThat(body.getDetail()).isEqualTo("Runtime Error Detail");
  }

  private static class TestException extends Exception {
  }

}