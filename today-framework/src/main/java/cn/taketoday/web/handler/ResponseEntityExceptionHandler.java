/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.validation.BindException;
import cn.taketoday.web.context.async.AsyncRequestTimeoutException;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.ErrorResponseException;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.MissingPathVariableException;
import cn.taketoday.web.bind.resolver.MissingServletRequestPartException;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;
import jakarta.servlet.http.HttpServletResponse;

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
   * Log category to use when no mapped handler is found for a request.
   *
   * @see #pageNotFoundLogger
   */
  public static final String PAGE_NOT_FOUND_LOG_CATEGORY = DispatcherHandler.PAGE_NOT_FOUND_LOG_CATEGORY;

  /**
   * Specific logger to use when no mapped handler is found for a request.
   *
   * @see #PAGE_NOT_FOUND_LOG_CATEGORY
   */
  protected static final Logger pageNotFoundLogger = LoggerFactory.getLogger(PAGE_NOT_FOUND_LOG_CATEGORY);

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
          MissingServletRequestPartException.class,
          RequestBindingException.class,
          MethodArgumentNotValidException.class,
          NoHandlerFoundException.class,
          AsyncRequestTimeoutException.class,
          ErrorResponseException.class,
          ConversionNotSupportedException.class,
          TypeMismatchException.class,
          HttpMessageNotReadableException.class,
          HttpMessageNotWritableException.class,
          BindException.class
  })
  @Nullable
  public final ResponseEntity<Object> handleException(Exception ex, RequestContext request) throws Exception {
    HttpHeaders headers = HttpHeaders.create();

    // ErrorResponse exceptions that expose HTTP response details

    if (ex instanceof ErrorResponse errorEx) {
      if (ex instanceof HttpRequestMethodNotSupportedException subEx) {
        return handleHttpRequestMethodNotSupported(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof HttpMediaTypeNotSupportedException subEx) {
        return handleHttpMediaTypeNotSupported(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof HttpMediaTypeNotAcceptableException subEx) {
        return handleHttpMediaTypeNotAcceptable(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof MissingPathVariableException subEx) {
        return handleMissingPathVariable(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof MissingRequestParameterException subEx) {
        return handleMissingServletRequestParameter(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof MissingServletRequestPartException subEx) { // FIXME Servlet
        return handleMissingServletRequestPart(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof RequestBindingException subEx) {
        return handleRequestBindingException(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof MethodArgumentNotValidException subEx) {
        return handleMethodArgumentNotValid(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof NoHandlerFoundException subEx) {
        return handleNoHandlerFoundException(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else if (ex instanceof AsyncRequestTimeoutException subEx) {
        return handleAsyncRequestTimeoutException(subEx, subEx.getHeaders(), subEx.getStatus(), request);
      }
      else {
        // Another ErrorResponseException
        return handleExceptionInternal(ex, null, errorEx.getHeaders(), errorEx.getStatus(), request);
      }
    }

    // Other, lower level exceptions

    if (ex instanceof ConversionNotSupportedException) {
      HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
      return handleConversionNotSupported((ConversionNotSupportedException) ex, headers, status, request);
    }
    else if (ex instanceof TypeMismatchException) {
      HttpStatus status = HttpStatus.BAD_REQUEST;
      return handleTypeMismatch((TypeMismatchException) ex, headers, status, request);
    }
    else if (ex instanceof HttpMessageNotReadableException) {
      HttpStatus status = HttpStatus.BAD_REQUEST;
      return handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, headers, status, request);
    }
    else if (ex instanceof HttpMessageNotWritableException) {
      HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
      return handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, headers, status, request);
    }
    else if (ex instanceof BindException) {
      HttpStatus status = HttpStatus.BAD_REQUEST;
      return handleBindException((BindException) ex, headers, status, request);
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
   * Customize the response for HttpRequestMethodNotSupportedException.
   * <p>This method logs a warning, and delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
          HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    pageNotFoundLogger.warn(ex.getMessage());
    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for HttpMediaTypeNotSupportedException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
          HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for HttpMediaTypeNotAcceptableException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
          HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for MissingPathVariableException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   * @since 4.0
   */
  @Nullable
  protected ResponseEntity<Object> handleMissingPathVariable(
          MissingPathVariableException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for MissingServletRequestParameterException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
          MissingRequestParameterException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for MissingServletRequestPartException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMissingServletRequestPart(
          MissingServletRequestPartException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for RequestBindingException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleRequestBindingException(
          RequestBindingException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for MethodArgumentNotValidException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
          MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for NoHandlerFoundException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   * @since 4.0
   */
  @Nullable
  protected ResponseEntity<Object> handleNoHandlerFoundException(
          NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for AsyncRequestTimeoutException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param webRequest the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
          AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatus status, RequestContext webRequest) {

    return handleExceptionInternal(ex, null, headers, status, webRequest);
  }

  /**
   * Customize the response for ConversionNotSupportedException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleConversionNotSupported(
          ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for TypeMismatchException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleTypeMismatch(
          TypeMismatchException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for HttpMessageNotReadableException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
          HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for HttpMessageNotWritableException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleHttpMessageNotWritable(
          HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * Customize the response for BindException.
   * <p>This method delegates to {@link #handleExceptionInternal}.
   *
   * @param ex the exception
   * @param headers the headers to be written to the response
   * @param status the selected response status
   * @param request the current request
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleBindException(
          BindException ex, HttpHeaders headers, HttpStatus status, RequestContext request) {

    return handleExceptionInternal(ex, null, headers, status, request);
  }

  /**
   * A single place to customize the response body of all exception types.
   * <p>The default implementation sets the {@link WebUtils#ERROR_EXCEPTION_ATTRIBUTE}
   * request attribute and creates a {@link ResponseEntity} from the given
   * body, headers, and status.
   *
   * @param ex the exception
   * @param body the body for the response
   * @param headers the headers for the response
   * @param status the response status
   * @param context the current request context
   * @return {@code ResponseEntity} or {@code null} if response is committed
   */
  @Nullable
  protected ResponseEntity<Object> handleExceptionInternal(
          Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, RequestContext context) {

    if (ServletDetector.runningInServlet(context)) {
      HttpServletResponse response = ServletUtils.getServletResponse(context);
      if (response != null && response.isCommitted()) {
        if (logger.isWarnEnabled()) {
          logger.warn("Ignoring exception, response committed. : {}", ex.toString());
        }
        return null;
      }
    }

    if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
      context.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    }

    if (body == null && ex instanceof ErrorResponse errorResponse) {
      body = errorResponse.getBody();
    }

    return new ResponseEntity<>(body, headers, status);
  }

}
