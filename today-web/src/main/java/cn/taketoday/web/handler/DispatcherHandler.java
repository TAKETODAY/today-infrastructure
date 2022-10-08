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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ArrayHolder;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.HandlerAdapter;
import cn.taketoday.web.HandlerAdapterNotFoundException;
import cn.taketoday.web.HandlerAdapterProvider;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestHandledListener;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.context.async.WebAsyncUtils;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import cn.taketoday.web.util.WebUtils;

/**
 * Central dispatcher for HTTP request handlers/controllers
 *
 * @author TODAY 2019-11-16 19:05
 * @since 3.0
 */
public class DispatcherHandler extends InfraHandler {

  public static final String BEAN_NAME = "cn.taketoday.web.handler.DispatcherHandler";

  /** Action mapping registry */
  private HandlerMapping handlerMapping;

  private HandlerAdapter handlerAdapter;

  /** exception handler */
  private HandlerExceptionHandler exceptionHandler;

  /** @since 4.0 */
  private SelectableReturnValueHandler returnValueHandler;

  /** Throw a NoHandlerFoundException if no Handler was found to process this request? @since 4.0 */
  private boolean throwExceptionIfNoHandlerFound = false;

  /** Detect all HandlerMappings or just expect "HandlerRegistry" bean?. */
  private boolean detectAllHandlerMapping = true;

  /** Detect all HandlerAdapters or just expect "HandlerAdapter" bean?. */
  private boolean detectAllHandlerAdapters = true;

  /** Detect all HandlerExceptionHandlers or just expect "HandlerExceptionHandler" bean?. */
  private boolean detectAllHandlerExceptionHandlers = true;

  private HttpRequestHandler notFoundHandler;

  private final ArrayHolder<RequestHandledListener> requestHandledActions = ArrayHolder.forGenerator(RequestHandledListener[]::new);

  public DispatcherHandler() { }

  public DispatcherHandler(ApplicationContext context) {
    super(context);
  }

  @Override
  protected void onRefresh(ApplicationContext context) {
    initStrategies(context);
  }

  /**
   * Initialize the strategy objects that this servlet uses.
   * <p>May be overridden in subclasses in order to initialize further strategy objects.
   */
  protected void initStrategies(ApplicationContext context) {
    initHandlerMapping(context);
    initHandlerAdapters(context);
    initReturnValueHandler(context);
    initExceptionHandler(context);
    initNotFoundHandler(context);
    initRequestHandledListeners(context);
  }

  /**
   * Initialize the HandlerRegistries used by this class.
   * <p>If no HandlerRegistry beans are defined in the BeanFactory for this namespace,
   * we default to BeanNameUrlHandlerRegistry.
   */
  private void initHandlerMapping(ApplicationContext context) {
    if (handlerMapping == null) {
      handlerMapping = HandlerMapping.find(context, detectAllHandlerMapping);
    }
  }

  /**
   * Initialize the HandlerAdapters used by this class.
   * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,
   * we default to RequestHandlerAdapter.
   */
  private void initHandlerAdapters(ApplicationContext context) {
    if (handlerAdapter == null) {
      handlerAdapter = HandlerAdapter.find(context, detectAllHandlerAdapters);
    }
  }

  /**
   * Initialize the ReturnValueHandler used by this class.
   * <p>If no ReturnValueHandlerManager beans are defined in the BeanFactory for this namespace,
   * we default to {@link ReturnValueHandlerManager#registerDefaultHandlers()}.
   *
   * @see ReturnValueHandler
   * @see ReturnValueHandlerManager
   */
  private void initReturnValueHandler(ApplicationContext context) {
    if (returnValueHandler == null) {
      ReturnValueHandlerManager manager;
      try {
        manager = BeanFactoryUtils.beanOfTypeIncludingAncestors(context, ReturnValueHandlerManager.class);
      }
      catch (NoSuchBeanDefinitionException e) {
        manager = new ReturnValueHandlerManager();
        manager.setApplicationContext(context);
        manager.registerDefaultHandlers();
      }
      this.returnValueHandler = manager.asSelectable();
    }
  }

  /**
   * Initialize the HandlerExceptionHandler used by this class.
   * <p>If no HandlerExceptionHandler beans are defined in the BeanFactory for this namespace,
   * we default to {@link ExceptionHandlerAnnotationExceptionHandler}.
   *
   * @see HandlerExceptionHandler
   */
  private void initExceptionHandler(ApplicationContext context) {
    if (exceptionHandler == null) {
      exceptionHandler = HandlerExceptionHandler.find(context, detectAllHandlerExceptionHandlers);
    }
  }

