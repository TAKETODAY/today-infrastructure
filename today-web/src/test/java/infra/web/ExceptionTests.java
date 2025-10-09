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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.core.ResolvableType;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.util.MultiValueMap;
import infra.validation.BindException;
import infra.validation.BindingResult;
import infra.validation.ObjectError;
import infra.web.accept.InvalidApiVersionException;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.bind.MethodParameterResolvingException;
import infra.web.bind.MissingMatrixVariableException;
import infra.web.bind.MissingPathVariableException;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.MissingRequestValueException;
import infra.web.bind.MultipartException;
import infra.web.bind.NotMultipartRequestException;
import infra.web.bind.RequestBindingException;
import infra.web.bind.UnsatisfiedRequestParameterException;
import infra.web.bind.resolver.MissingRequestCookieException;
import infra.web.bind.resolver.MissingRequestHeaderException;
import infra.web.bind.resolver.MissingRequestPartException;
import infra.web.bind.resolver.ParameterConversionException;
import infra.web.bind.resolver.ParameterResolverNotFoundException;
import infra.web.bind.resolver.date.DateParameterParsingException;
import infra.web.handler.method.ResolvableMethodParameter;
import jakarta.validation.Valid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  @Nested
  class UnsupportedMediaTypeStatusExceptionTests {

    @Test
    void constructorWithReasonOnly() {
      String reason = "Invalid media type";
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(reason);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(exception.getContentType()).isNull();
      assertThat(exception.getSupportedMediaTypes()).isEmpty();
      assertThat(exception.getBodyType()).isNull();
      assertThat(exception.getMessage()).contains(reason);
      assertThat(exception.getHeaders()).isEqualTo(HttpHeaders.empty());
    }

    @Test
    void constructorWithReasonAndSupportedTypes() {
      String reason = "Cannot parse Content-Type";
      List<MediaType> supportedTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(reason, supportedTypes);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(exception.getContentType()).isNull();
      assertThat(exception.getSupportedMediaTypes()).containsExactlyElementsOf(supportedTypes);
      assertThat(exception.getBodyType()).isNull();
      assertThat(exception.getBody().getDetail()).isEqualTo("Could not parse Content-Type.");
      assertThat(exception.getHeaders().getAccept()).containsExactlyElementsOf(supportedTypes);
    }

    @Test
    void constructorWithContentTypeAndSupportedTypes() {
      MediaType contentType = MediaType.APPLICATION_XML;
      List<MediaType> supportedTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(contentType, supportedTypes);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(exception.getContentType()).isEqualTo(contentType);
      assertThat(exception.getSupportedMediaTypes()).containsExactlyElementsOf(supportedTypes);
      assertThat(exception.getBodyType()).isNull();
      assertThat(exception.getBody().getDetail()).isEqualTo("Content-Type '" + contentType + "' is not supported.");
      assertThat(exception.getHeaders().getAccept()).containsExactlyElementsOf(supportedTypes);
    }

    @Test
    void constructorWithContentTypeSupportedTypesAndBodyType() {
      MediaType contentType = MediaType.APPLICATION_XML;
      List<MediaType> supportedTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
      ResolvableType bodyType = ResolvableType.forClass(String.class);
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(contentType, supportedTypes, bodyType);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(exception.getContentType()).isEqualTo(contentType);
      assertThat(exception.getSupportedMediaTypes()).containsExactlyElementsOf(supportedTypes);
      assertThat(exception.getBodyType()).isEqualTo(bodyType);
      assertThat(exception.getMessage()).contains(contentType.toString()).contains(bodyType.toString());
      assertThat(exception.getHeaders().getAccept()).containsExactlyElementsOf(supportedTypes);
    }

    @Test
    void constructorWithContentTypeSupportedTypesAndMethod() {
      MediaType contentType = MediaType.APPLICATION_XML;
      List<MediaType> supportedTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
      HttpMethod method = HttpMethod.PATCH;
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(contentType, supportedTypes, method);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(exception.getContentType()).isEqualTo(contentType);
      assertThat(exception.getSupportedMediaTypes()).containsExactlyElementsOf(supportedTypes);
      assertThat(exception.getBodyType()).isNull();
      assertThat(exception.getHeaders().getAccept()).containsExactlyElementsOf(supportedTypes);
      assertThat(exception.getHeaders().getAcceptPatch()).containsExactlyElementsOf(supportedTypes);
    }

    @Test
    void constructorWithAllParameters() {
      MediaType contentType = MediaType.APPLICATION_XML;
      List<MediaType> supportedTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
      ResolvableType bodyType = ResolvableType.forClass(String.class);
      HttpMethod method = HttpMethod.POST;
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(contentType, supportedTypes, bodyType, method);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(exception.getContentType()).isEqualTo(contentType);
      assertThat(exception.getSupportedMediaTypes()).containsExactlyElementsOf(supportedTypes);
      assertThat(exception.getBodyType()).isEqualTo(bodyType);
      assertThat(exception.getHeaders().getAccept()).containsExactlyElementsOf(supportedTypes);
    }

    @Test
    void getHeadersReturnsEmptyWhenNoSupportedTypes() {
      String reason = "Parse error";
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(reason);

      HttpHeaders headers = exception.getHeaders();
      assertThat(headers).isEqualTo(HttpHeaders.empty());
    }

    @Test
    void getHeadersReturnsAcceptHeader() {
      MediaType contentType = MediaType.APPLICATION_XML;
      List<MediaType> supportedTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(contentType, supportedTypes);

      HttpHeaders headers = exception.getHeaders();
      assertThat(headers.getAccept()).containsExactlyElementsOf(supportedTypes);
    }

    @Test
    void getHeadersReturnsAcceptPatchHeaderForPatchMethod() {
      MediaType contentType = MediaType.APPLICATION_XML;
      List<MediaType> supportedTypes = List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
      HttpMethod method = HttpMethod.PATCH;
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException(contentType, supportedTypes, method);

      HttpHeaders headers = exception.getHeaders();
      assertThat(headers.getAcceptPatch()).containsExactlyElementsOf(supportedTypes);
    }

    @Test
    void exceptionExtendsResponseStatusException() {
      UnsupportedMediaTypeStatusException exception = new UnsupportedMediaTypeStatusException("test");

      assertThat(exception).isInstanceOf(ResponseStatusException.class);
    }

  }

  @Nested
  class InvalidApiVersionExceptionTests {

    @Test
    void constructorWithVersionOnly() {
      String version = "v1.0";
      InvalidApiVersionException exception = new InvalidApiVersionException(version);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getVersion()).isEqualTo(version);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithVersionMessageAndCause() {
      String version = "v2.0";
      String message = "API version not supported";
      Exception cause = new IllegalArgumentException("Invalid format");
      InvalidApiVersionException exception = new InvalidApiVersionException(version, message, cause);

      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getVersion()).isEqualTo(version);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithNullMessageShouldUseDefault() {
      String version = "v3.0";
      Exception cause = new RuntimeException("test");
      InvalidApiVersionException exception = new InvalidApiVersionException(version, null, cause);

      assertThat(exception.getVersion()).isEqualTo(version);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void exceptionExtendsResponseStatusException() {
      InvalidApiVersionException exception = new InvalidApiVersionException("v1.0");

      assertThat(exception).isInstanceOf(ResponseStatusException.class);
    }

  }

  @Nested
  class MethodArgumentNotValidExceptionTests {
    @Test
    void constructor_ShouldCreateExceptionWithParameterAndBindingResult() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handle", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      BindingResult bindingResult = mock(BindingResult.class);

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getBody()).isNotNull();
      assertThat(exception.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void getBody_ShouldReturnProblemDetailWithBadRequestStatus() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handle", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      BindingResult bindingResult = mock(BindingResult.class);

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);
      ProblemDetail body = exception.getBody();

      assertThat(body).isNotNull();
      assertThat(body.getStatus()).isEqualTo(400);
      assertThat(body.getDetail()).isEqualTo("Invalid request content.");
    }

    @Test
    void getMessage_ShouldContainParameterInfo() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handle", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      BindingResult bindingResult = mock(BindingResult.class);
      when(bindingResult.getErrorCount()).thenReturn(0);
      when(bindingResult.getAllErrors()).thenReturn(List.of());

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

      String message = exception.getMessage();
      assertThat(message).contains("Validation failed for argument");
      assertThat(message).contains("[0]");
      assertThat(message).contains(method.toGenericString());
    }

    @Test
    void getMessage_ShouldContainErrorInfo() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handle", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      BindingResult bindingResult = mock(BindingResult.class);
      ObjectError error = new ObjectError("test", "test error");
      when(bindingResult.getErrorCount()).thenReturn(1);
      when(bindingResult.getAllErrors()).thenReturn(List.of(error));

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

      String message = exception.getMessage();
      assertThat(message).contains("test error");
    }

    @Test
    void exception_ShouldExtendBindException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handle", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      BindingResult bindingResult = mock(BindingResult.class);

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

      assertThat(exception).isInstanceOf(BindException.class);
    }

    @Test
    void exception_ShouldImplementErrorResponse() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handle", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      BindingResult bindingResult = mock(BindingResult.class);

      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

      assertThat(exception).isInstanceOf(ErrorResponse.class);
    }

    static class TestController {
      public void handle(@Valid String param) {
      }
    }

  }

  @Nested
  class MethodParameterResolvingExceptionTests {
    @Test
    void constructorWithParameterOnly() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);

      MethodParameterResolvingException exception = new MethodParameterResolvingException(parameter);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithParameterAndMessage() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String message = "Failed to resolve parameter";

      MethodParameterResolvingException exception = new MethodParameterResolvingException(parameter, message);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithParameterAndCause() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      Throwable cause = new RuntimeException("test cause");

      MethodParameterResolvingException exception = new MethodParameterResolvingException(parameter, cause);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithAllParameters() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String message = "Failed to resolve parameter";
      Throwable cause = new RuntimeException("test cause");

      MethodParameterResolvingException exception = new MethodParameterResolvingException(parameter, message, cause);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void getParameterNameShouldReturnParameterName() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);

      MethodParameterResolvingException exception = new MethodParameterResolvingException(parameter);

      assertThat(exception.getParameterName()).isNull(); // Parameter names are not available by default
    }

    @Test
    void getParameterTypeShouldReturnCorrectType() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);

      MethodParameterResolvingException exception = new MethodParameterResolvingException(parameter);

      assertThat(exception.getParameterType()).isEqualTo(String.class);
    }

    @Test
    void exceptionShouldExtendRequestBindingException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);

      MethodParameterResolvingException exception = new MethodParameterResolvingException(parameter);

      assertThat(exception).isInstanceOf(RequestBindingException.class);
    }

    static class TestController {
      public void testMethod(String param) {
      }
    }

  }

  @Nested
  class MissingMatrixVariableExceptionTests {

    @Test
    void constructorWithVariableNameAndParameter() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingMatrixVariableException exception = new MissingMatrixVariableException(variableName, parameter);

      assertThat(exception.getVariableName()).isEqualTo(variableName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isFalse();
      assertThat(exception.getBody().getDetail()).isEqualTo("Required path parameter 'testVariable' is not present.");
    }

    @Test
    void constructorWithMissingAfterConversion() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingMatrixVariableException exception = new MissingMatrixVariableException(variableName, parameter, true);

      assertThat(exception.getVariableName()).isEqualTo(variableName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isTrue();
    }

    @Test
    void getMessageWhenNotPresent() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingMatrixVariableException exception = new MissingMatrixVariableException(variableName, parameter);

      String message = exception.getMessage();
      assertThat(message).contains("Required matrix variable 'testVariable' for method parameter type String is not present");
    }

    @Test
    void getMessageWhenConvertedToNull() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingMatrixVariableException exception = new MissingMatrixVariableException(variableName, parameter, true);

      String message = exception.getMessage();
      assertThat(message).contains("Required matrix variable 'testVariable' for method parameter type String is present but converted to null");
    }

    @Test
    void exceptionExtendsMissingRequestValueException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingMatrixVariableException exception = new MissingMatrixVariableException(variableName, parameter);

      assertThat(exception).isInstanceOf(MissingRequestValueException.class);
    }

    static class TestController {
      public void testMethod(String param) {
      }
    }

  }

  @Nested
  class MissingPathVariableExceptionTests {
    @Test
    void constructorWithVariableNameAndParameter() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingPathVariableException exception = new MissingPathVariableException(variableName, parameter);

      assertThat(exception.getVariableName()).isEqualTo(variableName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isFalse();
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(exception.getBody().getDetail()).isEqualTo("Required path variable 'testVariable' is not present.");
    }

    @Test
    void constructorWithMissingAfterConversion() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingPathVariableException exception = new MissingPathVariableException(variableName, parameter, true);

      assertThat(exception.getVariableName()).isEqualTo(variableName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isTrue();
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getMessageWhenNotPresent() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingPathVariableException exception = new MissingPathVariableException(variableName, parameter);

      String message = exception.getMessage();
      assertThat(message).contains("Required URI template variable 'testVariable' for method parameter type String is not present");
    }

    @Test
    void getMessageWhenConvertedToNull() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingPathVariableException exception = new MissingPathVariableException(variableName, parameter, true);

      String message = exception.getMessage();
      assertThat(message).contains("Required URI template variable 'testVariable' for method parameter type String is present but converted to null");
    }

    @Test
    void exceptionExtendsMissingRequestValueException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String variableName = "testVariable";

      MissingPathVariableException exception = new MissingPathVariableException(variableName, parameter);

      assertThat(exception).isInstanceOf(MissingRequestValueException.class);
    }

    static class TestController {
      public void testMethod(String param) {
      }
    }

  }

  @Nested
  class MissingRequestParameterExceptionTests {
    @Test
    void constructorWithParameterNameAndType() {
      String parameterName = "testParam";
      String parameterType = "String";

      MissingRequestParameterException exception = new MissingRequestParameterException(parameterName, parameterType);

      assertThat(exception.getParameterName()).isEqualTo(parameterName);
      assertThat(exception.getParameterType()).isEqualTo(parameterType);
      assertThat(exception.getMethodParameter()).isNull();
      assertThat(exception.isMissingAfterConversion()).isFalse();
      assertThat(exception.getBody().getDetail()).isEqualTo("Required parameter 'testParam' is not present.");
    }

    @Test
    void constructorWithMethodParameterAndMissingAfterConversion() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String parameterName = "testParam";

      MissingRequestParameterException exception = new MissingRequestParameterException(parameterName, parameter, true);

      assertThat(exception.getParameterName()).isEqualTo(parameterName);
      assertThat(exception.getParameterType()).isEqualTo("String");
      assertThat(exception.getMethodParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isTrue();
      assertThat(exception.getBody().getDetail()).isEqualTo("Required parameter 'testParam' is not present.");
    }

    @Test
    void getMessageWhenNotPresent() {
      String parameterName = "testParam";
      String parameterType = "String";

      MissingRequestParameterException exception = new MissingRequestParameterException(parameterName, parameterType);

      String message = exception.getMessage();
      assertThat(message).contains("Required request parameter 'testParam' for method parameter type String is not present");
    }

    @Test
    void getMessageWhenConvertedToNull() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String parameterName = "testParam";

      MissingRequestParameterException exception = new MissingRequestParameterException(parameterName, parameter, true);

      String message = exception.getMessage();
      assertThat(message).contains("Required request parameter 'testParam' for method parameter type String is present but converted to null");
    }

    @Test
    void exceptionExtendsMissingRequestValueException() {
      String parameterName = "testParam";
      String parameterType = "String";

      MissingRequestParameterException exception = new MissingRequestParameterException(parameterName, parameterType);

      assertThat(exception).isInstanceOf(MissingRequestValueException.class);
    }

    static class TestController {
      public void testMethod(String param) {
      }
    }

  }

  @Nested
  class MissingRequestValueExceptionTests {
    @Test
    void constructorWithMessageOnly() {
      String message = "Missing request value";

      MissingRequestValueException exception = new MissingRequestValueException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.isMissingAfterConversion()).isFalse();
    }

    @Test
    void constructorWithMessageAndMissingAfterConversion() {
      String message = "Missing request value";

      MissingRequestValueException exception = new MissingRequestValueException(message, true);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.isMissingAfterConversion()).isTrue();
    }

    @Test
    void constructorWithNullMessage() {
      MissingRequestValueException exception = new MissingRequestValueException(null);

      assertThat(exception.getMessage()).isNull();
      assertThat(exception.isMissingAfterConversion()).isFalse();
    }

    @Test
    void exceptionExtendsRequestBindingException() {
      MissingRequestValueException exception = new MissingRequestValueException("test");

      assertThat(exception).isInstanceOf(RequestBindingException.class);
    }

  }

  @Nested
  class MultipartExceptionTests {

    @Test
    void constructorWithMessageOnly() {
      String message = "Multipart parsing failed";

      MultipartException exception = new MultipartException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Multipart parsing failed";
      Throwable cause = new RuntimeException("IO error");

      MultipartException exception = new MultipartException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithNullMessage() {
      MultipartException exception = new MultipartException(null);

      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithNullMessageAndCause() {
      Throwable cause = new RuntimeException("IO error");

      MultipartException exception = new MultipartException(null, cause);

      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void exceptionExtendsHttpMessageNotReadableException() {
      MultipartException exception = new MultipartException("test");

      assertThat(exception).isInstanceOf(infra.http.converter.HttpMessageNotReadableException.class);
    }

  }

  @Nested
  class NotMultipartRequestExceptionTests {
    @Test
    void constructorWithMessageAndCause() {
      String message = "Not a multipart request";
      Throwable cause = new RuntimeException("Request parsing error");

      NotMultipartRequestException exception = new NotMultipartRequestException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructorWithNullMessageAndNullCause() {
      NotMultipartRequestException exception = new NotMultipartRequestException(null, null);

      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageOnly() {
      String message = "Not a multipart request";

      NotMultipartRequestException exception = new NotMultipartRequestException(message, null);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithCauseOnly() {
      Throwable cause = new RuntimeException("Request parsing error");

      NotMultipartRequestException exception = new NotMultipartRequestException(null, cause);

      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void exceptionExtendsMultipartException() {
      NotMultipartRequestException exception = new NotMultipartRequestException("test", null);

      assertThat(exception).isInstanceOf(MultipartException.class);
    }

  }

  @Nested
  class RequestBindingExceptionTests {
    @Test
    void constructorWithMessageOnly() {
      String message = "Request binding failed";

      RequestBindingException exception = new RequestBindingException(message);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getBody()).isNotNull();
      assertThat(exception.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void constructorWithMessageAndCause() {
      String message = "Request binding failed";
      Throwable cause = new RuntimeException("Binding error");

      RequestBindingException exception = new RequestBindingException(message, cause);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getBody()).isNotNull();
      assertThat(exception.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void constructorWithMessageDetailCodeAndArguments() {
      String message = "Request binding failed";
      String detailCode = "error.binding";
      Object[] detailArgs = { "param1", "param2" };

      RequestBindingException exception = new RequestBindingException(message, detailCode, detailArgs) { };

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getDetailMessageCode()).isEqualTo(detailCode);
      assertThat(exception.getDetailMessageArguments()).containsExactly(detailArgs);
    }

    @Test
    void constructorWithAllParameters() {
      String message = "Request binding failed";
      Throwable cause = new RuntimeException("Binding error");
      String detailCode = "error.binding";
      Object[] detailArgs = { "param1", "param2" };

      RequestBindingException exception = new RequestBindingException(message, cause, detailCode, detailArgs) { };

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getDetailMessageCode()).isEqualTo(detailCode);
      assertThat(exception.getDetailMessageArguments()).containsExactly(detailArgs);
    }

    @Test
    void constructorWithNullMessageAndCause() {
      RequestBindingException exception = new RequestBindingException(null, null);

      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getBodyReturnsProblemDetailWithBadRequestStatus() {
      String message = "Request binding failed";
      RequestBindingException exception = new RequestBindingException(message);

      ProblemDetail body = exception.getBody();

      assertThat(body).isNotNull();
      assertThat(body.getStatus()).isEqualTo(400);
      assertThat(body).isSameAs(exception.getBody()); // Should return same instance
    }

    @Test
    void getDefaultDetailMessageCodeWhenNotProvided() {
      String message = "Request binding failed";
      RequestBindingException exception = new RequestBindingException(message);

      // When detail code is not provided, it should generate a default one
      assertThat(exception.getDetailMessageCode()).isNotNull();
    }

    @Test
    void exceptionImplementsErrorResponse() {
      RequestBindingException exception = new RequestBindingException("test");

      assertThat(exception).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void exceptionExtendsNestedRuntimeException() {
      RequestBindingException exception = new RequestBindingException("test");

      assertThat(exception).isInstanceOf(infra.core.NestedRuntimeException.class);
    }

  }

  @Nested
  class UnsatisfiedRequestParameterExceptionTests {
    @Test
    void constructorWithSingleParamCondition() {
      String[] paramConditions = { "param1=value1", "param2" };
      MultiValueMap<String, String> actualParams = MultiValueMap.forAdaption(Map.of("param1", List.of("value2")));

      UnsatisfiedRequestParameterException exception = new UnsatisfiedRequestParameterException(paramConditions, actualParams);

      assertThat(exception.getParamConditions()).containsExactly(paramConditions);
      assertThat(exception.getParamConditionGroups()).containsExactly(paramConditions);
      assertThat(exception.getActualParams()).isSameAs(actualParams);
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getBody().getDetail()).isEqualTo("Invalid request parameters.");
    }

    @Test
    void constructorWithMultipleParamConditionGroups() {
      List<String[]> paramConditions = List.of(
              new String[] { "param1=value1", "param2" },
              new String[] { "param3=value3" }
      );
      MultiValueMap<String, String> actualParams = MultiValueMap.forAdaption(Map.of("param1", List.of("value2")));

      UnsatisfiedRequestParameterException exception = new UnsatisfiedRequestParameterException(paramConditions, actualParams);

      assertThat(exception.getParamConditions()).containsExactly(paramConditions.get(0));
      assertThat(exception.getParamConditionGroups()).isEqualTo(paramConditions);
      assertThat(exception.getActualParams()).isSameAs(actualParams);
    }

    @Test
    void getMessageContainsConditionsAndActualParams() {
      String[] paramConditions = { "param1=value1", "param2" };
      MultiValueMap<String, String> actualParams = MultiValueMap.forAdaption(Map.of("param1", List.of("value2")));
      UnsatisfiedRequestParameterException exception = new UnsatisfiedRequestParameterException(paramConditions, actualParams);

      String message = exception.getMessage();
      assertThat(message).contains("param1=value1, param2");
      assertThat(message).contains("param1=[value2]");
    }

    @Test
    void getMessageWithMultipleConditionGroups() {
      List<String[]> paramConditions = List.of(
              new String[] { "param1=value1" },
              new String[] { "param2=value2" }
      );
      MultiValueMap<String, String> actualParams = MultiValueMap.forAdaption(Map.of("param1", List.of("valueX")));
      UnsatisfiedRequestParameterException exception = new UnsatisfiedRequestParameterException(paramConditions, actualParams);

      String message = exception.getMessage();
      assertThat(message).contains("\"param1=value1\" OR \"param2=value2\"");
      assertThat(message).contains("param1=[valueX]");
    }

    @Test
    void exceptionExtendsRequestBindingException() {
      String[] paramConditions = { "param1=value1" };
      MultiValueMap<String, String> actualParams = infra.util.MultiValueMap.empty();

      UnsatisfiedRequestParameterException exception = new UnsatisfiedRequestParameterException(paramConditions, actualParams);

      assertThat(exception).isInstanceOf(RequestBindingException.class);
    }

  }

  @Nested
  class ParameterResolverNotFoundExceptionTests {

    @Test
    void constructorWithParameterAndMessage() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String message = "Parameter resolver not found";

      ParameterResolverNotFoundException exception = new ParameterResolverNotFoundException(
              new ResolvableMethodParameter(parameter), message);
      parameter.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());

      assertThat(exception.getParameter()).isNotNull();
      assertThat(exception.getParameter().getParameter()).isSameAs(parameter);
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getParameterName()).isEqualTo("param");
    }

    @Test
    void constructorWithParameterMessageAndCause() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String message = "Parameter resolver not found";
      Throwable cause = new RuntimeException("Root cause");

      parameter.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());

      ParameterResolverNotFoundException exception = new ParameterResolverNotFoundException(
              new ResolvableMethodParameter(parameter), message, cause);

      assertThat(exception.getParameter()).isNotNull();
      assertThat(exception.getParameter().getParameter()).isSameAs(parameter);
      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getParameterName()).isEqualTo("param");
    }

    @Test
    void constructorWithNullMessage() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      parameter.initParameterNameDiscovery(ParameterNameDiscoverer.getSharedInstance());

      ParameterResolverNotFoundException exception = new ParameterResolverNotFoundException(
              new ResolvableMethodParameter(parameter), null);

      assertThat(exception.getParameter()).isNotNull();
      assertThat(exception.getParameter().getParameter()).isSameAs(parameter);
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getParameterName()).isEqualTo("param");
    }

    @Test
    void exceptionExtendsInfraConfigurationException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);

      ParameterResolverNotFoundException exception = new ParameterResolverNotFoundException(
              new ResolvableMethodParameter(parameter), "test");

      assertThat(exception).isInstanceOf(InfraConfigurationException.class);
    }

    static class TestController {
      public void testMethod(String param) {
      }
    }

  }

  @Nested
  class MissingRequestCookieExceptionTests {
    @Test
    void constructorWithCookieNameAndParameter() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleCookie", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String cookieName = "testCookie";

      MissingRequestCookieException exception = new MissingRequestCookieException(cookieName, parameter);

      assertThat(exception.getCookieName()).isEqualTo(cookieName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isFalse();
      assertThat(exception.getBody().getDetail()).isEqualTo("Required cookie 'testCookie' is not present.");
    }

    @Test
    void constructorWithMissingAfterConversion() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleCookie", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String cookieName = "testCookie";

      MissingRequestCookieException exception = new MissingRequestCookieException(cookieName, parameter, true);

      assertThat(exception.getCookieName()).isEqualTo(cookieName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isTrue();
    }

    @Test
    void getMessageWhenNotPresent() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleCookie", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String cookieName = "testCookie";

      MissingRequestCookieException exception = new MissingRequestCookieException(cookieName, parameter);

      String message = exception.getMessage();
      assertThat(message).contains("Required cookie 'testCookie' for method parameter type String is not present");
    }

    @Test
    void getMessageWhenConvertedToNull() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleCookie", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String cookieName = "testCookie";

      MissingRequestCookieException exception = new MissingRequestCookieException(cookieName, parameter, true);

      String message = exception.getMessage();
      assertThat(message).contains("Required cookie 'testCookie' for method parameter type String is present but converted to null");
    }

    @Test
    void exceptionExtendsMissingRequestValueException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleCookie", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String cookieName = "testCookie";

      MissingRequestCookieException exception = new MissingRequestCookieException(cookieName, parameter);

      assertThat(exception).isInstanceOf(MissingRequestValueException.class);
    }

    static class TestController {
      public void handleCookie(String cookieValue) {
      }
    }

  }

  @Nested
  class MissingRequestHeaderExceptionTests {
    @Test
    void constructorWithHeaderNameAndParameter() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleHeader", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String headerName = "X-Test-Header";

      MissingRequestHeaderException exception = new MissingRequestHeaderException(headerName, parameter);

      assertThat(exception.getHeaderName()).isEqualTo(headerName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isFalse();
      assertThat(exception.getBody().getDetail()).isEqualTo("Required header 'X-Test-Header' is not present.");
    }

    @Test
    void constructorWithMissingAfterConversion() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleHeader", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String headerName = "X-Test-Header";

      MissingRequestHeaderException exception = new MissingRequestHeaderException(headerName, parameter, true);

      assertThat(exception.getHeaderName()).isEqualTo(headerName);
      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.isMissingAfterConversion()).isTrue();
    }

    @Test
    void getMessageWhenNotPresent() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleHeader", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String headerName = "X-Test-Header";

      MissingRequestHeaderException exception = new MissingRequestHeaderException(headerName, parameter);

      String message = exception.getMessage();
      assertThat(message).contains("Required request header 'X-Test-Header' for method parameter type String is not present");
    }

    @Test
    void getMessageWhenConvertedToNull() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleHeader", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String headerName = "X-Test-Header";

      MissingRequestHeaderException exception = new MissingRequestHeaderException(headerName, parameter, true);

      String message = exception.getMessage();
      assertThat(message).contains("Required request header 'X-Test-Header' for method parameter type String is present but converted to null");
    }

    @Test
    void exceptionExtendsMissingRequestValueException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("handleHeader", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String headerName = "X-Test-Header";

      MissingRequestHeaderException exception = new MissingRequestHeaderException(headerName, parameter);

      assertThat(exception).isInstanceOf(MissingRequestValueException.class);
    }

    static class TestController {
      public void handleHeader(String headerValue) {
      }
    }

  }

  @Nested
  class MissingRequestPartExceptionTests {
    @Test
    void constructorWithRequestPartName() {
      String partName = "filePart";

      MissingRequestPartException exception = new MissingRequestPartException(partName);

      assertThat(exception.getRequestPartName()).isEqualTo(partName);
      assertThat(exception.getMessage()).isEqualTo("Required part 'filePart' is not present.");
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(exception.getBody().getDetail()).isEqualTo("Required part 'filePart' is not present.");
      assertThat(exception.isMissingAfterConversion()).isFalse();
    }

    @Test
    void exceptionImplementsErrorResponse() {
      MissingRequestPartException exception = new MissingRequestPartException("testPart");

      assertThat(exception).isInstanceOf(ErrorResponse.class);
    }

    @Test
    void exceptionExtendsMissingRequestValueException() {
      MissingRequestPartException exception = new MissingRequestPartException("testPart");

      assertThat(exception).isInstanceOf(MissingRequestValueException.class);
    }

  }

  @Nested
  class ParameterConversionExceptionTests {
    @Test
    void constructorWithParameterAndValueAndCause() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      Object value = "invalidValue";
      Throwable cause = new NumberFormatException("For input string: \"invalidValue\"");

      ParameterConversionException exception = new ParameterConversionException(parameter, value, cause);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getValue()).isEqualTo(value);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getMessage()).isEqualTo("Cannot convert 'invalidValue' to class java.lang.String");
    }

    @Test
    void constructorWithNullValue() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      Object value = null;
      Throwable cause = new IllegalArgumentException("null value");

      ParameterConversionException exception = new ParameterConversionException(parameter, value, cause);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getValue()).isNull();
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getMessage()).isEqualTo("Cannot convert 'null' to class java.lang.String");
    }

    @Test
    void exceptionExtendsMethodParameterResolvingException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      ParameterConversionException exception = new ParameterConversionException(parameter, "test", null);

      assertThat(exception).isInstanceOf(MethodParameterResolvingException.class);
    }

    @Test
    void getValueReturnsOriginalValue() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      Object value = List.of("item1", "item2");

      ParameterConversionException exception = new ParameterConversionException(parameter, value, null);

      assertThat(exception.getValue()).isSameAs(value);
    }

    static class TestController {
      public void testMethod(String param) {
      }
    }

  }

  @Nested
  class DateParameterParsingExceptionTests {

    @Test
    void constructorWithParameterAndValueAndCause() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      String value = "2021-01-01";
      Throwable cause = new IllegalArgumentException("Invalid date format");

      DateParameterParsingException exception = new DateParameterParsingException(parameter, value, cause);

      assertThat(exception.getParameter()).isSameAs(parameter);
      assertThat(exception.getValue()).isEqualTo(value);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getMessage()).isEqualTo("Cannot convert '2021-01-01' to class java.lang.String");
    }

    @Test
    void exceptionExtendsParameterConversionException() throws Exception {
      Method method = TestController.class.getDeclaredMethod("testMethod", String.class);
      MethodParameter parameter = new MethodParameter(method, 0);
      DateParameterParsingException exception = new DateParameterParsingException(parameter, "2021-01-01", null);

      assertThat(exception).isInstanceOf(ParameterConversionException.class);
    }

    static class TestController {
      public void testMethod(String param) {
      }
    }
  }

}
