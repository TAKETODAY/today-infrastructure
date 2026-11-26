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

package infra.web;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.ApplicationContext;
import infra.core.Conventions;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.ExceptionUtils;
import infra.util.StringUtils;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.InvalidApiVersionException;
import infra.web.accept.MissingApiVersionException;
import infra.web.async.WebAsyncManagerFactory;
import infra.web.context.support.SessionManagerDiscover;
import infra.web.handler.AsyncHandler;
import infra.web.handler.HandlerAdapterAware;
import infra.web.handler.HandlerNotFoundException;
import infra.web.handler.HandlerWrapper;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.ReturnValueHandlerNotFoundException;
import infra.web.handler.SimpleNotFoundHandler;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import infra.web.server.RequestContinueExpectedResolver;
import infra.web.util.WebUtils;
import infra.web.view.ViewRef;

import static infra.util.StringUtils.prependLeadingSlash;
import static infra.web.ServerHttpExchange.FORWARD_ATTRIBUTE;
import static infra.web.ServerHttpExchange.FORWARD_REQUEST_URI_ATTRIBUTE;

/**
 * Central dispatcher for HTTP request handlers/controllers.
 * <p>
 * This class is responsible for processing all incoming HTTP requests and dispatching them
 * to the appropriate handler/controller based on the request mappings.
 * <p>
 * The DispatcherHandler follows a well-defined request processing workflow:
 * <ul>
 * <li>Lookup appropriate handler for the incoming request using configured HandlerMapping</li>
 * <li>Determine the HandlerAdapter that supports the handler</li>
 * <li>Invoke the handler to process the request and generate a return value</li>
 * <li>Select an appropriate ReturnValueHandler to handle the return value</li>
 * <li>In case of exceptions, delegate to HandlerExceptionHandler for error handling</li>
 * <li>Notify registered RequestCompletedListener instances upon request completion</li>
 * </ul>
 *
 * It also handles special cases like asynchronous request processing and provides
 * hooks for customizing various aspects of request handling through configurable strategies.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2019-11-16 19:05
 */
public class DispatcherHandler extends WebLifecycleManager {

  /**
   * Attribute name for the set of paths already forwarded to in the current
   * forward chain, used to detect circular forwards.
   *
   * @see #forward(AbstractServerHttpExchange, String)
   */
  static final String FORWARDED_PATHS_ATTRIBUTE = Conventions.getQualifiedAttributeName(DispatcherHandler.class, "forwardedPaths");

  private final ArrayList<RequestCompletedListener> requestCompletedActions = new ArrayList<>();

  private final ArrayList<RequestContinueExpectedResolver> requestContinueExpectedResolvers = new ArrayList<>();

  final SessionManagerDiscover sessionManagerDiscover;

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

  protected @Nullable WebAsyncManagerFactory webAsyncManagerFactory;

  private @Nullable RequestToViewNameTranslator viewNameTranslator;

  /** @since 5.0 */
  private @Nullable DefaultFilterChain filterChain;

  /** @since 5.0 */
  private @Nullable ApiVersionStrategy apiVersionStrategy;

  /**
   * Create a new {@code DispatcherHandler} with the given application context.
   * <p>
   * If the context has already been refreshed or does not implement
   * {@code ConfigurableApplicationContext}, none of the above will occur under the
   * assumption that the user has performed these actions (or not) per his or her
   * specific needs.
   *
   * @param context the context to use
   */
  public DispatcherHandler(ApplicationContext context) {
    super(context);
    this.sessionManagerDiscover = new SessionManagerDiscover(context);
  }

  /**
   * Set the HandlerMapping to use for looking up handlers for incoming requests.
   *
   * @param handlerMapping the HandlerMapping instance to use
   * @throws IllegalArgumentException if the provided handlerMapping is null
   */
  public void setHandlerMapping(HandlerMapping handlerMapping) {
    Assert.notNull(handlerMapping, "HandlerMapping is required");
    this.handlerMapping = handlerMapping;
  }

  /**
   * Set the HandlerAdapter to use for adapting handlers to the actual request.
   *
   * @param handlerAdapter the HandlerAdapter instance to use
   * @throws IllegalArgumentException if the provided handlerAdapter is null
   */
  public void setHandlerAdapter(HandlerAdapter handlerAdapter) {
    Assert.notNull(handlerAdapter, "HandlerAdapter is required");
    this.handlerAdapter = handlerAdapter;
  }

