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
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanInitializationException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.util.PropertyPlaceholderHelper;
import cn.taketoday.util.PropertyPlaceholderHelper.PlaceholderResolver;
import cn.taketoday.util.StringValueResolver;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.support.WebApplicationObjectSupport;
import cn.taketoday.web.method.support.HandlerMethodArgumentResolver;
import cn.taketoday.web.method.support.HandlerMethodReturnValueHandler;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.FlashMapManager;
import cn.taketoday.web.servlet.HandlerExceptionResolver;
import cn.taketoday.web.servlet.HandlerInterceptor;
import cn.taketoday.web.servlet.LocaleResolver;
import cn.taketoday.web.servlet.View;
import cn.taketoday.web.servlet.ViewResolver;
import cn.taketoday.web.servlet.config.annotation.AsyncSupportConfigurer;
import cn.taketoday.web.servlet.config.annotation.InterceptorRegistration;
import cn.taketoday.web.servlet.config.annotation.InterceptorRegistry;
import cn.taketoday.web.servlet.config.annotation.WebMvcConfigurationSupport;
import cn.taketoday.web.servlet.handler.AbstractHandlerMapping;
import cn.taketoday.web.servlet.handler.MappedInterceptor;
import cn.taketoday.web.servlet.i18n.AcceptHeaderLocaleResolver;
import cn.taketoday.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import cn.taketoday.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import cn.taketoday.web.servlet.resource.ResourceUrlProvider;
import cn.taketoday.web.servlet.support.SessionFlashMapManager;
import cn.taketoday.web.servlet.theme.FixedThemeResolver;
import cn.taketoday.web.servlet.view.DefaultRequestToViewNameTranslator;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.util.pattern.PathPatternParser;
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
 * @since 4.0
 */
public class StandaloneMockMvcBuilder extends AbstractMockMvcBuilder<StandaloneMockMvcBuilder> {

  private final List<Object> controllers;

  @Nullable
  private List<Object> controllerAdvice;

  private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

  private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<>();

  private List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<>();

  private final List<MappedInterceptor> mappedInterceptors = new ArrayList<>();

  @Nullable
  private Validator validator;

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  @Nullable
  private FormattingConversionService conversionService;

  @Nullable
  private List<HandlerExceptionResolver> handlerExceptionResolvers;

  @Nullable
  private Long asyncRequestTimeout;

  @Nullable
  private List<ViewResolver> viewResolvers;

  private LocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

  @Nullable
  private FlashMapManager flashMapManager;

  @Nullable
  private PathPatternParser patternParser;

  private boolean useSuffixPatternMatch = false;

  private boolean useTrailingSlashPatternMatch = true;

  @Nullable
  private Boolean removeSemicolonContent;

  private final Map<String, String> placeholderValues = new HashMap<>();

