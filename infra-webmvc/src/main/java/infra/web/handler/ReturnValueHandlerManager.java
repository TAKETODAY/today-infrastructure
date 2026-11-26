/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import infra.context.support.ApplicationObjectSupport;
import infra.core.ArraySizeTrimmer;
import infra.core.ReactiveAdapterRegistry;
import infra.core.style.ToStringBuilder;
import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SyncTaskExecutor;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.web.ErrorResponse;
import infra.web.ErrorResponseViewResolver;
import infra.web.RedirectModelManager;
import infra.web.ReturnValueHandler;
import infra.web.ServerHttpExchange;
import infra.web.accept.ContentNegotiationManager;
import infra.web.handler.method.RequestBodyAdvice;
import infra.web.handler.method.ResponseBodyAdvice;
import infra.web.handler.method.support.HttpEntityMethodProcessor;
import infra.web.handler.method.support.ModelAttributeMethodProcessor;
import infra.web.handler.method.support.RequestResponseBodyMethodProcessor;
import infra.web.handler.result.AsyncTaskMethodReturnValueHandler;
import infra.web.handler.result.CallableMethodReturnValueHandler;
import infra.web.handler.result.DeferredResultReturnValueHandler;
import infra.web.handler.result.HttpHeadersReturnValueHandler;
import infra.web.handler.result.HttpStatusReturnValueHandler;
import infra.web.handler.result.ObjectHandlerMethodReturnValueHandler;
import infra.web.handler.result.RenderedImageReturnValueHandler;
import infra.web.handler.result.ResponseBodyEmitterReturnValueHandler;
import infra.web.handler.result.ResponseEntityReturnValueHandler;
import infra.web.handler.result.ServerResponseReturnValueHandler;
import infra.web.handler.result.StreamingResponseBodyReturnValueHandler;
import infra.web.handler.result.VoidReturnValueHandler;
import infra.web.view.BeanNameViewResolver;
import infra.web.view.ViewResolver;
import infra.web.view.ViewReturnValueHandler;

/**
 * Manages an ordered list of {@link ReturnValueHandler} implementations for
 * processing results from HTTP request handlers.
 *
 * <p>The first matching handler in list order processes the result. Use
 * {@link #selectHandler(Object, Object)} to find a match without invoking it.
 *
 * <p>New instances have no registered handlers. Configure the view resolver,
 * message converters, and other options before calling {@link #registerDefaultHandlers()}
 * to append built-in handlers for views, HTTP entities, response bodies, and
 * asynchronous results.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see SelectableReturnValueHandler
 * @since 2019-12-28 13:47
 */
public class ReturnValueHandlerManager extends ApplicationObjectSupport implements ArraySizeTrimmer, ReturnValueHandler, Iterable<ReturnValueHandler> {

  private final ArrayList<ReturnValueHandler> handlers = new ArrayList<>(8);

  // @since 4.0
  private final SelectableReturnValueHandler delegate = new SelectableReturnValueHandler(handlers);

  // @since 4.0
  private final ArrayList<Object> bodyAdvice = new ArrayList<>();

  private final ArrayList<ErrorResponse.Interceptor> errorResponseInterceptors = new ArrayList<>();

  private @Nullable ErrorResponseViewResolver errorResponseViewResolver;

  /**
   * Configure the resolver used by the default handlers to resolve HTML views
   * for {@link infra.http.ProblemDetail} responses.
   *
   * @param resolver the resolver, or {@code null} to use message converters
   * @since 5.0
   */
  public void setErrorResponseViewResolver(@Nullable ErrorResponseViewResolver resolver) {
    this.errorResponseViewResolver = resolver;
  }

  /**
   * Return the configured problem detail view resolver, if any.
   *
   * @return the resolver, or {@code null} if none is configured
   * @since 5.0
   */
  public @Nullable ErrorResponseViewResolver getErrorResponseViewResolver() {
    return this.errorResponseViewResolver;
  }

  // @since 4.0
  @SuppressWarnings("NullAway.Init")
  private List<HttpMessageConverter<?>> messageConverters;

  // @since 4.0
  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  private String imageFormatName = RenderedImageReturnValueHandler.IMAGE_PNG;

  private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

  /**
   * @since 3.0.1
   */
  private @Nullable RedirectModelManager redirectModelManager;

  private @Nullable ViewReturnValueHandler viewReturnValueHandler;

  private @Nullable ObjectHandlerMethodReturnValueHandler objectHandler;

  // @since 4.0
  private @Nullable ViewResolver viewResolver;

  private @Nullable AsyncTaskExecutor taskExecutor;

