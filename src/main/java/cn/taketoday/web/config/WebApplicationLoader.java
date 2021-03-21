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
import java.util.Objects;
import java.util.Properties;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.annotation.RequestAttribute;
import cn.taketoday.web.event.WebApplicationStartedEvent;
import cn.taketoday.web.handler.CompositeHandlerExceptionHandler;
import cn.taketoday.web.handler.DefaultExceptionHandler;
import cn.taketoday.web.handler.DispatcherHandler;
import cn.taketoday.web.handler.FunctionRequestAdapter;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.handler.RequestHandlerAdapter;
import cn.taketoday.web.handler.ViewControllerHandlerAdapter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.CompositeHandlerRegistry;
import cn.taketoday.web.registry.FunctionHandlerRegistry;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.registry.ResourceHandlerRegistry;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;
import cn.taketoday.web.resolver.ArrayParameterResolver;
import cn.taketoday.web.resolver.CollectionParameterResolver;
import cn.taketoday.web.resolver.CookieParameterResolver;
import cn.taketoday.web.resolver.HeaderParameterResolver;
import cn.taketoday.web.resolver.MapParameterResolver;
import cn.taketoday.web.resolver.ModelParameterResolver;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.resolver.ParameterResolvers;
import cn.taketoday.web.resolver.RequestBodyParameterResolver;
import cn.taketoday.web.resolver.StreamParameterResolver;
import cn.taketoday.web.resolver.ThrowableHandlerParameterResolver;
import cn.taketoday.web.resolver.date.DateParameterResolver;
import cn.taketoday.web.resolver.date.LocalDateParameterResolver;
import cn.taketoday.web.resolver.date.LocalDateTimeParameterResolver;
import cn.taketoday.web.resolver.date.LocalTimeParameterResolver;
import cn.taketoday.web.validation.CompositeValidator;
import cn.taketoday.web.validation.Validator;
import cn.taketoday.web.view.HttpStatusResultHandler;
import cn.taketoday.web.view.ImageResultHandler;
import cn.taketoday.web.view.MessageConverter;
import cn.taketoday.web.view.ModelAndViewResultHandler;
import cn.taketoday.web.view.ObjectResultHandler;
import cn.taketoday.web.view.ResourceResultHandler;
import cn.taketoday.web.view.ResponseBodyResultHandler;
import cn.taketoday.web.view.ResponseEntityResultHandler;
import cn.taketoday.web.view.ResultHandler;
import cn.taketoday.web.view.ResultHandlers;
import cn.taketoday.web.view.RuntimeResultHandler;
import cn.taketoday.web.view.TemplateResultHandler;
import cn.taketoday.web.view.VoidResultHandler;
import cn.taketoday.web.view.template.AbstractTemplateViewResolver;
import cn.taketoday.web.view.template.DefaultTemplateViewResolver;
import cn.taketoday.web.view.template.TemplateViewResolver;

import static cn.taketoday.context.exception.ConfigurationException.nonNull;
import static cn.taketoday.context.utils.ContextUtils.resolveProps;
import static cn.taketoday.context.utils.ContextUtils.resolveValue;
import static cn.taketoday.web.resolver.ConverterParameterResolver.convert;
import static cn.taketoday.web.resolver.DelegatingParameterResolver.delegate;

/**
 * @author TODAY <br>
 * 2019-07-10 23:12
 */