  private Supplier<RequestMappingHandlerMapping> handlerMappingFactory = RequestMappingHandlerMapping::new;

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
      instances.add(obj instanceof Class ? BeanUtils.newInstance((Class<?>) obj) : obj);
    }
    return instances;
  }

  /**
   * Register one or more {@link cn.taketoday.web.bind.annotation.ControllerAdvice}
   * instances to be used in tests (specified {@code Class} will be turned into instance).
   * <p>Normally {@code @ControllerAdvice} are auto-detected as long as they're declared
   * as Spring beans. However since the standalone setup does not load any Spring config,
   * they need to be registered explicitly here instead much like controllers.
   *
   * @since 4.0
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
   * Specify the timeout value for async execution. In Spring MVC Test, this
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
  public StandaloneMockMvcBuilder setCustomArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers) {
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
   * Set the HandlerExceptionResolver types to use as a list.
   */
  public StandaloneMockMvcBuilder setHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
    this.handlerExceptionResolvers = exceptionResolvers;
    return this;
  }

  /**
   * Set the HandlerExceptionResolver types to use as an array.
   */
  public StandaloneMockMvcBuilder setHandlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
    this.handlerExceptionResolvers = Arrays.asList(exceptionResolvers);
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
    this.viewResolvers = Collections.<ViewResolver>singletonList(new StaticViewResolver(view));
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
  public StandaloneMockMvcBuilder setFlashMapManager(FlashMapManager flashMapManager) {
    this.flashMapManager = flashMapManager;
    return this;
  }

  /**
   * Enable URL path matching with parsed
   * {@link cn.taketoday.web.util.pattern.PathPattern PathPatterns}
   * instead of String pattern matching with a {@link cn.taketoday.util.PathMatcher}.
   *
   * @param parser the parser to use
   * @since 4.0
   */
  public void setPatternParser(PathPatternParser parser) {
    this.patternParser = parser;
  }

  /**
   * Whether to use suffix pattern match (".*") when matching patterns to
   * requests. If enabled a method mapped to "/users" also matches to "/users.*".
   * <p>The default value is {@code false}.
   *
   * @deprecated as of 5.2.4. See class-level note in
   * {@link RequestMappingHandlerMapping} on the deprecation of path extension
   * config options.
   */
  @Deprecated
  public StandaloneMockMvcBuilder setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
    this.useSuffixPatternMatch = useSuffixPatternMatch;
    return this;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   * If enabled a method mapped to "/users" also matches to "/users/".
   * <p>The default value is {@code true}.
   */
  public StandaloneMockMvcBuilder setUseTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch) {
    this.useTrailingSlashPatternMatch = useTrailingSlashPatternMatch;
    return this;
  }

  /**
   * Set if ";" (semicolon) content should be stripped from the request URI. The value,
   * if provided, is in turn set on
   * {@link AbstractHandlerMapping#setRemoveSemicolonContent(boolean)}.
   */
  public StandaloneMockMvcBuilder setRemoveSemicolonContent(boolean removeSemicolonContent) {
    this.removeSemicolonContent = removeSemicolonContent;
    return this;
  }

  /**
   * In a standalone setup there is no support for placeholder values embedded in
   * request mappings. This method allows manually provided placeholder values so they
   * can be resolved. Alternatively consider creating a test that initializes a
   * {@link WebApplicationContext}.
   */
  public StandaloneMockMvcBuilder addPlaceholderValue(String name, String value) {
    this.placeholderValues.put(name, value);
    return this;
  }

  /**
   * Configure factory to create a custom {@link RequestMappingHandlerMapping}.
   *
   * @param factory the factory
   * @since 4.0
   */
  public StandaloneMockMvcBuilder setCustomHandlerMapping(Supplier<RequestMappingHandlerMapping> factory) {
    this.handlerMappingFactory = factory;
    return this;
  }

  @Override
  protected WebApplicationContext initWebAppContext() {
    MockServletContext servletContext = new MockServletContext();
    StubWebApplicationContext wac = new StubWebApplicationContext(servletContext);
    registerMvcSingletons(wac);
    servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
    return wac;
  }

  private void registerMvcSingletons(StubWebApplicationContext wac) {
    StandaloneConfiguration config = new StandaloneConfiguration();
    config.setApplicationContext(wac);
    ServletContext sc = wac.getServletContext();

    wac.addBeans(this.controllers);
    wac.addBeans(this.controllerAdvice);

    FormattingConversionService mvcConversionService = config.mvcConversionService();
    wac.addBean("mvcConversionService", mvcConversionService);
    ResourceUrlProvider resourceUrlProvider = config.mvcResourceUrlProvider();
    wac.addBean("mvcResourceUrlProvider", resourceUrlProvider);
    ContentNegotiationManager mvcContentNegotiationManager = config.mvcContentNegotiationManager();
    wac.addBean("mvcContentNegotiationManager", mvcContentNegotiationManager);
    Validator mvcValidator = config.mvcValidator();
    wac.addBean("mvcValidator", mvcValidator);

    RequestMappingHandlerMapping hm = config.getHandlerMapping(mvcConversionService, resourceUrlProvider);
    if (sc != null) {
      hm.setServletContext(sc);
    }
    hm.setApplicationContext(wac);
    hm.afterPropertiesSet();
    wac.addBean("requestMappingHandlerMapping", hm);

    RequestMappingHandlerAdapter ha = config.requestMappingHandlerAdapter(mvcContentNegotiationManager,
            mvcConversionService, mvcValidator);
    if (sc != null) {
      ha.setServletContext(sc);
    }
    ha.setApplicationContext(wac);
    ha.afterPropertiesSet();
    wac.addBean("requestMappingHandlerAdapter", ha);

    wac.addBean("handlerExceptionResolver", config.handlerExceptionResolver(mvcContentNegotiationManager));

    wac.addBeans(initViewResolvers(wac));
    wac.addBean(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, this.localeResolver);
    wac.addBean(DispatcherServlet.THEME_RESOLVER_BEAN_NAME, new FixedThemeResolver());
    wac.addBean(DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, new DefaultRequestToViewNameTranslator());

    this.flashMapManager = new SessionFlashMapManager();
    wac.addBean(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, this.flashMapManager);

    extendMvcSingletons(sc).forEach(wac::addBean);
  }

  private List<ViewResolver> initViewResolvers(WebApplicationContext wac) {
    this.viewResolvers = (this.viewResolvers != null ? this.viewResolvers :
                          Collections.singletonList(new InternalResourceViewResolver()));
    for (Object viewResolver : this.viewResolvers) {
      if (viewResolver instanceof WebApplicationObjectSupport) {
        ((WebApplicationObjectSupport) viewResolver).setApplicationContext(wac);
      }
    }
    return this.viewResolvers;
  }

  /**
   * This method could be used from a sub-class to register additional Spring
   * MVC infrastructure such as additional {@code HandlerMapping},
   * {@code HandlerAdapter}, and others.
   *
   * @param servletContext the ServletContext
   * @return a map with additional MVC infrastructure object instances
   * @since 4.0
   */
  protected Map<String, Object> extendMvcSingletons(@Nullable ServletContext servletContext) {
    return Collections.emptyMap();
  }

  /** Using the MVC Java configuration as the starting point for the "standalone" setup. */
  private class StandaloneConfiguration extends WebMvcConfigurationSupport {

    @SuppressWarnings("deprecation")
    public RequestMappingHandlerMapping getHandlerMapping(
            FormattingConversionService mvcConversionService,
            ResourceUrlProvider mvcResourceUrlProvider) {

      RequestMappingHandlerMapping handlerMapping = handlerMappingFactory.get();
      handlerMapping.setEmbeddedValueResolver(new StaticStringValueResolver(placeholderValues));
      if (patternParser != null) {
        handlerMapping.setPatternParser(patternParser);
      }
      else {
        handlerMapping.setUseSuffixPatternMatch(useSuffixPatternMatch);
        if (removeSemicolonContent != null) {
          handlerMapping.setRemoveSemicolonContent(removeSemicolonContent);
        }
      }
      handlerMapping.setUseTrailingSlashMatch(useTrailingSlashPatternMatch);
      handlerMapping.setOrder(0);
      handlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
      return handlerMapping;
    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.addAll(messageConverters);
    }

    @Override
    protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
      argumentResolvers.addAll(customArgumentResolvers);
    }

    @Override
    protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
      returnValueHandlers.addAll(customReturnValueHandlers);
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
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
      if (mvcValidator instanceof InitializingBean) {
        try {
          ((InitializingBean) mvcValidator).afterPropertiesSet();
        }
        catch (Exception ex) {
          throw new BeanInitializationException("Failed to initialize Validator", ex);
        }
      }
      return mvcValidator;
    }

    @Override
    protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
      if (handlerExceptionResolvers == null) {
        return;
      }
      for (HandlerExceptionResolver resolver : handlerExceptionResolvers) {
        if (resolver instanceof ApplicationContextAware) {
          ApplicationContext applicationContext = getApplicationContext();
          if (applicationContext != null) {
            ((ApplicationContextAware) resolver).setApplicationContext(applicationContext);
          }
        }
        if (resolver instanceof InitializingBean) {
          try {
            ((InitializingBean) resolver).afterPropertiesSet();
          }
          catch (Exception ex) {
            throw new IllegalStateException("Failure from afterPropertiesSet", ex);
          }
        }
        exceptionResolvers.add(resolver);
      }
    }
  }

  /**
   * A static resolver placeholder for values embedded in request mappings.
   */
  private static class StaticStringValueResolver implements StringValueResolver {

    private final PropertyPlaceholderHelper helper;

    private final PlaceholderResolver resolver;

    public StaticStringValueResolver(Map<String, String> values) {
      this.helper = new PropertyPlaceholderHelper("${", "}", ":", false);
      this.resolver = values::get;
    }

    @Override
    public String resolveStringValue(String strVal) throws BeansException {
      return this.helper.replacePlaceholders(strVal, this.resolver);
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

}
