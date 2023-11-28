/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.io.IOException;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.core.Ordered;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingPathVariableException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.resolver.MissingRequestPartException;
import cn.taketoday.web.context.async.AsyncRequestTimeoutException;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.util.WebUtils;

/**
 * The default implementation of the {@link HandlerExceptionHandler}
 * interface, resolving standard Framework MVC exceptions and translating them to corresponding
 * HTTP status codes.
 *
 * <p>This exception handler is enabled by default in the common Framework
 * {@link DispatcherHandler}.
 *
 * <table>
 * <caption>Supported Exceptions</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">Exception</th>
 * <th class="colLast">HTTP Status Code</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>HttpRequestMethodNotSupportedException</p></td>
 * <td><p>405 (SC_METHOD_NOT_ALLOWED)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>HttpMediaTypeNotSupportedException</p></td>
 * <td><p>415 (SC_UNSUPPORTED_MEDIA_TYPE)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>HttpMediaTypeNotAcceptableException</p></td>
 * <td><p>406 (SC_NOT_ACCEPTABLE)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>MissingPathVariableException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>MissingServletRequestParameterException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>MissingRequestPartException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ServletRequestBindingException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ConversionNotSupportedException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>TypeMismatchException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>HttpMessageNotReadableException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>HttpMessageNotWritableException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>MethodArgumentNotValidException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>BindException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>HandlerNotFoundException</p></td>
 * <td><p>404 (SC_NOT_FOUND)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>AsyncRequestTimeoutException</p></td>
 * <td><p>503 (SC_SERVICE_UNAVAILABLE)</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ResponseEntityExceptionHandler
 * @since 2020-03-29 21:01
 */
public class SimpleHandlerExceptionHandler extends AbstractHandlerExceptionHandler implements HandlerExceptionHandler {

  /**
   * Additional logger to use when no mapped handler is found for a request.
   *
   * @see NotFoundHandler#PAGE_NOT_FOUND_LOG_CATEGORY
   */
  protected static final Logger pageNotFoundLogger = LoggerFactory.getLogger(NotFoundHandler.PAGE_NOT_FOUND_LOG_CATEGORY);

  /**
   * Sets the {@linkplain #setOrder(int) order} to {@link #LOWEST_PRECEDENCE}.
   */
  public SimpleHandlerExceptionHandler() {
    setOrder(Ordered.LOWEST_PRECEDENCE);
    setWarnLogCategory(getClass().getName());
  }

  // @since 4.0
  @Nullable
  @Override
  protected Object handleInternal(RequestContext request, @Nullable Object handler, Throwable ex) {

    try {
      // ErrorResponse exceptions that expose HTTP response details
      if (ex instanceof ErrorResponse) {
        Object view = null;
        if (ex instanceof HttpRequestMethodNotSupportedException) {
          view = handleHttpRequestMethodNotSupported(
                  (HttpRequestMethodNotSupportedException) ex, request, handler);
        }
        else if (ex instanceof HttpMediaTypeNotSupportedException) {
          view = handleHttpMediaTypeNotSupported(
                  (HttpMediaTypeNotSupportedException) ex, request, handler);
        }
        else if (ex instanceof HttpMediaTypeNotAcceptableException) {
          view = handleHttpMediaTypeNotAcceptable(
                  (HttpMediaTypeNotAcceptableException) ex, request, handler);
        }
        else if (ex instanceof MissingPathVariableException) {
          view = handleMissingPathVariable(
                  (MissingPathVariableException) ex, request, handler);
        }
        else if (ex instanceof MissingRequestParameterException) {
          view = handleMissingServletRequestParameter(
                  (MissingRequestParameterException) ex, request, handler);
        }
        else if (ex instanceof MissingRequestPartException) {
          view = handleMissingRequestPartException(
                  (MissingRequestPartException) ex, request, handler);
        }
        else if (ex instanceof RequestBindingException) {
          view = handleRequestBindingException(
                  (RequestBindingException) ex, request, handler);
        }
        else if (ex instanceof MethodArgumentNotValidException) {
          view = handleMethodArgumentNotValidException(
                  (MethodArgumentNotValidException) ex, request, handler);
        }
        else if (ex instanceof HandlerNotFoundException) {
          view = handleHandlerNotFoundException(
                  (HandlerNotFoundException) ex, request, handler);
        }
        else if (ex instanceof AsyncRequestTimeoutException) {
          view = handleAsyncRequestTimeoutException(
                  (AsyncRequestTimeoutException) ex, request, handler);
        }

        if (view == null) {
          return handleErrorResponse((ErrorResponse) ex, request, handler);
        }
      }

      // Other, lower level exceptions

      if (ex instanceof ConversionNotSupportedException) {
        return handleConversionNotSupported(
                (ConversionNotSupportedException) ex, request, handler);
      }
      else if (ex instanceof TypeMismatchException) {
        return handleTypeMismatch(
                (TypeMismatchException) ex, request, handler);
      }
      else if (ex instanceof HttpMessageNotReadableException) {
        return handleHttpMessageNotReadable(
                (HttpMessageNotReadableException) ex, request, handler);
      }
      else if (ex instanceof HttpMessageNotWritableException) {
        return handleHttpMessageNotWritable(
                (HttpMessageNotWritableException) ex, request, handler);
      }
      else if (ex instanceof BindException) {
        return handleBindException((BindException) ex, request, handler);
      }
    }
    catch (Exception handlerEx) {
      logResultedInException(ex, handlerEx);
    }

    return null; //next
  }

