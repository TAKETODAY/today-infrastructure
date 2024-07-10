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

package cn.taketoday.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ArrayHolder;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.async.WebAsyncManagerFactory;
import cn.taketoday.web.handler.AsyncHandler;
import cn.taketoday.web.handler.HandlerAdapterAware;
import cn.taketoday.web.handler.HandlerNotFoundException;
import cn.taketoday.web.handler.HandlerWrapper;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.ReturnValueHandlerNotFoundException;
import cn.taketoday.web.handler.SimpleNotFoundHandler;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import cn.taketoday.web.handler.result.AsyncReturnValueHandler;
import cn.taketoday.web.util.WebUtils;

/**
 * Central dispatcher for HTTP request handlers/controllers
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2019-11-16 19:05
 */
public class DispatcherHandler extends InfraHandler {

  /** Action mapping registry */
  private HandlerMapping handlerMapping;

  private HandlerAdapter handlerAdapter;

  /** exception handler */
  private HandlerExceptionHandler exceptionHandler;

  /** @since 4.0 */
  private ReturnValueHandlerManager returnValueHandler;

  /** Throw a HandlerNotFoundException if no Handler was found to process this request? @since 4.0 */
  private boolean throwExceptionIfNoHandlerFound = false;

  /** Detect all HandlerMappings or just expect "HandlerRegistry" bean?. */
  private boolean detectAllHandlerMapping = true;

  /** Detect all HandlerAdapters or just expect "HandlerAdapter" bean?. */
  private boolean detectAllHandlerAdapters = true;

  /** Detect all HandlerExceptionHandlers or just expect "HandlerExceptionHandler" bean?. */
  private boolean detectAllHandlerExceptionHandlers = true;

  private NotFoundHandler notFoundHandler;

  private final ArrayHolder<RequestCompletedListener> requestCompletedActions = ArrayHolder.forGenerator(RequestCompletedListener[]::new);

  protected WebAsyncManagerFactory webAsyncManagerFactory;

  public DispatcherHandler() {

  }

