/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

import cn.taketoday.beans.ConversionNotSupportedException;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCapable;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.AsyncRequestTimeoutException;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.HttpRequestMethodNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.bind.MethodArgumentNotValidException;
import cn.taketoday.web.bind.MissingRequestParameterException;
import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.bind.resolver.MissingPathVariableException;
import cn.taketoday.web.bind.resolver.MissingServletRequestPartException;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.resource.ResourceHttpRequestHandler;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The default implementation of the {@link HandlerExceptionHandler}
 * interface, resolving standard Framework MVC exceptions and translating them to corresponding
 * HTTP status codes.
 *
 * <p>This exception resolver is enabled by default in the common Spring
 * {@link cn.taketoday.web.servlet.DispatcherServlet}.
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
 * <td><p>MissingServletRequestPartException</p></td>
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
 * <td><p>NoHandlerFoundException</p></td>
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
 * @author TODAY 2020-03-29 21:01
 * @see ResponseEntityExceptionHandler
 */
public class SimpleExceptionHandler
        extends AbstractHandlerExceptionHandler implements HandlerExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(SimpleExceptionHandler.class);

  /**
   * Log category to use when no mapped handler is found for a request.
   *
   * @see #pageNotFoundLogger
   */
  public static final String PAGE_NOT_FOUND_LOG_CATEGORY = DispatcherHandler.PAGE_NOT_FOUND_LOG_CATEGORY;

  /**
   * Additional logger to use when no mapped handler is found for a request.
   *
   * @see #PAGE_NOT_FOUND_LOG_CATEGORY
   */
  protected static final Logger pageNotFoundLogger = LoggerFactory.getLogger(PAGE_NOT_FOUND_LOG_CATEGORY);

  /**
   * Sets the {@linkplain #setOrder(int) order} to {@link #LOWEST_PRECEDENCE}.
   */
  public SimpleExceptionHandler() {
    setOrder(Ordered.LOWEST_PRECEDENCE);
    setWarnLogCategory(getClass().getName());
  }

  @Override
  public Object handleException(
          RequestContext context, Throwable target, Object handler) {
    logCatchThrowable(target);
    try {
      if (handler instanceof HandlerMethod) {
        return handleHandlerMethodInternal(target, context, (HandlerMethod) handler);
      }
      if (handler instanceof ViewController) {
        return handleViewControllerInternal(target, context, (ViewController) handler);
      }
      if (handler instanceof ResourceHttpRequestHandler) {
        return handleResourceHandlerInternal(target, context, (ResourceHttpRequestHandler) handler);
      }
      return handleExceptionInternal(target, context);
    }
    catch (Throwable handlerEx) {
      logResultedInException(target, handlerEx);
      // next in the chain
      return null;
    }
  }

  // @since 4.0
  @Nullable
  @Override
  protected Object doResolveException(RequestContext request, @Nullable Object handler, Throwable ex) {

    try {
      // ErrorResponse exceptions that expose HTTP response details
      if (ex instanceof ErrorResponse) {
        ModelAndView mav = null;
        if (ex instanceof HttpRequestMethodNotSupportedException) {
          mav = handleHttpRequestMethodNotSupported(
                  (HttpRequestMethodNotSupportedException) ex, request, handler);
        }
        else if (ex instanceof HttpMediaTypeNotSupportedException) {
          mav = handleHttpMediaTypeNotSupported(
                  (HttpMediaTypeNotSupportedException) ex, request, handler);
        }
        else if (ex instanceof HttpMediaTypeNotAcceptableException) {
          mav = handleHttpMediaTypeNotAcceptable(
                  (HttpMediaTypeNotAcceptableException) ex, request, handler);
        }
        else if (ex instanceof MissingPathVariableException) {
          mav = handleMissingPathVariable(
                  (MissingPathVariableException) ex, request, handler);
        }
        else if (ex instanceof MissingRequestParameterException) {
          mav = handleMissingServletRequestParameter(
                  (MissingRequestParameterException) ex, request, handler);
        }
        else if (ex instanceof MissingServletRequestPartException) {
          mav = handleMissingServletRequestPartException(
                  (MissingServletRequestPartException) ex, request, handler);
        }
        else if (ex instanceof RequestBindingException) {
          mav = handleServletRequestBindingException(
                  (RequestBindingException) ex, request, handler);
        }
        else if (ex instanceof MethodArgumentNotValidException) {
          mav = handleMethodArgumentNotValidException(
                  (MethodArgumentNotValidException) ex, request, handler);
        }
        else if (ex instanceof NoHandlerFoundException) {
          mav = handleNoHandlerFoundException(
                  (NoHandlerFoundException) ex, request, handler);
        }
        else if (ex instanceof AsyncRequestTimeoutException) {
          mav = handleAsyncRequestTimeoutException(
                  (AsyncRequestTimeoutException) ex, request, handler);
        }

        if (mav == null) {
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
      logger.warn("Failure while trying to resolve exception [{}]", ex.getClass().getName(), handlerEx);
    }

    return null;
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleMissingPathVariable(MissingPathVariableException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleMissingServletRequestParameter(MissingRequestParameterException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleMissingServletRequestPartException(MissingServletRequestPartException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleServletRequestBindingException(RequestBindingException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle the case where no handler was found during the dispatch.
   * <p>The default implementation returns {@code null} in which case the
   * exception is handled in {@link #handleErrorResponse}.
   *
   * @param ex the NoHandlerFoundException to be handled
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen
   * at the time of the exception (for example, if multipart resolution failed)
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex,
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
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  @Nullable
  protected ModelAndView handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    return null;
  }

  /**
   * Handle an {@link ErrorResponse} exception.
   * <p>The default implementation sets status and the headers of the response
   * to those obtained from the {@code ErrorResponse}. If available, the
   * {@link ProblemDetail#getDetail()}  is used as the message for
   * {@link HttpServletResponse#sendError(int, String)}.
   *
   * @param errorResponse the exception to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  protected ModelAndView handleErrorResponse(ErrorResponse errorResponse,
          RequestContext request, @Nullable Object handler) throws IOException {

    if (!request.isCommitted()) {
      HttpHeaders headers = errorResponse.getHeaders();
      request.responseHeaders().addAll(headers);

      int status = errorResponse.getRawStatusCode();
      String message = errorResponse.getBody().getDetail();
      if (message != null) {
        request.sendError(status, message);
      }
      else {
        request.sendError(status);
      }
    }
    else {
      logger.warn("Ignoring exception, response committed. : {}", errorResponse);
    }

    return new ModelAndView();
  }

  /**
   * Handle the case when a {@link cn.taketoday.web.bind.WebDataBinder} conversion cannot occur.
   * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
   * Alternatively, a fallback view could be chosen, or the ConversionNotSupportedException could be
   * rethrown as-is.
   *
   * @param ex the ConversionNotSupportedException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  protected ModelAndView handleConversionNotSupported(ConversionNotSupportedException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    sendServerError(ex, request);
    return new ModelAndView();
  }

  /**
   * Handle the case when a {@link cn.taketoday.web.bind.WebDataBinder} conversion error occurs.
   * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code ModelAndView}.
   * Alternatively, a fallback view could be chosen, or the TypeMismatchException could be rethrown as-is.
   *
   * @param ex the TypeMismatchException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    request.sendError(HttpServletResponse.SC_BAD_REQUEST);
    return new ModelAndView();
  }

  /**
   * Handle the case where a {@linkplain cn.taketoday.http.converter.HttpMessageConverter message converter}
   * cannot read from an HTTP request.
   * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code ModelAndView}.
   * Alternatively, a fallback view could be chosen, or the HttpMessageNotReadableException could be
   * rethrown as-is.
   *
   * @param ex the HttpMessageNotReadableException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    request.sendError(HttpServletResponse.SC_BAD_REQUEST);
    return new ModelAndView();
  }

  /**
   * Handle the case where a
   * {@linkplain cn.taketoday.http.converter.HttpMessageConverter message converter}
   * cannot write to an HTTP request.
   * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
   * Alternatively, a fallback view could be chosen, or the HttpMessageNotWritableException could
   * be rethrown as-is.
   *
   * @param ex the HttpMessageNotWritableException to be handled
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  protected ModelAndView handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
          RequestContext request, @Nullable Object handler) throws IOException {

    sendServerError(ex, request);
    return new ModelAndView();
  }

  /**
   * Handle the case where an {@linkplain ModelAttribute @ModelAttribute} method
   * argument has binding or validation errors and is not followed by another
   * method argument of type {@link BindingResult}.
   * <p>By default, an HTTP 400 error is sent back to the client.
   *
   * @param request current HTTP request
   * @param handler the executed handler
   * @return an empty ModelAndView indicating the exception was handled
   * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
   * @since 4.0
   */
  protected ModelAndView handleBindException(BindException ex, RequestContext request,
          @Nullable Object handler) throws IOException {

    request.sendError(HttpServletResponse.SC_BAD_REQUEST);
    return new ModelAndView();
  }

  /**
   * Invoked to send a server error. Sets the status to 500 and also sets the
   * request attribute "jakarta.servlet.error.exception" to the Exception.
   *
   * @since 4.0
   */
  protected void sendServerError(Exception ex, RequestContext request) throws IOException {
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    request.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  // old

  /**
   * record exception log occurred in target request handler
   *
   * @param target Throwable occurred in target request handler
   */
  protected void logCatchThrowable(Throwable target) {
    if (log.isDebugEnabled()) {
      log.debug("Catch Throwable: [{}]", target.toString(), target);
    }
  }

  /**
   * record log when an exception occurred in this exception handler
   *
   * @param target Throwable that occurred in request handler
   * @param handlerException Throwable occurred in this exception handler
   */
  protected void logResultedInException(Throwable target, Throwable handlerException) {
    log.error("Handling of [{}] resulted in Exception: [{}]",
            target.getClass().getName(),
            handlerException.getClass().getName(), handlerException);
  }

  /**
   * Resolve {@link ResourceHttpRequestHandler} exception
   *
   * @param ex Target {@link Throwable}
   * @param context Current request context
   * @param handler {@link ResourceHttpRequestHandler}
   * @throws Throwable If any {@link Exception} occurred
   */
  protected Object handleResourceHandlerInternal(
          Throwable ex, RequestContext context, ResourceHttpRequestHandler handler) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link ViewController} exception
   *
   * @param ex Target {@link Throwable}
   * @param context Current request context
   * @param viewController {@link ViewController}
   * @throws Throwable If any {@link Exception} occurred
   */
  protected Object handleViewControllerInternal(
          Throwable ex, RequestContext context, ViewController viewController) throws Throwable {
    return handleExceptionInternal(ex, context);
  }

  /**
   * Resolve {@link HandlerMethod} exception
   *
   * @param ex Target {@link Throwable}
   * @param context Current request context
   * @param handlerMethod {@link HandlerMethod}
   * @throws Throwable If any {@link Exception} occurred
   */
  protected Object handleHandlerMethodInternal(
          Throwable ex, RequestContext context, HandlerMethod handlerMethod) throws Throwable//
  {
    context.setStatus(getErrorStatusValue(ex));

    if (handlerMethod.isReturnTypeAssignableTo(RenderedImage.class)) {
      return resolveImageException(ex, context);
    }
    if (!handlerMethod.isReturn(void.class)
            && !handlerMethod.isReturn(Object.class)
            && !handlerMethod.isReturn(ModelAndView.class)
            && !(handlerMethod.isReturn(String.class) && !handlerMethod.isResponseBody())) {

      return handleExceptionInternal(ex, context);
    }

    writeErrorMessage(ex, context);
    return NONE_RETURN_VALUE;
  }

  /**
   * Write error message to request context, default is write json
   *
   * @param ex Throwable that occurred in request handler
   * @param context current request context
   */
  protected void writeErrorMessage(Throwable ex, RequestContext context) throws IOException {
    context.setContentType(MediaType.APPLICATION_JSON_VALUE);
    PrintWriter writer = context.getWriter();
    writer.write(buildDefaultErrorMessage(ex));
    writer.flush();
  }

  protected String buildDefaultErrorMessage(Throwable ex) {
    return new StringBuilder()
            .append("{\"message\":\"")
            .append(ex.getMessage())
            .append("\"}")
            .toString();
  }

  /**
   * Get error http status value, if target throwable is {@link HttpStatusCapable}
   * its return from {@link HttpStatusCapable#getStatus()}
   *
   * @param ex Throwable that occurred in request handler
   * @return Http status code
   */
  public int getErrorStatusValue(Throwable ex) {
    if (ex instanceof HttpStatusCapable) { // @since 3.0.1
      HttpStatus httpStatus = ((HttpStatusCapable) ex).getStatus();
      return httpStatus.value();
    }
    return HandlerMethod.getStatusValue(ex);
  }

  /**
   * resolve view exception
   *
   * @param ex Target {@link Exception}
   * @param context Current request context
   */
  public Object handleExceptionInternal(
          Throwable ex, RequestContext context) throws IOException {
    context.sendError(getErrorStatusValue(ex), ex.getMessage());
    return NONE_RETURN_VALUE;
  }

  /**
   * resolve image
   */
  public BufferedImage resolveImageException(
          Throwable ex, RequestContext context) throws IOException {
    ClassPathResource pathResource = new ClassPathResource("error/" + getErrorStatusValue(ex) + ".png");
    Assert.state(pathResource.exists(), "System Error");
    context.setContentType(MediaType.IMAGE_JPEG_VALUE);
    return ImageIO.read(pathResource.getInputStream());
  }

}