  /**
   * Set the HandlerExceptionHandler to use for handling exceptions thrown during request processing.
   *
   * @param exceptionHandler the HandlerExceptionHandler instance to use
   * @throws IllegalArgumentException if the provided exceptionHandler is null
   */
  public void setExceptionHandler(HandlerExceptionHandler exceptionHandler) {
    Assert.notNull(exceptionHandler, "exceptionHandler is required");
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * Sets the ReturnValueHandlerManager to use for handling return values from request handlers.
   *
   * @param returnValueHandler the ReturnValueHandlerManager instance to use
   * @throws IllegalArgumentException if the provided returnValueHandler is null
   * @since 4.0
   */
  public void setReturnValueHandler(ReturnValueHandlerManager returnValueHandler) {
    Assert.notNull(returnValueHandler, "ReturnValueHandlerManager is required");
    this.returnValueHandler = returnValueHandler;
  }

  /**
   * Set whether to detect all HandlerMapping beans in this handler's context. Otherwise,
   * just a single bean with name "HandlerMapping" will be expected.
   * <p>Default is "true". Turn this off if you want this handler to use a single
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
   * <p>Default is "true". Turn this off if you want this handler to use a single
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
   * <p>Default is "true". Turn this off if you want this handler to use a single
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
   * Sets the NotFoundHandler to use for handling requests when no suitable handler is found.
   *
   * @param notFoundHandler the NotFoundHandler instance to use
   * @throws IllegalArgumentException if the provided notFoundHandler is null
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
   * Sets the RequestToViewNameTranslator to use for translating request paths to view names.
   * <p>This translator is used when no explicit view name is returned by a handler,
   * allowing for automatic view name resolution based on the request URL.
   *
   * @param viewNameTranslator the RequestToViewNameTranslator instance to use,
   * or {@code null} to disable automatic view name translation
   * @since 5.0
   */
  public void setViewNameTranslator(@Nullable RequestToViewNameTranslator viewNameTranslator) {
    this.viewNameTranslator = viewNameTranslator;
  }

  /**
   * add RequestHandledListener array to the list of listeners to be notified when a request is handled.
   *
   * @param array RequestHandledListener array
   * @since 4.0
   */
  public void addRequestCompletedActions(RequestCompletedListener @Nullable ... array) {
    CollectionUtils.addAll(requestCompletedActions, array);
    requestCompletedActions.trimToSize();
  }

  /**
   * add RequestHandledListener list to the list of listeners to be notified when a request is handled.
   *
   * @param list RequestHandledListener list
   * @since 4.0
   */
  public void addRequestCompletedActions(@Nullable Collection<RequestCompletedListener> list) {
    requestCompletedActions.addAll(list);
    requestCompletedActions.trimToSize();
  }

  /**
   * Set RequestHandledListener list to the list of listeners to be notified when a request is handled.
   *
   * @param list RequestHandledListener list
   * @since 4.0
   */
  public void setRequestCompletedActions(@Nullable Collection<RequestCompletedListener> list) {
    requestCompletedActions.clear();
    CollectionUtils.addAll(requestCompletedActions, list);
    requestCompletedActions.trimToSize();
  }

  /**
   * Set the {@link ApiVersionStrategy} to use for resolving and validating
   * the API version of incoming requests.
   *
   * @param apiVersionStrategy the strategy to use, or {@code null} to disable
   * API versioning
   * @since 5.0
   */
  public void setApiVersionStrategy(@Nullable ApiVersionStrategy apiVersionStrategy) {
    this.apiVersionStrategy = apiVersionStrategy;
  }

  /**
   * Return the configured {@link ApiVersionStrategy}.
   *
   * @since 5.0
   */
  public @Nullable ApiVersionStrategy getApiVersionStrategy() {
    return this.apiVersionStrategy;
  }

  @Override
  protected void onRefresh(ApplicationContext context) {
    initStrategies(context);
  }

  /**
   * Initialize the strategy objects that this handler uses.
   * <p>May be overridden in subclasses in order to initialize further strategy objects.
   */
  protected void initStrategies(ApplicationContext context) {
    initHandlerMapping(context);
    initHandlerAdapters(context);
    initReturnValueHandler(context);
    initExceptionHandler(context);
    initNotFoundHandler(context);
    initFilters(context);
    initWebAsyncManagerFactory(context);
    initRequestToViewNameTranslator(context);
    initRequestCompletedListeners(context);
    initRequestContinueExpectedResolvers(context);
    initApiVersionStrategy(context);
  }

  /**
   * Initialize the {@link ApiVersionStrategy} used by this class.
   * <p>If no {@code ApiVersionStrategy} bean is defined in the BeanFactory,
   * API versioning is disabled.
   */
  private void initApiVersionStrategy(ApplicationContext context) {
    if (apiVersionStrategy == null) {
      setApiVersionStrategy(BeanFactoryUtils.find(context, ApiVersionStrategy.class));
      if (apiVersionStrategy != null) {
        logStrategy(apiVersionStrategy);
      }
    }
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
   * Initialize the {@link Filter Filters} used by this class.
   * <p>Collects all {@link Filter} beans from the application context
   * and sorts them according to their order (via {@link infra.core.annotation.Order @Order}
   * or {@link infra.core.Ordered Ordered}).
   *
   * @see Filter
   * @see AnnotationAwareOrderComparator
   */
  private void initFilters(ApplicationContext context) {
    var matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, Filter.class, true, false);

    if (!matchingBeans.isEmpty()) {
      var filters = new ArrayList<>(matchingBeans.values());
      AnnotationAwareOrderComparator.sort(filters);
      this.filterChain = DefaultFilterChain.initChain(filters, this);
      if (log.isDebugEnabled()) {
        log.debug("Detected {} Filter(s): {}", filters.size(), filters);
      }
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

  /**
   * Initialize the RequestContinueExpectedResolvers used by this class.
   * <p>Collects all RequestContinueExpectedResolver beans from the application context
   * and sorts them according to their order.
   *
   * @param context the ApplicationContext to search for resolvers
   * @see RequestContinueExpectedResolver
   */
  private void initRequestContinueExpectedResolvers(ApplicationContext context) {
    var matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
            context, RequestContinueExpectedResolver.class, true, false);

    requestContinueExpectedResolvers.addAll(matchingBeans.values());
    AnnotationAwareOrderComparator.sort(requestContinueExpectedResolvers);
    requestContinueExpectedResolvers.trimToSize();
  }

  /**
   * Initialize the RequestToViewNameTranslator used by this mock instance.
   * <p>If no implementation is configured then we default to DefaultRequestToViewNameTranslator.
   *
   * @since 5.0
   */
  private void initRequestToViewNameTranslator(ApplicationContext context) {
    setViewNameTranslator(BeanFactoryUtils.find(context, RequestToViewNameTranslator.class));
    if (viewNameTranslator != null) {
      logStrategy(viewNameTranslator);
    }
  }

  // ------------------------------------------------------------------------
  // Handler
  // ------------------------------------------------------------------------

  /**
   * Find a suitable handler to handle this HTTP request
   *
   * @param exchange the current HTTP exchange
   * @return Target handler, if returns {@code null} indicates that there isn't a
   * handler to handle this request
   */
  public @Nullable Object lookupHandler(final ServerHttpExchange exchange) throws Exception {
    return handlerMapping.getHandler(exchange);
  }

  /**
   * handle async results
   *
   * @param exchange the current HTTP exchange
   * @param handler sync handler
   * @param concurrentResult async result
   */
  public void handleConcurrentResult(ServerHttpExchange exchange, @Nullable Object handler, @Nullable Object concurrentResult) throws Exception {
    if (exchange.isRequestCompleted()) {
      return;
    }
    if (concurrentResult instanceof Throwable failure && exchange.isCommitted()) {
      completeRequest(exchange, failure);
      throw ExceptionUtils.sneakyThrow(failure);
    }
    Throwable notHandled = null;
    try {
      if (concurrentResult instanceof Throwable failure) {
        notHandled = failure;
      }
      else {
        if (handler instanceof AsyncHandler asyncHandler) {
          handler = asyncHandler.wrapConcurrentResult(concurrentResult);
        }
      }
    }
    catch (Throwable ex) {
      notHandled = ex;
    }

    processDispatchResultAndComplete(exchange, handler, concurrentResult, notHandled);
  }

  /**
   * Handle the given HTTP request.
   * <p>When filters are configured, the request is executed through the filter
   * chain and {@link #completeRequest(ServerHttpExchange, Throwable)} is invoked after
   * the chain returns. This guarantees that a short-circuiting filter still
   * completes the request; the call is idempotent when the terminal handler has
   * already completed it. Without filters, dispatching and completion are handled
   * by {@link #handleRequestInternal(ServerHttpExchange)}.
   * <p>Asynchronous requests remain open after this method returns and are
   * completed when their concurrent result is dispatched through
   * {@link #handleConcurrentResult(ServerHttpExchange, Object, Object)}.
   * <p>Filter exceptions are resolved through the exception handling strategies
   * while the request is incomplete and the response is uncommitted. Otherwise,
   * they propagate to the transport without rewriting the response.
   * While asynchronous handling is active, the failure is dispatched as an
   * asynchronous result instead. Only the first accepted result is processed;
   * application work itself is not interrupted.
   * All failures, including {@link Error Errors}, retain their original type
   * and identity throughout exception handling and request completion.
   *
   * @param exchange current HTTP request and HTTP response
   * @throws Exception in case of any kind of un-handling failure
   * @see #handleRequestInternal(ServerHttpExchange)
   * @see #completeRequest(ServerHttpExchange, Throwable)
   * @since 4.0
   */
  public void handleRequest(ServerHttpExchange exchange) throws Exception {
    logRequest(exchange);
    var filterChain = this.filterChain;
    if (filterChain == null) {
      handleRequestInternal(exchange);
    }
    else {
      try {
        filterChain.doFilter(exchange);
      }
      catch (Throwable failure) {
        if (!exchange.isRequestCompleted()) {
          if (exchange.isConcurrentHandlingStarted()) {
            exchange.asyncRequest().dispatch(failure);
            return;
          }
          if (!exchange.isCommitted()) {
            processDispatchResultAndComplete(exchange, null, null, failure);
            return;
          }
        }
        completeRequest(exchange, failure);
        throw ExceptionUtils.sneakyThrow(failure);
      }
      completeRequest(exchange, null);
    }
  }

  /**
   * Send a response result without resolving a handler or completing the request.
   * <p>The result is passed unchanged to the configured return value handlers.
   *
   * @param exchange the current request exchange
   * @param result the response result, or {@code null} to leave the response unchanged
   * @throws IOException if writing the response fails with an I/O error
   * @throws IllegalStateException if the response is committed, the request is
   * completed, or a return value handler fails with another checked exception
   * @since 5.0
   */
  void send(ServerHttpExchange exchange, @Nullable Object result) throws IOException {
    Assert.state(!exchange.isRequestCompleted(), "Request is already completed");
    Assert.state(!exchange.isCommitted(), "Response is already committed");
    if (result == null) {
      return;
    }
    ReturnValueHandler selected = returnValueHandler.selectHandler(null, result);
    if (selected == null) {
      throw new ReturnValueHandlerNotFoundException(result, null);
    }
    try {
      handleReturnValue(selected, exchange, null, result);
    }
    catch (IOException | RuntimeException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to send response", ex);
    }
  }

  private void handleRequestInternal(ServerHttpExchange exchange) throws Exception {
    Object handler = null;
    Object returnValue = null;
    Throwable notHandled = null;
    try {
      // Resolve the API version for the current request, if API versioning is configured.
      Comparable<?> apiVersion = resolveApiVersion(exchange);
      // Determine handler for the current request.
      handler = lookupHandler(exchange);
      if (handler == null) {
        returnValue = handlerNotFound(exchange);
      }
      else {
        handleDeprecations(exchange, handler, apiVersion);
        if (handler instanceof HandlerAdapterAware aware) {
          aware.setHandlerAdapter(handlerAdapter);
        }
        if (handler instanceof HttpRequestHandler requestHandler) {
          // specially for HttpRequestHandler
          returnValue = requestHandler.handleRequest(exchange);
        }
        else {
          // adaptation for handling this request
          returnValue = handlerAdapter.handle(exchange, handler);
        }
      }
    }
    catch (Throwable ex) {
      notHandled = ex;
    }
    processDispatchResultAndComplete(exchange, handler, returnValue, notHandled);
  }

  /**
   * Process the dispatch result, log the outcome, and complete the request.
   */
  private void processDispatchResultAndComplete(ServerHttpExchange exchange, @Nullable Object handler,
          @Nullable Object returnValue, @Nullable Throwable failure) throws Exception {
    try {
      processDispatchResult(exchange, handler, returnValue, failure);
      failure = null;
    }
    catch (Throwable ex) {
      failure = ex;
    }
    logResult(exchange, failure);
    completeRequest(exchange, failure);
  }

  /**
   * Handle the result of handler selection and handler invocation, which is
   * either a view or an Exception to be resolved to a view.
   */
  protected void processDispatchResult(ServerHttpExchange exchange, @Nullable Object handler,
          @Nullable Object returnValue, @Nullable Throwable exception) throws Exception {

    handler = HandlerWrapper.unwrapIfNecessary(handler);
    if (exception != null) {
      exception = ExceptionUtils.unwrapIfNecessary(exception);
      returnValue = processHandlerException(exchange, handler, exception);

      HandlerMatchingMetadata matchingMetadata = HandlerMatchingMetadata.find(exchange);
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
          if (returnValue == null) {
            // find default view
            String defaultViewName = getDefaultViewName(exchange);
            if (defaultViewName != null) {
              returnValue = ViewRef.forViewName(defaultViewName);
              selected = returnValueHandler.selectHandler(null, returnValue);
              if (selected == null) {
                throw new ReturnValueHandlerNotFoundException(null, handler);
              }
              handler = null;
            }
            else {
              throw new ReturnValueHandlerNotFoundException(null, handler);
            }
          }
          else {
            throw new ReturnValueHandlerNotFoundException(returnValue, handler);
          }
        }
      }

      try {
        handleReturnValue(selected, exchange, handler, returnValue);
      }
      catch (Throwable failure) {
        if (exception == null) {
          processDispatchResult(exchange, handler, null, failure);
        }
        else {
          throw ExceptionUtils.sneakyThrow(failure);
        }
      }
    }
  }

