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

package infra.web.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import infra.core.ResolvableType;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 16:41
 */
class RestExceptionTests {

  @Nested
  class UnknownContentTypeExceptionTests {
    @Test
    void constructorWithIntStatusCode() {
      Type targetType = String.class;
      MediaType contentType = MediaType.APPLICATION_JSON;
      int statusCode = 404;
      String statusText = "Not Found";
      HttpHeaders responseHeaders = HttpHeaders.forWritable();
      byte[] responseBody = "test".getBytes();

      UnknownContentTypeException exception = new UnknownContentTypeException(targetType, contentType, statusCode, statusText, responseHeaders, responseBody);

      assertThat(exception.getTargetType()).isEqualTo(targetType);
      assertThat(exception.getContentType()).isEqualTo(contentType);
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(statusCode));
      assertThat(exception.getRawStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(responseHeaders);
      assertThat(exception.getResponseBody()).isEqualTo(responseBody);
    }

    @Test
    void constructorWithHttpStatusCode() {
      Type targetType = String.class;
      MediaType contentType = MediaType.APPLICATION_JSON;
      HttpStatusCode statusCode = HttpStatusCode.valueOf(404);
      String statusText = "Not Found";
      HttpHeaders responseHeaders = HttpHeaders.forWritable();
      byte[] responseBody = "test".getBytes();

      UnknownContentTypeException exception = new UnknownContentTypeException(targetType, contentType, statusCode, statusText, responseHeaders, responseBody);

      assertThat(exception.getTargetType()).isEqualTo(targetType);
      assertThat(exception.getContentType()).isEqualTo(contentType);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getRawStatusCode()).isEqualTo(statusCode.value());
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(responseHeaders);
      assertThat(exception.getResponseBody()).isEqualTo(responseBody);
    }

    @Test
    void getTargetTypeReturnsCorrectType() {
      Type targetType = String.class;
      UnknownContentTypeException exception = new UnknownContentTypeException(targetType, MediaType.APPLICATION_JSON, 200, "OK", null, new byte[0]);

      assertThat(exception.getTargetType()).isEqualTo(targetType);
    }

    @Test
    void getContentTypeReturnsCorrectContentType() {
      MediaType contentType = MediaType.APPLICATION_XML;
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, contentType, 200, "OK", null, new byte[0]);

