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
package cn.taketoday.web.config;

import java.util.Collections;
import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Environment;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.conversion.TypeConverter;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConvertUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.OrderUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.WebApplicationStartedEvent;
import cn.taketoday.web.handler.CompositeHandlerExceptionHandler;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.RequestHandlerAdapter;
import cn.taketoday.web.handler.ViewControllerHandlerAdapter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.CompositeHandlerRegistry;
import cn.taketoday.web.registry.FunctionHandlerRegistry;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.registry.ResourceHandlerRegistry;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.resolver.ParameterResolvers;
import cn.taketoday.web.validation.Validator;
import cn.taketoday.web.validation.WebValidator;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlers;
import cn.taketoday.web.view.RuntimeReturnValueHandler;

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
    final WebMvcConfiguration mvcConfiguration = getWebMvcConfiguration(context);

    configureTemplateLoader(context, mvcConfiguration);
    configureResourceHandler(context, mvcConfiguration);
    configureFunctionHandler(context, mvcConfiguration);
    configureViewControllerHandler(context, mvcConfiguration);
    configureExceptionHandler(context, mvcConfiguration);
    configureResultHandler(context, mvcConfiguration);
    configureConversionService(context, mvcConfiguration);
    configureHandlerAdapter(context, mvcConfiguration);
    configureParameterResolver(context, mvcConfiguration);
    configureHandlerRegistry(context, mvcConfiguration);

    // @since 3.0
    configureValidators(context, mvcConfiguration);

    // check all Components
    checkFrameWorkComponents(context);
    initializerStartup(context, mvcConfiguration);

    context.publishEvent(new WebApplicationStartedEvent(context));

    Runtime.getRuntime().addShutdownHook(new Thread(context::close));
    System.gc();

    logStartup(context);
  }

  protected void logStartup(WebApplicationContext context) {
    if (context.getEnvironment().getFlag(ENABLE_WEB_STARTED_LOG, true)) {
      log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
               System.currentTimeMillis() - context.getStartupDate()//
      );
    }
  }

  private void configureHandlerRegistry(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureHandlerRegistry(context.getBeans(HandlerRegistry.class), mvcConfiguration); //fix
  }

  protected void configureHandlerRegistry(List<HandlerRegistry> registries, WebMvcConfiguration mvcConfiguration) {
    final DispatcherHandler obtainDispatcher = obtainDispatcher();
    final HandlerRegistry handlerRegistry = obtainDispatcher.getHandlerRegistry();
    if (handlerRegistry != null) {
      registries.add(handlerRegistry);
    }
    // 自定义
    mvcConfiguration.configureHandlerRegistry(registries);

    obtainDispatcher.setHandlerRegistry(registries.size() == 1
                                        ? registries.get(0)
                                        : new CompositeHandlerRegistry(registries));
  }

  private void configureHandlerAdapter(
          final WebApplicationContext context, final WebMvcConfiguration mvcConfiguration) {
    configureHandlerAdapter(context.getBeans(HandlerAdapter.class), mvcConfiguration);
  }

  /**
   * Configure {@link HandlerAdapter}
   *
   * @param adapters
   *         {@link HandlerAdapter}s
   * @param mvcConfiguration
   *         {@link WebMvcConfiguration}
   */
  protected void configureHandlerAdapter(
          final List<HandlerAdapter> adapters, final WebMvcConfiguration mvcConfiguration) {
    // 先看有的
    final DispatcherHandler obtainDispatcher = obtainDispatcher();
    final HandlerAdapter[] handlerAdapters = obtainDispatcher.getHandlerAdapters();
    if (handlerAdapters != null) {
      Collections.addAll(adapters, handlerAdapters);
    }
    // 添加默认的
    adapters.add(new RequestHandlerAdapter(Ordered.HIGHEST_PRECEDENCE << 1));
    final WebApplicationContext context = obtainApplicationContext();
    // ViewControllerHandlerRegistry must configured
    final ViewControllerHandlerRegistry viewControllerRegistry = context.getBean(ViewControllerHandlerRegistry.class);
    if (viewControllerRegistry != null) {
      final ViewControllerHandlerAdapter bean = context.getBean(ViewControllerHandlerAdapter.class);
      if (bean == null) {
        ViewControllerHandlerAdapter viewControllerHandlerAdapter = null;
        for (final HandlerAdapter adapter : adapters) {
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
    // 用户自定义
    mvcConfiguration.configureHandlerAdapter(adapters);
    // 排序
    sort(adapters);
    // apply request handler
    obtainDispatcher.setHandlerAdapters(adapters.toArray(new HandlerAdapter[adapters.size()]));
  }

  /** @since sort objects 3.0.3 */
  protected void sort(List<?> adapters) {
    OrderUtils.reversedSort(adapters);
  }

  protected void configureViewControllerHandler(
          WebApplicationContext context, WebMvcConfiguration mvcConfiguration) throws Throwable {
    ViewControllerHandlerRegistry registry = context.getBean(ViewControllerHandlerRegistry.class);
    final Environment environment = context.getEnvironment();
    if (environment.getFlag(ENABLE_WEB_MVC_XML, true)) {
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
   * @param handlers
   *         handlers in application-context {@link #obtainApplicationContext()}
   */
  protected void configureExceptionHandler(
          List<HandlerExceptionHandler> handlers, WebMvcConfiguration mvcConfiguration) {
    final DispatcherHandler dispatcherHandler = obtainDispatcher();
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

  protected void configureFunctionHandler(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    final FunctionHandlerRegistry registry = context.getBean(FunctionHandlerRegistry.class);
    if (registry != null) {
      mvcConfiguration.configureFunctionHandler(registry);
    }
  }

  /**
   * Configure Freemarker's TemplateLoader s
   *
   * @since 2.3.7
   */
  protected void configureTemplateLoader(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    final Class<Object> loaderClass = ClassUtils.loadClass("freemarker.cache.TemplateLoader");
    if (loaderClass != null) {
      List<?> beans = context.getBeans(loaderClass);
      mvcConfiguration.configureTemplateLoader(beans);
    }
  }

  private void configureConversionService(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureConversionService(context.getBeans(TypeConverter.class), mvcConfiguration);
  }

  /**
   * Configure {@link TypeConverter} to resolve convert request parameters
   *
   * @param typeConverters
   *         Type converters
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureConversionService(
          List<TypeConverter> typeConverters, WebMvcConfiguration mvcConfiguration) {
    mvcConfiguration.configureConversionService(typeConverters);
    ConvertUtils.addConverter(typeConverters);// FIXME ConversionService
  }

  private void configureResultHandler(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureResultHandler(context.getBeans(ReturnValueHandler.class), mvcConfiguration);
  }

  /**
   * Configure {@link ReturnValueHandler} to resolve handler method result
   *
   * @param handlers
   *         {@link ReturnValueHandler} registry
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureResultHandler(List<ReturnValueHandler> handlers, WebMvcConfiguration mvcConfiguration) {
    final DispatcherHandler obtainDispatcher = obtainDispatcher();
    final RuntimeReturnValueHandler[] existingHandlers = obtainDispatcher.getResultHandlers();
    if (ObjectUtils.isNotEmpty(existingHandlers)) {
      Collections.addAll(handlers, existingHandlers);
    }
    final WebApplicationContext context = obtainApplicationContext();
    // @since 3.0
    final ReturnValueHandlers returnValueHandlers = context.getBean(ReturnValueHandlers.class);
    Assert.state(returnValueHandlers != null, "No ResultHandlers");
    // user config
    mvcConfiguration.configureResultHandler(handlers);

    returnValueHandlers.addHandlers(handlers);
    // apply result handler
    obtainDispatcher.setResultHandlers(returnValueHandlers.getRuntimeHandlers());
  }

  private void configureParameterResolver(
          WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    configureParameterResolver(context.getBeans(ParameterResolver.class), mvcConfiguration);
  }

  /**
   * Configure {@link ParameterResolver}s to resolve handler method arguments
   *
   * @param resolvers
   *         Resolvers registry
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureParameterResolver(
          List<ParameterResolver> resolvers, WebMvcConfiguration mvcConfiguration) {
    final WebApplicationContext context = obtainApplicationContext();
    final ParameterResolvers parameterResolvers = context.getBean(ParameterResolvers.class);
    Assert.state(parameterResolvers != null, "No ParameterResolvers");

    // user customize multipartConfig
    final MultipartConfiguration multipartConfig = context.getBean(MultipartConfiguration.class);
    mvcConfiguration.configureMultipart(multipartConfig);

    // User customize parameter resolver
    // ------------------------------------------
    mvcConfiguration.configureParameterResolver(resolvers); // user configure

    parameterResolvers.addResolver(resolvers);
  }

  /**
   * Configure {@link ResourceHandlerRegistry}
   *
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureResourceHandler(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    final ResourceHandlerRegistry registry = context.getBean(ResourceHandlerRegistry.class);
    if (registry != null) {
      mvcConfiguration.configureResourceHandler(registry);
    }
  }

  /**
   * configure {@link Validator}s
   *
   * @since 3.0
   */
  protected void configureValidators(WebApplicationContext context, WebMvcConfiguration mvcConfiguration) {
    final WebValidator webValidator = context.getBean(WebValidator.class);
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
   * @param context
   *         {@link ApplicationContext} object
   *
   * @throws Throwable
   *         If any initialize exception occurred
   */
  protected void initializerStartup(final WebApplicationContext context,
                                    final WebMvcConfiguration mvcConfiguration) throws Throwable //
  {
    final List<WebApplicationInitializer> initializers = context.getBeans(WebApplicationInitializer.class);
    configureInitializer(initializers, mvcConfiguration);

    for (final WebApplicationInitializer initializer : initializers) {
      initializer.onStartup(context);
    }
  }

  /**
   * Configure {@link WebApplicationInitializer}
   *
   * @param initializers
   *         {@link WebApplicationInitializer}s
   * @param config
   *         {@link CompositeWebMvcConfiguration}
   */
  protected void configureInitializer(List<WebApplicationInitializer> initializers, WebMvcConfiguration config) {
    config.configureInitializer(initializers);
    sort(initializers);
  }

  /**
   * Get {@link WebMvcConfiguration}
   *
   * @param applicationContext
   *         {@link ApplicationContext} object
   */
  protected WebMvcConfiguration getWebMvcConfiguration(ApplicationContext applicationContext) {
    return new CompositeWebMvcConfiguration(applicationContext.getBeans(WebMvcConfiguration.class));
  }

  /**
   * Initialize framework.
   *
   * @throws Throwable
   *         if any Throwable occurred
   */
  protected ViewControllerHandlerRegistry configViewControllerHandlerRegistry(ViewControllerHandlerRegistry registry) throws Throwable {
    // find the configure file
    log.info("TODAY WEB Framework Is Looking For ViewController Configuration File.");
    final String webMvcConfigLocation = getWebMvcConfigLocation();
    if (StringUtils.isEmpty(webMvcConfigLocation)) {
      log.info("Configuration File does not exist.");
      return registry;
    }
    if (registry == null) {
      final WebApplicationContext context = obtainApplicationContext();
      context.registerBean(ViewControllerHandlerRegistry.class);
      registry = context.getBean(ViewControllerHandlerRegistry.class);
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
  protected void checkFrameWorkComponents(WebApplicationContext applicationContext) {
    // no-op
  }

  public DispatcherHandler obtainDispatcher() {
    if (dispatcher == null) {
      final WebApplicationContext context = obtainApplicationContext();
      DispatcherHandler dispatcherHandler = context.getBean(DispatcherHandler.class);
      if (dispatcherHandler == null) {
        dispatcherHandler = createDispatcher(context);
        Assert.state(dispatcherHandler != null, "DispatcherHandler must not be null, sub class must create its instance");
        context.registerBean(dispatcherHandler);
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
