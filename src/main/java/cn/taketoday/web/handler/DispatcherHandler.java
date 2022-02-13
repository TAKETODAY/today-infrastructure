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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.util.WebUtils;

/**
 * Central dispatcher for HTTP request handlers/controllers
 *
 * @author TODAY 2019-11-16 19:05
 * @since 3.0
 */
public class DispatcherHandler {
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  public static final String BEAN_NAME = "cn.taketoday.web.handler.DispatcherHandler";

  /** Log category to use when no mapped handler is found for a request. */
  public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "cn.taketoday.web.handler.PageNotFound";

  /** Additional logger to use when no mapped handler is found for a request. */
  protected static final Logger pageNotFoundLogger = LoggerFactory.getLogger(PAGE_NOT_FOUND_LOG_CATEGORY);

  /** Action mapping registry */
  private HandlerRegistry handlerRegistry;

  private HandlerAdapter[] handlerAdapters;
  /** exception handler */
  private HandlerExceptionHandler exceptionHandler;
  /** @since 4.0 */
  private SelectableReturnValueHandler returnValueHandler;

  /** Throw a NoHandlerFoundException if no Handler was found to process this request? @since 4.0 */
  private boolean throwExceptionIfNoHandlerFound = false;

  /** Whether to log potentially sensitive info (request params at DEBUG + headers at TRACE). */
  private boolean enableLoggingRequestDetails = false;

  private final WebApplicationContext webApplicationContext;

  public DispatcherHandler(WebApplicationContext context) {
    this.webApplicationContext = context;
  }

  // @since 4.0
  public void init() {
    initStrategies(webApplicationContext);
  }

  /**
   * Initialize the strategy objects that this servlet uses.
   * <p>May be overridden in subclasses in order to initialize further strategy objects.
   */
  protected void initStrategies(ApplicationContext context) {



  }

  // Handler
  // ----------------------------------

  /**
   * Find a suitable handler to handle this HTTP request
   *
   * @param context Current HTTP request context
   * @return Target handler, if returns {@code null} indicates that there isn't a
   * handler to handle this request
   */
  @Nullable
  public Object lookupHandler(final RequestContext context) {
    return handlerRegistry.lookup(context);
  }

  /**
   * Find a {@link HandlerAdapter} for input handler
   *
   * @param handler HTTP handler
   * @return A {@link HandlerAdapter}
   * @throws HandlerAdapterNotFoundException If there isn't a {@link HandlerAdapter} for target handler
   */
  public HandlerAdapter lookupHandlerAdapter(final Object handler) {
    if (handler instanceof HandlerAdapter) {
      return (HandlerAdapter) handler;
    }
    if (handler instanceof HandlerAdapterProvider) {
      return ((HandlerAdapterProvider) handler).getHandlerAdapter();
    }
    for (final HandlerAdapter requestHandler : handlerAdapters) {
      if (requestHandler.supports(handler)) {
        return requestHandler;
      }
    }
    throw new HandlerAdapterNotFoundException(handler);
  }

  /**
   * Find {@link ReturnValueHandler} for handler and handler execution result
   *
   * @param handler HTTP handler
   * @param returnValue Handler execution result
   * @return {@link ReturnValueHandler}
   * @throws ReturnValueHandlerNotFoundException If there isn't a {@link ReturnValueHandler} for target handler and
   * handler execution result
   */
  public ReturnValueHandler lookupReturnValueHandler(Object handler, @Nullable Object returnValue) {
    if (handler instanceof ReturnValueHandler) {
      return (ReturnValueHandler) handler;
    }
    if (handler instanceof ReturnValueHandlerProvider) {
      return ((ReturnValueHandlerProvider) handler).getReturnValueHandler();
    }
    ReturnValueHandler selected = this.returnValueHandler.selectHandler(handler, returnValue);
    if (selected == null) {
      if (returnValue == null) {
        throw new ReturnValueHandlerNotFoundException(handler);
      }
      throw new ReturnValueHandlerNotFoundException(returnValue, handler);
    }
    return selected;
  }

