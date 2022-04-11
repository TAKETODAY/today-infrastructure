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
package cn.taketoday.web.config;

import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.factory.config.SingletonBeanRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.RequestHandlerAdapter;
import cn.taketoday.web.handler.ViewControllerHandlerAdapter;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;

/**
 * @author TODAY 2019-07-10 23:12
 */
@Deprecated
public class WebApplicationLoader
        extends WebApplicationContextSupport implements WebApplicationInitializer {
  public static final String ENABLE_WEB_MVC_XML = "enable.webmvc.xml";
  public static final String ENABLE_WEB_STARTED_LOG = "enable.started.log";
  public static final String WEB_MVC_CONFIG_LOCATION = "WebMvcConfigLocation";

  private DispatcherHandler dispatcher;

  public void onStartup() throws Throwable {
    onStartup(obtainApplicationContext());
  }

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {
    WebMvcConfiguration mvcConfiguration = getWebMvcConfiguration(context);

    configureViewControllerHandler(context, mvcConfiguration);
    configureHandlerAdapter(context, mvcConfiguration);

    initializerStartup(context, mvcConfiguration);

    System.gc();

    logStartup(context);
  }

  protected void logStartup(WebApplicationContext context) {
    if (TodayStrategies.getFlag(ENABLE_WEB_STARTED_LOG, true)) {
      log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
              System.currentTimeMillis() - context.getStartupDate()//
      );
    }
  }

  private void configureHandlerAdapter(
          WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureHandlerAdapter(context.getBeans(HandlerAdapter.class), mvcConfiguration);
  }

  /**
   * Configure {@link HandlerAdapter}
   *
   * @param adapters {@link HandlerAdapter}s
   * @param mvcConfiguration {@link WebMvcConfiguration}
   */
  protected void configureHandlerAdapter(
          List<HandlerAdapter> adapters, WebMvcConfiguration mvcConfiguration) {
    // 先看有的
    DispatcherHandler obtainDispatcher = obtainDispatcher();
    HandlerAdapter[] handlerAdapters = obtainDispatcher.getHandlerAdapters();
    if (handlerAdapters != null) {
      Collections.addAll(adapters, handlerAdapters);
    }
    // 添加默认的
    adapters.add(new RequestHandlerAdapter(Ordered.HIGHEST_PRECEDENCE));
    WebApplicationContext context = obtainApplicationContext();
    // ViewControllerHandlerRegistry must configured
    ViewControllerHandlerRegistry viewControllerRegistry = context.getBean(ViewControllerHandlerRegistry.class);
    if (viewControllerRegistry != null) {
      ViewControllerHandlerAdapter bean = context.getBean(ViewControllerHandlerAdapter.class);
      if (bean == null) {
        ViewControllerHandlerAdapter viewControllerHandlerAdapter = null;
        for (HandlerAdapter adapter : adapters) {
          if (adapter instanceof ViewControllerHandlerAdapter) {
            viewControllerHandlerAdapter = (ViewControllerHandlerAdapter) adapter;
            break;
          }
        }
        if (viewControllerHandlerAdapter == null) {
          adapters.add(new ViewControllerHandlerAdapter(Ordered.HIGHEST_PRECEDENCE));
        }
      }
    }
    // 排序
    sort(adapters);
    // apply request handler
    obtainDispatcher.setHandlerAdapters(adapters.toArray(new HandlerAdapter[adapters.size()]));
  }

  /** @since sort objects 3.0.3 */
  protected void sort(List<?> adapters) {
    AnnotationAwareOrderComparator.sort(adapters);
  }

  protected void configureViewControllerHandler(
          WebApplicationContext context, WebMvcConfiguration mvcConfiguration) throws Throwable {
    ViewControllerHandlerRegistry registry = context.getBean(ViewControllerHandlerRegistry.class);
    if (TodayStrategies.getFlag(ENABLE_WEB_MVC_XML, true)) {
      registry = configViewControllerHandlerRegistry(registry);
    }
    if (registry != null) {
      mvcConfiguration.configureViewController(registry);
    }
  }

  //

  /**
   * Invoke all {@link WebApplicationInitializer}s
   *
   * @param context {@link ApplicationContext} object
   * @throws Throwable If any initialize exception occurred
   */
  protected void initializerStartup(WebApplicationContext context,
          WebMvcConfiguration mvcConfiguration) throws Throwable //
  {
    List<WebApplicationInitializer> initializers = context.getBeans(WebApplicationInitializer.class);
    configureInitializer(initializers, mvcConfiguration);

    for (WebApplicationInitializer initializer : initializers) {
      initializer.onStartup(context);
    }
  }

  /**
   * Configure {@link WebApplicationInitializer}
   *
   * @param initializers {@link WebApplicationInitializer}s
   * @param config {@link CompositeWebMvcConfiguration}
   */
  protected void configureInitializer(List<WebApplicationInitializer> initializers, WebMvcConfiguration config) {
    config.configureInitializer(initializers);
    sort(initializers);
  }

  /**
   * Get {@link WebMvcConfiguration}
   *
   * @param applicationContext {@link ApplicationContext} object
   */
  protected WebMvcConfiguration getWebMvcConfiguration(ApplicationContext applicationContext) {
    return new CompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));
  }

  /**
   * Initialize framework.
   *
   * @throws Throwable if any Throwable occurred
   */
  protected ViewControllerHandlerRegistry configViewControllerHandlerRegistry(
          @Nullable ViewControllerHandlerRegistry registry) throws Throwable {
    // find the configure file
    log.info("TODAY WEB Framework Is Looking For ViewController Configuration File.");
    String webMvcConfigLocation = getWebMvcConfigLocation();
    if (StringUtils.isEmpty(webMvcConfigLocation)) {
      log.info("Configuration File does not exist.");
      return registry;
    }
    if (registry == null) {
      WebApplicationContext context = obtainApplicationContext();
      registry = context.getBean(ViewControllerHandlerRegistry.DEFAULT_BEAN_NAME, ViewControllerHandlerRegistry.class);
    }
    registry.configure(webMvcConfigLocation);
    return registry;
  }

  /**
   * @see ViewControllerHandlerRegistry#configure(String)
   */
  protected String getWebMvcConfigLocation() {
    return obtainApplicationContext()
            .getEnvironment()
            .getProperty(WEB_MVC_CONFIG_LOCATION);
  }

  public DispatcherHandler obtainDispatcher() {
    if (dispatcher == null) {
      // FIXME DispatcherHandler automatic registration
      WebApplicationContext context = obtainApplicationContext();
      DispatcherHandler dispatcherHandler = context.getBean(DispatcherHandler.class);
      if (dispatcherHandler == null) {
        dispatcherHandler = createDispatcher(context);
        Assert.state(dispatcherHandler != null, "DispatcherHandler must not be null, sub class must create its instance");
        context.unwrapFactory(SingletonBeanRegistry.class)
                .registerSingleton(DispatcherHandler.BEAN_NAME, dispatcherHandler);
      }
      this.dispatcher = dispatcherHandler;
    }
    return dispatcher;
  }

  protected DispatcherHandler createDispatcher(WebApplicationContext context) {
    return new DispatcherHandler(context);
  }

  public void setDispatcher(DispatcherHandler dispatcherHandler) {
    this.dispatcher = dispatcherHandler;
  }

}