  /**
   * Delegate to the selected {@link ReturnValueHandler} to handle the return value.
   * <p>
   * This method invokes the handler's {@code handleReturnValue} method to process
   * the result generated by the controller and write it to the HTTP response.
   *
   * @param exchange the current HTTP exchange
   * @param handler the selected handler (or {@code null} if not applicable)
   * @param returnValue the return value from the handler invocation
   * @param selected the specific {@code ReturnValueHandler} chosen to process this return value
   * @throws Exception if handling the return value fails
   */
  protected void handleReturnValue(ReturnValueHandler selected, ServerHttpExchange exchange, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    selected.handleReturnValue(exchange, handler, returnValue);
  }

  /**
   * Determine an error view via the registered HandlerExceptionHandlers.
   *
   * @param exchange current HTTP exchange
   * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
   * @param ex the exception that got thrown during handler execution
   * @return a corresponding view to forward to
   * @throws Exception if no handler can handle the exception
   */
  protected @Nullable Object processHandlerException(ServerHttpExchange exchange, @Nullable Object handler, Throwable ex) throws Exception {
    // Success and error responses may use different content types
    HandlerMatchingMetadata matchingMetadata = HandlerMatchingMetadata.find(exchange);
    if (matchingMetadata != null) {
      matchingMetadata.setProducibleMediaTypes(null);
    }

    // Reset the response before rendering the error.
    exchange.reset();

    // Expose the exception as an exchange attribute.
    exchange.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);

