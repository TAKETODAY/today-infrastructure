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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.HandlerAdapterNotFoundException;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlerNotFoundException;
import cn.taketoday.web.view.ReturnValueHandlerProvider;
import cn.taketoday.web.view.SelectableReturnValueHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Central dispatcher for HTTP request handlers/controllers
 *
 * @author TODAY 2019-11-16 19:05
 * @since 3.0
 */
public class DispatcherHandler extends WebApplicationContextSupport {
  public static final String DEFAULT_BEAN_NAME = "cn.taketoday.web.handler.DispatcherHandler";

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

  public DispatcherHandler() { }

  public DispatcherHandler(WebApplicationContext context) {
    setApplicationContext(context);
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
    try {
      return handlerRegistry.lookup(context);
    }
    catch (Throwable e) {
      handleException(null, e, context);
    }
  }

  /**
   * Find a {@link HandlerAdapter} for input handler
   *
   * @param handler HTTP handler
   * @return A {@link HandlerAdapter}
   * @throws HandlerAdapterNotFoundException If there isn't a {@link HandlerAdapter} for target handler
   */
  private HandlerAdapter lookupHandlerAdapter(final Object handler) {
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
   * @param context Current HTTP request context
   * @throws Throwable If {@link Throwable} cannot handle
   */
  public void handle(final RequestContext context) throws Throwable {
    handle(lookupHandler(context), context);
  }

  /**
   * Handle HTTP request
   *
   * @param handler HTTP handler
   * @param context Current HTTP request context
   * @throws Throwable If {@link Throwable} cannot handle
   */
  public void handle(@Nullable final Object handler, final RequestContext context) throws Throwable {
    try {
      final Object returnValue = lookupHandlerAdapter(handler).handle(context, handler);
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
   */
  public void dispatch(RequestContext context) throws Throwable {

    try {
      // Determine handler for the current request.
      Object handler = lookupHandler(context);
      if (handler == null) {
        noHandlerFound(context);
        return;
      }

      // Determine handler adapter for the current request.
      HandlerAdapter ha = lookupHandlerAdapter(handler);

      // Actually invoke the handler.
      Object returnValue = lookupHandlerAdapter(handler).handle(context, handler);
      if (returnValue != HandlerAdapter.NONE_RETURN_VALUE) {
        lookupReturnValueHandler(handler, returnValue)
                .handleReturnValue(context, handler, returnValue);
      }

      if (asyncManager.isConcurrentHandlingStarted()) {
        return;
      }

    }
    catch (Exception ex) {
      dispatchException = ex;
    }
    catch (Throwable err) {
      // As of 4.3, we're processing Errors thrown from handler methods as well,
      // making them available for @ExceptionHandler methods and other scenarios.
    }
    processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);

  }

  /**
   * Handle the result of handler selection and handler invocation, which is
   * either a ModelAndView or an Exception to be resolved to a ModelAndView.
   */
  private void processDispatchResult(
          HttpServletRequest request, HttpServletResponse response,
          @Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
          @Nullable Exception exception) throws Exception {

    boolean errorView = false;

    if (exception != null) {
      if (exception instanceof ModelAndViewDefiningException) {
        logger.debug("ModelAndViewDefiningException encountered", exception);
        mv = ((ModelAndViewDefiningException) exception).getModelAndView();
      }
      else {
        Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
        mv = processHandlerException(request, response, handler, exception);
        errorView = (mv != null);
      }
    }

    // Did the handler return a view to render?
    if (mv != null && !mv.wasCleared()) {
      render(mv, request, response);
      if (errorView) {
        WebUtils.clearErrorRequestAttributes(request);
      }
    }
    else {
      if (log.isTraceEnabled()) {
        log.trace("No view rendering, null ModelAndView returned.");
      }
    }

    if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
      // Concurrent handling started during a forward
      return;
    }

    if (mappedHandler != null) {
      // Exception (if any) is already handled..
      mappedHandler.triggerAfterCompletion(request, response, null);
    }
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
      request.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * Destroy Application
   */
  public void destroy() {
    final ApplicationContext context = getApplicationContext();
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
    log.info(msg);
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

}
