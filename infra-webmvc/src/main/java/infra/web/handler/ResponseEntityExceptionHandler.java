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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import infra.beans.ConversionNotSupportedException;
import infra.beans.TypeMismatchException;
import infra.context.MessageSource;
import infra.context.MessageSourceAware;
import infra.core.i18n.LocaleContextHolder;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.http.ResponseEntity;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.validation.BindException;
import infra.web.ErrorResponse;
import infra.web.ErrorResponseException;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.HttpRequestMethodNotSupportedException;
import infra.web.NotFoundHandler;
import infra.web.RequestContext;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.ExceptionHandler;
import infra.web.async.AsyncRequestTimeoutException;
import infra.web.bind.MethodArgumentNotValidException;
import infra.web.bind.MissingPathVariableException;
import infra.web.bind.MissingRequestParameterException;
import infra.web.bind.RequestBindingException;
import infra.web.bind.resolver.MissingRequestPartException;
import infra.web.multipart.MultipartException;
import infra.web.util.WebUtils;

/**
 * A class with an {@code @ExceptionHandler} method that handles all Spring MVC
 * raised exceptions by returning a {@link ResponseEntity} with RFC 9457
 * formatted error details in the body.
 *
 * <p>Convenient as a base class of an {@link ControllerAdvice @ControllerAdvice}
 * for global exception handling in an application. Subclasses can override
 * individual methods that handle a specific exception, override
 * {@link #handleExceptionInternal} to override common handling of all exceptions,
 * or override {@link #createResponseEntity} to intercept the final step of creating
 * the {@link ResponseEntity} from the selected HTTP status code, headers, and body.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #handleException(Exception, RequestContext)
 * @see SimpleHandlerExceptionHandler
 * @since 4.0 2022/3/2 18:12
 */
public class ResponseEntityExceptionHandler implements MessageSourceAware {

