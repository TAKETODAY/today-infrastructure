/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.web;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import cn.taketoday.annotation.config.task.TaskExecutionAutoConfiguration;
import cn.taketoday.annotation.config.validation.ValidationAutoConfiguration;
import cn.taketoday.annotation.config.web.WebMvcProperties.Format;
import cn.taketoday.annotation.config.web.WebProperties.Resources;
import cn.taketoday.annotation.config.web.WebProperties.Resources.Chain.Strategy;
import cn.taketoday.annotation.config.web.servlet.DispatcherServletAutoConfiguration;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.framework.annotation.ConditionalOnWebApplication;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.validation.DefaultMessageCodesResolver;
import cn.taketoday.validation.MessageCodesResolver;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.config.AsyncSupportConfigurer;
import cn.taketoday.web.config.CompositeWebMvcConfigurer;
import cn.taketoday.web.config.ContentNegotiationConfigurer;
import cn.taketoday.web.config.CorsRegistry;
import cn.taketoday.web.config.DefaultServletHandlerConfigurer;
import cn.taketoday.web.config.InterceptorRegistry;
import cn.taketoday.web.config.PathMatchConfigurer;
import cn.taketoday.web.config.ResourceChainRegistration;
import cn.taketoday.web.config.ResourceHandlerRegistration;
import cn.taketoday.web.config.ResourceHandlerRegistry;
import cn.taketoday.web.config.ViewControllerRegistry;
import cn.taketoday.web.config.ViewResolverRegistry;
import cn.taketoday.web.config.WebMvcConfigurationSupport;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.config.format.DateTimeFormatters;
import cn.taketoday.web.config.format.WebConversionService;
import cn.taketoday.web.context.support.RequestHandledEventPublisher;
import cn.taketoday.web.handler.AbstractHandlerExceptionHandler;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import cn.taketoday.web.handler.method.RequestMappingHandlerAdapter;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.i18n.AcceptHeaderLocaleResolver;
import cn.taketoday.web.i18n.FixedLocaleResolver;
import cn.taketoday.web.resource.EncodedResourceResolver;
import cn.taketoday.web.resource.ResourceResolver;
import cn.taketoday.web.resource.VersionResourceResolver;
import cn.taketoday.web.servlet.filter.FormContentFilter;
import cn.taketoday.web.servlet.filter.HiddenHttpMethodFilter;
import cn.taketoday.web.servlet.filter.OrderedFormContentFilter;
import cn.taketoday.web.servlet.filter.OrderedHiddenHttpMethodFilter;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.view.BeanNameViewResolver;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewResolver;
import cn.taketoday.web.view.ViewReturnValueHandler;

import static cn.taketoday.annotation.config.task.TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;

/**
 * Web MVC configuration
 * <p>
 * config framework
 * </p>
 */
@AutoConfiguration(after = {
        DispatcherServletAutoConfiguration.class,
        TaskExecutionAutoConfiguration.class,
        ValidationAutoConfiguration.class
})
@ConditionalOnWebApplication
@DisableDependencyInjection
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
public class WebMvcAutoConfiguration extends WebMvcConfigurationSupport {

  private final BeanFactory beanFactory;
  private final WebProperties webProperties;
  private final WebMvcProperties mvcProperties;

  private final WebMvcRegistrations mvcRegistrations;
  private final CompositeWebMvcConfigurer webMvcConfigurers;
  private final ObjectProvider<HttpMessageConverters> messageConvertersProvider;
  private final ObjectProvider<ResourceHandlerRegistrationCustomizer> registrationCustomizersProvider;