    Object returnValue = exceptionHandler.handleException(exchange, ex, handler);
    if (returnValue == null) {
      // not found a suitable handler to handle this exception,
      return handleUnresolvedException(exchange, ex, handler);
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

  protected @Nullable Object handleUnresolvedException(ServerHttpExchange exchange, Throwable unresolved, @Nullable Object handler) throws Exception {
    // throw it to top level to handle
    throw ExceptionUtils.sneakyThrow(unresolved);
  }

  /**
   * No handler found &rarr; set appropriate HTTP response status.
   *
   * @param exchange current HTTP exchange
   * @throws Exception if preparing the response failed
   */
  protected @Nullable Object handlerNotFound(ServerHttpExchange exchange) throws Exception {
    if (throwExceptionIfNoHandlerFound) {
      throw new HandlerNotFoundException(
              exchange.getMethodAsString(), exchange.getRequestURI(), exchange.requestHeaders());
    }
    else {
      return notFoundHandler.handleNotFound(exchange);
    }
  }

  /**
   * Complete the request lifecycle and propagate any unhandled failure.
   * <p>This is the single completion entry point shared by the filter chain and
   * the terminal handler dispatch. Completion is idempotent: if the request is
   * already completed, this method returns immediately without notifying
   * listeners or rethrowing {@code notHandled}, so a short-circuiting filter and
   * a handler that completed on its own cannot run cleanup twice. It also
   * returns while asynchronous handling is still in progress, leaving completion
   * to the dispatch of the concurrent result.
   * <p>Otherwise completion proceeds in order:
   * <ol>
   *   <li>Notify all {@link RequestCompletedListener listeners}. A listener
   *   failure is logged and, when {@code notHandled} is present, suppressed onto
   *   it.</li>
   *   <li>If {@code notHandled} is present and the response is not committed,
   *   map it to an HTTP status and send an error response. A failure while doing
   *   so is suppressed onto {@code notHandled}.</li>
   *   <li>Delegate to {@link AbstractServerHttpExchange#completeRequest(Throwable)} to
   *   release request-scoped resources and fire the completion callbacks.</li>
   * </ol>
   * <p>After completion, the unhandled failure, if any, is rethrown. When such a
   * failure exists, exceptions raised by listeners, by the error response, or by
   * the exchange cleanup are suppressed onto it rather than replacing it; when no
   * failure was being propagated, a completion failure is thrown as is.
   *
   * @param exchange the current HTTP exchange
   * @param notHandled the failure left unhandled during dispatch, or
   * {@code null} if the request completed normally
   * @throws Exception the unhandled failure, or a failure raised while
   * completing the request
   * @see RequestCompletedListener
   * @see AbstractServerHttpExchange#completeRequest(Throwable)
   * @since 5.0
   */
  protected void completeRequest(ServerHttpExchange exchange, @Nullable Throwable notHandled) throws Exception {
    if (exchange.isRequestCompleted() || exchange.isConcurrentHandlingStarted()) {
      return;
    }

    if (!requestCompletedActions.isEmpty()) {
      for (RequestCompletedListener action : requestCompletedActions) {
        try {
          action.requestCompleted(exchange, notHandled);
        }
        catch (Throwable ex) {
          log.warn("RequestCompletedListener failed", ex);
          if (notHandled != null && ex != notHandled) {
            notHandled.addSuppressed(ex);
          }
        }
      }
    }

    if (notHandled != null && !exchange.isCommitted()) {
      try {
        var httpStatus = HttpStatusProvider.getStatusCode(notHandled);
        exchange.sendError(httpStatus.first, httpStatus.second);
        notHandled = null;
      }
      catch (Throwable ex) {
        if (ex != notHandled) {
          notHandled.addSuppressed(ex);
        }
      }
    }

    try {
      WebUtils.nativeContext(exchange, AbstractServerHttpExchange.class)
              .completeRequest(notHandled);
    }
    catch (Throwable ex) {
      if (notHandled == null) {
        throw ExceptionUtils.sneakyThrow(ex);
      }
      if (ex != notHandled) {
        notHandled.addSuppressed(ex);
      }
    }

    if (notHandled != null) {
      throw ExceptionUtils.sneakyThrow(notHandled);
    }
  }

  /**
   * Determine whether the request expects a 100 Continue response.
   *
   * @param exchange current request
   * @return {@code true} if the request expects a 100 Continue response,
   * {@code false} if it does not, or {@code null} if uncertain
   * @since 5.0
   */
  public @Nullable Boolean requestContinueExpected(ServerHttpExchange exchange) {
    for (RequestContinueExpectedResolver resolver : requestContinueExpectedResolvers) {
      Boolean resolved = resolver.shouldContinue(exchange);
      if (resolved != null) {
        return resolved;
      }
    }
    return null;
  }

  /**
   * Translate the supplied request into a default view name.
   *
   * @param exchange the current HTTP exchange
   * @return the view name (or {@code null} if no default found)
   * @throws Exception if view name translation failed
   */
  protected @Nullable String getDefaultViewName(ServerHttpExchange exchange) throws Exception {
    return viewNameTranslator != null ? viewNameTranslator.getViewName(exchange) : null;
  }

  /**
   * Forward the request to a new path, re-dispatching through the handler
   * pipeline without involving filters.
   * <p>Similar to Servlet's {@code RequestDispatcher.forward()}.
   * The response buffer is cleared and the request path is updated before
   * re-dispatching.
   *
   * @param exchange the current HTTP exchange
   * @param path the new path to forward to
   * @throws Exception if forwarding fails
   * @throws IllegalStateException if the response has already been committed,
   * or a circular forward is detected
   * @since 5.0
   */
  void forward(AbstractServerHttpExchange exchange, String path) throws Exception {
    if (exchange.isCommitted()) {
      throw new IllegalStateException("Cannot forward after response has been committed");
    }

    String queryString = null;
    int queryIndex = path.indexOf('?');
    if (queryIndex >= 0) {
      queryString = path.substring(queryIndex + 1);
      path = path.substring(0, queryIndex);
    }

    path = prependLeadingSlash(path);

    String requestURI = exchange.getRequestURI();

    @SuppressWarnings("unchecked")
    var forwardedPaths = (Set<String>) exchange.getAttribute(FORWARDED_PATHS_ATTRIBUTE);
    if (forwardedPaths == null) {
      forwardedPaths = new HashSet<>();
      forwardedPaths.add(requestURI);
      exchange.setAttribute(FORWARDED_PATHS_ATTRIBUTE, forwardedPaths);
    }

    if (!forwardedPaths.add(path)) {
      throw new IllegalStateException(
              "Circular forward detected: path '%s' has already been forwarded to".formatted(path));
    }

    exchange.setAttribute(FORWARD_REQUEST_URI_ATTRIBUTE, requestURI);
    exchange.setAttribute(FORWARD_ATTRIBUTE, Boolean.TRUE);
    exchange.reset();
    exchange.requestURI = path;
    exchange.queryString = queryString;
    exchange.parameters = null;
    exchange.requestPath = null;
    exchange.uri = null;
    HandlerMatchingMetadata.set(exchange, null);

    if (log.isDebugEnabled()) {
      log.debug("Forwarding {} {} -> {}", exchange.getMethod(), requestURI, path);
    }
    handleRequestInternal(exchange);
  }

  /**
   * Resolve and validate the API version for the current request, if API
   * versioning is configured.
   *
   * <p>The resolved version is stored as the {@link HandlerMapping#API_VERSION_ATTRIBUTE}
   * request attribute so that handler mappings can match against it.
   *
   * @param exchange the current HTTP exchange
   * @return the resolved and validated API version, or {@code null} if API
   * versioning is disabled or no version applies to the request
   * @throws MissingApiVersionException if the version is required but not specified
   * @throws InvalidApiVersionException if the version is not supported
   * @see ApiVersionStrategy#resolveParseAndValidateVersion(ServerHttpExchange)
   */
  private @Nullable Comparable<?> resolveApiVersion(ServerHttpExchange exchange) {
    ApiVersionStrategy strategy = this.apiVersionStrategy;
    if (strategy == null) {
      return null;
    }
    Comparable<?> version = strategy.resolveParseAndValidateVersion(exchange);
    if (version != null) {
      exchange.setAttribute(HandlerMapping.API_VERSION_ATTRIBUTE, version);
    }
    return version;
  }

  /**
   * Handle API version deprecations for the resolved version, if applicable.
   *
   * @param exchange the current HTTP exchange
   * @param handler the selected handler, possibly wrapped in a {@link HandlerWrapper}
   * @param apiVersion the resolved API version, or {@code null}
   * @see ApiVersionStrategy#handleDeprecations(Comparable, Object, ServerHttpExchange)
   */
  private void handleDeprecations(ServerHttpExchange exchange, Object handler, @Nullable Comparable<?> apiVersion) {
    ApiVersionStrategy strategy = this.apiVersionStrategy;
    if (apiVersion == null || strategy == null) {
      return;
    }
    Object rawHandler = HandlerWrapper.unwrapIfNecessary(handler);
    strategy.handleDeprecations(apiVersion, rawHandler, exchange);
  }

  // @since 4.0
  private void logRequest(ServerHttpExchange exchange) {
    if (log.isDebugEnabled()) {
      String params;
      if (exchange.isMultipart()) {
        params = "multipart";
      }
      else if (isEnableLoggingRequestDetails()) {
        params = exchange.getParameters().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
      }
      else {
        // Avoid request body parsing for form data
        params = StringUtils.startsWithIgnoreCase(exchange.getContentTypeAsString(), MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                || !exchange.getParameters().isEmpty() ? "masked" : "";
      }

      String queryString = exchange.getQueryString();
      String queryClause = StringUtils.isNotEmpty(queryString) ? "?" + queryString : "";
      String message = exchange.getMethod() + " " + exchange.getRequestURL() + queryClause;

      if (!params.isEmpty()) {
        message += ", parameters={%s}".formatted(params);
      }

      if (log.isTraceEnabled()) {
        StringBuilder headers = new StringBuilder();
        HttpHeaders httpHeaders = exchange.requestHeaders();
        if (!httpHeaders.isEmpty()) {
          if (isEnableLoggingRequestDetails()) {
            // first
            Iterator<String> headerNames = httpHeaders.names().iterator();
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

  private void logResult(ServerHttpExchange exchange, @Nullable Throwable failureCause) {
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
        if (exchange.isConcurrentHandlingStarted()) {
          log.debug("Exiting but response remains open for further handling");
          return;
        }

        String headers = "";  // nothing below trace
        if (log.isTraceEnabled()) {
          HttpHeaders httpHeaders = exchange.responseHeaders();
          if (isEnableLoggingRequestDetails()) {
            headers = httpHeaders.entries().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(", "));
          }
          else {
            headers = httpHeaders.isEmpty() ? "" : "masked";
          }
          headers = ", headers={%s}".formatted(headers);
        }
        HttpStatus httpStatus = HttpStatus.resolve(exchange.getStatus());
        log.debug("{} Completed {}{}", exchange, httpStatus != null ? httpStatus : exchange.getStatus(), headers);
      }
    }
  }

  private void logStrategy(Object strategy) {
    if (log.isDebugEnabled()) {
      log.debug("Detected {}", strategy.getClass().getName());
    }
  }

  private static final class DefaultFilterChain implements FilterChain {

    private final DispatcherHandler dispatcherHandler;

    private final @Nullable Filter filter;

    private final @Nullable DefaultFilterChain chain;

    /**
     * Create an immutable chain with the given filters and terminal handler.
     *
     * @param filters the list of web filters to apply; must not be null
     * @param dispatcherHandler the handler to invoke when all filters have completed
     */
    static DefaultFilterChain initChain(List<Filter> filters, DispatcherHandler dispatcherHandler) {
      DefaultFilterChain chain = new DefaultFilterChain(dispatcherHandler, null, null);
      var iterator = filters.listIterator(filters.size());
      while (iterator.hasPrevious()) {
        chain = new DefaultFilterChain(dispatcherHandler, iterator.previous(), chain);
      }
      return chain;
    }

    /**
     * Create one immutable link in the chain.
     */
    private DefaultFilterChain(DispatcherHandler dispatcherHandler,
            @Nullable Filter currentFilter, @Nullable DefaultFilterChain chain) {
      this.dispatcherHandler = dispatcherHandler;
      this.filter = currentFilter;
      this.chain = chain;
    }

    /**
     * Proceed with the next filter in the chain, or invoke the terminal handler
     * if no filters remain.
     *
     * @param exchange the current HTTP exchange
     * @throws Exception if any filter or the terminal handler fails
     */
    @Override
    public void doFilter(ServerHttpExchange exchange) throws Exception {
      if (filter != null && chain != null) {
        filter.doFilter(exchange, chain);
      }
      else {
        dispatcherHandler.handleRequestInternal(exchange);
      }
    }

  }
}
