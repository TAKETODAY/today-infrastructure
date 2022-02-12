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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.ResponseBodyAdvice;
import cn.taketoday.web.resolver.HttpEntityMethodProcessor;
import cn.taketoday.web.resolver.RequestResponseBodyMethodProcessor;
import cn.taketoday.web.view.RedirectModelManager;
import cn.taketoday.web.view.ViewResolver;
import cn.taketoday.web.view.ViewReturnValueHandler;

/**
 * return-value handler manager
 *
 * @author TODAY 2019-12-28 13:47
 */
public class ReturnValueHandlerManager
        extends WebApplicationContextSupport implements ArraySizeTrimmer {

  private final ArrayList<ReturnValueHandler> handlers = new ArrayList<>(8);

  /**
   * @since 3.0.1
   */
  @Nullable
  private RedirectModelManager redirectModelManager;

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
  private final ArrayList<Object> requestResponseBodyAdvice = new ArrayList<>();

  private String imageFormatName = RenderedImageReturnValueHandler.IMAGE_PNG;

  public ReturnValueHandlerManager() {
    this.messageConverters = new ArrayList<>(4);
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
  }

  public ReturnValueHandlerManager(List<HttpMessageConverter<?>> messageConverters) {
    setMessageConverters(messageConverters);
  }

  public void addHandlers(ReturnValueHandler... handlers) {
    Assert.notNull(handlers, "handler must not be null");
    Collections.addAll(this.handlers, handlers);
  }

  public void addHandlers(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.addAll(handlers);
  }

  public void setHandlers(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.clear();
    this.handlers.addAll(handlers);
  }

  public void sort() {
    sort(handlers);
  }

  public void sort(List<ReturnValueHandler> handlers) {
    AnnotationAwareOrderComparator.sort(handlers);
  }

  public List<ReturnValueHandler> getHandlers() {
    return handlers;
  }

  public ReturnValueHandler getHandler(final Object handler) {
    Assert.notNull(handler, "handler must not be null");
    for (final ReturnValueHandler resolver : getHandlers()) {
      if (resolver.supportsHandler(handler)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * Get correspond view resolver, If there isn't a suitable resolver will be
   * throws {@link ReturnValueHandlerNotFoundException}
   *
   * @return A suitable {@link ReturnValueHandler}
   */
  public ReturnValueHandler obtainHandler(Object handler) {
    final ReturnValueHandler returnValueHandler = getHandler(handler);
    if (returnValueHandler == null) {
      throw new ReturnValueHandlerNotFoundException(handler);
    }
    return returnValueHandler;
  }

  //

  /**
   * register default return-value handlers
   */
  public void registerDefaultHandlers() {
    log.info("Registering default return-value handlers");
    ViewReturnValueHandler viewHandler = obtainViewHandler();

    ArrayList<ReturnValueHandler> internalHandlers = new ArrayList<>();
    RenderedImageReturnValueHandler imageHandler = getRenderedImageHandler();

    ModelAndViewReturnValueHandler modelAndViewHandler = new ModelAndViewReturnValueHandler(internalHandlers);

    internalHandlers.add(imageHandler);
    internalHandlers.add(viewHandler);
    internalHandlers.add(new VoidReturnValueHandler(modelAndViewHandler));
    internalHandlers.add(modelAndViewHandler);
    internalHandlers.add(new HttpStatusReturnValueHandler());
    internalHandlers.add(new HttpHeadersReturnValueHandler());

    // Iterate ReturnValueHandler in runtime
    SelectableReturnValueHandler compositeHandler = new SelectableReturnValueHandler(internalHandlers);

    ObjectHandlerMethodReturnValueHandler objectHandler = getObjectHandler(compositeHandler);

    //
    List<ReturnValueHandler> handlers = getHandlers();
    handlers.add(imageHandler);
    handlers.add(viewHandler);

    handlers.add(new VoidReturnValueHandler(modelAndViewHandler));
    handlers.add(modelAndViewHandler);
    handlers.add(new HttpStatusReturnValueHandler());

    List<HttpMessageConverter<?>> messageConverters = getMessageConverters();
    handlers.add(new HttpEntityMethodProcessor(
            messageConverters, contentNegotiationManager, requestResponseBodyAdvice, redirectModelManager));

    handlers.add(new RequestResponseBodyMethodProcessor(messageConverters, contentNegotiationManager, requestResponseBodyAdvice));

    // fall back
    handlers.add(objectHandler);

    compositeHandler.trimToSize();
  }

  private ViewReturnValueHandler obtainViewHandler() {
    if (viewReturnValueHandler == null) {
      if (viewResolver != null) {
        viewReturnValueHandler = new ViewReturnValueHandler(viewResolver);
        viewReturnValueHandler.setModelManager(redirectModelManager);
      }
    }
    Assert.state(viewReturnValueHandler != null, "No ViewReturnValueHandler");
    return viewReturnValueHandler;
  }

  @NonNull
  private ObjectHandlerMethodReturnValueHandler getObjectHandler(SelectableReturnValueHandler compositeHandler) {
    ObjectHandlerMethodReturnValueHandler objectHandler = get(ObjectHandlerMethodReturnValueHandler.class);
    // image handler
    if (objectHandler == null) {
      objectHandler = new ObjectHandlerMethodReturnValueHandler(compositeHandler);
    }
    return objectHandler;
  }

  @NonNull
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
  boolean contains(Class<?> handlerClass, List<ReturnValueHandler> handlers) {
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
  final <T> T get(Class<T> handlerClass, List<ReturnValueHandler> handlers) {
    for (final ReturnValueHandler handler : handlers) {
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
    Assert.notNull(imageFormatName, "imageFormatName must not be null");
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
  public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager;
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
    CollectionUtils.addAll(requestResponseBodyAdvice, list);
  }

  /**
   * @since 4.0
   */
  public List<Object> getRequestResponseBodyAdvice() {
    return requestResponseBodyAdvice;
  }

  public void setViewReturnValueHandler(@Nullable ViewReturnValueHandler viewReturnValueHandler) {
    this.viewReturnValueHandler = viewReturnValueHandler;
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
            && Objects.equals(requestResponseBodyAdvice, that.requestResponseBodyAdvice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(handlers, redirectModelManager,
            viewReturnValueHandler, objectHandler,
            viewResolver, messageConverters, contentNegotiationManager,
            requestResponseBodyAdvice, imageFormatName);
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("handlers", handlers)
            .append("viewResolver", viewResolver)
            .append("messageConverters", messageConverters)
            .append("requestResponseBodyAdvice", requestResponseBodyAdvice)
            .toString();
  }
}
