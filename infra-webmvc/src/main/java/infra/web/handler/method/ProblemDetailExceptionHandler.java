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

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.beans.ConversionNotSupportedException;
import infra.beans.TypeMismatchException;
import infra.context.MessageSource;
import infra.core.annotation.AnnotatedElementUtils;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.validation.method.MethodValidationException;
import infra.web.ErrorResponse;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.ResponseStatus;
import infra.web.server.ResponseStatusException;
import infra.web.util.DisconnectedClientHelper;

/**
 * Last-resort exception handler that renders any exception as an RFC 9457 problem detail.
 *
 * <p>This handler is invoked by the exception handler method infrastructure after
 * application {@link ExceptionHandler @ExceptionHandler} methods, local or declared
 * in a {@code @ControllerAdvice}. It is not intended to be declared as an application bean.
 *
 * <p>{@link ErrorResponse} exceptions are used as is. Exceptions resembling a client
 * disconnect result in a 500 status, since the lost connection may be to another host.
 * {@link ResponseStatusException} and {@link ResponseStatus @ResponseStatus}, including
 * on causes, determine the status and localized reason. Conversion, message conversion,
 * and method validation failures use the same status and detail as
 * {@code ResponseEntityExceptionHandler}. Other exceptions produce a 500 response
 * without exposing their message.
 *
 * <p>The type, title, and detail can be customized through
 * {@link ErrorResponse#updateAndGetBody(MessageSource, Locale)}. Unexpected 5xx errors
 * are logged at ERROR level; other exceptions are logged at DEBUG level.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public final class ProblemDetailExceptionHandler {

  private static final DisconnectedClientHelper disconnectedClientHelper =
          new DisconnectedClientHelper("infra.web.handler.method.DisconnectedClient");

  private static final Logger logger = LoggerFactory.getLogger(ProblemDetailExceptionHandler.class);

  private final @Nullable MessageSource messageSource;

  /**
   * Create a new instance.
   * @param messageSource the source for problem type, title, detail, and response status reasons
   */
  public ProblemDetailExceptionHandler(@Nullable MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Handle the exception and return the error response to render.
   * @param ex the exception to handle
   * @param locale the locale for message lookups
   * @return the error response
   */
  @ExceptionHandler
  public ErrorResponse handleException(Exception ex, Locale locale) {
    ErrorResponse response = resolveErrorResponse(ex, locale);
    if (!disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
      if (response.getStatusCode().is5xxServerError() && !(response instanceof Throwable)) {
        logger.error("Unexpected exception rendered as {} problem detail", response.getStatusCode(), ex);
      }
      else if (logger.isDebugEnabled()) {
        logger.debug("Rendering {} problem detail for {}", response.getStatusCode(), ex);
      }
    }
    response.updateAndGetBody(this.messageSource, locale);
    return response;
  }

  private ErrorResponse resolveErrorResponse(Exception ex, Locale locale) {
    if (ex instanceof ErrorResponse response) {
      return response;
    }
    if (DisconnectedClientHelper.isClientDisconnectedException(ex)) {
      return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }
    ErrorResponse status = resolveResponseStatus(ex, locale);
    if (status != null) {
      return status;
    }
    if (ex instanceof ConversionNotSupportedException conversion) {
      Object[] args = { conversion.getPropertyName(), conversion.getValue() };
      return ErrorResponse.builder(ex, HttpStatus.INTERNAL_SERVER_ERROR,
                      "Failed to convert '" + args[0] + "' with value: '" + args[1] + "'")
              .detailMessageArguments(args).build();
    }
    if (ex instanceof TypeMismatchException mismatch) {
      Object[] args = { mismatch.getPropertyName(), mismatch.getValue(),
              mismatch.getRequiredType() != null ? mismatch.getRequiredType().getSimpleName() : "" };
      return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST,
                      "Failed to convert '" + args[0] + "' with value: '" + args[1] + "'")
              .detailMessageCode(ErrorResponse.getDefaultDetailMessageCode(TypeMismatchException.class, null))
              .detailMessageArguments(args).build();
    }
    if (ex instanceof HttpMessageNotReadableException) {
      return ErrorResponse.create(ex, HttpStatus.BAD_REQUEST, "Failed to read request");
    }
    if (ex instanceof HttpMessageNotWritableException) {
      return ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write request");
    }
    if (ex instanceof MethodValidationException) {
      return ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Validation failed");
    }
    return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, null);
  }

  private @Nullable ErrorResponse resolveResponseStatus(Throwable ex, Locale locale) {
    Throwable current = ex;
    while (current != null) {
      if (current instanceof ResponseStatusException status) {
        return status;
      }
      ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(current.getClass(), ResponseStatus.class);
      if (annotation != null) {
        String reason = annotation.reason();
        return createErrorResponse(current, annotation.code(), reason.isEmpty() ? null :
                this.messageSource != null ? this.messageSource.getMessage(reason, null, reason, locale) : reason);
      }
      Throwable cause = current.getCause();
      current = cause != current ? cause : null;
    }
    return null;
  }

  private static ErrorResponse createErrorResponse(Throwable ex, HttpStatusCode status, @Nullable String detail) {
    ProblemDetail body = ProblemDetail.forStatus(status);
    body.setDetail(detail);
    return ErrorResponse.builder(ex, body).build();
  }
}
