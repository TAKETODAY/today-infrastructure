/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;

import infra.context.support.ApplicationObjectSupport;
import infra.core.ArraySizeTrimmer;
import infra.core.ReactiveAdapterRegistry;
import infra.core.style.ToStringBuilder;
import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.ByteArrayHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.web.ErrorResponse;
import infra.web.RedirectModelManager;
import infra.web.RequestContext;
import infra.web.ReturnValueHandler;
import infra.web.accept.ContentNegotiationManager;
import infra.web.async.WebAsyncTask;
import infra.web.bind.resolver.HttpEntityMethodProcessor;
import infra.web.bind.resolver.RequestResponseBodyMethodProcessor;
import infra.web.handler.method.ModelAttributeMethodProcessor;
import infra.web.handler.method.RequestBodyAdvice;
import infra.web.handler.method.ResponseBodyAdvice;
import infra.web.handler.method.ResponseBodyEmitterReturnValueHandler;
import infra.web.handler.method.ResponseEntityReturnValueHandler;
import infra.web.handler.result.AsyncTaskMethodReturnValueHandler;
import infra.web.handler.result.CallableMethodReturnValueHandler;
import infra.web.handler.result.DeferredResultReturnValueHandler;
import infra.web.handler.result.HttpHeadersReturnValueHandler;
import infra.web.handler.result.HttpStatusReturnValueHandler;
import infra.web.handler.result.ObjectHandlerMethodReturnValueHandler;
import infra.web.handler.result.RenderedImageReturnValueHandler;
import infra.web.handler.result.StreamingResponseBodyReturnValueHandler;
import infra.web.handler.result.VoidReturnValueHandler;
import infra.web.view.BeanNameViewResolver;
import infra.web.view.ViewResolver;
import infra.web.view.ViewReturnValueHandler;

/**
 * return-value handler manager
 *
 * @author TODAY 2019-12-28 13:47
 */
public class ReturnValueHandlerManager extends ApplicationObjectSupport implements ArraySizeTrimmer, ReturnValueHandler, Iterable<ReturnValueHandler> {

  private final ArrayList<ReturnValueHandler> handlers = new ArrayList<>(8);

  // @since 4.0
  private final SelectableReturnValueHandler delegate = new SelectableReturnValueHandler(handlers);

  /**
   * @since 3.0.1
   */
  @Nullable
  private RedirectModelManager redirectModelManager;

  @Nullable
  private ViewReturnValueHandler viewReturnValueHandler;

  @Nullable
  private ObjectHandlerMethodReturnValueHandler objectHandler;

  // @since 4.0
  @Nullable
  private ViewResolver viewResolver;

  // @since 4.0
  private List<HttpMessageConverter<?>> messageConverters;

  // @since 4.0
  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  // @since 4.0
  private final ArrayList<Object> bodyAdvice = new ArrayList<>();

  @Nullable
  private AsyncTaskExecutor taskExecutor;

  private String imageFormatName = RenderedImageReturnValueHandler.IMAGE_PNG;

  private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

  private final ArrayList<ErrorResponse.Interceptor> errorResponseInterceptors = new ArrayList<>();

  public ReturnValueHandlerManager() {
    this.messageConverters = new ArrayList<>(4);
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
  }

  public ReturnValueHandlerManager(List<HttpMessageConverter<?>> messageConverters) {
    setMessageConverters(messageConverters);
  }

  /**
   * Configure the registry for reactive library types to be supported as
   * return values from controller methods.
   */
  public void setReactiveAdapterRegistry(@Nullable ReactiveAdapterRegistry reactiveAdapterRegistry) {
    this.reactiveAdapterRegistry = reactiveAdapterRegistry == null
            ? ReactiveAdapterRegistry.getSharedInstance() : reactiveAdapterRegistry;
  }

