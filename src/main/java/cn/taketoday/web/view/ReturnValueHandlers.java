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
package cn.taketoday.web.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.Environment;
import cn.taketoday.http.converter.AllEncompassingFormHttpMessageConverter;
import cn.taketoday.http.converter.ByteArrayHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.config.CompositeWebMvcConfiguration;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.handler.method.RequestBodyAdvice;
import cn.taketoday.web.handler.method.ResponseBodyAdvice;
import cn.taketoday.web.resolver.HttpEntityMethodProcessor;
import cn.taketoday.web.resolver.RequestResponseBodyMethodProcessor;
import cn.taketoday.web.view.template.AbstractTemplateRenderer;
import cn.taketoday.web.view.template.TemplateRenderer;

/**
 * return-value handlers
 *
 * @author TODAY 2019-12-28 13:47
 */
public class ReturnValueHandlers
        extends WebApplicationContextSupport implements ArraySizeTrimmer {
  public static final String DOWNLOAD_BUFF_SIZE = "download.buff.size";

  private final ArrayList<ReturnValueHandler> handlers = new ArrayList<>(8);
  /**
   * @since 3.0.1
   */
  private int downloadFileBufferSize = 10240;
  /**
   * @since 3.0.1
   */
  @Nullable
  private RedirectModelManager redirectModelManager;
  /**
   * @since 3.0.1
   */
  @Nullable
  private TemplateRenderer templateRenderer;

  @Nullable
  private TemplateRendererReturnValueHandler templateRendererHandler;

  @Nullable
  private ObjectHandlerMethodReturnValueHandler objectHandler;

  // @since 4.0
  private List<HttpMessageConverter<?>> messageConverters;

  // @since 4.0
  private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

  // @since 4.0
  private final ArrayList<Object> requestResponseBodyAdvice = new ArrayList<>();

  private String imageFormatName = RenderedImageReturnValueHandler.IMAGE_PNG;

  public ReturnValueHandlers() {
    this.messageConverters = new ArrayList<>(4);
    this.messageConverters.add(new ByteArrayHttpMessageConverter());
    this.messageConverters.add(new StringHttpMessageConverter());
    this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());
  }

  public void addHandlers(ReturnValueHandler... handlers) {
    Assert.notNull(handlers, "handler must not be null");
    Collections.addAll(this.handlers, handlers);
    sort();
  }

  public void addHandlers(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.addAll(handlers);
    sort();
  }

  public void setHandlers(List<ReturnValueHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.clear();
    this.handlers.addAll(handlers);
    sort();
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
   * init handlers
   */
  public void initHandlers() {
    WebApplicationContext context = obtainApplicationContext();
    Environment environment = context.getEnvironment();
    Integer bufferSize = environment.getProperty(DOWNLOAD_BUFF_SIZE, Integer.class);
    if (bufferSize != null) {
      setDownloadFileBufferSize(bufferSize);
    }
    // @since 3.0.3
    if (redirectModelManager == null) {
      setRedirectModelManager(context.getBean(RedirectModelManager.class));
    }
    if (templateRenderer == null) {
      WebMvcConfiguration mvcConfiguration
              = new CompositeWebMvcConfiguration(context.getBeans(WebMvcConfiguration.class));
      setTemplateRenderer(getTemplateRenderer(context, mvcConfiguration));
    }
  }

  protected TemplateRenderer getTemplateRenderer(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    TemplateRenderer templateRenderer = BeanFactoryUtils.requiredBean(context, TemplateRenderer.class);
    if (templateRenderer instanceof AbstractTemplateRenderer) {
      mvcConfiguration.configureTemplateRenderer((AbstractTemplateRenderer) templateRenderer);
    }
    return templateRenderer;
  }

  /**
   * register default return-value handlers
   */
  public void registerDefaultHandlers() {
    registerDefaultHandlers(this.templateRenderer);
  }

  /**
   * register default {@link ReturnValueHandler}s
   *
   * @since 3.0
   */
  public void registerDefaultHandlers(TemplateRenderer templateRenderer) {
    log.info("Registering default return-value handlers");

    applyDefaults(templateRenderer);

    ArrayList<ReturnValueHandler> internalHandlers = new ArrayList<>();
    RenderedImageReturnValueHandler imageHandler = getRenderedImageHandler();

    ResourceReturnValueHandler resourceHandler = new ResourceReturnValueHandler(getDownloadFileBufferSize());
    ModelAndViewReturnValueHandler modelAndViewHandler = new ModelAndViewReturnValueHandler(internalHandlers);

    internalHandlers.add(imageHandler);
    internalHandlers.add(resourceHandler);
    internalHandlers.add(templateRendererHandler);
    internalHandlers.add(new VoidReturnValueHandler(modelAndViewHandler));
    internalHandlers.add(modelAndViewHandler);
    internalHandlers.add(new CharSequenceReturnValueHandler(templateRendererHandler));
    internalHandlers.add(new HttpStatusReturnValueHandler());

    sort(internalHandlers);

    // Iterate ReturnValueHandler in runtime
    SelectableReturnValueHandler compositeHandler = new SelectableReturnValueHandler(internalHandlers);

    ObjectHandlerMethodReturnValueHandler objectHandler = getObjectHandler(compositeHandler);

    //
    List<ReturnValueHandler> handlers = getHandlers();
    handlers.add(imageHandler);
    handlers.add(resourceHandler);
    handlers.add(templateRendererHandler);

    handlers.add(new VoidReturnValueHandler(modelAndViewHandler));
    handlers.add(objectHandler);
    handlers.add(modelAndViewHandler);
    handlers.add(new CharSequenceReturnValueHandler(templateRendererHandler));
    handlers.add(new HttpStatusReturnValueHandler());

    handlers.add(new HttpEntityMethodProcessor(
            getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice, redirectModelManager));

    handlers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), contentNegotiationManager, requestResponseBodyAdvice));
    compositeHandler.trimToSize();

    // ordering
    sort();
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

  private void applyDefaults(TemplateRenderer templateRenderer) {
    // templateRendererHandler
    if (templateRendererHandler == null) {
      TemplateRendererReturnValueHandler handler
              = new TemplateRendererReturnValueHandler(templateRenderer);
      handler.setModelManager(getRedirectModelManager());
      this.templateRendererHandler = handler;
    }
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
  @SuppressWarnings("unchecked") //
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

  //

  public void setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  @Nullable
  public RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
  }

  public void setDownloadFileBufferSize(int downloadFileBufferSize) {
    this.downloadFileBufferSize = downloadFileBufferSize;
  }

  public int getDownloadFileBufferSize() {
    return downloadFileBufferSize;
  }

  @Nullable
  public TemplateRenderer getTemplateRenderer() {
    return templateRenderer;
  }

  public void setTemplateRenderer(@Nullable TemplateRenderer templateRenderer) {
    this.templateRenderer = templateRenderer;
  }

  public void setTemplateRendererHandler(@Nullable TemplateRendererReturnValueHandler templateRendererHandler) {
    this.templateRendererHandler = templateRendererHandler;
  }

  @Nullable
  public TemplateRendererReturnValueHandler getTemplateRendererHandler() {
    return templateRendererHandler;
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
   * Add one or more {@code RequestBodyAdvice} instances to intercept the
   * request before it is read and converted for {@code @RequestBody} and
   * {@code HttpEntity} method arguments.
   */
  public void setRequestBodyAdvice(@Nullable List<RequestBodyAdvice> requestBodyAdvice) {
    if (requestBodyAdvice != null) {
      this.requestResponseBodyAdvice.addAll(requestBodyAdvice);
    }
  }

  /**
   * Add one or more {@code ResponseBodyAdvice} instances to intercept the
   * response before {@code @ResponseBody} or {@code ResponseEntity} return
   * values are written to the response body.
   */
  public void setResponseBodyAdvice(@Nullable List<ResponseBodyAdvice<?>> responseBodyAdvice) {
    if (responseBodyAdvice != null) {
      this.requestResponseBodyAdvice.addAll(responseBodyAdvice);
    }
  }
}
