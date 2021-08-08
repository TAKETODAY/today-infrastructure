/**
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
package cn.taketoday.web.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.Environment;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.config.CompositeWebMvcConfiguration;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.view.template.AbstractTemplateViewResolver;
import cn.taketoday.web.view.template.DefaultTemplateViewResolver;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * @author TODAY 2019-12-28 13:47
 */
public class ResultHandlers extends WebApplicationContextSupport {
  private final ArrayList<ResultHandler> handlers = new ArrayList<>(8);
  /**
   * @since 3.0.1
   */
  private int downloadFileBufferSize = 10240;
  /**
   * @since 3.0.1
   */
  private MessageConverter messageConverter;
  /**
   * @since 3.0.1
   */
  private RedirectModelManager redirectModelManager;
  /**
   * @since 3.0.1
   */
  private TemplateViewResolver templateViewResolver;

  public void addHandlers(ResultHandler... handlers) {
    Assert.notNull(handlers, "handler must not be null");
    Collections.addAll(this.handlers, handlers);
    sort();
  }

  public void addHandlers(List<ResultHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.addAll(handlers);
    sort();
  }

  public void setHandlers(List<ResultHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.clear();
    this.handlers.addAll(handlers);
    sort();
  }

  public void sort() {
    OrderUtils.reversedSort(handlers);
  }

  public List<ResultHandler> getHandlers() {
    return handlers;
  }

  public RuntimeResultHandler[] getRuntimeHandlers() {
    final ArrayList<RuntimeResultHandler> ret = new ArrayList<>();
    for (final ResultHandler handler : handlers) {
      if (handler instanceof RuntimeResultHandler) {
        ret.add((RuntimeResultHandler) handler);
      }
    }
    return ret.toArray(new RuntimeResultHandler[0]);
  }

  public ResultHandler getHandler(final Object handler) {
    Assert.notNull(handler, "handler must not be null");
    for (final ResultHandler resolver : getHandlers()) {
      if (resolver.supportsHandler(handler)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * Get correspond view resolver, If there isn't a suitable resolver will be
   * throw {@link IllegalArgumentException}
   *
   * @return A suitable {@link ResultHandler}
   */
  public ResultHandler obtainHandler(final Object handler) {
    final ResultHandler resultHandler = getHandler(handler);
    if (resultHandler == null) {
      throw new IllegalStateException("There isn't have a result resolver to resolve : [" + handler + "]");
    }
    return resultHandler;
  }

  //

  /**
   * init handlers
   */
  public void initHandlers(WebApplicationContext context) {
    final Environment environment = context.getEnvironment();
    final Integer bufferSize = environment.getProperty(Constant.DOWNLOAD_BUFF_SIZE, Integer.class);
    if (bufferSize != null) {
      setDownloadFileBufferSize(bufferSize);
    }
    // @since 3.0.3
    if (messageConverter == null) {
      setMessageConverter(context.getBean(MessageConverter.class));
    }
    if (redirectModelManager == null) {
      setRedirectModelManager(context.getBean(RedirectModelManager.class));
    }
    if (templateViewResolver == null) {
      WebMvcConfiguration mvcConfiguration
              = new CompositeWebMvcConfiguration(context.getBeans(WebMvcConfiguration.class));
      setTemplateViewResolver(getTemplateResolver(context, mvcConfiguration));
    }
  }

  protected TemplateViewResolver getTemplateResolver(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    TemplateViewResolver templateResolver = context.getBean(TemplateViewResolver.class);
    if (templateResolver == null) {
      context.registerBean(DefaultTemplateViewResolver.class);
      templateResolver = context.getBean(TemplateViewResolver.class);
    }

    if (templateResolver instanceof AbstractTemplateViewResolver) {
      mvcConfiguration.configureTemplateViewResolver((AbstractTemplateViewResolver) templateResolver);
    }
    return templateResolver;
  }

  /**
   * register default result-handlers
   */
  public void registerDefaultResultHandlers() {
    registerDefaultResultHandlers(this.templateViewResolver);
  }

  /**
   * register default {@link ResultHandler}s
   *
   * @since 3.0
   */
  public void registerDefaultResultHandlers(TemplateViewResolver templateViewResolver) {
    log.info("Registering default result-handlers");

    final List<ResultHandler> handlers = getHandlers();
    final int bufferSize = getDownloadFileBufferSize();
    final MessageConverter messageConverter = getMessageConverter();
    Assert.state(messageConverter != null, "No MessageConverter in this web application");

    final RedirectModelManager modelManager = getRedirectModelManager();

    VoidResultHandler voidResultHandler
            = new VoidResultHandler(templateViewResolver, messageConverter, bufferSize);
    ObjectResultHandler objectResultHandler
            = new ObjectResultHandler(templateViewResolver, messageConverter, bufferSize);
    ModelAndViewResultHandler modelAndViewResultHandler
            = new ModelAndViewResultHandler(templateViewResolver, messageConverter, bufferSize);
    ResponseEntityResultHandler responseEntityResultHandler
            = new ResponseEntityResultHandler(templateViewResolver, messageConverter, bufferSize);
    TemplateResultHandler templateResultHandler = new TemplateResultHandler(templateViewResolver);

    voidResultHandler.setModelManager(modelManager);
    objectResultHandler.setModelManager(modelManager);
    templateResultHandler.setModelManager(modelManager);
    modelAndViewResultHandler.setModelManager(modelManager);
    responseEntityResultHandler.setModelManager(modelManager);

    handlers.add(new ImageResultHandler());
    handlers.add(new ResourceResultHandler(bufferSize));
    handlers.add(templateResultHandler);

    handlers.add(voidResultHandler);
    handlers.add(objectResultHandler);
    handlers.add(modelAndViewResultHandler);
    handlers.add(responseEntityResultHandler);

    handlers.add(new ResponseBodyResultHandler(messageConverter));
    handlers.add(new HttpStatusResultHandler());

    // ordering
    sort();
  }

  //

  public void setRedirectModelManager(RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
  }

  public RedirectModelManager getRedirectModelManager() {
    return redirectModelManager;
  }

  public void setMessageConverter(MessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  public MessageConverter getMessageConverter() {
    return messageConverter;
  }

  public void setDownloadFileBufferSize(int downloadFileBufferSize) {
    this.downloadFileBufferSize = downloadFileBufferSize;
  }

  public int getDownloadFileBufferSize() {
    return downloadFileBufferSize;
  }

  public void setTemplateViewResolver(TemplateViewResolver templateViewResolver) {
    this.templateViewResolver = templateViewResolver;
  }

  public TemplateViewResolver getTemplateResolver() {
    return templateViewResolver;
  }

}