  /**
   * Create a manager with no registered handlers and default message converters
   * for byte arrays, strings, and form data.
   */
  public ReturnValueHandlerManager() {
    this.messageConverters = new ArrayList<>(4);
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
  }

  /**
   * Create a manager with no registered handlers and the given message converters.
   *
   * @param messageConverters the converters to use (never {@code null})
   * @see #setMessageConverters(List)
   */
  public ReturnValueHandlerManager(List<HttpMessageConverter<?>> messageConverters) {
    setMessageConverters(messageConverters);
  }

  /**
   * Configure the registry used to adapt reactive return values.
   * The default response body emitter handler uses this registry only when a
   * {@linkplain #setTaskExecutor(AsyncTaskExecutor) task executor} is configured;
   * otherwise it uses the shared registry.
   *
   * @param reactiveAdapterRegistry the registry to use, or {@code null} to use
   * {@link ReactiveAdapterRegistry#getSharedInstance() the shared registry}
   */
  public void setReactiveAdapterRegistry(@Nullable ReactiveAdapterRegistry reactiveAdapterRegistry) {
    this.reactiveAdapterRegistry = reactiveAdapterRegistry == null
            ? ReactiveAdapterRegistry.getSharedInstance() : reactiveAdapterRegistry;
  }

  /**
   * Return the configured registry, which defaults to the shared registry.
   */
  public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
    return this.reactiveAdapterRegistry;
  }

  /**
   * Append the given handlers in the order supplied.
   *
   * @param handlers the handlers to append (never {@code null})
   */
  public void addHandlers(ReturnValueHandler... handlers) {
    Assert.notNull(handlers, "ReturnValueHandler is required");
    Collections.addAll(this.handlers, handlers);
  }

  /**
   * Append the handlers in the given list in order.
   *
   * @param handlers the handlers to append (never {@code null})
   */
  public void addHandlers(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "ReturnValueHandler is required");
    this.handlers.addAll(handlers);
  }

  /**
   * Replace all registered handlers with those in the given list, preserving their order.
   *
   * @param handlers the replacement handlers (never {@code null})
   */
  public void setHandlers(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "ReturnValueHandler is required");
    this.handlers.clear();
    this.handlers.addAll(handlers);
  }

  @Override
  public Iterator<ReturnValueHandler> iterator() {
    return handlers.iterator();
  }

  @Override
  public void forEach(Consumer<? super ReturnValueHandler> action) {
    handlers.forEach(action);
  }

  @Override
  public Spliterator<ReturnValueHandler> spliterator() {
    return handlers.spliterator();
  }

  /**
   * Return the live, mutable list of handlers in selection order.
   * Changes to this list affect subsequent selection by this manager and any
   * delegates created via {@link #asSelectable()}.
   */
  public List<ReturnValueHandler> getHandlers() {
    return handlers;
  }

  /**
   * Replace the interceptors used by the default handlers before rendering a
   * {@link infra.http.ProblemDetail} response.
   *
   * @param interceptors the interceptors to use
   * @since 5.0
   */
  public void setErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
    this.errorResponseInterceptors.clear();
    this.errorResponseInterceptors.addAll(interceptors);
  }

  /**
   * Return the live, mutable list of configured error response interceptors.
   *
   * @since 5.0
   */
  public List<ErrorResponse.Interceptor> getErrorResponseInterceptors() {
    return this.errorResponseInterceptors;
  }

  /**
   * Create a selectable handler backed by this manager's live handler list.
   * Subsequent changes to the list are visible to the returned handler.
   *
   * @return a new selectable handler sharing the registered handlers
   */
  public SelectableReturnValueHandler asSelectable() {
    return new SelectableReturnValueHandler(handlers);
  }

  /**
   * Select the first matching handler in list order without invoking it.
   * A {@code null} return value participates in matching, while
   * {@link ReturnValueHandler#NONE_RETURN_VALUE} matches only against the request handler.
   *
   * @param handler the request handler, or {@code null} to match by return value only
   * @param returnValue the result to match, which may be {@code null} or
   * {@link ReturnValueHandler#NONE_RETURN_VALUE}
   * @return the first matching handler, or {@code null} if none matches or if
   * {@code handler} is {@code null} and no return value is available
   * @see ReturnValueHandler#select(Iterable, Object, Object)
   */
  public @Nullable ReturnValueHandler selectHandler(@Nullable Object handler, @Nullable Object returnValue) {
    return ReturnValueHandler.select(handlers, handler, returnValue);
  }

  //---------------------------------------------------------------------
  // Implementation of ReturnValueHandler interface
  //---------------------------------------------------------------------

  /**
   * Delegate processing to the first matching return value handler.
   *
   * @param exchange the current HTTP request and response context
   * @param handler the request handler, which may be {@code null}
   * @param returnValue the result to process, which may be {@code null} or
   * {@link ReturnValueHandler#NONE_RETURN_VALUE}
   * @throws ReturnValueHandlerNotFoundException if no handler matches
   * @throws Exception if the selected handler fails to process the return value
   */
  @Override
  public void handleReturnValue(ServerHttpExchange exchange, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    delegate.handleReturnValue(exchange, handler, returnValue);
  }

  /**
   * {@inheritDoc}
   * <p>Checks support without a return value.
   */
  @Override
  public boolean supportsHandler(Object handler) {
    return delegate.supportsHandler(handler);
  }

  /**
   * {@inheritDoc}
   * <p>Checks support without a request handler.
   */
  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return delegate.supportsReturnValue(returnValue);
  }

  //

  /**
   * Append the default return value handlers using the current configuration.
   * Existing handlers remain at the front of the list and are considered first.
   * Each call appends the defaults again without deduplicating entries.
   *
   * @throws IllegalStateException if no application context is available for the
   * default {@link BeanNameViewResolver} and no view resolver or view handler is configured
   */
  public void registerDefaultHandlers() {
    logger.debug("Registering default return-value handlers to {}", this);
    ViewReturnValueHandler viewHandler = obtainViewHandler();

    ArrayList<ReturnValueHandler> internalHandlers = new ArrayList<>();
    var imageHandler = getRenderedImageHandler();

    internalHandlers.add(imageHandler);
    internalHandlers.add(viewHandler);
    internalHandlers.add(new VoidReturnValueHandler(viewHandler));
    internalHandlers.add(new HttpStatusReturnValueHandler());
    internalHandlers.add(new HttpHeadersReturnValueHandler());
    internalHandlers.add(new CallableMethodReturnValueHandler());
    internalHandlers.add(new AsyncTaskMethodReturnValueHandler(getApplicationContext()));
    internalHandlers.add(new DeferredResultReturnValueHandler());

    // Iterate ReturnValueHandler in runtime
    var compositeHandler = new SelectableReturnValueHandler(internalHandlers);

    var objectHandler = getObjectHandler(compositeHandler);

    List<ReturnValueHandler> handlers = getHandlers();
    handlers.add(imageHandler);
    handlers.add(viewHandler);

    handlers.add(new VoidReturnValueHandler(viewHandler));
    handlers.add(new HttpStatusReturnValueHandler());
    handlers.add(new HttpHeadersReturnValueHandler());
    handlers.add(new CallableMethodReturnValueHandler());
    handlers.add(new ModelAttributeMethodProcessor(false));
    handlers.add(new AsyncTaskMethodReturnValueHandler(getApplicationContext()));
    handlers.add(new DeferredResultReturnValueHandler());
    handlers.add(new StreamingResponseBodyReturnValueHandler());

    List<HttpMessageConverter<?>> messageConverters = getMessageConverters();

    HttpEntityMethodProcessor httpEntityMethodProcessor = new HttpEntityMethodProcessor(messageConverters,
            contentNegotiationManager, bodyAdvice, redirectModelManager, errorResponseInterceptors);
    httpEntityMethodProcessor.setErrorResponseViewResolver(errorResponseViewResolver);
    httpEntityMethodProcessor.setViewReturnValueHandler(viewHandler);

    ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler;
    if (taskExecutor != null) {
      responseBodyEmitterHandler = new ResponseBodyEmitterReturnValueHandler(
              messageConverters, reactiveAdapterRegistry, taskExecutor, contentNegotiationManager);
    }
    else {
      responseBodyEmitterHandler = new ResponseBodyEmitterReturnValueHandler(messageConverters, contentNegotiationManager);
    }

    handlers.add(responseBodyEmitterHandler);
    handlers.add(new ServerResponseReturnValueHandler(this));
    handlers.add(new ResponseEntityReturnValueHandler(httpEntityMethodProcessor, responseBodyEmitterHandler));
    var responseBodyProcessor = new RequestResponseBodyMethodProcessor(messageConverters,
            contentNegotiationManager, bodyAdvice, errorResponseInterceptors);
    responseBodyProcessor.setErrorResponseViewResolver(errorResponseViewResolver);
    responseBodyProcessor.setViewReturnValueHandler(viewHandler);
    handlers.add(responseBodyProcessor);
    handlers.add(new ModelAttributeMethodProcessor(true));

    // fall back
    handlers.add(objectHandler);

    compositeHandler.trimToSize();
  }

  private ViewReturnValueHandler obtainViewHandler() {
    if (viewReturnValueHandler == null) {
      ViewResolver viewResolver = this.viewResolver;
      if (viewResolver == null) {
        BeanNameViewResolver resolver = new BeanNameViewResolver();
        resolver.setApplicationContext(applicationContext());
        viewResolver = resolver;
      }
      viewReturnValueHandler = new ViewReturnValueHandler(viewResolver);
    }
    return viewReturnValueHandler;
  }

  private ObjectHandlerMethodReturnValueHandler getObjectHandler(SelectableReturnValueHandler compositeHandler) {
    ObjectHandlerMethodReturnValueHandler objectHandler = get(ObjectHandlerMethodReturnValueHandler.class);
    // image handler
    if (objectHandler == null) {
      objectHandler = new ObjectHandlerMethodReturnValueHandler(compositeHandler);
    }
    return objectHandler;
  }

  private RenderedImageReturnValueHandler getRenderedImageHandler() {
    RenderedImageReturnValueHandler imageHandler = get(RenderedImageReturnValueHandler.class);
    // image handler
    if (imageHandler == null) {
      imageHandler = new RenderedImageReturnValueHandler();
      imageHandler.setFormatName(imageFormatName);
    }
    return imageHandler;
  }

  /**
   * Remove all registered handlers that match the given predicate.
   *
   * @param filter the predicate that identifies handlers to remove
   * @return {@code true} if any handler was removed
   * @since 4.0
   */
  public boolean removeIf(Predicate<ReturnValueHandler> filter) {
    return handlers.removeIf(filter);
  }

  /**
   * Check whether any registered handler is an instance of the given type.
   *
   * @param handlerClass the type to look for, or {@code null} to return {@code false}
   * @return {@code true} if a matching handler is registered
   * @since 4.0
   */
  public boolean contains(@Nullable Class<?> handlerClass) {
    return handlerClass != null && contains(handlerClass, handlers);
  }

  /**
   * Check whether the given list contains a handler of the requested type.
   *
   * @param handlerClass the type to look for
   * @param handlers the handlers to search
   * @return {@code true} if the list contains an instance of the given type
   * @since 4.0
   */
  protected boolean contains(Class<?> handlerClass, List<ReturnValueHandler> handlers) {
    return get(handlerClass, handlers) != null;
  }

  /**
   * Return the first registered handler that is an instance of the given type.
   *
   * @param <T> the requested handler type
   * @param handlerClass the type to look for, or {@code null} to return {@code null}
   * @return the first matching handler, or {@code null} if none matches
   * @since 4.0
   */
  public <T> @Nullable T get(@Nullable Class<T> handlerClass) {
    if (handlerClass == null) {
      return null;
    }
    return get(handlerClass, handlers);
  }

  /**
   * Return the first handler in the given list that is an instance of the given type.
   *
   * @param <T> the requested handler type
   * @param handlerClass the type to look for
   * @param handlers the handlers to search in list order
   * @return the first matching handler, or {@code null} if none matches
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  protected final <T> @Nullable T get(Class<T> handlerClass, List<ReturnValueHandler> handlers) {
    for (ReturnValueHandler handler : handlers) {
      if (handlerClass.isInstance(handler)) {
        return (T) handler;
      }
    }
    return null;
  }

  /**
   * Trim the backing list's capacity to the number of registered handlers.
   *
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    handlers.trimToSize();
  }

  /**
   * Set the resolver to use when creating the default view return value handler.
   * This setting does not affect an existing view return value handler.
   *
   * @param webViewResolver the resolver to use, or {@code null} to use a
   * {@link BeanNameViewResolver}
   * @see #setViewReturnValueHandler(ViewReturnValueHandler)
   */
  public void setViewResolver(@Nullable ViewResolver webViewResolver) {
    this.viewResolver = webViewResolver;
  }

  /**
   * Set the manager used by the default HTTP entity processor to save redirect models.
   *
   * @param redirectModelManager the manager to use, or {@code null} if none is configured
   */
  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  /**
   * Return the configured redirect model manager, or {@code null} if none is configured.
   */
  public @Nullable RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
  }

  /**
   * Store an object handler without registering it for return value selection.
   * Default registration looks for a fallback object handler in the registered
   * handler list, not in this property.
   *
   * @param objectHandler the object handler to store, or {@code null} to clear it
   * @see #getObjectHandler()
   */
  public void setObjectHandler(@Nullable ObjectHandlerMethodReturnValueHandler objectHandler) {
    this.objectHandler = objectHandler;
  }

  /**
   * Return the object handler set via {@link #setObjectHandler(ObjectHandlerMethodReturnValueHandler)},
   * or {@code null} if none is set. This method does not search the registered handlers.
   */
  public @Nullable ObjectHandlerMethodReturnValueHandler getObjectHandler() {
    return objectHandler;
  }

  /**
   * Set the output format for newly created default rendered image handlers.
   * An already registered rendered image handler is reused without changing its format.
   *
   * @param imageFormatName the image format name (never {@code null})
   */
  public void setImageFormatName(String imageFormatName) {
    Assert.notNull(imageFormatName, "imageFormatName is required");
    this.imageFormatName = imageFormatName;
  }

  /**
   * Return the configured image format name, which defaults to PNG.
   */
  public String getImageFormatName() {
    return imageFormatName;
  }

  /**
   * Set the manager used by the default handlers to determine requested media types.
   *
   * @param contentNegotiationManager the manager to use, or {@code null} to use
   * a {@linkplain ContentNegotiationManager#ContentNegotiationManager() default manager}
   * @since 4.0
   */
  public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager == null
            ? new ContentNegotiationManager() : contentNegotiationManager;
  }

  /**
   * Set the message converters used when creating the default handlers
   * that read request bodies or write response bodies.
   *
   * @param messageConverters the converter list to use (never {@code null});
   * must not be empty when registering the default handlers
   * @since 4.0
   */
  public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    Assert.notNull(messageConverters, "messageConverters is required");
    this.messageConverters = messageConverters;
  }

  /**
   * Return the configured converter list, not a copy.
   *
   * @since 4.0
   */
  public List<HttpMessageConverter<?>> getMessageConverters() {
    return this.messageConverters;
  }

  /**
   * Append the given request and response body advice entries.
   *
   * @param list the advice entries to append, or {@code null} to leave the list unchanged
   * @see RequestBodyAdvice
   * @see ResponseBodyAdvice
   * @since 4.0
   */
  public void addRequestResponseBodyAdvice(@Nullable List<Object> list) {
    CollectionUtils.addAll(bodyAdvice, list);
  }

  /**
   * Replace the configured request and response body advice entries.
   *
   * @param list the replacement advice entries, or {@code null} to clear existing entries
   * @see RequestBodyAdvice
   * @see ResponseBodyAdvice
   * @since 4.0
   */
  public void setRequestResponseBodyAdvice(@Nullable List<Object> list) {
    bodyAdvice.clear();
    CollectionUtils.addAll(bodyAdvice, list);
  }

  /**
   * Return the live, mutable list of configured request and response body advice entries.
   *
   * @since 4.0
   */
  public List<Object> getRequestResponseBodyAdvice() {
    return bodyAdvice;
  }

  /**
   * Set the view handler to use when registering the default handlers.
   *
   * @param viewReturnValueHandler the view handler to use, or {@code null} to
   * create one from the configured or default view resolver on the next registration
   * @see #setViewResolver(ViewResolver)
   */
  public void setViewReturnValueHandler(@Nullable ViewReturnValueHandler viewReturnValueHandler) {
    this.viewReturnValueHandler = viewReturnValueHandler;
  }

  /**
   * Set the executor used by the default response body emitter handler for
   * blocking writes of items emitted by reactive return values.
   * If no executor is configured, the handler uses a {@link SyncTaskExecutor}.
   *
   * @param taskExecutor the executor to use
   * @see #setReactiveAdapterRegistry(ReactiveAdapterRegistry)
   * @since 4.0
   */
  public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ReturnValueHandlerManager that))
      return false;
    return Objects.equals(handlers, that.handlers)
            && Objects.equals(viewResolver, that.viewResolver)
            && Objects.equals(objectHandler, that.objectHandler)
            && Objects.equals(imageFormatName, that.imageFormatName)
            && Objects.equals(messageConverters, that.messageConverters)
            && Objects.equals(redirectModelManager, that.redirectModelManager)
            && Objects.equals(viewReturnValueHandler, that.viewReturnValueHandler)
            && Objects.equals(contentNegotiationManager, that.contentNegotiationManager)
            && Objects.equals(bodyAdvice, that.bodyAdvice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(handlers, redirectModelManager,
            viewReturnValueHandler, objectHandler,
            viewResolver, messageConverters, contentNegotiationManager,
            bodyAdvice, imageFormatName);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("handlers", handlers)
            .append("viewResolver", viewResolver)
            .append("messageConverters", messageConverters)
            .append("requestResponseBodyAdvice", bodyAdvice)
            .toString();
  }
}
