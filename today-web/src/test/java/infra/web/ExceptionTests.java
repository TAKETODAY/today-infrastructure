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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 15:35
 */
public class ExceptionTests {

  @Nested
  class HandlerAdapterNotFoundExceptionTests {

    @Test
    void constructorWithHandlerSetsMessageAndHandler() {
      Object handler = new Object();
      HandlerAdapterNotFoundException exception = new HandlerAdapterNotFoundException(handler);

      assertThat(exception.getMessage()).isEqualTo("No HandlerAdapter for handler: [" + handler + ']');
      assertThat(exception.getHandler()).isSameAs(handler);
    }

    @Test
    void constructorWithNullHandler() {
      HandlerAdapterNotFoundException exception = new HandlerAdapterNotFoundException(null);

      assertThat(exception.getMessage()).isEqualTo("No HandlerAdapter for handler: [null]");
      assertThat(exception.getHandler()).isNull();
    }

    @Test
    void constructorWithStringHandler() {
      String handler = "testHandler";
      HandlerAdapterNotFoundException exception = new HandlerAdapterNotFoundException(handler);

      assertThat(exception.getMessage()).isEqualTo("No HandlerAdapter for handler: [" + handler + ']');
      assertThat(exception.getHandler()).isEqualTo(handler);
    }

    @Test
    void exceptionExtendsInfraConfigurationException() {
      Object handler = new Object();
      HandlerAdapterNotFoundException exception = new HandlerAdapterNotFoundException(handler);

      assertThat(exception).isInstanceOf(InfraConfigurationException.class);
    }

  }

  @Nested
  class InternalServerExceptionTests {
    @Test
    void constructorWithMessageAndCause() {
      String message = "Internal server error occurred";
      Throwable cause = new RuntimeException("Root cause");

      InternalServerException exception = new InternalServerException(message, cause);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).isEqualTo("500 INTERNAL_SERVER_ERROR \"Internal server error occurred\"");
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithMessageOnly() {
      String message = "Internal server error";

      InternalServerException exception = new InternalServerException(message);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).isEqualTo("500 INTERNAL_SERVER_ERROR \"Internal server error\"");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithNoParameters() {
      InternalServerException exception = new InternalServerException();

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).isEqualTo("500 INTERNAL_SERVER_ERROR");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void failedFactoryMethodWithoutParameters() {
      InternalServerException exception = InternalServerException.failed();

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).isEqualTo("500 INTERNAL_SERVER_ERROR");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void failedFactoryMethodWithMessage() {
      String message = "Test error message";

      InternalServerException exception = InternalServerException.failed(message);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).isEqualTo("500 INTERNAL_SERVER_ERROR \"Test error message\"");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void failedFactoryMethodWithMessageAndCause() {
      String message = "Test error message";
      Throwable cause = new IllegalArgumentException("Cause");

      InternalServerException exception = InternalServerException.failed(message, cause);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getMessage()).isEqualTo("500 INTERNAL_SERVER_ERROR \"Test error message\"");
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void exceptionExtendsResponseStatusException() {
      InternalServerException exception = new InternalServerException();

      assertThat(exception).isInstanceOf(ResponseStatusException.class);
    }

  }

  @Nested
  class MethodNotAllowedExceptionTests {
    @Test
    void constructorWithHttpMethodAndSupportedMethods() {
      HttpMethod method = HttpMethod.POST;
      Set<HttpMethod> supportedMethods = Set.of(HttpMethod.GET, HttpMethod.HEAD);

      MethodNotAllowedException exception = new MethodNotAllowedException(method, supportedMethods);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      assertThat(exception.getHttpMethod()).isEqualTo("POST");
      assertThat(exception.getSupportedMethods()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.HEAD);
      assertThat(exception.getMessage()).isEqualTo("405 METHOD_NOT_ALLOWED \"Request method 'POST' is not supported.\"");
    }

    @Test
    void constructorWithStringMethodAndNullSupportedMethods() {
      String method = "PATCH";

      MethodNotAllowedException exception = new MethodNotAllowedException(method, null);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      assertThat(exception.getHttpMethod()).isEqualTo("PATCH");
      assertThat(exception.getSupportedMethods()).isEmpty();
      assertThat(exception.getMessage()).isEqualTo("405 METHOD_NOT_ALLOWED \"Request method 'PATCH' is not supported.\"");
    }

    @Test
    void constructorWithStringMethodAndEmptySupportedMethods() {
      String method = "DELETE";
      Set<HttpMethod> supportedMethods = Set.of();

      MethodNotAllowedException exception = new MethodNotAllowedException(method, supportedMethods);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      assertThat(exception.getHttpMethod()).isEqualTo("DELETE");
      assertThat(exception.getSupportedMethods()).isEmpty();
      assertThat(exception.getMessage()).isEqualTo("405 METHOD_NOT_ALLOWED \"Request method 'DELETE' is not supported.\"");
    }

    @Test
    void constructorWithNullMethodThrowsException() {
      Set<HttpMethod> supportedMethods = Set.of(HttpMethod.GET);

      assertThatIllegalArgumentException()
              .isThrownBy(() -> new MethodNotAllowedException((String) null, supportedMethods))
              .withMessage("'method' is required");
    }

