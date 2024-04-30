/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.validation.BindException;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.ErrorResponseException;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.NotFoundHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingPathVariableException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.resolver.MissingRequestPartException;
import cn.taketoday.web.async.AsyncRequestTimeoutException;
import cn.taketoday.web.multipart.MaxUploadSizeExceededException;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;

/**
 * A convenient base class for {@link ControllerAdvice @ControllerAdvice} classes
 * that wish to provide centralized exception handling across all
 * {@code @RequestMapping} methods through {@code @ExceptionHandler} methods.
 *
 * <p>This base class provides an {@code @ExceptionHandler} method for handling
 * internal Framework MVC exceptions. This method returns a {@code ResponseEntity}
 * for writing to the response with a {@link HttpMessageConverter message converter},
 * in contrast to
 * {@link SimpleHandlerExceptionHandler SimpleHandlerExceptionHandler} which returns a
 * {@link ModelAndView ModelAndView}.
 *
 * <p>If there is no need to write error content to the response body, or when
 * using view resolution (e.g., via {@code ContentNegotiatingViewResolver}),
 * then {@code DefaultHandlerExceptionHandler} is good enough.
 *
 * <p>Note that in order for an {@code @ControllerAdvice} subclass to be
 * detected, {@link HandlerExceptionHandler} must be configured.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #handleException(Exception, RequestContext)
 * @see SimpleHandlerExceptionHandler
 * @since 4.0 2022/3/2 18:12
 */
public class ResponseEntityExceptionHandler {

  /**
   * Common logger for use in subclasses.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

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
          MaxUploadSizeExceededException.class,
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
      else if (ex instanceof ErrorResponseException subEx) {
        return handleErrorResponseException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else if (ex instanceof MaxUploadSizeExceededException subEx) {
        return handleMaxUploadSizeExceededException(subEx, subEx.getHeaders(), subEx.getStatusCode(), request);
      }
      else {
        // Another ErrorResponse
        return handleExceptionInternal(ex, null, errorEx.getHeaders(), errorEx.getStatusCode(), request);
      }
    }

    // Other, lower level exceptions

    if (ex instanceof ConversionNotSupportedException subEx) {
      return handleConversionNotSupported(subEx, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    else if (ex instanceof TypeMismatchException subEx) {
      return handleTypeMismatch(subEx, HttpStatus.BAD_REQUEST, request);
    }
    else if (ex instanceof HttpMessageNotReadableException subEx) {
      return handleHttpMessageNotReadable(subEx, HttpStatus.BAD_REQUEST, request);
    }
    else if (ex instanceof HttpMessageNotWritableException subEx) {
      return handleHttpMessageNotWritable(subEx, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    else if (ex instanceof BindException subEx) {
      return handleBindException(subEx, HttpStatus.BAD_REQUEST, request);
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
   * Customize the handling of {@link ConversionNotSupportedException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and then delegates to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleConversionNotSupported(
          ConversionNotSupportedException ex, HttpStatusCode status, RequestContext request) {

    ProblemDetail body = ProblemDetail.forStatusAndDetail(status,
            "Failed to convert '%s' with value: '%s'".formatted(ex.getPropertyName(), ex.getValue()));

    return handleExceptionInternal(ex, body, null, status, request);
  }

  /**
   * Customize the handling of any {@link MaxUploadSizeExceededException}.
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
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
          MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the handling of {@link TypeMismatchException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and then delegates to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpStatusCode status, RequestContext request) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status,
            "Unexpected type for '%s' with value: '%s'".formatted(ex.getPropertyName(), ex.getValue()));

    return handleExceptionInternal(ex, body, null, status, request);
  }

  /**
   * Customize the handling of {@link HttpMessageNotReadableException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and then delegates to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpStatusCode status, RequestContext request) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Failed to read request body");
    return handleExceptionInternal(ex, body, null, status, request);
  }

  /**
   * Customize the handling of {@link HttpMessageNotWritableException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and then delegates to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex, HttpStatusCode status, RequestContext request) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Failed to write response body");
    return handleExceptionInternal(ex, body, null, status, request);
  }

  /**
   * Customize the handling of {@link BindException}.
   * <p>By default this method creates a {@link ProblemDetail} with the status
   * and a short detail message, and then delegates to
   * {@link #handleExceptionInternal}.
   *
   * @param ex the exception to handle
   * @param status the status code to use for the response
   * @param request the current request
   * @return a {@code ResponseEntity} for the response to use, possibly
   * {@code null} when the response is already committed
   */
  @Nullable
  protected ResponseEntity<Object> handleBindException(BindException ex, HttpStatusCode status, RequestContext request) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Failed to bind request");
    return handleExceptionInternal(ex, body, null, status, request);
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
      body = errorResponse.getBody();
    }

    return createResponseEntity(body, headers, statusCode, request);
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