  /**
   * Create a new {@code DispatcherHandler} with the given application context.
   * <p>
   *
   * If the context has already been refreshed or does not implement
   * {@code ConfigurableApplicationContext}, none of the above will occur under the
   * assumption that the user has performed these actions (or not) per his or her
   * specific needs.
   *
   * @param context the context to use
   * @see #initApplicationContext
   * @see #configureAndRefreshApplicationContext
   */
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
    initWebAsyncManagerFactory(context);
    initRequestCompletedListeners(context);
  }

  /**
   * Initialize the HandlerRegistries used by this class.
   * <p>If no HandlerRegistry beans are defined in the BeanFactory for this namespace,
   * we default to BeanNameUrlHandlerRegistry.
   */
  private void initHandlerMapping(ApplicationContext context) {
    if (handlerMapping == null) {
      setHandlerMapping(HandlerMapping.find(context, detectAllHandlerMapping));
      logStrategy(handlerMapping);
    }
  }

  /**
   * Initialize the HandlerAdapters used by this class.
   * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,
   * we default to RequestHandlerAdapter.
   */
  private void initHandlerAdapters(ApplicationContext context) {
    if (handlerAdapter == null) {
      setHandlerAdapter(HandlerAdapter.find(context, detectAllHandlerAdapters));
      logStrategy(handlerAdapter);
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
      setReturnValueHandler(manager);
      logStrategy(manager);
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
      setExceptionHandler(HandlerExceptionHandler.find(context, detectAllHandlerExceptionHandlers));
      logStrategy(exceptionHandler);
    }
  }

  /**
   * Initialize the NotFoundHandler used by this class.
   * <p>If no NotFoundHandler beans are defined in the BeanFactory for this namespace,
   * we default to {@link SimpleNotFoundHandler}.
   *
   * @see SimpleNotFoundHandler
   */
  private void initNotFoundHandler(ApplicationContext context) {
    if (notFoundHandler == null) {
      notFoundHandler = BeanFactoryUtils.find(context, NotFoundHandler.class);
      if (notFoundHandler == null) {
        setNotFoundHandler(NotFoundHandler.sharedInstance);
      }
      logStrategy(notFoundHandler);
    }
  }

  /**
   * Initialize the WebAsyncManagerFactory used by this class.
   *
   * @see WebAsyncManagerFactory
   */
  private void initWebAsyncManagerFactory(ApplicationContext context) {
    if (webAsyncManagerFactory == null) {
      setWebAsyncManagerFactory(WebAsyncManagerFactory.find(context));
      logStrategy(webAsyncManagerFactory);
    }
  }

  /**
   * Collect all the RequestHandledListener used by this class.
   *
   * @see RequestCompletedListener
   */
  private void initRequestCompletedListeners(ApplicationContext context) {
    var matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, RequestCompletedListener.class, true, false);
    var handlers = new ArrayList<>(matchingBeans.values());
    AnnotationAwareOrderComparator.sort(handlers);

    addRequestCompletedActions(handlers);
  }

  // ------------------------------------------------------------------------
  // Handler
  // ------------------------------------------------------------------------

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
   * handle async results
   *
   * @param context async request
   * @param handler sync handler
   * @param concurrentResult async result
   */
  public void handleConcurrentResult(RequestContext context, @Nullable Object handler, @Nullable Object concurrentResult) throws Throwable {
    Throwable throwable = null;
    try {
      if (handler instanceof AsyncHandler asyncHandler) {
        handler = asyncHandler.wrapConcurrentResult(concurrentResult);
      }
      if (handler instanceof AsyncReturnValueHandler valueHandler) {
        valueHandler.handleAsyncReturnValue(context, concurrentResult);
      }
      else {
        if (concurrentResult instanceof Throwable asyncError) {
          throwable = asyncError;
        }
        processDispatchResult(context, handler, concurrentResult, throwable);
        throwable = null;
      }
    }
    catch (Throwable ex) {
      throwable = ex; // not handled
    }

    logResult(context, throwable);
    requestCompleted(context, throwable);
  }

  /**
   * Handling HTTP request.
   * <p>
   * This method will throw un-handling exception to up stream
   *
   * @param context current HTTP request and HTTP response
   * @throws Throwable in case of any kind of un-handling failure
   * @see HandlerAdapter
   * @see HandlerMapping
   * @see HandlerExceptionHandler
   * @see ReturnValueHandler
   * @see NotFoundHandler
   * @see HttpRequestHandler
   * @since 4.0
   */
  public void handleRequest(RequestContext context) throws Throwable {
    logRequest(context);
    Object handler = null;
    Object returnValue = null;
    Throwable throwable = null;
    try {
      // Determine handler for the current request.
      handler = lookupHandler(context);
      if (handler == null) {
        returnValue = handlerNotFound(context);
      }
      else {
        if (handler instanceof HandlerAdapterAware aware) {
          aware.setHandlerAdapter(handlerAdapter);
        }
        if (handler instanceof HttpRequestHandler requestHandler) {
          // specially for HttpRequestHandler
          returnValue = requestHandler.handleRequest(context);
        }
        else {
          // adaptation for handling this request
          returnValue = lookupHandlerAdapter(handler).handle(context, handler);
        }
      }
    }
    catch (Throwable ex) {
      throwable = ex; // from request handler
    }

    try {
      processDispatchResult(context, handler, returnValue, throwable);
      throwable = null; // handled
    }
    catch (Throwable ex) {
      throwable = ex; // not handled
    }
    logResult(context, throwable);

    if (!context.isConcurrentHandlingStarted()) {
      requestCompleted(context, throwable);
    }
  }

  /**
   * Find a {@link HandlerAdapter} for input handler
   *
   * @param handler HTTP handler
   * @return A {@link HandlerAdapter}
   * @throws HandlerAdapterNotFoundException If there isn't a {@link HandlerAdapter} for target handler
   */
  private HandlerAdapter lookupHandlerAdapter(@Nullable Object handler) {
    if (handler instanceof HandlerAdapter) {
      return (HandlerAdapter) handler;
    }
    if (handler instanceof HandlerAdapterProvider) {
      return ((HandlerAdapterProvider) handler).getHandlerAdapter();
    }
    return handlerAdapter;
  }

  /**
   * Handle the result of handler selection and handler invocation, which is
   * either a view or an Exception to be resolved to a view.
   */
  protected void processDispatchResult(RequestContext request, @Nullable Object handler,
          @Nullable Object returnValue, @Nullable Throwable exception) throws Throwable {

    if (handler instanceof HandlerWrapper wrapper) {
      handler = wrapper.getRawHandler();
    }
    if (exception != null) {
      exception = ExceptionUtils.unwrapIfNecessary(exception);
      returnValue = processHandlerException(request, handler, exception);

      HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
      if (matchingMetadata != null) {
        handler = matchingMetadata.getHandler();
      }
      else {
        handler = null;
      }
    }

    // Did the handler return a view to render?
    if (returnValue != HttpRequestHandler.NONE_RETURN_VALUE) {
      ReturnValueHandler selected;
      if (handler instanceof ReturnValueHandler) {
        selected = (ReturnValueHandler) handler;
      }
      else {
        selected = returnValueHandler.selectHandler(handler, returnValue);
        if (selected == null) {
          if (returnValue == null && handler != null) {
            throw new ReturnValueHandlerNotFoundException(handler);
          }
          throw new ReturnValueHandlerNotFoundException(returnValue, handler);
        }
      }

      try {
        selected.handleReturnValue(request, handler, returnValue);
      }
      catch (Throwable e) {
        processDispatchResult(request, handler, null, e);
      }
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
  protected Object processHandlerException(RequestContext request, @Nullable Object handler, Throwable ex) throws Throwable {
    // Success and error responses may use different content types
    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      matchingMetadata.setProducibleMediaTypes(null);
    }

    // clear context
    request.reset();

    // prepare context throwable
    request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);

    // Check registered HandlerExceptionHandlers...
    Object returnValue = exceptionHandler.handleException(request, ex, handler);
    if (returnValue == null) {
      // not found a suitable handler to handle this exception,
      // throw it to top level to handle
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
      return notFoundHandler.handleNotFound(request);
    }
  }

  protected void requestCompleted(RequestContext request, @Nullable Throwable notHandled) throws Throwable {
    RequestCompletedListener[] actions = requestCompletedActions.array;
    if (actions != null) {
      for (RequestCompletedListener action : actions) {
        action.requestCompleted(request, notHandled);
      }
    }

    // exception not handled
    if (notHandled != null) {
      // try application level error handling
      try {
        var httpStatus = HttpStatusProvider.getStatusCode(notHandled);
        request.sendError(httpStatus.first, httpStatus.second);
      }
      catch (Throwable e) {
        notHandled.addSuppressed(e);
        request.requestCompleted(notHandled);
        throw notHandled;
      }
    }
    request.requestCompleted(null);
  }

  public void setHandlerMapping(HandlerMapping handlerMapping) {
    Assert.notNull(handlerMapping, "HandlerMapping is required");
    this.handlerMapping = handlerMapping;
  }

  public void setHandlerAdapter(HandlerAdapter handlerAdapter) {
    Assert.notNull(handlerAdapter, "HandlerAdapter is required");
    this.handlerAdapter = handlerAdapter;
  }

  public void setExceptionHandler(HandlerExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler is required");
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * Set ReturnValueHandlerManager
   *
   * @param returnValueHandler ReturnValueHandlerManager
   */
  public void setReturnValueHandler(ReturnValueHandlerManager returnValueHandler) {
    Assert.notNull(returnValueHandler, "ReturnValueHandlerManager is required");
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
   * Set whether to throw a HandlerNotFoundException when no Handler was found for this request.
   * This exception can then be caught with a HandlerExceptionHandler or an
   * {@code @ExceptionHandler} controller method.
   *
   * @since 4.0
   */
  public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
    this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
  }

  /**
   * Set not found handler
   *
   * @param notFoundHandler HttpRequestHandler
   * @since 4.0
   */
  public void setNotFoundHandler(NotFoundHandler notFoundHandler) {
    Assert.notNull(notFoundHandler, "NotFoundHandler is required");
    this.notFoundHandler = notFoundHandler;
  }

  /**
   * Set WebAsyncManagerFactory
   *
   * @param factory WebAsyncManagerFactory
   * @since 4.0
   */
  public void setWebAsyncManagerFactory(WebAsyncManagerFactory factory) {
    Assert.notNull(factory, "WebAsyncManagerFactory is required");
    this.webAsyncManagerFactory = factory;
  }

  /**
   * add RequestHandledListener array to the list of listeners to be notified when a request is handled.
   *
   * @param array RequestHandledListener array
   * @since 4.0
   */
  public void addRequestCompletedActions(@Nullable RequestCompletedListener... array) {
    requestCompletedActions.add(array);
  }

  /**
   * add RequestHandledListener list to the list of listeners to be notified when a request is handled.
   *
   * @param list RequestHandledListener list
   * @since 4.0
   */
  public void addRequestCompletedActions(@Nullable Collection<RequestCompletedListener> list) {
    requestCompletedActions.addAll(list);
  }

  /**
   * Set RequestHandledListener list to the list of listeners to be notified when a request is handled.
   *
   * @param list RequestHandledListener list
   * @since 4.0
   */
  public void setRequestCompletedActions(@Nullable Collection<RequestCompletedListener> list) {
    requestCompletedActions.set(list);
  }

  // @since 4.0
  private void logRequest(RequestContext request) {
    if (log.isDebugEnabled()) {
      String params;
      String contentType = request.getContentType();
      if (StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
        params = "multipart";
      }
      else if (isEnableLoggingRequestDetails()) {
        params = request.getParameters().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
      }
      else {
        // Avoid request body parsing for form data
        params = StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                || !request.getParameters().isEmpty() ? "masked" : "";
      }

      String queryString = request.getQueryString();
      String queryClause = StringUtils.isNotEmpty(queryString) ? "?" + queryString : "";
      String message = request.getMethod() + " " + request.getRequestURL() + queryClause;

      if (!params.isEmpty()) {
        message += ", parameters={%s}".formatted(params);
      }

      message = URLDecoder.decode(message, StandardCharsets.UTF_8);
      if (log.isTraceEnabled()) {
        StringBuilder headers = new StringBuilder();
        HttpHeaders httpHeaders = request.requestHeaders();
        if (!httpHeaders.isEmpty()) {
          if (isEnableLoggingRequestDetails()) {
            // first
            Iterator<String> headerNames = httpHeaders.keySet().iterator();
            if (headerNames.hasNext()) {
              String name = headerNames.next();
              headers.append(name)
                      .append(':')
                      .append(httpHeaders.getValuesAsList(name));

              while (headerNames.hasNext()) {
                name = headerNames.next();
                headers.append(", ");
                headers.append(name);
                headers.append(':');
                headers.append(httpHeaders.get(name));
              }
            }
          }
          else {
            headers.append("masked");
          }
        }

        log.trace("%s, headers={%s} in DispatcherHandler '%s'".formatted(message, headers, beanName));
      }
      else {
        log.debug(message);
      }
    }
  }

  private void logResult(RequestContext request, @Nullable Throwable failureCause) {
    if (log.isDebugEnabled()) {
      if (failureCause != null) {
        if (log.isTraceEnabled()) {
          log.trace("Failed to complete request", failureCause);
        }
        else {
          log.debug("Failed to complete request", failureCause);
        }
      }
      else {
        if (request.isConcurrentHandlingStarted()) {
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
          headers = ", headers={%s}".formatted(headers);
        }
        HttpStatus httpStatus = HttpStatus.resolve(request.getStatus());
        log.debug("{} Completed {}{}", request, httpStatus != null ? httpStatus : request.getStatus(), headers);
      }
    }
  }

  private void logStrategy(Object strategy) {
    if (log.isDebugEnabled()) {
      log.debug("Detected {}", strategy.getClass().getName());
    }
  }

}