  /**
   * Initialize the NotFoundHandler used by this class.
   * <p>If no NotFoundHandler beans are defined in the BeanFactory for this namespace,
   * we default to {@link NotFoundHandler}.
   *
   * @see NotFoundHandler
   */
  private void initNotFoundHandler(ApplicationContext context) {
    if (notFoundHandler == null) {
      notFoundHandler = BeanFactoryUtils.find(context, NotFoundHandler.class);
      if (notFoundHandler == null) {
        notFoundHandler = new NotFoundHandler();
      }
    }
  }

  /**
   * Collect all the RequestHandledListener used by this class.
   *
   * @see RequestHandledListener
   */
  private void initRequestHandledListeners(ApplicationContext context) {
    var matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, RequestHandledListener.class, true, false);
    var handlers = new ArrayList<>(matchingBeans.values());
    AnnotationAwareOrderComparator.sort(handlers);

    addRequestHandledActions(handlers);
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
  public Object lookupHandler(final RequestContext context) throws Exception {
    return handlerMapping.getHandler(context);
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
    return handlerAdapter;
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
  public ReturnValueHandler lookupReturnValueHandler(
          @Nullable Object handler, @Nullable Object returnValue) {
    if (handler instanceof ReturnValueHandler) {
      return (ReturnValueHandler) handler;
    }
    ReturnValueHandler selected = returnValueHandler.selectHandler(handler, returnValue);
    if (selected == null) {
      if (returnValue == null && handler != null) {
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
      if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
        lookupReturnValueHandler(handler, returnValue)
                .handleReturnValue(context, handler, returnValue);
      }
    }
    catch (Throwable e) {
      handleException(handler, e, context);
    }
    finally {
      // @since 3.0 cleanup MultipartFiles
      context.requestCompleted();
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
    else if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
      returnValueHandler.handleReturnValue(context, null, returnValue);
    }
  }

  /**
   * Set whether to throw a NoHandlerFoundException when no Handler was found for this request.
   * This exception can then be caught with a HandlerExceptionHandler or an
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
    long startTime = System.currentTimeMillis();

    Object handler = null;
    Object returnValue = null;
    Throwable throwable = null;
    try {
      // Determine handler for the current request.
      handler = lookupHandler(context);
      if (handler == null) {
        returnValue = handlerNotFound(context);
      }
      else if (handler instanceof HttpRequestHandler requestHandler) {
        // specially for RequestHandler
        returnValue = requestHandler.handleRequest(context);
      }
      else {
        // adaptation for handling this request
        returnValue = lookupHandlerAdapter(handler).handle(context, handler);
      }
    }
    catch (Throwable ex) {
      throwable = ex;
    }
    finally {
      try {
        processDispatchResult(context, handler, returnValue, throwable);
        throwable = null; // handled
      }
      catch (Throwable ex) {
        throwable = ex; // not handled
      }
      context.requestCompleted();
      if (log.isDebugEnabled()) {
        logResult(context, throwable);
      }
      publishRequestHandledEvent(context, startTime, throwable);
    }
  }

  /**
   * Handle the result of handler selection and handler invocation, which is
   * either a view or an Exception to be resolved to a view.
   */
  private void processDispatchResult(
          RequestContext request, @Nullable Object handler,
          @Nullable Object returnValue, @Nullable Throwable exception) throws Throwable {

    if (exception != null) {
      returnValue = processHandlerException(request, handler, exception);
      handler = null;
    }

    // Did the handler return a view to render?
    if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
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
   * @throws Throwable if no handler can handle the exception
   */
  @Nullable
  protected Object processHandlerException(
          RequestContext request, @Nullable Object handler, Throwable ex) throws Throwable {
    // clear context
    request.reset();
    // prepare context throwable
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
    // Check registered HandlerExceptionHandlers...
    Object returnValue = exceptionHandler.handleException(request, ex, handler);
    if (returnValue == null) {
      throw ex;
    }
    else if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
      if (log.isTraceEnabled()) {
        log.trace("Using resolved error view: {}", returnValue, ex);
      }
      else if (log.isDebugEnabled()) {
        log.debug("Using resolved error view: {}", returnValue);
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
  @Nullable
  protected Object handlerNotFound(RequestContext request) throws Throwable {
    if (throwExceptionIfNoHandlerFound) {
      throw new HandlerNotFoundException(
              request.getMethodValue(), request.getRequestURI(), request.requestHeaders());
    }
    else {
      return notFoundHandler.handleRequest(request);
    }
  }

  private void logResult(RequestContext request, @Nullable Throwable failureCause) {
    if (failureCause != null) {
      if (log.isTraceEnabled()) {
        log.trace("Failed to complete request", failureCause);
      }
      else {
        log.debug("Failed to complete request: {}", failureCause.toString());
      }
    }
    else {

      if (WebAsyncUtils.isConcurrentHandlingStarted(request)) {
        log.debug("Exiting but response remains open for further handling");
        return;
      }

      String headers = "";  // nothing below trace
      if (log.isTraceEnabled()) {
        HttpHeaders httpHeaders = request.responseHeaders();
        if (isEnableLoggingRequestDetails()) {
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
      log.debug("{} Completed {}{}", request, httpStatus != null ? httpStatus : request.getStatus(), headers);
    }
  }

  protected void publishRequestHandledEvent(
          RequestContext request, long startTime, @Nullable Throwable failureCause) {
    RequestHandledListener[] requestHandledListeners = requestHandledActions.get();
    if (ObjectUtils.isNotEmpty(requestHandledListeners)) {
      for (RequestHandledListener action : requestHandledListeners) {
        action.requestHandled(request, startTime, failureCause);
      }
    }
  }

  public HandlerMapping getHandlerMapping() {
    return handlerMapping;
  }

  public HandlerExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  public void setHandlerMapping(HandlerMapping handlerMapping) {
    Assert.notNull(handlerMapping, "HandlerMapping is required");
    this.handlerMapping = handlerMapping;
  }

  public void setHandlerAdapters(HandlerAdapter... handlerAdapters) {
    this.handlerAdapter = new HandlerAdapters(handlerAdapters);
  }

  public void setHandlerAdapter(HandlerAdapter handlerAdapter) {
    this.handlerAdapter = handlerAdapter;
  }

  public void setExceptionHandler(HandlerExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler is required");
    this.exceptionHandler = exceptionHandler;
  }

  public SelectableReturnValueHandler getReturnValueHandler() {
    return returnValueHandler;
  }

  public void setReturnValueHandler(SelectableReturnValueHandler returnValueHandler) {
    Assert.notNull(returnValueHandler, "returnValueHandler is required");
    this.returnValueHandler = returnValueHandler;
  }

  /**
   * Set whether to detect all HandlerMapping beans in this handler's context. Otherwise,
   * just a single bean with name "HandlerMapping" will be expected.
   * <p>Default is "true". Turn this off if you want this servlet to use a single
   * HandlerMapping, despite multiple HandlerMapping beans being defined in the context.
   *
   * @since 4.0
   */
  public void setDetectAllHandlerMapping(boolean detectAllHandlerMapping) {
    this.detectAllHandlerMapping = detectAllHandlerMapping;
  }

  /**
   * Set whether to detect all HandlerAdapter beans in this handler's context. Otherwise,
   * just a single bean with name "handlerAdapter" will be expected.
   * <p>Default is "true". Turn this off if you want this servlet to use a single
   * HandlerAdapter, despite multiple HandlerAdapter beans being defined in the context.
   *
   * @since 4.0
   */
  public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
    this.detectAllHandlerAdapters = detectAllHandlerAdapters;
  }

  /**
   * Set whether to detect all HandlerExceptionHandler beans in this handler's context. Otherwise,
   * just a single bean with name "handlerExceptionHandler" will be expected.
   * <p>Default is "true". Turn this off if you want this servlet to use a single
   * HandlerExceptionHandler, despite multiple HandlerExceptionHandler beans being defined in the context.
   *
   * @since 4.0
   */
  public void setDetectAllHandlerExceptionHandlers(boolean detectAllHandlerExceptionHandlers) {
    this.detectAllHandlerExceptionHandlers = detectAllHandlerExceptionHandlers;
  }

  /**
   * not found handler
   *
   * @param notFoundHandler HttpRequestHandler
   * @since 4.0
   */
  public void setNotFoundHandler(HttpRequestHandler notFoundHandler) {
    this.notFoundHandler = notFoundHandler;
  }

  /**
   * add RequestHandledListener array to the list of listeners to be notified when a request is handled.
   *
   * @param array RequestHandledListener array
   * @since 4.0
   */
  public void addRequestHandledActions(RequestHandledListener... array) {
    requestHandledActions.add(array);
  }

  /**
   * add RequestHandledListener list to the list of listeners to be notified when a request is handled.
   *
   * @param list RequestHandledListener list
   * @since 4.0
   */
  public void addRequestHandledActions(Collection<RequestHandledListener> list) {
    requestHandledActions.addAll(list);
  }

}