    @Test
    void getHeadersWithSupportedMethods() {
      String method = "PUT";
      Set<HttpMethod> supportedMethods = Set.of(HttpMethod.GET, HttpMethod.POST);

      MethodNotAllowedException exception = new MethodNotAllowedException(method, supportedMethods);
      HttpHeaders headers = exception.getHeaders();

      assertThat(headers).isNotNull();
      assertThat(headers.getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
    }

    @Test
    void getHeadersWithoutSupportedMethods() {
      String method = "PATCH";
      Set<HttpMethod> supportedMethods = Set.of();

      MethodNotAllowedException exception = new MethodNotAllowedException(method, supportedMethods);
      HttpHeaders headers = exception.getHeaders();

      assertThat(headers).isNotNull();
      assertThat(headers).isEqualTo(HttpHeaders.empty());
    }

  }

  @Nested
  class ErrorResponseExceptionTests {

    @Test
    void constructorWithStatusAndCause() {
      HttpStatusCode status = HttpStatus.BAD_REQUEST;
      Throwable cause = new RuntimeException("test cause");
      ErrorResponseException exception = new ErrorResponseException(status, cause);

      assertThat(exception.getStatusCode()).isEqualTo(status);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getBody().getStatus()).isEqualTo(status.value());
      assertThat(exception.getHeaders()).isEmpty();
    }

    @Test
    void constructorWithProblemDetail() {
      HttpStatusCode status = HttpStatus.NOT_FOUND;
      ProblemDetail body = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
      body.setDetail("Custom detail");
      ErrorResponseException exception = new ErrorResponseException(status, body, null);

      assertThat(exception.getStatusCode()).isEqualTo(status);
      assertThat(exception.getBody()).isSameAs(body);
      assertThat(exception.getBody().getDetail()).isEqualTo("Custom detail");
    }

    @Test
    void constructorWithMessageSourceCodeAndArguments() {
      HttpStatusCode status = HttpStatus.BAD_REQUEST;
      ProblemDetail body = ProblemDetail.forStatus(status);
      String messageCode = "test.message.code";
      Object[] messageArguments = { "arg1", "arg2" };

      ErrorResponseException exception = new ErrorResponseException(
              status, body, null, messageCode, messageArguments);

      assertThat(exception.getStatusCode()).isEqualTo(status);
      assertThat(exception.getDetailMessageCode()).isEqualTo(messageCode);
      assertThat(exception.getDetailMessageArguments()).containsExactly(messageArguments);
    }

    @Test
    void settersModifyProblemDetailFields() {
      ErrorResponseException exception = new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR, null);
      ProblemDetail body = exception.getBody();

      URI type = URI.create("https://example.com/problem");
      String title = "Custom Title";
      String detail = "Custom Detail";
      URI instance = URI.create("https://example.com/problem/1");

      exception.setType(type);
      exception.setTitle(title);
      exception.setDetail(detail);
      exception.setInstance(instance);

      assertThat(body.getType()).isEqualTo(type);
      assertThat(body.getTitle()).isEqualTo(title);
      assertThat(body.getDetail()).isEqualTo(detail);
      assertThat(body.getInstance()).isEqualTo(instance);
    }

    @Test
    void getMessageContainsStatusAndBodyInfo() {
      ErrorResponseException exception = new ErrorResponseException(HttpStatus.BAD_REQUEST, null);
      exception.setDetail("Test detail");

      String message = exception.getMessage();

      assertThat(message).contains("400");
      assertThat(message).contains("BAD_REQUEST");
      assertThat(message).contains("Test detail");
    }

  }

  @Nested
  class ResponseStatusExceptionTests {
    @Test
    void constructorWithStatusOnly() {
      ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getReason()).isNull();
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getMessage()).isEqualTo("400 BAD_REQUEST");
    }

    @Test
    void constructorWithStatusAndReason() {
      String reason = "Invalid input";
      ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getReason()).isEqualTo(reason);
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getMessage()).isEqualTo("400 BAD_REQUEST \"Invalid input\"");
    }

    @Test
    void constructorWithStatusReasonAndCause() {
      String reason = "Invalid input";
      Throwable cause = new IllegalArgumentException("Input must be numeric");
      ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, reason, cause);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getReason()).isEqualTo(reason);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getMessage()).isEqualTo("400 BAD_REQUEST \"Invalid input\"");
    }

    @Test
    void constructorWithRawStatusCode() {
      String reason = "Not found";
      Throwable cause = new RuntimeException("Resource missing");
      ResponseStatusException exception = new ResponseStatusException(404, reason, cause);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(exception.getReason()).isEqualTo(reason);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getMessage()).isEqualTo("404 NOT_FOUND \"Not found\"");
    }

    @Test
    void getHeadersReturnsEmptyHeaders() {
      ResponseStatusException exception = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      HttpHeaders headers = exception.getHeaders();

      assertThat(headers).isNotNull();
      assertThat(headers.isEmpty()).isTrue();
    }

    @Test
    void updateAndGetBodyWithMessageSourceAndResolvableReason() {
      // This test verifies that when reason is a message code, it gets resolved via MessageSource
      // However, mocking MessageSource and Locale would be required for a complete test
      ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.code");

      assertThat(exception.getReason()).isEqualTo("error.code");
      assertThat(exception.getBody().getDetail()).isEqualTo("error.code");
    }

  }



}