public class WebApplicationLoader
        extends WebApplicationContextSupport implements WebApplicationInitializer {

  private DispatcherHandler dispatcher;

  @Override
  public void onStartup(WebApplicationContext context) throws Throwable {

    final WebMvcConfiguration mvcConfiguration = getWebMvcConfiguration(context);

    configureTemplateLoader(context, mvcConfiguration);
    configureResourceHandler(context, mvcConfiguration);
    configureFunctionHandler(context, mvcConfiguration);
    configureExceptionHandler(context.getBeans(HandlerExceptionHandler.class),
                              mvcConfiguration);

    configureResultHandler(context.getBeans(ResultHandler.class), mvcConfiguration);
    configureTypeConverter(context.getBeans(TypeConverter.class), mvcConfiguration);
    configureHandlerAdapter(context.getBeans(HandlerAdapter.class), mvcConfiguration);
    configureParameterResolver(context.getBeans(ParameterResolver.class), mvcConfiguration);

    configureViewControllerHandler(context, mvcConfiguration);
    configureHandlerRegistry(context.getBeans(HandlerRegistry.class), mvcConfiguration);//fix
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
    if (context.getEnvironment().getFlag(Constant.ENABLE_WEB_STARTED_LOG, true)) {
      log.info("Your Application Started Successfully, It takes a total of [{}] ms.", //
               System.currentTimeMillis() - context.getStartupDate()//
      );
    }
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

  /**
   * Configure {@link HandlerAdapter}
   *
   * @param adapters
   *         {@link HandlerAdapter}s
   * @param mvcConfiguration
   *         {@link WebMvcConfiguration}
   */
  protected void configureHandlerAdapter(final List<HandlerAdapter> adapters, WebMvcConfiguration mvcConfiguration) {
    // 先看有的
    final DispatcherHandler obtainDispatcher = obtainDispatcher();
    final HandlerAdapter[] handlerAdapters = obtainDispatcher.getHandlerAdapters();
    if (handlerAdapters != null) {
      Collections.addAll(adapters, handlerAdapters);
    }
    // 添加默认的
    adapters.add(new RequestHandlerAdapter(Ordered.HIGHEST_PRECEDENCE << 1));
    adapters.add(new FunctionRequestAdapter(Ordered.HIGHEST_PRECEDENCE - 1));
    adapters.add(new ViewControllerHandlerAdapter(Ordered.HIGHEST_PRECEDENCE - 2));
//    adapters.add(new NotFoundRequestAdapter(-100));

    // 用户自定义
    mvcConfiguration.configureHandlerAdapter(adapters);

    // 排序
    OrderUtils.reversedSort(adapters);

    // apply request handler
    obtainDispatcher.setHandlerAdapters(adapters.toArray(new HandlerAdapter[adapters.size()]));
  }

  protected void configureViewControllerHandler(WebApplicationContext context,
                                                WebMvcConfiguration mvcConfiguration) throws Throwable {

    ViewControllerHandlerRegistry registry = context.getBean(ViewControllerHandlerRegistry.class);

    final Environment environment = context.getEnvironment();
    if (environment.getFlag(Constant.ENABLE_WEB_MVC_XML, true)) {
      registry = configViewControllerHandlerRegistry(registry);
    }
    if (registry != null) {
      mvcConfiguration.configureViewController(registry);
    }
  }

  protected void configureExceptionHandler(List<HandlerExceptionHandler> handlers,
                                           WebMvcConfiguration mvcConfiguration) {

    final DispatcherHandler dispatcherHandler = obtainDispatcher();
    final HandlerExceptionHandler exceptionHandler = dispatcherHandler.getExceptionHandler();
    if (exceptionHandler != null) {
      handlers.add(exceptionHandler);
    }

    // user config
    mvcConfiguration.configureExceptionHandlers(handlers);

    if (handlers.isEmpty()) {
      final WebApplicationContext context = obtainApplicationContext();
      DefaultExceptionHandler defaultHandler = context.getBean(DefaultExceptionHandler.class);
      if (defaultHandler == null) {
        context.registerBean(DefaultExceptionHandler.class);
        defaultHandler = context.getBean(DefaultExceptionHandler.class);
      }
      handlers.add(defaultHandler);
    }

    // set
    dispatcherHandler.setExceptionHandler(handlers.size() == 1
                                          ? handlers.get(0)
                                          : new CompositeHandlerExceptionHandler(handlers));
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

  /**
   * Configure {@link TypeConverter} to resolve convert request parameters
   *
   * @param typeConverters
   *         Type converters
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureTypeConverter(List<TypeConverter> typeConverters, WebMvcConfiguration mvcConfiguration) {
    mvcConfiguration.configureTypeConverter(typeConverters);

    ConvertUtils.addConverter(typeConverters);
  }

  /**
   * Configure {@link ResultHandler} to resolve handler method result
   *
   * @param handlers
   *         {@link ResultHandler} registry
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureResultHandler(List<ResultHandler> handlers, WebMvcConfiguration mvcConfiguration) {
    final DispatcherHandler obtainDispatcher = obtainDispatcher();
    final RuntimeResultHandler[] resultHandlers = obtainDispatcher.getResultHandlers();
    if (ObjectUtils.isNotEmpty(resultHandlers)) {
      Collections.addAll(handlers, resultHandlers);
    }
    final WebApplicationContext context = obtainApplicationContext();

    final TemplateViewResolver viewResolver = getTemplateViewResolver(mvcConfiguration);
    final Environment environment = context.getEnvironment();
    int bufferSize = Integer.parseInt(environment.getProperty(Constant.DOWNLOAD_BUFF_SIZE, "10240"));

    final MessageConverter messageConverter = context.getBean(MessageConverter.class);

    handlers.add(new ImageResultHandler());
    handlers.add(new ResourceResultHandler(bufferSize));
    handlers.add(new TemplateResultHandler(viewResolver));
    handlers.add(new VoidResultHandler(viewResolver, messageConverter, bufferSize));
    handlers.add(new ObjectResultHandler(viewResolver, messageConverter, bufferSize));
    handlers.add(new ModelAndViewResultHandler(viewResolver, messageConverter, bufferSize));
    handlers.add(new ResponseEntityResultHandler(viewResolver, messageConverter, bufferSize));

    handlers.add(new ResponseBodyResultHandler(messageConverter));

    handlers.add(new HttpStatusResultHandler());

    // 自定义
    mvcConfiguration.configureResultHandler(handlers);

    ResultHandlers.addHandler(handlers);

    // apply result handler
    obtainDispatcher.setResultHandlers(ResultHandlers.getRuntimeHandlers());
  }

  protected TemplateViewResolver getTemplateViewResolver(final WebMvcConfiguration mvcConfiguration) {

    final WebApplicationContext context = obtainApplicationContext();
    TemplateViewResolver templateViewResolver = context.getBean(TemplateViewResolver.class);

    if (templateViewResolver == null) {
      context.registerBean(DefaultTemplateViewResolver.class);
      templateViewResolver = context.getBean(TemplateViewResolver.class);
    }

    configureTemplateViewResolver(templateViewResolver, mvcConfiguration);
    return templateViewResolver;
  }

  /**
   * @param templateResolver
   *         {@link TemplateViewResolver} object
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureTemplateViewResolver(TemplateViewResolver templateResolver, WebMvcConfiguration mvcConfiguration) {
    if (templateResolver instanceof AbstractTemplateViewResolver) {
      mvcConfiguration.configureTemplateViewResolver((AbstractTemplateViewResolver) templateResolver);
    }
  }

  /**
   * Configure {@link ParameterResolver}s to resolve handler method arguments
   *
   * @param resolvers
   *         Resolvers registry
   * @param mvcConfiguration
   *         All {@link WebMvcConfiguration} object
   */
  protected void configureParameterResolver(List<ParameterResolver> resolvers, WebMvcConfiguration mvcConfiguration) {
    // Use ConverterParameterResolver to resolve primitive types
    // --------------------------------------------------------------------------

    resolvers.add(convert(m -> m.is(String.class), s -> s));
    resolvers.add(convert(m -> m.is(Long.class) || m.is(long.class), Long::parseLong));
    resolvers.add(convert(m -> m.is(Integer.class) || m.is(int.class), Integer::parseInt));
    resolvers.add(convert(m -> m.is(Short.class) || m.is(short.class), Short::parseShort));
    resolvers.add(convert(m -> m.is(Float.class) || m.is(float.class), Float::parseFloat));
    resolvers.add(convert(m -> m.is(Double.class) || m.is(double.class), Double::parseDouble));
    resolvers.add(convert(m -> m.is(Boolean.class) || m.is(boolean.class), Boolean::parseBoolean));

    // For some useful context annotations @off
    // --------------------------------------------

    resolvers.add(delegate(m -> m.isAnnotationPresent(RequestAttribute.class), //
      (ctx, m) -> ctx.attribute(m.getName())//
    ));

    resolvers.add(delegate(m -> m.isAnnotationPresent(Value.class), //
      (ctx, m) -> resolveValue(m.getAnnotation(Value.class), m.getParameterClass())//
    ));
    resolvers.add(delegate(m -> m.isAnnotationPresent(Env.class), //
      (ctx, m) -> resolveValue(m.getAnnotation(Env.class), m.getParameterClass())//
    ));

    final WebApplicationContext context = obtainApplicationContext();
    final Properties properties = context.getEnvironment().getProperties();

    resolvers.add(delegate(m -> m.isAnnotationPresent(Props.class), //
      (ctx, m) -> resolveProps(m.getAnnotation(Props.class), m.getParameterClass(), properties)//
    ));

    resolvers.add(delegate(m -> m.isAnnotationPresent(Autowired.class), //@off
      (ctx, m) -> {
        final Autowired autowired = m.getAnnotation(Autowired.class);
        final String name = autowired.value();

        final Object bean;
        if (StringUtils.isEmpty(name)) {
            bean = context.getBean(m.getParameterClass());
        }
        else {
            bean = context.getBean(name, m.getParameterClass());
        }
        if (bean == null && autowired.required()) {
          throw new NoSuchBeanDefinitionException(m.getParameterClass());
        }
        return bean;
      }
    ));

    // HandlerMethod @on
    resolvers.add(delegate(m -> m.is(HandlerMethod.class), (ctx, m) -> m.getHandlerMethod()));

    // For cookies
    // ------------------------------------------
    resolvers.add(new CookieParameterResolver());
    resolvers.add(new CookieParameterResolver.CookieArrayParameterResolver());
    resolvers.add(new CookieParameterResolver.CookieAnnotationParameterResolver());
    resolvers.add(new CookieParameterResolver.CookieCollectionParameterResolver());

    // For multipart
    // -------------------------------------------

    configureMultipart(resolvers, context.getBean(MultipartConfiguration.class), mvcConfiguration);

    // Header
    resolvers.add(new HeaderParameterResolver());

    resolvers.add(new MapParameterResolver());
    resolvers.add(new ModelParameterResolver());
    resolvers.add(new ArrayParameterResolver());
    resolvers.add(new StreamParameterResolver());

    final MessageConverter messageConverter = context.getBean(MessageConverter.class);
    resolvers.add(new RequestBodyParameterResolver(messageConverter));
    resolvers.add(new ThrowableHandlerParameterResolver());

    resolvers.add(new CollectionParameterResolver());
    //resolvers.add(new DataBinderParameterResolver());

    // Date API support
    resolvers.add(new DateParameterResolver());
    resolvers.add(new LocalDateParameterResolver());
    resolvers.add(new LocalTimeParameterResolver());
    resolvers.add(new LocalDateTimeParameterResolver());

    // User customize parameter resolver
    // ------------------------------------------

    mvcConfiguration.configureParameterResolver(resolvers); // user configure
    // post
    postConfigureParameterResolver(resolvers, mvcConfiguration);

    final ParameterResolvers parameterResolvers = context.getBean(ParameterResolvers.class);
    parameterResolvers.addResolver(resolvers);
  }

  protected void postConfigureParameterResolver(final List<ParameterResolver> resolvers,
                                                final WebMvcConfiguration mvcConfiguration) {

  }

  protected void configureMultipart(List<ParameterResolver> resolvers,
                                    MultipartConfiguration multipartConfiguration, WebMvcConfiguration mvcConfiguration) {

    Objects.requireNonNull(multipartConfiguration, "Multipart Config Can't be null");
    mvcConfiguration.configureMultipart(multipartConfiguration);
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
    final CompositeValidator compositeValidator = context.getBean(CompositeValidator.class);
    if (compositeValidator != null) {
      log.info("Enable Bean Validation");
      // user Manual config
      mvcConfiguration.configureValidators(compositeValidator);
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
    OrderUtils.reversedSort(initializers);
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

  protected String getWebMvcConfigLocation() {
    return obtainApplicationContext().getEnvironment().getProperty(Constant.WEB_MVC_CONFIG_LOCATION);
  }

  /**
   * Check Components
   */
  protected void checkFrameWorkComponents(WebApplicationContext applicationContext) {}

  public DispatcherHandler obtainDispatcher() {
    if (dispatcher == null) {
      final WebApplicationContext context = obtainApplicationContext();
      DispatcherHandler dispatcherHandler = context.getBean(DispatcherHandler.class);
      if (dispatcherHandler == null) {
        dispatcherHandler = createDispatcher(context);
        nonNull(dispatcherHandler, "DispatcherHandler must not be null, sub class must create its intsance");
        context.registerBean(dispatcherHandler);
      }
      this.dispatcher = dispatcherHandler;
    }
    return dispatcher;
  }

  protected DispatcherHandler createDispatcher(WebApplicationContext context) {
    return null;
  }

  public void setDispatcher(DispatcherHandler dispatcherHandler) {
    this.dispatcher = dispatcherHandler;
  }

}