  /**
   * Return the configured reactive type registry of adapters.
   */
  public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
    return this.reactiveAdapterRegistry;
  }

  public void addHandlers(ReturnValueHandler... handlers) {
    Assert.notNull(handlers, "ReturnValueHandler is required");
    Collections.addAll(this.handlers, handlers);
  }

  public void addHandlers(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "ReturnValueHandler is required");
    this.handlers.addAll(handlers);
  }

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

  public List<ReturnValueHandler> getHandlers() {
    return handlers;
  }

  /**
   * Configure a list of {@link ErrorResponse.Interceptor}'s to apply when
   * rendering an RFC 7807 {@link infra.http.ProblemDetail}
   * error response.
   *
   * @param interceptors the interceptors to use
   * @since 5.0
   */
  public void setErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
    this.errorResponseInterceptors.clear();
    this.errorResponseInterceptors.addAll(interceptors);
  }

  /**
   * Return the {@link #setErrorResponseInterceptors(List) configured}
   * {@link ErrorResponse.Interceptor}'s.
   *
   * @since 5.0
   */
  public List<ErrorResponse.Interceptor> getErrorResponseInterceptors() {
    return this.errorResponseInterceptors;
  }

  /**
   * @return Returns SelectableReturnValueHandler
   */
  public SelectableReturnValueHandler asSelectable() {
    return new SelectableReturnValueHandler(handlers);
  }

  @Nullable
  public ReturnValueHandler getHandler(final Object handler) {
    return ReturnValueHandler.select(getHandlers(), handler, null);
  }

  /**
   * @param returnValue if returnValue is {@link ReturnValueHandler#NONE_RETURN_VALUE} match handler only
   * @return null if returnValue is {@link ReturnValueHandler#NONE_RETURN_VALUE} or no one matched
   */
  @Nullable
  public ReturnValueHandler selectHandler(@Nullable Object handler, @Nullable Object returnValue) {
    return ReturnValueHandler.select(handlers, handler, returnValue);
  }

  //---------------------------------------------------------------------
  // Implementation of ReturnValueHandler interface
  //---------------------------------------------------------------------

  /**
   * @param context Current HTTP request context
   * @param handler Target HTTP handler
   * @param returnValue Handler execution result
   * @throws ReturnValueHandlerNotFoundException not found ReturnValueHandler
   * @throws Exception throws when write data to response
   */
  @Override
  public void handleReturnValue(RequestContext context, @Nullable Object handler, @Nullable Object returnValue) throws Exception {
    delegate.handleReturnValue(context, handler, returnValue);
  }

  @Override
  public boolean supportsHandler(Object handler) {
    return delegate.supportsHandler(handler);
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return delegate.supportsReturnValue(returnValue);
  }

  //

  /**
   * register default return-value handlers
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

    ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler;
    if (taskExecutor != null) {
      responseBodyEmitterHandler = new ResponseBodyEmitterReturnValueHandler(
              messageConverters, reactiveAdapterRegistry, taskExecutor, contentNegotiationManager);
    }
    else {
      responseBodyEmitterHandler = new ResponseBodyEmitterReturnValueHandler(messageConverters, contentNegotiationManager);
    }

    handlers.add(responseBodyEmitterHandler);
    handlers.add(new ResponseEntityReturnValueHandler(httpEntityMethodProcessor, responseBodyEmitterHandler));
    handlers.add(new RequestResponseBodyMethodProcessor(messageConverters, contentNegotiationManager, bodyAdvice, errorResponseInterceptors));
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
        resolver.setApplicationContext(obtainApplicationContext());
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
   * @since 4.0
   */
  public boolean removeIf(Predicate<ReturnValueHandler> filter) {
    return handlers.removeIf(filter);
  }

  /**
   * @since 4.0
   */
  public boolean contains(@Nullable Class<?> handlerClass) {
    return handlerClass != null && contains(handlerClass, handlers);
  }

  /**
   * @since 4.0
   */
  protected boolean contains(Class<?> handlerClass, List<ReturnValueHandler> handlers) {
    return get(handlerClass, handlers) != null;
  }

  /**
   * @since 4.0
   */
  @Nullable
  public <T> T get(@Nullable Class<T> handlerClass) {
    if (handlerClass == null) {
      return null;
    }
    return get(handlerClass, handlers);
  }

  /**
   * @since 4.0
   */
  @Nullable
  @SuppressWarnings("unchecked")
  protected final <T> T get(Class<T> handlerClass, List<ReturnValueHandler> handlers) {
    for (ReturnValueHandler handler : handlers) {
      if (handlerClass.isInstance(handler)) {
        return (T) handler;
      }
    }
    return null;
  }

  /**
   * @since 4.0
   */
  @Override
  public void trimToSize() {
    handlers.trimToSize();
  }

  public void setViewResolver(@Nullable ViewResolver webViewResolver) {
    this.viewResolver = webViewResolver;
  }

  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  @Nullable
  public RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
  }

  public void setObjectHandler(@Nullable ObjectHandlerMethodReturnValueHandler objectHandler) {
    this.objectHandler = objectHandler;
  }

  @Nullable
  public ObjectHandlerMethodReturnValueHandler getObjectHandler() {
    return objectHandler;
  }

  public void setImageFormatName(String imageFormatName) {
    Assert.notNull(imageFormatName, "imageFormatName is required");
    this.imageFormatName = imageFormatName;
  }

  public String getImageFormatName() {
    return imageFormatName;
  }

  /**
   * Set the {@link ContentNegotiationManager} to use to determine requested media types.
   * If not set, the default constructor is used.
   *
   * @since 4.0
   */
  public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager == null
            ? new ContentNegotiationManager() : contentNegotiationManager;
  }

  /**
   * Provide the converters to use in argument resolvers and return value
   * handlers that support reading and/or writing to the body of the
   * request and response.
   *
   * @since 4.0
   */
  public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    Assert.notNull(messageConverters, "messageConverters is required");
    this.messageConverters = messageConverters;
  }

  /**
   * Return the configured message body converters.
   *
   * @since 4.0
   */
  public List<HttpMessageConverter<?>> getMessageConverters() {
    return this.messageConverters;
  }

  /**
   * Add one or more {@code RequestBodyAdvice} {@code ResponseBodyAdvice}
   *
   * @see RequestBodyAdvice
   * @see ResponseBodyAdvice
   * @since 4.0
   */
  public void addRequestResponseBodyAdvice(@Nullable List<Object> list) {
    CollectionUtils.addAll(bodyAdvice, list);
  }

  /**
   * Set one or more {@code RequestBodyAdvice} {@code ResponseBodyAdvice}
   *
   * <p>
   * clear all and add all
   *
   * @see RequestBodyAdvice
   * @see ResponseBodyAdvice
   * @since 4.0
   */
  public void setRequestResponseBodyAdvice(@Nullable List<Object> list) {
    bodyAdvice.clear();
    CollectionUtils.addAll(bodyAdvice, list);
  }

  /**
   * @since 4.0
   */
  public List<Object> getRequestResponseBodyAdvice() {
    return bodyAdvice;
  }

  public void setViewReturnValueHandler(@Nullable ViewReturnValueHandler viewReturnValueHandler) {
    this.viewReturnValueHandler = viewReturnValueHandler;
  }

  /**
   * Set the default {@link AsyncTaskExecutor} to use when a controller method
   * return a {@link Callable}. Controller methods can override this default on
   * a per-request basis by returning an {@link WebAsyncTask}.
   * <p>By default a {@link SimpleAsyncTaskExecutor} instance is used.
   * It's recommended to change that default in production as the simple executor
   * does not re-use threads.
   *
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
