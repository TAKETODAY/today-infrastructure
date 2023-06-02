/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.session.config.EnableWebSession;
import cn.taketoday.stereotype.Component;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.config.AsyncSupportConfigurer;
import cn.taketoday.web.config.DelegatingWebMvcConfiguration;
import cn.taketoday.web.config.InterceptorRegistration;
import cn.taketoday.web.config.InterceptorRegistry;
import cn.taketoday.web.config.PathMatchConfigurer;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.handler.MappedInterceptor;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;
import cn.taketoday.web.i18n.AcceptHeaderLocaleResolver;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.view.RedirectModelManager;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewRef;
import cn.taketoday.web.view.ViewResolver;
import cn.taketoday.web.view.ViewResolverComposite;
import cn.taketoday.web.view.ViewReturnValueHandler;
import jakarta.servlet.ServletContext;

/**
 * A {@code MockMvcBuilder} that accepts {@code @Controller} registrations
 * thus allowing full control over the instantiation and initialization of
 * controllers and their dependencies similar to plain unit tests, and also
 * making it possible to test one controller at a time.
 *
 * <p>This builder creates the minimum infrastructure required by the
 * {@link DispatcherServlet} to serve requests with annotated controllers and
 * also provides methods for customization. The resulting configuration and
 * customization options are equivalent to using MVC Java config except
 * using builder style methods.
 *
 * <p>To configure view resolution, either select a "fixed" view to use for every
 * request performed (see {@link #setSingleView(View)}) or provide a list of
 * {@code ViewResolver}s (see {@link #setViewResolvers(ViewResolver...)}).
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandaloneMockMvcBuilder extends AbstractMockMvcBuilder<StandaloneMockMvcBuilder> {

  private final List<Object> controllers;

  @Nullable
  private List<Object> controllerAdvice;

  private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

  private List<ParameterResolvingStrategy> customArgumentResolvers = new ArrayList<>();

  private List<ReturnValueHandler> customReturnValueHandlers = new ArrayList<>();

  private final List<MappedInterceptor> mappedInterceptors = new ArrayList<>();

  @Nullable
  private Validator validator;

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  @Nullable
  private FormattingConversionService conversionService;

  @Nullable
  private List<HandlerExceptionHandler> handlerExceptionHandlers;

  @Nullable
  private Long asyncRequestTimeout;

  @Nullable
  private List<ViewResolver> viewResolvers;

  @Nullable
  private LocaleResolver localeResolver;

  @Nullable
  private RedirectModelManager flashMapManager;

  private boolean useTrailingSlashPatternMatch = true;

  private final Map<String, Object> placeholderValues = new HashMap<>();

  private Supplier<RequestMappingHandlerMapping> handlerMappingFactory = RequestMappingHandlerMapping::new;

  @Nullable
  private RedirectModelManager redirectModelManager;

  @Nullable
  private ReturnValueHandlerManager returnValueHandlerManager;

  @Nullable
  private ParameterResolvingRegistry parameterResolvingRegistry;

  /**
   * Protected constructor. Not intended for direct instantiation.
   *
   * @see MockMvcBuilders#standaloneSetup(Object...)
   */
  protected StandaloneMockMvcBuilder(Object... controllers) {
    this.controllers = instantiateIfNecessary(controllers);
  }

  private static List<Object> instantiateIfNecessary(Object[] specified) {
    List<Object> instances = new ArrayList<>(specified.length);
    for (Object obj : specified) {
      instances.add(obj instanceof Class<?> clazz ? BeanUtils.newInstance(clazz) : obj);
    }
    return instances;
  }

  /**
   * Register one or more {@link cn.taketoday.web.annotation.ControllerAdvice}
   * instances to be used in tests (specified {@code Class} will be turned into instance).
   * <p>Normally {@code @ControllerAdvice} are auto-detected as long as they're declared
   * as Infra beans. However since the standalone setup does not load any Infra config,
   * they need to be registered explicitly here instead much like controllers.
   */
  public StandaloneMockMvcBuilder setControllerAdvice(Object... controllerAdvice) {
    this.controllerAdvice = instantiateIfNecessary(controllerAdvice);
    return this;
  }

  /**
   * Set the message converters to use in argument resolvers and in return value
   * handlers, which support reading and/or writing to the body of the request
   * and response. If no message converters are added to the list, a default
   * list of converters is added instead.
   */
  public StandaloneMockMvcBuilder setMessageConverters(HttpMessageConverter<?>... messageConverters) {
    this.messageConverters = Arrays.asList(messageConverters);
    return this;
  }

  /**
   * Provide a custom {@link Validator} instead of the one created by default.
   * The default implementation used, assuming JSR-303 is on the classpath, is
   * {@link cn.taketoday.validation.beanvalidation.LocalValidatorFactoryBean}.
   */
  public StandaloneMockMvcBuilder setValidator(Validator validator) {
    this.validator = validator;
    return this;
  }

  /**
   * Provide a conversion service with custom formatters and converters.
   * If not set, a {@link DefaultFormattingConversionService} is used by default.
   */
  public StandaloneMockMvcBuilder setConversionService(FormattingConversionService conversionService) {
    this.conversionService = conversionService;
    return this;
  }

  /**
   * Add interceptors mapped to all incoming requests.
   */
  public StandaloneMockMvcBuilder addInterceptors(HandlerInterceptor... interceptors) {
    addMappedInterceptors(null, interceptors);
    return this;
  }

  /**
   * Add interceptors mapped to a set of path patterns.
   */
  public StandaloneMockMvcBuilder addMappedInterceptors(
          @Nullable String[] pathPatterns, HandlerInterceptor... interceptors) {

    for (HandlerInterceptor interceptor : interceptors) {
      this.mappedInterceptors.add(new MappedInterceptor(pathPatterns, null, interceptor));
    }
    return this;
  }

  /**
   * Set a ContentNegotiationManager.
   */
  public StandaloneMockMvcBuilder setContentNegotiationManager(ContentNegotiationManager manager) {
    this.contentNegotiationManager = manager;
    return this;
  }

  /**
   * Specify the timeout value for async execution. In Web MVC Test, this
   * value is used to determine how to long to wait for async execution to
   * complete so that a test can verify the results synchronously.
   *
   * @param timeout the timeout value in milliseconds
   */
  public StandaloneMockMvcBuilder setAsyncRequestTimeout(long timeout) {
    this.asyncRequestTimeout = timeout;
    return this;
  }

  /**
   * Provide custom resolvers for controller method arguments.
   */
  public StandaloneMockMvcBuilder setCustomArgumentResolvers(ParameterResolvingStrategy... argumentResolvers) {
    this.customArgumentResolvers = Arrays.asList(argumentResolvers);
    return this;
  }

  /**
   * Provide custom handlers for controller method return values.
   */
  public StandaloneMockMvcBuilder setCustomReturnValueHandlers(HandlerMethodReturnValueHandler... handlers) {
    this.customReturnValueHandlers = Arrays.asList(handlers);
    return this;
  }

  /**
   * Set the HandlerExceptionHandler types to use as a list.
   */
  public StandaloneMockMvcBuilder setHandlerExceptionHandlers(List<HandlerExceptionHandler> exceptionResolvers) {
    this.handlerExceptionHandlers = exceptionResolvers;
    return this;
  }

  /**
   * Set the HandlerExceptionHandler types to use as an array.
   */
  public StandaloneMockMvcBuilder setHandlerExceptionHandlers(HandlerExceptionHandler... exceptionResolvers) {
    this.handlerExceptionHandlers = Arrays.asList(exceptionResolvers);
    return this;
  }

  /**
   * Set up view resolution with the given {@link ViewResolver ViewResolvers}.
   * If not set, an {@link InternalResourceViewResolver} is used by default.
   */
  public StandaloneMockMvcBuilder setViewResolvers(ViewResolver... resolvers) {
    this.viewResolvers = Arrays.asList(resolvers);
    return this;
  }

  /**
   * Sets up a single {@link ViewResolver} that always returns the provided
   * view instance. This is a convenient shortcut if you need to use one
   * View instance only -- e.g. rendering generated content (JSON, XML, Atom).
   */
  public StandaloneMockMvcBuilder setSingleView(View view) {
    this.viewResolvers = Collections.singletonList(new StaticViewResolver(view));
    return this;
  }

  /**
   * Provide a LocaleResolver instance.
   * If not provided, the default one used is {@link AcceptHeaderLocaleResolver}.
   */
  public StandaloneMockMvcBuilder setLocaleResolver(LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
    return this;
  }

  /**
   * Provide a custom FlashMapManager instance.
   * If not provided, {@code SessionFlashMapManager} is used by default.
   */
  public StandaloneMockMvcBuilder setFlashMapManager(RedirectModelManager flashMapManager) {
    this.flashMapManager = flashMapManager;
    return this;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   * If enabled a method mapped to "/users" also matches to "/users/".
   */
  public StandaloneMockMvcBuilder setUseTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
    this.useTrailingSlashPatternMatch = useTrailingSlashPatternMatch;
    return this;
  }

  /**
   * In a standalone setup there is no support for placeholder values embedded in
   * request mappings. This method allows manually provided placeholder values so they
   * can be resolved. Alternatively consider creating a test that initializes a
   * {@link WebApplicationContext}.
   */
  public StandaloneMockMvcBuilder addPlaceholderValue(String name, Object value) {
    this.placeholderValues.put(name, value);
    return this;
  }

  /**
   * Configure factory to create a custom {@link RequestMappingHandlerMapping}.
   *
   * @param factory the factory
   */
  public StandaloneMockMvcBuilder setCustomHandlerMapping(Supplier<RequestMappingHandlerMapping> factory) {
    this.handlerMappingFactory = factory;
    return this;
  }

  public StandaloneMockMvcBuilder setRedirectModelManager(@Nullable RedirectModelManager redirectModelManager) {
    this.redirectModelManager = redirectModelManager;
    return this;
  }

  public StandaloneMockMvcBuilder setReturnValueHandlerManager(ReturnValueHandlerManager returnValueHandlerManager) {
    this.returnValueHandlerManager = returnValueHandlerManager;
    return this;
  }

  public StandaloneMockMvcBuilder setParameterResolvingRegistry(ParameterResolvingRegistry parameterResolvingRegistry) {
    this.parameterResolvingRegistry = parameterResolvingRegistry;
    return this;
  }

  @Override
  protected WebApplicationContext initWebAppContext() {
    MockServletContext servletContext = new MockServletContext();
    StubWebApplicationContext wac = new StubWebApplicationContext(servletContext);

    var reader = new AnnotatedBeanDefinitionReader(wac);
    reader.register(StandaloneConfiguration.class);

    registerMvcSingletons(wac);

    servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
    return wac;
  }

  private void registerMvcSingletons(StubWebApplicationContext wac) {
    ServletContext sc = wac.getServletContext();
    wac.addBeans(this);

    wac.addBeans(this.controllers);
    wac.addBeans(this.controllerAdvice);
    if (parameterResolvingRegistry != null) {
      wac.addBeans(parameterResolvingRegistry);
    }

    if (returnValueHandlerManager != null) {
      wac.addBeans(returnValueHandlerManager);
    }

    if (redirectModelManager != null) {
      wac.addBean(RedirectModelManager.BEAN_NAME, redirectModelManager);
    }

    if (viewResolvers == null) {
      wac.addBeans(new InternalResourceViewResolver());
    }
    else {
      wac.addBeans(viewResolvers);
    }

    if (localeResolver != null) {
      wac.addBean(LocaleResolver.BEAN_NAME, this.localeResolver);
    }

    if (flashMapManager != null) {
      wac.addBean(RedirectModelManager.BEAN_NAME, this.flashMapManager);
    }

    wac.refresh();
    extendMvcSingletons(sc).forEach(wac::addBean);
  }

  /**
   * This method could be used from a subclass to register additional Spring
   * MVC infrastructure such as additional {@code HandlerMapping},
   * {@code HandlerAdapter}, and others.
   *
   * @param servletContext the ServletContext
   * @return a map with additional MVC infrastructure object instances
   */
  protected Map<String, Object> extendMvcSingletons(@Nullable ServletContext servletContext) {
    return Collections.emptyMap();
  }

  /**
   * Using the MVC Java configuration as the starting point for the "standalone" setup.
   */
  @EnableWebSession
  @Configuration(proxyBeanMethods = false)
  private class StandaloneConfiguration extends DelegatingWebMvcConfiguration {

    public StandaloneConfiguration(List<WebMvcConfigurer> configurers) {
      super(configurers);
    }

    @Override
    @Component
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(ViewReturnValueHandler.class)
    public ViewAttributeReturnValueHandler viewReturnValueHandler(
            @Qualifier(LocaleResolver.BEAN_NAME) LocaleResolver localeResolver, List<ViewResolver> viewResolvers) {
      ViewResolver viewResolver;
      if (viewResolvers.size() == 1) {
        viewResolver = CollectionUtils.firstElement(viewResolvers);
        Assert.state(viewResolver != null, "No ViewResolver");
      }
      else {
        ViewResolverComposite composite = new ViewResolverComposite();
        composite.setViewResolvers(viewResolvers);
        viewResolver = composite;
      }

      ViewAttributeReturnValueHandler handler = new ViewAttributeReturnValueHandler(viewResolver);
      handler.setLocaleResolver(localeResolver);
      return handler;
    }

    @Autowired
    void init(ConfigurableEnvironment environment) {
      environment.getPropertySources().
              addFirst(new MapPropertySource("placeholderValues", placeholderValues));
    }

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
      return handlerMappingFactory.get();
    }

    @Override
    protected void configurePathMatch(PathMatchConfigurer configurer) {
      super.configurePathMatch(configurer);
      configurer.setUseTrailingSlashMatch(useTrailingSlashPatternMatch);
    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.addAll(messageConverters);
    }

    @Override
    protected void modifyParameterResolvingRegistry(ParameterResolvingRegistry registry) {
      registry.getCustomizedStrategies().add(customArgumentResolvers);
    }

    @Override
    protected void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {
      manager.addHandlers(customReturnValueHandlers);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      super.addInterceptors(registry);

      for (MappedInterceptor interceptor : mappedInterceptors) {
        InterceptorRegistration registration = registry.addInterceptor(interceptor.getInterceptor());
        if (interceptor.getPathPatterns() != null) {
          registration.addPathPatterns(interceptor.getPathPatterns());
        }
      }
    }

    @Override
    public ContentNegotiationManager mvcContentNegotiationManager() {
      return (contentNegotiationManager != null) ? contentNegotiationManager : super.mvcContentNegotiationManager();
    }

    @Override
    public FormattingConversionService mvcConversionService() {
      return (conversionService != null ? conversionService : super.mvcConversionService());
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
      if (asyncRequestTimeout != null) {
        configurer.setDefaultTimeout(asyncRequestTimeout);
      }
    }

    @Override
    public Validator mvcValidator() {
      Validator mvcValidator = (validator != null) ? validator : super.mvcValidator();
      if (mvcValidator instanceof InitializingBean initializingBean) {
        try {
          initializingBean.afterPropertiesSet();
        }
        catch (Exception ex) {
          throw new BeanInitializationException("Failed to initialize Validator", ex);
        }
      }
      return mvcValidator;
    }

    @Override
    protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
      if (handlerExceptionHandlers == null) {
        return;
      }
      for (HandlerExceptionHandler resolver : handlerExceptionHandlers) {
        if (resolver instanceof ApplicationContextAware applicationContextAware) {
          ApplicationContext applicationContext = getApplicationContext();
          if (applicationContext != null) {
            applicationContextAware.setApplicationContext(applicationContext);
          }
        }
        if (resolver instanceof InitializingBean initializingBean) {
          try {
            initializingBean.afterPropertiesSet();
          }
          catch (Exception ex) {
            throw new IllegalStateException("Failure from afterPropertiesSet", ex);
          }
        }
        handlers.add(resolver);
      }
    }
  }

  /**
   * A {@link ViewResolver} that always returns same View.
   */
  private static class StaticViewResolver implements ViewResolver {

    private final View view;

    public StaticViewResolver(View view) {
      this.view = view;
    }

    @Override
    @Nullable
    public View resolveViewName(String viewName, Locale locale) {
      return this.view;
    }
  }

  static class ViewAttributeReturnValueHandler extends ViewReturnValueHandler {

    public ViewAttributeReturnValueHandler(ViewResolver viewResolver) {
      super(viewResolver);
    }

    @Override
    public void renderView(RequestContext context, View view) {
      super.renderView(context, view);
      context.setAttribute(MvcResult.VIEW_ATTRIBUTE, view);
    }

    @Override
    public void renderView(RequestContext context, ViewRef viewRef) {
      super.renderView(context, viewRef);
      String viewName = viewRef.getViewName();
      context.setAttribute(MvcResult.VIEW_NAME_ATTRIBUTE, viewName);
    }

  }

}