  /**
   * Handle the case where no handler was found for the HTTP method.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the HttpRequestMethodNotSupportedException to be handled
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen
   * at the time of the exception (for example, if multipart resolution failed)
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case where no
   * {@linkplain cn.taketoday.http.converter.HttpMessageConverter message converters}
   * were found for PUT or POSTed content.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the HttpMediaTypeNotSupportedException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case where no
   * {@linkplain cn.taketoday.http.converter.HttpMessageConverter message converters}
   * were found that were acceptable for the client (expressed via the {@code Accept} header.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the HttpMediaTypeNotAcceptableException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case when a declared path variable does not match any extracted URI variable.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the MissingPathVariableException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleMissingPathVariable(MissingPathVariableException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case when a required parameter is missing.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the MissingServletRequestParameterException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleMissingServletRequestParameter(MissingRequestParameterException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case where an {@linkplain RequestPart @RequestPart}, a {@link MultipartFile},
   * or a {@code jakarta.servlet.http.Part} argument is required but is missing.
   * <p>By default, an HTTP 400 error is sent back to the client.
   *
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleMissingRequestPartException(MissingRequestPartException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case when an unrecoverable binding exception occurs - e.g.
   * required header, required cookie.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the exception to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleRequestBindingException(RequestBindingException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case where an argument annotated with {@code @Valid} such as
   * an {@link RequestBody} or {@link RequestPart} argument fails validation.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case where no handler was found during the dispatch.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the HandlerNotFoundException to be handled
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen
   * at the time of the exception (for example, if multipart resolution failed)
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleHandlerNotFoundException(HandlerNotFoundException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    pageNotFoundLogger.warn(ex.getMessage());
    return null;
  }

  /**
   * Handle the case where an async request timed out.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the {@link AsyncRequestTimeoutException} to be handled
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen
   * at the time of the exception (for example, if multipart resolution failed)
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  @Nullable
  protected Object handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle an {@link ErrorResponse} exception.
   * <p>The default implementation sets status and the headers of the response
   * to those obtained from the {@code ErrorResponse}. If available, the
   * {@link ProblemDetail#getDetail()}  is used as the message for
   * {@link RequestContext#sendError(int, String)}.
   *
   * @param errorResponse the exception to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  protected Object handleErrorResponse(ErrorResponse errorResponse,
          RequestContext request, @Nullable Object handler) throws IOException {

    if (!request.isCommitted()) {
      HttpHeaders headers = errorResponse.getHeaders();
      request.mergeToResponse(headers);

      String message = errorResponse.getBody().getDetail();
      request.sendError(errorResponse.getStatusCode(), message);
    }
    else {
      logger.warn("Ignoring exception, response committed. : {}", errorResponse);
    }

    return NONE_RETURN_VALUE;
  }

  /**
   * Handle the case when a {@link cn.taketoday.web.bind.WebDataBinder} conversion cannot occur.
   * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code Object}.
   * Alternatively, a fallback view could be chosen, or the ConversionNotSupportedException could be
   * rethrown as-is.
   *
   * @param ex the ConversionNotSupportedException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  protected Object handleConversionNotSupported(ConversionNotSupportedException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    sendServerError(ex, request);
    return NONE_RETURN_VALUE;
  }

  /**
   * Handle the case when a {@link cn.taketoday.web.bind.WebDataBinder} conversion error occurs.
   * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code Object}.
   * Alternatively, a fallback view could be chosen, or the TypeMismatchException could be rethrown as-is.
   *
   * @param ex the TypeMismatchException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  protected Object handleTypeMismatch(TypeMismatchException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    request.sendError(HttpStatus.BAD_REQUEST);
    return NONE_RETURN_VALUE;
  }

  /**
   * Handle the case where a {@linkplain cn.taketoday.http.converter.HttpMessageConverter message converter}
   * cannot read from an HTTP request.
   * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code Object}.
   * Alternatively, a fallback view could be chosen, or the HttpMessageNotReadableException could be
   * rethrown as-is.
   *
   * @param ex the HttpMessageNotReadableException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  protected Object handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    request.sendError(HttpStatus.BAD_REQUEST);
    return NONE_RETURN_VALUE;
  }

  /**
   * Handle the case where a
   * {@linkplain cn.taketoday.http.converter.HttpMessageConverter message converter}
   * cannot write to an HTTP request.
   * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code Object}.
   * Alternatively, a fallback view could be chosen, or the HttpMessageNotWritableException could
   * be rethrown as-is.
   *
   * @param ex the HttpMessageNotWritableException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  protected Object handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    sendServerError(ex, request);
    return NONE_RETURN_VALUE;
  }

  /**
   * Handle the case where an argument has binding or validation errors and is
   * not followed by another method argument of type {@link BindingResult}.
   * <p>By default, an HTTP 400 error is sent back to the client.
   *
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty Object indicating the exception was handled
   * @throws IOException potentially thrown from {@link RequestContext#sendError}
   * @since 4.0
   */
  protected Object handleBindException(BindException ex, RequestContext request,
          @Nullable Object handler) throws IOException {

    request.sendError(HttpStatus.BAD_REQUEST);
    return NONE_RETURN_VALUE;
  }

  /**
   * Invoked to send a server error. Sets the status to 500 and also sets the
   * request attribute {@link WebUtils#ERROR_EXCEPTION_ATTRIBUTE} to the Exception.
   *
   * @see WebUtils#ERROR_EXCEPTION_ATTRIBUTE
   * @since 4.0
   */
  protected void sendServerError(Exception ex, RequestContext request) throws IOException {
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    request.sendError(HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