  public WebMvcAutoConfiguration(
          BeanFactory beanFactory,
          WebProperties webProperties,
          WebMvcProperties mvcProperties,
          List<WebMvcConfigurer> mvcConfigurers,
          ObjectProvider<WebMvcRegistrations> mvcRegistrations,
          ObjectProvider<ResourceHandlerRegistrationCustomizer> customizers,
          ObjectProvider<HttpMessageConverters> messageConvertersProvider) {
    this.beanFactory = beanFactory;
    this.mvcProperties = mvcProperties;
    this.webProperties = webProperties;
    this.mvcRegistrations = mvcRegistrations.getIfUnique();
    this.webMvcConfigurers = new CompositeWebMvcConfigurer(mvcConfigurers);
    this.messageConvertersProvider = messageConvertersProvider;
    this.registrationCustomizersProvider = customizers;
  }

  @Override
  protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
    if (this.mvcRegistrations != null) {
      RequestMappingHandlerAdapter adapter = this.mvcRegistrations.createRequestMappingHandlerAdapter();
      if (adapter != null) {
        return adapter;
      }
    }
    return super.createRequestMappingHandlerAdapter();
  }

  @Override
  protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
    if (mvcRegistrations != null) {
      RequestMappingHandlerMapping mapping = mvcRegistrations.createRequestMappingHandlerMapping();
      if (mapping != null) {
        return mapping;
      }
    }
    return super.createRequestMappingHandlerMapping();
  }

  @Override
  protected ExceptionHandlerAnnotationExceptionHandler createAnnotationExceptionHandler() {
    if (mvcRegistrations != null) {
      ExceptionHandlerAnnotationExceptionHandler handler = mvcRegistrations.createAnnotationExceptionHandler();
      if (handler != null) {
        return handler;
      }
    }
    return super.createAnnotationExceptionHandler();
  }

  @Component
  @Override
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public FormattingConversionService mvcConversionService() {
    Format format = mvcProperties.getFormat();
    WebConversionService conversionService = new WebConversionService(
            new DateTimeFormatters()
                    .dateFormat(format.getDate())
                    .timeFormat(format.getTime())
                    .dateTimeFormat(format.getDateTime())
    );
    addFormatters(conversionService);
    return conversionService;
  }

  @Nullable
  @Component
  @ConditionalOnMissingBean
  static RequestHandledEventPublisher requestHandledEventPublisher(
          WebMvcProperties webMvcProperties, ApplicationEventPublisher eventPublisher) {
    if (webMvcProperties.isPublishRequestHandledEvents()) {
      return new RequestHandledEventPublisher(eventPublisher);
    }
    return null;
  }

  @Component
  @ConditionalOnBean(View.class)
  @ConditionalOnMissingBean
  static BeanNameViewResolver beanNameViewResolver() {
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
    return resolver;
  }

  @Component
  @ConditionalOnMissingBean(name = LocaleResolver.BEAN_NAME)
  public LocaleResolver webLocaleResolver() {
    if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
      return new FixedLocaleResolver(this.webProperties.getLocale());
    }
    AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setDefaultLocale(this.webProperties.getLocale());
    return localeResolver;
  }

  @Override
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(ViewReturnValueHandler.class)
  public ViewReturnValueHandler viewReturnValueHandler(
          @Qualifier(LocaleResolver.BEAN_NAME) LocaleResolver localeResolver, List<ViewResolver> viewResolvers) {
    ViewReturnValueHandler handler = super.viewReturnValueHandler(localeResolver, viewResolvers);
    handler.setPutAllOutputRedirectModel(mvcProperties.getView().isPutAllOutputRedirectModel());
    return handler;
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    messageConvertersProvider.ifAvailable(
            customConverters -> converters.addAll(customConverters.getConverters()));
    webMvcConfigurers.configureMessageConverters(converters);
  }

  @Override
  protected void addFormatters(FormatterRegistry registry) {
    ApplicationConversionService.addBeans(registry, this.beanFactory);
    webMvcConfigurers.addFormatters(registry);
  }

  @Override
  protected void addInterceptors(InterceptorRegistry registry) {
    webMvcConfigurers.addInterceptors(registry);
  }

  @Override
  protected void addCorsMappings(CorsRegistry registry) {
    webMvcConfigurers.addCorsMappings(registry);
  }

  @Override
  protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    webMvcConfigurers.configureDefaultServletHandling(configurer);
  }

  @Override
  protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    webMvcConfigurers.extendMessageConverters(converters);
  }

  @Override
  protected void addViewControllers(ViewControllerRegistry registry) {
    webMvcConfigurers.addViewControllers(registry);
  }

  @Override
  protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    webMvcConfigurers.configureExceptionHandlers(handlers);
  }

  @Override
  protected void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    if (mvcProperties.isLogResolvedException()) {
      for (HandlerExceptionHandler handler : handlers) {
        if (handler instanceof AbstractHandlerExceptionHandler abstractHandler) {
          abstractHandler.setWarnLogCategory(handler.getClass().getName());
        }
      }
    }

    webMvcConfigurers.extendExceptionHandlers(handlers);
  }

  @Nullable
  @Override
  protected Validator getValidator() {
    return webMvcConfigurers.getValidator();
  }

  @Nullable
  @Override
  protected MessageCodesResolver getMessageCodesResolver() {
    if (mvcProperties.getMessageCodesResolverFormat() != null) {
      DefaultMessageCodesResolver resolver = new DefaultMessageCodesResolver();
      resolver.setMessageCodeFormatter(mvcProperties.getMessageCodesResolverFormat());
      return resolver;
    }
    return null;
  }

  @Override
  public Validator mvcValidator() {
    if (ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
      var validatorAdapter = ClassUtils.load(
              "cn.taketoday.annotation.config.validation.ValidatorAdapter", getClass().getClassLoader());
      if (validatorAdapter != null) {
        Method method = ReflectionUtils.getMethod(validatorAdapter, "get", ApplicationContext.class, Validator.class);
        return (Validator) ReflectionUtils.invokeMethod(method, null, getApplicationContext(), getValidator());
      }
    }

    return super.mvcValidator();
  }

  @Override
  protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    if (beanFactory.containsBean(APPLICATION_TASK_EXECUTOR_BEAN_NAME)) {
      Object taskExecutor = beanFactory.getBean(APPLICATION_TASK_EXECUTOR_BEAN_NAME);
      if (taskExecutor instanceof AsyncTaskExecutor asyncTaskExecutor) {
        configurer.setTaskExecutor(asyncTaskExecutor);
      }
    }
    Duration timeout = mvcProperties.getAsync().getRequestTimeout();
    if (timeout != null) {
      configurer.setDefaultTimeout(timeout.toMillis());
    }

    // user config can override default config 'applicationTaskExecutor' and 'timeout'
    webMvcConfigurers.configureAsyncSupport(configurer);
  }

  @Override
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    WebMvcProperties.Contentnegotiation contentnegotiation = mvcProperties.getContentnegotiation();
    configurer.favorParameter(contentnegotiation.isFavorParameter());
    if (contentnegotiation.getParameterName() != null) {
      configurer.parameterName(contentnegotiation.getParameterName());
    }
    Map<String, MediaType> mediaTypes = mvcProperties.getContentnegotiation().getMediaTypes();
    configurer.mediaTypes(mediaTypes);

    webMvcConfigurers.configureContentNegotiation(configurer);
  }

  @Override
  protected void configurePathMatch(PathMatchConfigurer configurer) {
    webMvcConfigurers.configurePathMatch(configurer);
  }

  @Override
  protected void configureViewResolvers(ViewResolverRegistry registry) {
    webMvcConfigurers.configureViewResolvers(registry);
  }

  @Override
  protected void modifyParameterResolvingRegistry(ParameterResolvingRegistry registry) {
    webMvcConfigurers.configureParameterResolving(registry, registry.getCustomizedStrategies());
  }

  @Override
  protected void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {
    webMvcConfigurers.modifyReturnValueHandlerManager(manager);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Resources resourceProperties = webProperties.getResources();
    if (!resourceProperties.isAddMappings()) {
      log.debug("Default resource handling disabled");
      return;
    }
    addResourceHandler(registry, mvcProperties.getWebjarsPathPattern(), "classpath:/META-INF/resources/webjars/");
    addResourceHandler(registry, mvcProperties.getStaticPathPattern(), resourceProperties.getStaticLocations());

    // User maybe override
    webMvcConfigurers.addResourceHandlers(registry);
  }

  private void addResourceHandler(ResourceHandlerRegistry registry, String pattern, String... locations) {
    if (registry.hasMappingForPattern(pattern)) {
      return;
    }
    Resources resourceProperties = webProperties.getResources();

    ResourceHandlerRegistration registration = registry.addResourceHandler(pattern);
    registration.addResourceLocations(locations);
    registration.setCachePeriod(getSeconds(resourceProperties.getCache().getPeriod()));
    registration.setCacheControl(resourceProperties.getCache().getHttpCacheControl());
    registration.setUseLastModified(resourceProperties.getCache().isUseLastModified());
    customizeResourceHandlerRegistration(registration);
  }

  private Integer getSeconds(Duration cachePeriod) {
    return cachePeriod != null ? (int) cachePeriod.getSeconds() : null;
  }

  private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
    registrationCustomizersProvider.ifAvailable(customizer -> customizer.customize(registration));
  }

  @Component
  @ConditionalOnEnabledResourceChain
  static ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(
          WebProperties webProperties) {
    return new ResourceHandlerRegistrationCustomizer(webProperties.getResources());
  }

  @ConditionalOnClass(name = ServletDetector.SERVLET_CLASS)
  @Configuration(proxyBeanMethods = false)
  static class WebServletConfig {

    @Component
    @ConditionalOnMissingBean
    static InternalResourceViewResolver internalResourceViewResolver(WebMvcProperties mvcProperties) {
      InternalResourceViewResolver resolver = new InternalResourceViewResolver();
      resolver.setPrefix(mvcProperties.getView().getPrefix());
      resolver.setSuffix(mvcProperties.getView().getSuffix());
      resolver.setOrder(Ordered.LOWEST_PRECEDENCE);
      return resolver;
    }

    @Component
    @ConditionalOnMissingBean(HiddenHttpMethodFilter.class)
    @ConditionalOnProperty(prefix = "web.mvc.hiddenmethod.filter", name = "enabled")
    static OrderedHiddenHttpMethodFilter hiddenHttpMethodFilter() {
      return new OrderedHiddenHttpMethodFilter();
    }

    @Component
    @ConditionalOnMissingBean(FormContentFilter.class)
    @ConditionalOnProperty(prefix = "web.mvc.formcontent.filter", name = "enabled", matchIfMissing = true)
    static OrderedFormContentFilter formContentFilter() {
      return new OrderedFormContentFilter();
    }
  }

  static class ResourceHandlerRegistrationCustomizer {

    private final Resources resources;

    ResourceHandlerRegistrationCustomizer(Resources resourceProperties) {
      this.resources = resourceProperties;
    }

    public void customize(ResourceHandlerRegistration registration) {
      Resources.Chain properties = resources.getChain();
      configureResourceChain(properties, registration.resourceChain(properties.isCache()));
    }

    private void configureResourceChain(Resources.Chain properties, ResourceChainRegistration chain) {
      Strategy strategy = properties.getStrategy();
      if (properties.isCompressed()) {
        chain.addResolver(new EncodedResourceResolver());
      }
      if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
        chain.addResolver(getVersionResourceResolver(strategy));
      }
    }

    private ResourceResolver getVersionResourceResolver(Strategy properties) {
      VersionResourceResolver resolver = new VersionResourceResolver();
      if (properties.getFixed().isEnabled()) {
        String version = properties.getFixed().getVersion();
        String[] paths = properties.getFixed().getPaths();
        resolver.addFixedVersionStrategy(version, paths);
      }
      if (properties.getContent().isEnabled()) {
        String[] paths = properties.getContent().getPaths();
        resolver.addContentVersionStrategy(paths);
      }
      return resolver;
    }

  }

}