  /**
   * Common logger for use in subclasses.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  protected MessageSource messageSource;

  @Override
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Provides handling for standard Framework MVC exceptions.
   *
   * @param ex the target exception
   * @param request the current request
   */
  @ExceptionHandler({
          HttpRequestMethodNotSupportedException.class,
          HttpMediaTypeNotSupportedException.class,
          HttpMediaTypeNotAcceptableException.class,
          MissingPathVariableException.class,
          MissingRequestParameterException.class,
          MissingRequestPartException.class,
          RequestBindingException.class,
          MethodArgumentNotValidException.class,
          HandlerNotFoundException.class,
          AsyncRequestTimeoutException.class,
          ErrorResponseException.class,
          MultipartException.class,
          ConversionNotSupportedException.class,
          TypeMismatchException.class,
          HttpMessageNotReadableException.class,
          HttpMessageNotWritableException.class,
          BindException.class
  })
  @Nullable
  public final ResponseEntity<Object> handleException(Exception ex, RequestContext request) throws Exception {
    // ErrorResponse exceptions that expose HTTP response details

    if (ex instanceof ErrorResponse errorEx) {
      if (ex instanceof HttpRequestMethodNotSupportedException subEx) {
        return handleHttpRequestMethodNotSupported(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof HttpMediaTypeNotSupportedException subEx) {
        return handleHttpMediaTypeNotSupported(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof HttpMediaTypeNotAcceptableException subEx) {
        return handleHttpMediaTypeNotAcceptable(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof MissingPathVariableException subEx) {
        return handleMissingPathVariable(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof MissingRequestParameterException subEx) {
        return handleMissingRequestParameter(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof MissingRequestPartException subEx) {
        return handleMissingRequestPart(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof RequestBindingException subEx) {
        return handleRequestBindingException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof MethodArgumentNotValidException subEx) {
        return handleMethodArgumentNotValid(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof HandlerNotFoundException subEx) {
        return handleHandlerNotFoundException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof AsyncRequestTimeoutException subEx) {
        return handleAsyncRequestTimeoutException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof MultipartException subEx) {
        return handleMultipartException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof ErrorResponseException subEx) {
        return handleErrorResponseException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else {
        // Another ErrorResponse
        return handleExceptionInternal(ex, null, errorEx.getHeaders(), errorEx.getStatusCode(), request);
      }
    }

    // Other, lower level exceptions
    HttpHeaders headers = HttpHeaders.forWritable();
    if (ex instanceof ConversionNotSupportedException subEx) {
      return handleConversionNotSupported(subEx, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    else if (ex instanceof TypeMismatchException subEx) {
      return handleTypeMismatch(subEx, headers, HttpStatus.BAD_REQUEST, request);
    }
    else if (ex instanceof HttpMessageNotReadableException subEx) {
      return handleHttpMessageNotReadable(subEx, headers, HttpStatus.BAD_REQUEST, request);
    }
    else if (ex instanceof HttpMessageNotWritableException subEx) {
      return handleHttpMessageNotWritable(subEx, headers, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    else if (ex instanceof BindException subEx) {
      return handleBindException(subEx, headers, HttpStatus.BAD_REQUEST, request);
    }
    else {
      // Unknown exception, typically a wrapper with a common MVC exception as cause
      // (since @ExceptionHandler type declarations also match first-level causes):
      // We only deal with top-level MVC exceptions here, so let's rethrow the given
      // exception for further processing through the HandlerExceptionHandler chain.
      throw ex;
    }
  }

  /**
   * Customize the handling of {@link HttpRequestMethodNotSupportedException}.
   * <p>This method logs a warning and delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
          HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    NotFoundHandler.pageNotFoundLogger.warn(ex.getMessage());
    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link HttpMediaTypeNotSupportedException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
          HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link HttpMediaTypeNotAcceptableException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
          HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link MissingPathVariableException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMissingPathVariable(
          MissingPathVariableException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link MissingRequestParameterException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMissingRequestParameter(
          MissingRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link MissingRequestPartException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMissingRequestPart(
          MissingRequestPartException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link RequestBindingException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleRequestBindingException(
          RequestBindingException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link MethodArgumentNotValidException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
          MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link HandlerNotFoundException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   * @since 4.0
   */
  @Nullable
  protected ResponseEntity<Object> handleHandlerNotFoundException(
          HandlerNotFoundException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link AsyncRequestTimeoutException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
          AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of any {@link ErrorResponseException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleErrorResponseException(
          ErrorResponseException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of any {@link MultipartException}.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMultipartException(MultipartException ex,
          HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link ConversionNotSupportedException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and also looks up an override for the detail
   * via {@link MessageSource}, before delegating to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
          HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    Object[] args = { ex.getPropertyName(), ex.getValue() };
    String defaultDetail = "Failed to convert '%s' with value: '%s'".formatted(args[0], args[1]);
    ProblemDetail body = createProblemDetail(ex, status, defaultDetail, null, args, request);

    return handleExceptionInternal(ex, body, headers, status, request);
  }

  /**
   * Customize the handling of {@link TypeMismatchException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and also looks up an override for the detail
   * via {@link MessageSource}, before delegating to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
          HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    Object[] args = { ex.getPropertyName(), ex.getValue() };

    String defaultDetail = "Failed to convert '%s' with value: '%s'".formatted(args[0], args[1]);
    String messageCode = ErrorResponse.getDefaultDetailMessageCode(TypeMismatchException.class, null);
    ProblemDetail body = createProblemDetail(ex, status, defaultDetail, messageCode, args, request);

    return handleExceptionInternal(ex, body, headers, status, request);
  }

  /**
   * Customize the handling of {@link HttpMessageNotReadableException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and also looks up an override for the detail
   * via {@link MessageSource}, before delegating to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
          HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    ProblemDetail body = createProblemDetail(ex, status, "Failed to read request", null, null, request);
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  /**
   * Customize the handling of {@link HttpMessageNotWritableException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and also looks up an override for the detail
   * via {@link MessageSource}, before delegating to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
          HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    ProblemDetail body = createProblemDetail(ex, status, "Failed to write request", null, null, request);
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  /**
   * Customize the handling of {@link BindException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and then delegates to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param headers the headers to use for the response
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {
    ProblemDetail body = createProblemDetail(ex, status, "Failed to bind request", null, null, request);
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  /**
   * Internal handler method that all others in this class delegate to, for
   * common handling, and for the creation of a {@link ResponseEntity}.
   * <p>The default implementation does the following:
   * <ul>
   * <li>return {@code null} if response is already committed
   * <li>extract the {@link ErrorResponse#getBody() body} from
   * {@link ErrorResponse} exceptions, if the {@code body} is {@code null}.
   * </ul>
   *
   * @param ex the exception to handle
   * @param body the body to use for the response
   * @param headers the headers to use for the response
   * @param statusCode the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
          @Nullable Object body, @Nullable HttpHeaders headers, HttpStatusCode statusCode, RequestContext request) {

    if (HttpStatus.INTERNAL_SERVER_ERROR.isSameCodeAs(statusCode)) {
      request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    }

    if (body == null && ex instanceof ErrorResponse errorResponse) {
      body = errorResponse.updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
    }

    return createResponseEntity(body, headers, statusCode, request);
  }

  /**
   * Convenience method to create a {@link ProblemDetail} for any exception
   * that doesn't implement {@link ErrorResponse}, also performing a
   * {@link MessageSource} lookup for the "detail" field.
   *
   * @param ex the exception being handled
   * @param status the status to associate with the exception
   * @param defaultDetail default value for the "detail" field
   * @param detailMessageCode the code to use to look up the "detail" field
   * through a {@code MessageSource}; if {@code null} then
   * {@link ErrorResponse#getDefaultDetailMessageCode(Class, String)} is used
   * to determine the default message code to use
   * @param detailMessageArguments the arguments to go with the detailMessageCode
   * @param request the current request
   * @return the created {@code ProblemDetail} instance
   * @since 5.0
   */
  protected ProblemDetail createProblemDetail(Exception ex, HttpStatusCode status, String defaultDetail,
          @Nullable String detailMessageCode, Object @Nullable [] detailMessageArguments, RequestContext request) {

    var builder = ErrorResponse.builder(ex, status, defaultDetail);
    if (detailMessageCode != null) {
      builder.detailMessageCode(detailMessageCode);
    }
    if (detailMessageArguments != null) {
      builder.detailMessageArguments(detailMessageArguments);
    }
    return builder.build().updateAndGetBody(this.messageSource, LocaleContextHolder.getLocale());
  }

  /**
   * Create the {@link ResponseEntity} to use from the given body, headers,
   * and statusCode. Subclasses can override this method to inspect and possibly
   * modify the body, headers, or statusCode, e.g. to re-create an instance of
   * {@link ProblemDetail} as an extension of {@link ProblemDetail}.
   *
   * @param body the body to use for the response
   * @param headers the headers to use for the response
   * @param statusCode the status code to use for the response
   * @param request the current request
   * @return the {@code ResponseEntity} instance to use
   */
  protected ResponseEntity<Object> createResponseEntity(@Nullable Object body,
          @Nullable HttpHeaders headers, HttpStatusCode statusCode, RequestContext request) {

    return new ResponseEntity<>(body, headers, statusCode);
  }

}
