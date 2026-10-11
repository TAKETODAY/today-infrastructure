/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

import infra.beans.ConversionNotSupportedException;
import infra.beans.TypeMismatchException;
import infra.context.support.StaticMessageSource;
import infra.core.MethodParameter;
import infra.core.annotation.AnnotatedElementUtils;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.ProblemDetail;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.validation.method.MethodValidationException;
import infra.validation.method.MethodValidationResult;
import infra.web.ErrorResponse;
import infra.web.ErrorResponseException;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.ResponseStatus;
import infra.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Tests for {@link ProblemDetailExceptionHandler}. */
class ProblemDetailExceptionHandlerTests {

  private final StaticMessageSource messageSource = new StaticMessageSource();
  private final ProblemDetailExceptionHandler handler = new ProblemDetailExceptionHandler(messageSource);

  @Test
  void handlerMethodIsAnnotated() throws Exception {
    assertThat(AnnotatedElementUtils.hasAnnotation(ProblemDetailExceptionHandler.class
            .getMethod("handleException", Exception.class, Locale.class), ExceptionHandler.class)).isTrue();
  }

  @Test
  void errorResponseIsUsedAsIs() {
    ErrorResponseException ex = new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, null);
    ex.getHeaders().set(HttpHeaders.RETRY_AFTER, "60");
    assertThat(handle(ex)).isSameAs(ex);
    assertThat(ex.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
  }

  @Test
  void errorResponseWithLocalizedDetail() {
    messageSource.addMessage(ErrorResponse.getDefaultDetailMessageCode(ErrorResponseException.class, null),
            Locale.FRENCH, "Indisponible");
    assertThat(handle(new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE, null), Locale.FRENCH)
            .getBody().getDetail()).isEqualTo("Indisponible");
  }

  @Test
  void responseStatusException() {
    ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "No such order");
    assertThat(handle(ex)).isSameAs(ex);
    assertThat(ex.getBody().getDetail()).isEqualTo("No such order");
  }

  @Test
  void responseStatusExceptionAsCause() {
    ResponseStatusException cause = new ResponseStatusException(HttpStatus.CONFLICT, "Order already placed");
    assertThat(handle(new IllegalStateException("wrapper", cause))).isSameAs(cause);
  }

  @Test
  void responseStatusAnnotation() {
    ProblemDetail body = handle(new GoneException()).getBody();
    assertThat(body.getStatus()).isEqualTo(410);
    assertThat(body.getDetail()).isEqualTo("Resource is gone");
  }

  @Test
  void responseStatusAnnotationWithLocalizedReason() {
    messageSource.addMessage("gone.reason", Locale.FRENCH, "La ressource n'existe plus");
    ProblemDetail body = handle(new LocalizedGoneException(), Locale.FRENCH).getBody();
    assertThat(body.getStatus()).isEqualTo(410);
    assertThat(body.getDetail()).isEqualTo("La ressource n'existe plus");
  }

  @Test
  void responseStatusAnnotationWithoutReason() {
    ProblemDetail body = handle(new ServerErrorException()).getBody();
    assertThat(body.getStatus()).isEqualTo(500);
    assertThat(body.getDetail()).isNull();
  }

  @Test
  void responseStatusAnnotationOnCause() {
    ProblemDetail body = handle(new IllegalStateException("wrapper", new GoneException())).getBody();
    assertThat(body.getStatus()).isEqualTo(410);
    assertThat(body.getDetail()).isEqualTo("Resource is gone");
  }

  @Test
  void responseStatusAnnotationOnCauseHasPrecedenceOverTypeMismatch() {
    assertThat(handle(new TypeMismatchException("value", Integer.class, new GoneException()))
            .getBody().getStatus()).isEqualTo(410);
  }

  @Test
  void typeMismatch() {
    ProblemDetail body = handle(mismatch()).getBody();
    assertThat(body.getStatus()).isEqualTo(400);
    assertThat(body.getDetail()).isEqualTo("Failed to convert 'id' with value: 'abc'");
  }

  @Test
  void typeMismatchWithLocalizedDetail() {
    messageSource.addMessage(ErrorResponse.getDefaultDetailMessageCode(TypeMismatchException.class, null),
            Locale.FRENCH, "Conversion de ''{0}'' en {2} impossible");
    assertThat(handle(mismatch(), Locale.FRENCH).getBody().getDetail())
            .isEqualTo("Conversion de 'id' en Integer impossible");
  }

  private MethodArgumentTypeMismatchException mismatch() {
    return new MethodArgumentTypeMismatchException("abc", Integer.class, "id", mock(MethodParameter.class), null);
  }

  @Test
  void conversionNotSupported() {
    ProblemDetail body = handle(new ConversionNotSupportedException(new Object(), String.class, null)).getBody();
    assertThat(body.getStatus()).isEqualTo(500);
    assertThat(body.getDetail()).startsWith("Failed to convert");
  }

  @Test
  void httpMessageNotReadable() {
    ProblemDetail body = handle(new HttpMessageNotReadableException("Invalid JSON", null)).getBody();
    assertThat(body.getStatus()).isEqualTo(400);
    assertThat(body.getDetail()).isEqualTo("Failed to read request");
  }

  @Test
  void httpMessageNotWritable() {
    ProblemDetail body = handle(new HttpMessageNotWritableException("Serialization failed")).getBody();
    assertThat(body.getStatus()).isEqualTo(500);
    assertThat(body.getDetail()).isEqualTo("Failed to write request");
  }

  @Test
  void methodValidation() {
    ProblemDetail body = handle(new MethodValidationException(mock(MethodValidationResult.class))).getBody();
    assertThat(body.getStatus()).isEqualTo(500);
    assertThat(body.getDetail()).isEqualTo("Validation failed");
  }

  @Test
  void disconnectedClient() {
    ProblemDetail body = handle(new IOException("Broken pipe")).getBody();
    assertThat(body.getStatus()).isEqualTo(500);
    assertThat(body.getDetail()).isNull();
  }

  @Test
  void unknownExceptionDoesNotExposeMessage() {
    ProblemDetail body = handle(new IllegalStateException("Secret internal state")).getBody();
    assertThat(body.getStatus()).isEqualTo(500);
    assertThat(body.getDetail()).isNull();
    assertThat(body.getTitle()).isEqualTo("Internal Server Error");
  }

  @Test
  void unknownExceptionWithLocalizedDetail() {
    messageSource.addMessage(ErrorResponse.getDefaultDetailMessageCode(IllegalStateException.class, null),
            Locale.FRENCH, "Erreur interne");
    assertThat(handle(new IllegalStateException("Secret internal state"), Locale.FRENCH)
            .getBody().getDetail()).isEqualTo("Erreur interne");
  }

  @Test
  void withoutMessageSource() {
    assertThat(new ProblemDetailExceptionHandler(null).handleException(new LocalizedGoneException(), Locale.ENGLISH)
            .getBody().getDetail()).isEqualTo("gone.reason");
  }

  private ErrorResponse handle(Exception ex) {
    return handle(ex, Locale.ENGLISH);
  }

  private ErrorResponse handle(Exception ex, Locale locale) {
    ErrorResponse response = handler.handleException(ex, locale);
    assertThat(response.getBody().getStatus()).isEqualTo(response.getStatusCode().value());
    return response;
  }

  @ResponseStatus(code = HttpStatus.GONE, reason = "Resource is gone")
  private static class GoneException extends RuntimeException { }

  @ResponseStatus(code = HttpStatus.GONE, reason = "gone.reason")
  private static class LocalizedGoneException extends RuntimeException { }

  @ResponseStatus
  private static class ServerErrorException extends RuntimeException { }
}