  /**
   * Handle HTTP request
   *
   * @param handler HTTP handler
   * @param context Current HTTP request context
   * @throws Throwable If {@link Throwable} cannot handle
   */
  @Deprecated
  public void handle(@Nullable Object handler, RequestContext context) throws Throwable {
    try {
      Object returnValue = lookupHandlerAdapter(handler).handle(context, handler);
      if (returnValue != HandlerAdapter.NONE_RETURN_VALUE) {
        lookupReturnValueHandler(handler, returnValue)
                .handleReturnValue(context, handler, returnValue);
      }
    }
    catch (Throwable e) {
      handleException(handler, e, context);
    }
    finally {
      // @since 3.0 cleanup MultipartFiles
      context.cleanupMultipartFiles();
    }
  }

  /**
   * Handle {@link Exception} occurred in target handler
   *
   * @param handler HTTP handler
   * @param exception {@link Throwable} occurred in target handler
   * @param context Current HTTP request context
   * @throws Throwable If {@link Throwable} cannot be handled
   * @throws ReturnValueHandlerNotFoundException not found ReturnValueHandler
   * @throws IOException throws when write data to response
   */
  public void handleException(
          @Nullable Object handler, Throwable exception, RequestContext context) throws Throwable {
    // clear context
    context.reset();
    // prepare context throwable
    context.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, exception);
    // handle exception
    Object returnValue = exceptionHandler.handleException(context, exception, handler);
    if (returnValue == null) {
      throw exception;
    }
    else if (returnValue != HandlerAdapter.NONE_RETURN_VALUE) {
      returnValueHandler.handleReturnValue(context, null, returnValue);
    }
  }

  /**
   * Set whether to throw a NoHandlerFoundException when no Handler was found for this request.
   * This exception can then be caught with a HandlerExceptionResolver or an
   * {@code @ExceptionHandler} controller method.
   *
   * @since 4.0
   */
  public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
    this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
  }

  /**
   * Process the actual dispatching to the handler.
   *
   * @param context current HTTP request and HTTP response
   * @throws Exception in case of any kind of processing failure
   * @since 4.0
   */
  public void dispatch(RequestContext context) throws Throwable {
    Object handler = null;
    Object returnValue = null;
    Throwable throwable = null;
    try {
      // Determine handler for the current request.
      handler = lookupHandler(context);
      if (handler == null) {
        noHandlerFound(context);
        return;
      }
      // Actually invoke the handler.
      returnValue = lookupHandlerAdapter(handler).handle(context, handler);
    }
    catch (Throwable ex) {
      throwable = ex;
    }
    finally {
      processDispatchResult(context, handler, returnValue, throwable);
      // @since 3.0 cleanup MultipartFiles
      context.cleanupMultipartFiles();
      logResult(context, throwable);
    }
  }

  /**
   * Handle the result of handler selection and handler invocation, which is
   * either a ModelAndView or an Exception to be resolved to a ModelAndView.
   */
  private void processDispatchResult(
          RequestContext request, @Nullable Object handler,
          @Nullable Object returnValue, @Nullable Throwable exception) throws Throwable {

    if (exception != null) {
      returnValue = processHandlerException(request, handler, exception);
    }

    // Did the handler return a view to render?
    if (returnValue != HandlerAdapter.NONE_RETURN_VALUE) {
      lookupReturnValueHandler(handler, returnValue)
              .handleReturnValue(request, handler, returnValue);
    }
  }

  /**
   * Determine an error view via the registered HandlerExceptionHandlers.
   *
   * @param request current HTTP request
   * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
   * @param ex the exception that got thrown during handler execution
   * @return a corresponding view to forward to
   * @throws Exception if no error view found
   */
  @Nullable
  protected Object processHandlerException(
          RequestContext request, @Nullable Object handler, Throwable ex) throws Throwable {
    // clear context
    request.reset();
    // prepare context throwable
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    // Check registered HandlerExceptionResolvers...
    Object returnValue = exceptionHandler.handleException(request, ex, handler);
    if (returnValue == null) {
      throw ex;
    }
    else if (returnValue != HandlerAdapter.NONE_RETURN_VALUE) {
      if (logger.isTraceEnabled()) {
        logger.trace("Using resolved error view: {}", returnValue, ex);
      }
      else if (logger.isDebugEnabled()) {
        logger.debug("Using resolved error view: {}", returnValue);
      }
    }
    return returnValue;
  }

  /**
   * No handler found &rarr; set appropriate HTTP response status.
   *
   * @param request current HTTP request
   * @throws Exception if preparing the response failed
   */
  protected void noHandlerFound(RequestContext request) throws Exception {
    if (pageNotFoundLogger.isWarnEnabled()) {
      pageNotFoundLogger.warn("No mapping for {} {}", request.getMethodValue(), request.getRequestPath());
    }
    if (this.throwExceptionIfNoHandlerFound) {
      throw new NoHandlerFoundException(
              request.getMethodValue(), request.getRequestPath(), request.requestHeaders());
    }
    else {
      request.sendError(HttpStatus.NOT_FOUND.value());
    }
  }

  private void logResult(RequestContext request, @Nullable Throwable failureCause) {
    if (logger.isDebugEnabled()) {
      if (failureCause != null) {
        if (logger.isTraceEnabled()) {
          logger.trace("Failed to complete request", failureCause);
        }
        else {
          logger.debug("Failed to complete request: {}", failureCause.toString());
        }
      }
      else {
        String headers = "";  // nothing below trace
        if (logger.isTraceEnabled()) {
          HttpHeaders httpHeaders = request.responseHeaders();
          if (this.enableLoggingRequestDetails) {
            headers = httpHeaders.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(", "));
          }
          else {
            headers = httpHeaders.isEmpty() ? "" : "masked";
          }
          headers = ", headers={" + headers + "}";
        }
        HttpStatus httpStatus = HttpStatus.resolve(request.getStatus());
        logger.debug("Completed {}{}", httpStatus != null ? httpStatus : request.getStatus(), headers);
      }
    }
  }

  // @since 4.0
  public WebApplicationContext getWebApplicationContext() {
    return webApplicationContext;
  }

  /**
   * Destroy Application
   */
  public void destroy() {
    final ApplicationContext context = getWebApplicationContext();
    if (context != null) {
      final State state = context.getState();
      if (state != State.CLOSING && state != State.CLOSED) {
        context.close();

        final DateFormat dateFormat = new SimpleDateFormat(Constant.DEFAULT_DATE_FORMAT);
        final String msg = new StringBuilder("Your application destroyed at: [")
                .append(dateFormat.format(System.currentTimeMillis()))
                .append("] on startup date: [")
                .append(dateFormat.format(context.getStartupDate()))
                .append(']')
                .toString();

        log(msg);
      }
    }
  }

  /**
   * Log internal
   *
   * @param msg Log message
   */
  protected void log(final String msg) {
    logger.info(msg);
  }

  public HandlerAdapter[] getHandlerAdapters() {
    return handlerAdapters;
  }

  public HandlerRegistry getHandlerRegistry() {
    return handlerRegistry;
  }

  public HandlerExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public void setHandlerRegistry(HandlerRegistry handlerRegistry) {
    Assert.notNull(handlerRegistry, "HandlerRegistry must not be null");
    this.handlerRegistry = handlerRegistry;
  }

  public void setHandlerAdapters(HandlerAdapter... handlerAdapters) {
    Assert.notNull(handlerAdapters, "handlerAdapters must not be null");
    this.handlerAdapters = handlerAdapters;
  }

  public void setExceptionHandler(HandlerExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler must not be null");
    this.exceptionHandler = exceptionHandler;
  }

  public SelectableReturnValueHandler getReturnValueHandler() {
    return returnValueHandler;
  }

  public void setReturnValueHandler(SelectableReturnValueHandler returnValueHandler) {
    Assert.notNull(returnValueHandler, "returnValueHandler must not be null");
    this.returnValueHandler = returnValueHandler;
  }

  /**
   * Whether to log request params at DEBUG level, and headers at TRACE level.
   * Both may contain sensitive information.
   * <p>By default set to {@code false} so that request details are not shown.
   *
   * @param enable whether to enable or not
   * @since 4.0
   */
  public void setEnableLoggingRequestDetails(boolean enable) {
    this.enableLoggingRequestDetails = enable;
  }

  /**
   * Whether logging of potentially sensitive, request details at DEBUG and
   * TRACE level is allowed.
   *
   * @since 4.0
   */
  public boolean isEnableLoggingRequestDetails() {
    return this.enableLoggingRequestDetails;
  }
}
