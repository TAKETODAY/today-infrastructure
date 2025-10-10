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

package infra.web.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

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

}