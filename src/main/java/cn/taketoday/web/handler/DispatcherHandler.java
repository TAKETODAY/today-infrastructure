/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContext.State;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.view.SelectableReturnValueHandler;
import cn.taketoday.web.view.HandlerAdapterNotFoundException;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlerNotFoundException;
import cn.taketoday.web.view.ReturnValueHandlerProvider;

/**
 * Central dispatcher for HTTP request handlers/controllers
 *
 * @author TODAY 2019-11-16 19:05
 * @since 3.0
 */
public class DispatcherHandler extends WebApplicationContextSupport {

  /** Action mapping registry */
  private HandlerRegistry handlerRegistry;
  private HandlerAdapter[] handlerAdapters;
  /** exception handler */
  private HandlerExceptionHandler exceptionHandler;
  /** @since 4.0 */
  private SelectableReturnValueHandler returnValueHandler;

  public DispatcherHandler() { }

  public DispatcherHandler(WebApplicationContext context) {
    setApplicationContext(context);
  }

  // Handler
  // ----------------------------------

  /**
   * Find a suitable handler to handle this HTTP request
   *
   * @param context
   *         Current HTTP request context
   *
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
   * @param handler
   *         HTTP handler
   *
   * @return A {@link HandlerAdapter}
   *
   * @throws HandlerAdapterNotFoundException
   *         If there isn't a {@link HandlerAdapter} for target handler
   */
  private HandlerAdapter lookupHandlerAdapter(final Object handler) {
    if (handler instanceof HandlerAdapter) {
      return (HandlerAdapter) handler;
    }
    if (handler instanceof HandlerAdapterCapable) {
      return ((HandlerAdapterCapable) handler).getHandlerAdapter();
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
   * @param handler
   *         HTTP handler
   * @param returnValue
   *         Handler execution result
   *
   * @return {@link ReturnValueHandler}
   *
   * @throws ReturnValueHandlerNotFoundException
   *         If there isn't a {@link ReturnValueHandler} for target handler and
   *         handler execution result
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
      throw new ReturnValueHandlerNotFoundException(returnValue, handler);
    }
    return selected;
  }

  /**
   * Handle HTTP request
   *
   * @param context
   *         Current HTTP request context
   *
   * @throws Throwable
   *         If {@link Throwable} cannot handle
   */
  public void handle(final RequestContext context) throws Throwable {
    handle(lookupHandler(context), context);
  }

  /**
   * Handle HTTP request
   *
   * @param handler
   *         HTTP handler
   * @param context
   *         Current HTTP request context
   *
   * @throws Throwable
   *         If {@link Throwable} cannot handle
   */
  public void handle(@Nullable final Object handler, final RequestContext context) throws Throwable {
    try {
      final Object result = lookupHandlerAdapter(handler).handle(context, handler);
      if (result != HandlerAdapter.NONE_RETURN_VALUE) {
        lookupReturnValueHandler(handler, result)
                .handleReturnValue(context, handler, result);
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
   * @param handler
   *         HTTP handler
   * @param exception
   *         {@link Throwable} occurred in target handler
   * @param context
   *         Current HTTP request context
   *
   * @throws Throwable
   *         If {@link Throwable} occurred in
   *         {@link HandlerExceptionHandler} cannot handled
   */
  public void handleException(final Object handler,
                              final Throwable exception,
                              final RequestContext context) throws Throwable {
    // clear context
    context.reset();
    // handle exception
    final Object result = getExceptionHandler()
            .handleException(context, ExceptionUtils.unwrapThrowable(exception), handler);
    if (result != HandlerAdapter.NONE_RETURN_VALUE) {
      returnValueHandler.handleSelectively(context, handler, result);
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
   * @param msg
   *         Log message
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
