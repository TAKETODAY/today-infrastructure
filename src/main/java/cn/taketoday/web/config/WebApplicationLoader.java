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
import cn.taketoday.web.ApplicationStartedEvent;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.CompositeHandlerExceptionHandler;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.RequestHandlerAdapter;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.SelectableReturnValueHandler;
import cn.taketoday.web.handler.ViewControllerHandlerAdapter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.HandlerRegistries;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;
import cn.taketoday.web.validation.Validator;
import cn.taketoday.web.validation.WebValidator;

/**
 * @author TODAY 2019-07-10 23:12
 */
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
    configureExceptionHandler(context, mvcConfiguration);
    configureReturnValueHandler(context, mvcConfiguration);
    configureHandlerAdapter(context, mvcConfiguration);
    configureParameterResolving(context, mvcConfiguration);
    configureHandlerRegistry(context, mvcConfiguration);

    // @since 3.0
    configureValidators(context, mvcConfiguration);

    // check all Components
    checkFrameworkComponents(context);
    initializerStartup(context, mvcConfiguration);

    context.publishEvent(new ApplicationStartedEvent(context));
    context.registerShutdownHook();

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

  private void configureHandlerRegistry(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureHandlerRegistry(context.getBeans(HandlerRegistry.class), mvcConfiguration); //fix
  }

  protected void configureHandlerRegistry(
          List<HandlerRegistry> registries, WebMvcConfiguration mvcConfiguration) {
    DispatcherHandler obtainDispatcher = obtainDispatcher();
    HandlerRegistry handlerRegistry = obtainDispatcher.getHandlerRegistry();
    if (handlerRegistry != null) {
      registries.add(handlerRegistry);
    }
    // 自定义
    mvcConfiguration.configureHandlerRegistry(registries);

    obtainDispatcher.setHandlerRegistry(registries.size() == 1
                                        ? registries.get(0)
                                        : new HandlerRegistries(registries));
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

  /**
   * configure HandlerExceptionHandler
   */
  private void configureExceptionHandler(
          WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureExceptionHandler(context.getBeans(HandlerExceptionHandler.class), mvcConfiguration);
  }

  /**
   * configure HandlerExceptionHandler
   *
   * @param handlers handlers in application-context {@link #obtainApplicationContext()}
   */
  protected void configureExceptionHandler(
          List<HandlerExceptionHandler> handlers, WebMvcConfiguration mvcConfiguration) {
    DispatcherHandler dispatcherHandler = obtainDispatcher();
    HandlerExceptionHandler exceptionHandler = dispatcherHandler.getExceptionHandler();
    if (exceptionHandler != null) {
      handlers.add(exceptionHandler);
    }
    // user config
    mvcConfiguration.configureExceptionHandlers(handlers);
    // at least one exception-handler
    if (handlers.size() == 1) {
      exceptionHandler = handlers.get(0);
    }
    else {
      sort(handlers); // @since 3.0.3 exception handlers order
      exceptionHandler = new CompositeHandlerExceptionHandler(handlers);
    }
    // set
    dispatcherHandler.setExceptionHandler(exceptionHandler);
  }

  private void configureReturnValueHandler(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureReturnValueHandler(context.getBeans(ReturnValueHandler.class), mvcConfiguration);
  }

  /**
   * Configure {@link ReturnValueHandler} to resolve handler method result
   *
   * @param handlers {@link ReturnValueHandler} registry
   * @param mvcConfiguration All {@link WebMvcConfiguration} object
   */
  protected void configureReturnValueHandler(
          List<ReturnValueHandler> handlers, WebMvcConfiguration mvcConfiguration) {
    DispatcherHandler obtainDispatcher = obtainDispatcher();
    SelectableReturnValueHandler existingHandlers = obtainDispatcher.getReturnValueHandler();
    if (existingHandlers != null) {
      handlers.addAll(existingHandlers.getInternalHandlers());
    }
    WebApplicationContext context = obtainApplicationContext();
    // @since 3.0
    ReturnValueHandlerManager manager = context.getBean(ReturnValueHandlerManager.class);
    Assert.state(manager != null, "No ReturnValueHandlers");
    // user config
    mvcConfiguration.configureResultHandler(handlers);

    manager.addHandlers(handlers);
    // apply result handler
    SelectableReturnValueHandler selectable =
            new SelectableReturnValueHandler(manager.getHandlers());
    selectable.trimToSize();
    obtainDispatcher.setReturnValueHandler(selectable);
  }

  private void configureParameterResolving(
          WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureParameterResolving(context.getBeans(ParameterResolvingStrategy.class), mvcConfiguration);
  }

  /**
   * Configure {@link ParameterResolvingStrategy}s to resolve handler method arguments
   *
   * @param customizedStrategies Resolvers registry
   * @param mvcConfiguration All {@link WebMvcConfiguration} object
   */
  protected void configureParameterResolving(
          List<ParameterResolvingStrategy> customizedStrategies, WebMvcConfiguration mvcConfiguration) {
    WebApplicationContext context = obtainApplicationContext();
    ParameterResolvingRegistry registry = context.getBean(ParameterResolvingRegistry.class);
    Assert.state(registry != null, "No ParameterResolvingRegistry in context");

    // user customize multipartConfig
    MultipartConfiguration multipartConfig = context.getBean(MultipartConfiguration.class);
    mvcConfiguration.configureMultipart(multipartConfig);

    // User customize parameter resolver
    // ------------------------------------------
    mvcConfiguration.configureParameterResolving(registry, customizedStrategies); // user configure

    registry.getCustomizedStrategies().add(customizedStrategies);
  }

  /**
   * configure {@link Validator}s
   *
   * @since 3.0
   */
  protected void configureValidators(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    WebValidator webValidator = context.getBean(WebValidator.class);
    if (webValidator != null) {
      log.info("Enable Bean Validation using web validator: {}", webValidator);
      // user Manual config
      mvcConfiguration.configureValidators(webValidator);
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

  /**
   * Check Components
   */
  protected void checkFrameworkComponents(WebApplicationContext applicationContext) {
    // no-op
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
