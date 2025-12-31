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

import infra.core.Pair;
import infra.core.conversion.ConversionException;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.web.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 15:17
 */
class HttpStatusProviderTests {

  @Test
  void getStatusCodeFromHttpStatusProviderException() {
    HttpStatusCode expectedStatus = HttpStatus.BAD_REQUEST;
    String expectedMessage = "Test message";
    HttpStatusProviderException exception = new HttpStatusProviderException(expectedStatus, expectedMessage);

    Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(exception);

    assertThat(result.getFirst()).isEqualTo(expectedStatus);
    assertThat(result.getSecond()).isEqualTo(expectedMessage);
  }

  @Test
  void getStatusCodeFromConversionException() {
    String expectedMessage = "Conversion failed";
    ConversionException exception = new ConversionException(expectedMessage) {

    };

    Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(exception);

    assertThat(result.getFirst()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(result.getSecond()).isEqualTo(expectedMessage);
  }

  @Test
  void getStatusCodeFromResponseStatusAnnotatedException() {
    ResponseStatusAnnotatedException exception = new ResponseStatusAnnotatedException("Test reason");

    Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(exception);

    assertThat(result.getFirst()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(result.getSecond()).isEqualTo("Test reason");
  }

  @Test
  void getStatusCodeFromResponseStatusAnnotatedExceptionWithEmptyReason() {
    ResponseStatusAnnotatedExceptionWithEmptyReason exception = new ResponseStatusAnnotatedExceptionWithEmptyReason("Exception message");

    Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(exception);

    assertThat(result.getFirst()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(result.getSecond()).isEqualTo("Exception message");
  }

  @Test
  void getStatusCodeFromResponseStatusAnnotatedExceptionWithNullReason() {
    ResponseStatusAnnotatedExceptionWithNullReason exception = new ResponseStatusAnnotatedExceptionWithNullReason();

    Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(exception);

    assertThat(result.getFirst()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(result.getSecond()).isNull();
  }

  @Test
  void getStatusCodeFromRegularException() {
    String expectedMessage = "Regular exception message";
    Exception exception = new Exception(expectedMessage);

    Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(exception);

    assertThat(result.getFirst()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(result.getSecond()).isEqualTo(expectedMessage);
  }

  @Test
  void getStatusCodeFromNullException() {
    NullPointerException exception = new NullPointerException();

    Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(exception);

    assertThat(result.getFirst()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(result.getSecond()).isNull();
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  static class HttpStatusProviderException extends RuntimeException implements HttpStatusProvider {

    private final HttpStatusCode statusCode;

    public HttpStatusProviderException(HttpStatusCode statusCode, String message) {
      super(message);
      this.statusCode = statusCode;
    }

    @Override
    public HttpStatusCode getStatusCode() {
      return this.statusCode;
    }
  }

  @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Test reason")
  static class ResponseStatusAnnotatedException extends RuntimeException {

    public ResponseStatusAnnotatedException(String reason) {
      super(reason);
    }
  }

  @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "")
  static class ResponseStatusAnnotatedExceptionWithEmptyReason extends RuntimeException {

    public ResponseStatusAnnotatedExceptionWithEmptyReason(String message) {
      super(message);
    }
  }

  @ResponseStatus(code = HttpStatus.NOT_FOUND)
  static class ResponseStatusAnnotatedExceptionWithNullReason extends RuntimeException {
  }

}