      assertThat(exception.getContentType()).isEqualTo(contentType);
    }

    @Test
    void getStatusCodeReturnsCorrectStatusCode() {
      HttpStatusCode statusCode = HttpStatusCode.valueOf(500);
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, MediaType.APPLICATION_JSON, statusCode, "Internal Server Error", null, new byte[0]);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void getRawStatusCodeReturnsCorrectValue() {
      int rawStatusCode = 403;
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, MediaType.APPLICATION_JSON, rawStatusCode, "Forbidden", null, new byte[0]);

      assertThat(exception.getRawStatusCode()).isEqualTo(rawStatusCode);
    }

    @Test
    void getStatusTextReturnsCorrectText() {
      String statusText = "Bad Request";
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, MediaType.APPLICATION_JSON, 400, statusText, null, new byte[0]);

      assertThat(exception.getStatusText()).isEqualTo(statusText);
    }

    @Test
    void getResponseHeadersReturnsCorrectHeaders() {
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.set("X-Test", "value");
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, MediaType.APPLICATION_JSON, 200, "OK", headers, new byte[0]);

      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
    }

    @Test
    void getResponseBodyReturnsCorrectBody() {
      byte[] body = "error response".getBytes();
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, MediaType.APPLICATION_JSON, 200, "OK", null, body);

      assertThat(exception.getResponseBody()).isEqualTo(body);
    }

    @Test
    void getResponseBodyAsStringWithCharset() {
      MediaType contentType = new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8);
      byte[] body = "错误信息".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, contentType, 200, "OK", null, body);

      assertThat(exception.getResponseBodyAsString()).isEqualTo("错误信息");
    }

    @Test
    void getResponseBodyAsStringWithoutCharset() {
      MediaType contentType = MediaType.APPLICATION_JSON; // No charset
      byte[] body = "{\"error\": \"message\"}".getBytes();
      UnknownContentTypeException exception = new UnknownContentTypeException(String.class, contentType, 200, "OK", null, body);

      assertThat(exception.getResponseBodyAsString()).isEqualTo("{\"error\": \"message\"}");
    }

    @Test
    void exceptionMessageContainsTargetTypeAndContentType() {
      Type targetType = java.util.List.class;
      MediaType contentType = MediaType.APPLICATION_JSON;
      UnknownContentTypeException exception = new UnknownContentTypeException(targetType, contentType, 200, "OK", null, new byte[0]);

      assertThat(exception.getMessage()).contains("List");
      assertThat(exception.getMessage()).contains("application/json");
    }

  }

  @Nested
  class UnknownHttpStatusCodeExceptionTests {
    @Test
    void constructorWithIntStatusCode() {
      int rawStatusCode = 600;
      String statusText = "Custom Status";
      HttpHeaders responseHeaders = HttpHeaders.forWritable();
      byte[] responseBody = "test response".getBytes();
      Charset responseCharset = java.nio.charset.StandardCharsets.UTF_8;

      UnknownHttpStatusCodeException exception = new UnknownHttpStatusCodeException(
              rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);

      assertThat(exception.getRawStatusCode()).isEqualTo(rawStatusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(responseHeaders);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(responseBody);
    }

    @Test
    void constructorWithMessage() {
      String message = "Custom error message";
      int rawStatusCode = 601;
      String statusText = "Another Custom Status";
      HttpHeaders responseHeaders = HttpHeaders.forWritable();
      byte[] responseBody = "another test response".getBytes();
      Charset responseCharset = java.nio.charset.StandardCharsets.UTF_8;

      UnknownHttpStatusCodeException exception = new UnknownHttpStatusCodeException(
              message, rawStatusCode, statusText, responseHeaders, responseBody, responseCharset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getRawStatusCode()).isEqualTo(rawStatusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(responseHeaders);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(responseBody);
    }

    @Test
    void constructorHandlesNullHeaders() {
      int rawStatusCode = 602;
      String statusText = "Status with null headers";
      byte[] responseBody = "response with null headers".getBytes();
      Charset responseCharset = java.nio.charset.StandardCharsets.UTF_8;

      UnknownHttpStatusCodeException exception = new UnknownHttpStatusCodeException(
              rawStatusCode, statusText, null, responseBody, responseCharset);

      assertThat(exception.getRawStatusCode()).isEqualTo(rawStatusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(responseBody);
    }

    @Test
    void constructorHandlesNullResponseBody() {
      int rawStatusCode = 603;
      String statusText = "Status with null body";
      HttpHeaders responseHeaders = HttpHeaders.forWritable();
      Charset responseCharset = java.nio.charset.StandardCharsets.UTF_8;

      UnknownHttpStatusCodeException exception = new UnknownHttpStatusCodeException(
              rawStatusCode, statusText, responseHeaders, null, responseCharset);

      assertThat(exception.getRawStatusCode()).isEqualTo(rawStatusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(responseHeaders);
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
    }

    @Test
    void constructorHandlesNullCharset() {
      int rawStatusCode = 604;
      String statusText = "Status with null charset";
      HttpHeaders responseHeaders = HttpHeaders.forWritable();
      byte[] responseBody = "response with null charset".getBytes();

      UnknownHttpStatusCodeException exception = new UnknownHttpStatusCodeException(
              rawStatusCode, statusText, responseHeaders, responseBody, null);

      assertThat(exception.getRawStatusCode()).isEqualTo(rawStatusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(responseHeaders);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(responseBody);
    }

    @Test
    void exceptionMessageFormattedCorrectly() {
      int rawStatusCode = 605;
      String statusText = "Formatted Status";
      UnknownHttpStatusCodeException exception = new UnknownHttpStatusCodeException(
              rawStatusCode, statusText, null, null, null);

      assertThat(exception.getMessage()).isEqualTo("Unknown status code [%d] %s".formatted(rawStatusCode, statusText));
    }

  }

  @Nested
  class HttpClientErrorExceptionTests {

    @Test
    void constructorWithStatusCodeOnly() {
      HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;

      HttpClientErrorException exception = new HttpClientErrorException(statusCode);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo("BAD_REQUEST");
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
    }

    @Test
    void constructorWithStatusCodeAndStatusText() {
      HttpStatusCode statusCode = HttpStatus.UNAUTHORIZED;
      String statusText = "Unauthorized";

      HttpClientErrorException exception = new HttpClientErrorException(statusCode, statusText);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
    }

    @Test
    void constructorWithStatusCodeStatusTextAndContent() {
      HttpStatusCode statusCode = HttpStatus.FORBIDDEN;
      String statusText = "Forbidden";
      byte[] body = "Access denied".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = new HttpClientErrorException(statusCode, statusText, body, charset);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
      assertThat(exception.getResponseHeaders()).isNull();
    }

    @Test
    void constructorWithStatusCodeStatusTextHeadersAndContent() {
      HttpStatusCode statusCode = HttpStatus.NOT_FOUND;
      String statusText = "Not Found";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.TEXT_PLAIN);
      byte[] body = "Resource not found".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = new HttpClientErrorException(statusCode, statusText, headers, body, charset);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
    }

    @Test
    void constructorWithMessageStatusCodeStatusTextHeadersAndContent() {
      String message = "Custom error message";
      HttpStatusCode statusCode = HttpStatus.METHOD_NOT_ALLOWED;
      String statusText = "Method Not Allowed";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.TEXT_PLAIN);
      byte[] body = "Method not supported".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = new HttpClientErrorException(message, statusCode, statusText, headers, body, charset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
    }

    @Test
    void createReturnsBadRequestFor400() {
      HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
      String statusText = "Bad Request";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Bad request".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.BadRequest.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsUnauthorizedFor401() {
      HttpStatusCode statusCode = HttpStatus.UNAUTHORIZED;
      String statusText = "Unauthorized";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Unauthorized".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.Unauthorized.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsForbiddenFor403() {
      HttpStatusCode statusCode = HttpStatus.FORBIDDEN;
      String statusText = "Forbidden";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Forbidden".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.Forbidden.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsNotFoundFor404() {
      HttpStatusCode statusCode = HttpStatus.NOT_FOUND;
      String statusText = "Not Found";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Not found".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.NotFound.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsMethodNotAllowedFor405() {
      HttpStatusCode statusCode = HttpStatus.METHOD_NOT_ALLOWED;
      String statusText = "Method Not Allowed";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Method not allowed".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.MethodNotAllowed.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsNotAcceptableFor406() {
      HttpStatusCode statusCode = HttpStatus.NOT_ACCEPTABLE;
      String statusText = "Not Acceptable";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Not acceptable".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.NotAcceptable.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsConflictFor409() {
      HttpStatusCode statusCode = HttpStatus.CONFLICT;
      String statusText = "Conflict";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Conflict".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.Conflict.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsGoneFor410() {
      HttpStatusCode statusCode = HttpStatus.GONE;
      String statusText = "Gone";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Gone".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.Gone.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsUnsupportedMediaTypeFor415() {
      HttpStatusCode statusCode = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      String statusText = "Unsupported Media Type";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Unsupported media type".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.UnsupportedMediaType.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsUnprocessableEntityFor422() {
      HttpStatusCode statusCode = HttpStatus.UNPROCESSABLE_ENTITY;
      String statusText = "Unprocessable Entity";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Unprocessable entity".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.UnprocessableEntity.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsTooManyRequestsFor429() {
      HttpStatusCode statusCode = HttpStatus.TOO_MANY_REQUESTS;
      String statusText = "Too Many Requests";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Too many requests".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.TooManyRequests.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsGenericExceptionForOther4xx() {
      HttpStatusCode statusCode = HttpStatusCode.valueOf(499);
      String statusText = "Custom 4xx Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Custom error".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpClientErrorException.class);
      assertThat(exception).isNotInstanceOfAny(
              HttpClientErrorException.BadRequest.class,
              HttpClientErrorException.Unauthorized.class,
              HttpClientErrorException.Forbidden.class,
              HttpClientErrorException.NotFound.class,
              HttpClientErrorException.MethodNotAllowed.class,
              HttpClientErrorException.NotAcceptable.class,
              HttpClientErrorException.Conflict.class,
              HttpClientErrorException.Gone.class,
              HttpClientErrorException.UnsupportedMediaType.class,
              HttpClientErrorException.UnprocessableEntity.class,
              HttpClientErrorException.TooManyRequests.class
      );
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createWithMessageReturnsCustomMessage() {
      String message = "Custom error message";
      HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
      String statusText = "Bad Request";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Bad request".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createWithNullMessageReturnsDefaultMessage() {
      HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
      String statusText = "Bad Request";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Bad request".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpClientErrorException exception = HttpClientErrorException.create(null, statusCode, statusText, headers, body, charset);

      assertThat(exception.getMessage()).isNotNull();
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

  }

  @Nested
  class RestClientResponseExceptionTests {
    @Test
    void constructorWithIntStatusCode() {
      String message = "Test exception";
      int statusCode = 500;
      String statusText = "Internal Server Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.APPLICATION_JSON);
      byte[] responseBody = "{\"error\":\"test\"}".getBytes();
      Charset responseCharset = StandardCharsets.UTF_8;

      RestClientResponseException exception = new RestClientResponseException(message, statusCode, statusText, headers, responseBody, responseCharset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(statusCode));
      assertThat(exception.getRawStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(responseBody);
      assertThat(exception).extracting("responseCharset").isEqualTo(responseCharset);
    }

    @Test
    void constructorWithHttpStatusCode() {
      String message = "Test exception";
      HttpStatusCode statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
      String statusText = "Internal Server Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.APPLICATION_JSON);
      byte[] responseBody = "{\"error\":\"test\"}".getBytes();
      Charset responseCharset = StandardCharsets.UTF_8;

      RestClientResponseException exception = new RestClientResponseException(message, statusCode, statusText, headers, responseBody, responseCharset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getRawStatusCode()).isEqualTo(statusCode.value());
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(responseBody);
      assertThat(exception).extracting("responseCharset").isEqualTo(responseCharset);
    }

    @Test
    void constructorHandlesNullResponseBody() {
      String message = "Test exception";
      HttpStatusCode statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
      String statusText = "Internal Server Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      Charset responseCharset = StandardCharsets.UTF_8;

      RestClientResponseException exception = new RestClientResponseException(message, statusCode, statusText, headers, null, responseCharset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
      assertThat(exception).extracting("responseCharset").isEqualTo(responseCharset);
    }

    @Test
    void getStatusCodeReturnsCorrectValue() {
      HttpStatusCode statusCode = HttpStatus.BAD_GATEWAY;
      RestClientResponseException exception = new RestClientResponseException("Test", statusCode, "Bad Gateway", null, new byte[0], null);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void getRawStatusCodeReturnsCorrectValue() {
      int rawStatusCode = 502;
      HttpStatusCode statusCode = HttpStatusCode.valueOf(rawStatusCode);
      RestClientResponseException exception = new RestClientResponseException("Test", statusCode, "Bad Gateway", null, new byte[0], null);

      assertThat(exception.getRawStatusCode()).isEqualTo(rawStatusCode);
    }

    @Test
    void getStatusTextReturnsCorrectValue() {
      String statusText = "Service Unavailable";
      RestClientResponseException exception = new RestClientResponseException("Test", 503, statusText, null, new byte[0], null);

      assertThat(exception.getStatusText()).isEqualTo(statusText);
    }

    @Test
    void getResponseHeadersReturnsCorrectHeaders() {
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.set("X-Custom-Header", "custom-value");
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", headers, new byte[0], null);

      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
    }

    @Test
    void getResponseBodyAsByteArrayReturnsCorrectBody() {
      byte[] body = "error details".getBytes();
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, body, null);

      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
    }

    @Test
    void getResponseBodyAsStringWithDefaultCharset() {
      byte[] body = "错误信息".getBytes(StandardCharsets.UTF_8);
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, body, StandardCharsets.UTF_8);

      assertThat(exception.getResponseBodyAsString()).isEqualTo("错误信息");
    }

    @Test
    void getResponseBodyAsStringWithFallbackCharset() {
      byte[] body = "error details".getBytes(StandardCharsets.ISO_8859_1);
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, body, null);

      assertThat(exception.getResponseBodyAsString(StandardCharsets.ISO_8859_1)).isEqualTo("error details");
    }

    @Test
    void getResponseBodyAsStringWithoutCharsetUsesDefault() {
      byte[] body = "error details".getBytes();
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, body, null);

      assertThat(exception.getResponseBodyAsString()).isEqualTo("error details");
    }

    @Test
    void getResponseBodyAsWithClassType() {
      Function<ResolvableType, ?> bodyConvertFunction = type -> "converted body";
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, "body".getBytes(), null);
      exception.setBodyConvertFunction(bodyConvertFunction);

      String result = exception.getResponseBodyAs(String.class);

      assertThat(result).isEqualTo("converted body");
    }

    @Test
    void getResponseBodyAsWithParameterizedTypeReference() {
      Function<ResolvableType, ?> bodyConvertFunction = type -> java.util.List.of("item1", "item2");
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, "body".getBytes(), null);
      exception.setBodyConvertFunction(bodyConvertFunction);

      java.util.List<String> result = exception.getResponseBodyAs(new infra.core.ParameterizedTypeReference<java.util.List<String>>() { });

      assertThat(result).containsExactly("item1", "item2");
    }

    @Test
    void getResponseBodyAsThrowsIllegalStateExceptionWhenConvertFunctionNotSet() {
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, "body".getBytes(), null);

      assertThatThrownBy(() -> exception.getResponseBodyAs(String.class))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Function to convert body not set");
    }

    @Test
    void setBodyConvertFunctionStoresFunction() {
      Function<ResolvableType, ?> bodyConvertFunction = type -> "converted";
      RestClientResponseException exception = new RestClientResponseException("Test", 500, "Internal Server Error", null, "body".getBytes(), null);

      exception.setBodyConvertFunction(bodyConvertFunction);

      assertThat(exception).extracting("bodyConvertFunction").isSameAs(bodyConvertFunction);
    }

  }

  @Nested
  class HttpStatusCodeExceptionTests {
    @Test
    void constructorWithStatusCodeOnly() {
      HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;

      TestHttpStatusCodeException exception = new TestHttpStatusCodeException(statusCode);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo("BAD_REQUEST");
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
      assertThat(exception.getMessage()).isEqualTo("400 BAD_REQUEST");
    }

    @Test
    void constructorWithStatusCodeAndStatusText() {
      HttpStatusCode statusCode = HttpStatus.UNAUTHORIZED;
      String statusText = "Unauthorized";

      TestHttpStatusCodeException exception = new TestHttpStatusCodeException(statusCode, statusText);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
      assertThat(exception.getMessage()).isEqualTo("401 Unauthorized");
    }

    @Test
    void constructorWithStatusCodeStatusTextAndContent() {
      HttpStatusCode statusCode = HttpStatus.FORBIDDEN;
      String statusText = "Forbidden";
      byte[] body = "Access denied".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      TestHttpStatusCodeException exception = new TestHttpStatusCodeException(statusCode, statusText, body, charset);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getMessage()).isEqualTo("403 Forbidden");
    }

    @Test
    void constructorWithStatusCodeStatusTextHeadersAndContent() {
      HttpStatusCode statusCode = HttpStatus.NOT_FOUND;
      String statusText = "Not Found";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.TEXT_PLAIN);
      byte[] body = "Resource not found".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      TestHttpStatusCodeException exception = new TestHttpStatusCodeException(statusCode, statusText, headers, body, charset);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
      assertThat(exception.getMessage()).isEqualTo("404 Not Found");
    }

    @Test
    void constructorWithMessageStatusCodeStatusTextHeadersAndContent() {
      String message = "Custom error message";
      HttpStatusCode statusCode = HttpStatus.METHOD_NOT_ALLOWED;
      String statusText = "Method Not Allowed";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.TEXT_PLAIN);
      byte[] body = "Method not supported".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      TestHttpStatusCodeException exception = new TestHttpStatusCodeException(message, statusCode, statusText, headers, body, charset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
    }

    @Test
    void constructorWithHttpStatusUsesReasonPhrase() {
      HttpStatusCode statusCode = HttpStatus.INTERNAL_SERVER_ERROR;

      TestHttpStatusCodeException exception = new TestHttpStatusCodeException(statusCode, "");

      assertThat(exception.getMessage()).isEqualTo("500 Internal Server Error");
    }

    @Test
    void constructorWithCustomStatusCodeAndEmptyStatusText() {
      HttpStatusCode statusCode = HttpStatusCode.valueOf(499);

      TestHttpStatusCodeException exception = new TestHttpStatusCodeException(statusCode, "");

      assertThat(exception.getMessage()).isEqualTo("499 ");
    }

    static class TestHttpStatusCodeException extends HttpStatusCodeException {

      TestHttpStatusCodeException(HttpStatusCode statusCode) {
        super(statusCode);
      }

      TestHttpStatusCodeException(HttpStatusCode statusCode, String statusText) {
        super(statusCode, statusText);
      }

      TestHttpStatusCodeException(HttpStatusCode statusCode, String statusText,
              byte[] responseBody, Charset responseCharset) {
        super(statusCode, statusText, responseBody, responseCharset);
      }

      TestHttpStatusCodeException(HttpStatusCode statusCode, String statusText,
              HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
        super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
      }

      TestHttpStatusCodeException(String message, HttpStatusCode statusCode, String statusText,
              HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
        super(message, statusCode, statusText, responseHeaders, responseBody, responseCharset);
      }
    }

  }

  @Nested
  class HttpServerErrorExceptionTests {
    @Test
    void constructorWithStatusCodeOnly() {
      HttpStatusCode statusCode = HttpStatus.INTERNAL_SERVER_ERROR;

      HttpServerErrorException exception = new HttpServerErrorException(statusCode);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo("INTERNAL_SERVER_ERROR");
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
    }

    @Test
    void constructorWithStatusCodeAndStatusText() {
      HttpStatusCode statusCode = HttpStatus.NOT_IMPLEMENTED;
      String statusText = "Not Implemented";

      HttpServerErrorException exception = new HttpServerErrorException(statusCode, statusText);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isNull();
      assertThat(exception.getResponseBodyAsByteArray()).isEmpty();
    }

    @Test
    void constructorWithStatusCodeStatusTextAndContent() {
      HttpStatusCode statusCode = HttpStatus.BAD_GATEWAY;
      String statusText = "Bad Gateway";
      byte[] body = "Bad gateway error".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = new HttpServerErrorException(statusCode, statusText, body, charset);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
      assertThat(exception.getResponseHeaders()).isNull();
    }

    @Test
    void constructorWithStatusCodeStatusTextHeadersAndContent() {
      HttpStatusCode statusCode = HttpStatus.SERVICE_UNAVAILABLE;
      String statusText = "Service Unavailable";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.TEXT_PLAIN);
      byte[] body = "Service unavailable".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = new HttpServerErrorException(statusCode, statusText, headers, body, charset);

      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
    }

    @Test
    void constructorWithMessageStatusCodeStatusTextHeadersAndContent() {
      String message = "Custom error message";
      HttpStatusCode statusCode = HttpStatus.GATEWAY_TIMEOUT;
      String statusText = "Gateway Timeout";
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.setContentType(MediaType.TEXT_PLAIN);
      byte[] body = "Gateway timeout".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = new HttpServerErrorException(message, statusCode, statusText, headers, body, charset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
      assertThat(exception.getStatusText()).isEqualTo(statusText);
      assertThat(exception.getResponseHeaders()).isEqualTo(headers);
      assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(body);
    }

    @Test
    void createReturnsInternalServerErrorFor500() {
      HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
      String statusText = "Internal Server Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Internal server error".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpServerErrorException.InternalServerError.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsNotImplementedFor501() {
      HttpStatus statusCode = HttpStatus.NOT_IMPLEMENTED;
      String statusText = "Not Implemented";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Not implemented".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpServerErrorException.NotImplemented.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsBadGatewayFor502() {
      HttpStatus statusCode = HttpStatus.BAD_GATEWAY;
      String statusText = "Bad Gateway";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Bad gateway".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpServerErrorException.BadGateway.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsServiceUnavailableFor503() {
      HttpStatus statusCode = HttpStatus.SERVICE_UNAVAILABLE;
      String statusText = "Service Unavailable";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Service unavailable".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsGatewayTimeoutFor504() {
      HttpStatus statusCode = HttpStatus.GATEWAY_TIMEOUT;
      String statusText = "Gateway Timeout";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Gateway timeout".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpServerErrorException.GatewayTimeout.class);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createReturnsGenericExceptionForOther5xx() {
      HttpStatusCode statusCode = HttpStatusCode.valueOf(599);
      String statusText = "Custom 5xx Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Custom error".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(statusCode, statusText, headers, body, charset);

      assertThat(exception).isInstanceOf(HttpServerErrorException.class);
      assertThat(exception).isNotInstanceOfAny(
              HttpServerErrorException.InternalServerError.class,
              HttpServerErrorException.NotImplemented.class,
              HttpServerErrorException.BadGateway.class,
              HttpServerErrorException.ServiceUnavailable.class,
              HttpServerErrorException.GatewayTimeout.class
      );
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createWithMessageReturnsCustomMessage() {
      String message = "Custom error message";
      HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
      String statusText = "Internal Server Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Internal server error".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);

      assertThat(exception.getMessage()).isEqualTo(message);
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

    @Test
    void createWithNullMessageReturnsDefaultMessage() {
      HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
      String statusText = "Internal Server Error";
      HttpHeaders headers = HttpHeaders.forWritable();
      byte[] body = "Internal server error".getBytes();
      Charset charset = StandardCharsets.UTF_8;

      HttpServerErrorException exception = HttpServerErrorException.create(null, statusCode, statusText, headers, body, charset);

      assertThat(exception.getMessage()).isNotNull();
      assertThat(exception.getStatusCode()).isEqualTo(statusCode);
    }

  }


}