/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.webmvc.config;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import infra.annotation.ConditionalOnWebApplication;
import infra.annotation.config.task.TaskExecutionAutoConfiguration;
import infra.annotation.config.web.ConditionalOnEnabledResourceChain;
import infra.annotation.config.web.WebProperties;
import infra.annotation.config.web.WebProperties.Resources;
import infra.annotation.config.web.WebProperties.Resources.Chain.Strategy;
import infra.annotation.config.web.WebResourcesRuntimeHints;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.context.ApplicationContext;
import infra.context.ApplicationEventPublisher;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.annotation.Lazy;
import infra.context.annotation.Role;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.Ordered;
import infra.core.task.AsyncTaskExecutor;
import infra.format.FormatterRegistry;
import infra.format.support.ApplicationConversionService;
import infra.format.support.FormattingConversionService;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.config.ServerHttpMessageConvertersCustomizer;
import infra.stereotype.Component;
import infra.util.ClassUtils;
import infra.util.LambdaSafe;
import infra.util.ReflectionUtils;
import infra.validation.DefaultMessageCodesResolver;
import infra.validation.MessageCodesResolver;
import infra.validation.Validator;
import infra.web.DispatcherHandler;
import infra.web.HandlerExceptionHandler;
import infra.web.LocaleResolver;
import infra.web.RequestContextUtils;
import infra.web.accept.ApiVersionDeprecationHandler;
import infra.web.accept.ApiVersionParser;
import infra.web.accept.ApiVersionResolver;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.config.annotation.ApiVersionConfigurer;
import infra.web.config.annotation.AsyncSupportConfigurer;
import infra.web.config.annotation.CompositeWebMvcConfigurer;
import infra.web.config.annotation.ContentNegotiationConfigurer;
import infra.web.config.annotation.CorsRegistry;
import infra.web.config.annotation.InterceptorRegistry;
import infra.web.config.annotation.PathMatchConfigurer;
import infra.web.config.annotation.ResourceChainRegistration;
import infra.web.config.annotation.ResourceHandlerRegistration;
import infra.web.config.annotation.ResourceHandlerRegistry;
import infra.web.config.annotation.ViewControllerRegistry;
import infra.web.config.annotation.ViewResolverRegistry;
import infra.web.config.annotation.WebMvcConfigurationSupport;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.config.format.DateTimeFormatters;
import infra.web.config.format.WebConversionService;
import infra.web.context.support.RequestHandledEventPublisher;
import infra.web.handler.AbstractHandlerExceptionHandler;
import infra.web.handler.ReturnValueHandlerManager;
import infra.web.handler.method.ExceptionHandlerAnnotationExceptionHandler;
import infra.web.handler.method.RequestMappingHandlerAdapter;
import infra.web.handler.method.RequestMappingHandlerMapping;
import infra.web.i18n.AcceptHeaderLocaleResolver;
import infra.web.i18n.FixedLocaleResolver;
import infra.web.resource.EncodedResourceResolver;
import infra.web.resource.ResourceResolver;
import infra.web.resource.VersionResourceResolver;
import infra.web.view.BeanNameViewResolver;
import infra.web.view.View;
import infra.webmvc.DispatcherHandlerCustomizer;
import infra.webmvc.config.WebMvcProperties.ApiVersion.Use;
import infra.webmvc.config.WebMvcProperties.Format;

import static infra.annotation.ConditionalOnWebApplication.Type.MVC;
import static infra.annotation.config.task.TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebMvcConfigurationSupport}.
 * <p>
 * Provides various auto-configured {@link WebMvcConfigurer} beans that enhance
 * the default MVC setup. This includes configuration for resource handling,
 * view resolution, message conversion, exception handling, and other MVC features.
 * <p>
 * The configuration is applied only when a web application context is detected
 * and can be customized via the {@link WebMvcProperties} and {@link WebProperties}
 * configuration properties.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@DisableDIAutoConfiguration(after = TaskExecutionAutoConfiguration.class,
        afterName = "infra.validation.config.ValidationAutoConfiguration")
@ConditionalOnWebApplication(type = MVC)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@ImportRuntimeHints(WebResourcesRuntimeHints.class)
@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
public class WebMvcAutoConfiguration extends WebMvcConfigurationSupport {

  private final BeanFactory beanFactory;

  private final WebProperties webProperties;

  private final WebMvcProperties mvcProperties;

  private final CompositeWebMvcConfigurer configurers;

  private final @Nullable WebMvcRegistrations mvcRegistrations;

  private final @Nullable ResourceHandlerRegistrationCustomizer registrationCustomizer;

  private final @Nullable ApiVersionParser<?> apiVersionParser;

  private final @Nullable ApiVersionDeprecationHandler apiVersionDeprecationHandler;

  private final ObjectProvider<ApiVersionResolver> apiVersionResolvers;

  private final ObjectProvider<ServerHttpMessageConvertersCustomizer> httpMessageConvertersCustomizerProvider;

  public WebMvcAutoConfiguration(BeanFactory beanFactory, WebProperties webProperties,
          WebMvcProperties mvcProperties, List<WebMvcConfigurer> mvcConfigurers,
          ObjectProvider<ApiVersionResolver> apiVersionResolvers, @Nullable ApiVersionParser<?> apiVersionParser,
          @Nullable ApiVersionDeprecationHandler apiVersionDeprecationHandler, ObjectProvider<WebMvcRegistrations> mvcRegistrations,
          @Nullable ResourceHandlerRegistrationCustomizer registrationCustomizer,
          ObjectProvider<ServerHttpMessageConvertersCustomizer> httpMessageConvertersCustomizerProvider) {
    this.beanFactory = beanFactory;
    this.mvcProperties = mvcProperties;
    this.webProperties = webProperties;
    this.mvcRegistrations = mvcRegistrations.getIfUnique();
    this.apiVersionParser = apiVersionParser;
    this.apiVersionResolvers = apiVersionResolvers;
    this.registrationCustomizer = registrationCustomizer;
    this.apiVersionDeprecationHandler = apiVersionDeprecationHandler;
    this.httpMessageConvertersCustomizerProvider = httpMessageConvertersCustomizerProvider;
    this.configurers = new CompositeWebMvcConfigurer(mvcConfigurers);
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
    Format format = mvcProperties.format;
    WebConversionService conversionService = new WebConversionService(
            new DateTimeFormatters()
                    .dateFormat(format.date)
                    .timeFormat(format.time)
                    .dateTimeFormat(format.dateTime)
    );
    addFormatters(conversionService);
    return conversionService;
  }

  @Component
  @ConditionalOnMissingBean
  static @Nullable RequestHandledEventPublisher requestHandledEventPublisher(
          WebMvcProperties webMvcProperties, ApplicationEventPublisher eventPublisher) {
    if (webMvcProperties.publishRequestHandledEvents) {
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

  @Override
  @Component
  @ConditionalOnMissingBean(name = LocaleResolver.BEAN_NAME)
  public LocaleResolver webLocaleResolver() {
    if (this.webProperties.localeResolver == WebProperties.LocaleResolver.FIXED) {
      return new FixedLocaleResolver(this.webProperties.locale);
    }
    AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setDefaultLocale(this.webProperties.locale);
    return localeResolver;
  }

  @SuppressWarnings("unchecked")
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static DispatcherHandler dispatcherHandler(ApplicationContext context, List<DispatcherHandlerCustomizer<?>> customizers, WebMvcProperties properties) {
    DispatcherHandler handler = new DispatcherHandler(context);
    handler.setThrowExceptionIfNoHandlerFound(properties.throwExceptionIfNoHandlerFound);
    handler.setEnableLoggingRequestDetails(properties.logRequestDetails);
    LambdaSafe.callbacks(DispatcherHandlerCustomizer.class, customizers, handler)
            .withLogger(WebMvcAutoConfiguration.class)
            .invoke(customizer -> customizer.customize(handler));
    return handler;
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static BeanFactoryPostProcessor webScopeConfigurer() {
    return RequestContextUtils::registerScopes;
  }

  @Override
  public void configureMessageConverters(ServerBuilder builder) {
    for (ServerHttpMessageConvertersCustomizer customizer : httpMessageConvertersCustomizerProvider) {
      customizer.customize(builder);
    }
    configurers.configureMessageConverters(builder);
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    configurers.configureMessageConverters(converters);
  }

  @Override
  protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    configurers.extendMessageConverters(converters);
  }

  @Override
  protected void addFormatters(FormatterRegistry registry) {
    ApplicationConversionService.addBeans(registry, this.beanFactory);
    configurers.addFormatters(registry);
  }

  @Override
  protected void addInterceptors(InterceptorRegistry registry) {
    configurers.addInterceptors(registry);
  }

  @Override
  protected void addCorsMappings(CorsRegistry registry) {
    configurers.addCorsMappings(registry);
  }

  @Override
  protected void addViewControllers(ViewControllerRegistry registry) {
    if (mvcProperties.registerWebViewXml) {
      registry.registerWebViewXml();
    }
    configurers.addViewControllers(registry);
  }

  @Override
  protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    configurers.configureExceptionHandlers(handlers);
  }

  @Override
  protected void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    if (mvcProperties.logResolvedException) {
      for (HandlerExceptionHandler handler : handlers) {
        if (handler instanceof AbstractHandlerExceptionHandler abstractHandler) {
          abstractHandler.setWarnLogCategory(handler.getClass().getName());
        }
      }
    }

    configurers.extendExceptionHandlers(handlers);
  }

  @Override
  protected @Nullable Validator getValidator() {
    return configurers.getValidator();
  }

  @Override
  protected @Nullable MessageCodesResolver getMessageCodesResolver() {
    if (mvcProperties.messageCodesResolverFormat != null) {
      DefaultMessageCodesResolver resolver = new DefaultMessageCodesResolver();
      resolver.setMessageCodeFormatter(mvcProperties.messageCodesResolverFormat);
      return resolver;
    }
    return null;
  }

  @Override
  @SuppressWarnings("NullAway")
  public Validator mvcValidator() {
    if (ClassUtils.isPresent("jakarta.validation.Validator", getClass().getClassLoader())) {
      var validatorAdapter = ClassUtils.load(
              "infra.annotation.config.validation.ValidatorAdapter", getClass().getClassLoader());
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
    Duration timeout = mvcProperties.async.requestTimeout;
    if (timeout != null) {
      configurer.setDefaultTimeout(timeout.toMillis());
    }

    // user config can override default config 'applicationTaskExecutor' and 'timeout'
    configurers.configureAsyncSupport(configurer);
  }

  @Override
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    WebMvcProperties.Contentnegotiation contentnegotiation = mvcProperties.contentnegotiation;
    configurer.favorParameter(contentnegotiation.favorParameter);
    if (contentnegotiation.parameterName != null) {
      configurer.parameterName(contentnegotiation.parameterName);
    }
    configurer.mediaTypes(mvcProperties.contentnegotiation.mediaTypes);

    configurers.configureContentNegotiation(configurer);
  }

  @Override
  protected void configurePathMatch(PathMatchConfigurer configurer) {
    configurers.configurePathMatch(configurer);
  }

  @Override
  protected void configureViewResolvers(ViewResolverRegistry registry) {
    configurers.configureViewResolvers(registry);
  }

  @Override
  protected void modifyParameterResolvingRegistry(ParameterResolvingRegistry registry) {
    configurers.configureParameterResolving(registry, registry.getCustomizedStrategies());
  }

  @Override
  protected void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {
    configurers.modifyReturnValueHandlerManager(manager);
  }

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    var properties = mvcProperties.apiVersion;
    if (properties.required != null) {
      configurer.setVersionRequired(properties.required);
    }
    if (properties.defaultVersion != null) {
      configurer.setDefaultVersion(properties.defaultVersion);
    }

    if (properties.supported != null) {
      for (String v : properties.supported) {
        configurer.addSupportedVersions(v);
      }
    }

    if (properties.detectSupported != null) {
      configurer.detectSupportedVersions(properties.detectSupported);
    }

    configureApiVersioningUse(configurer, properties.use);
    for (ApiVersionResolver resolver : apiVersionResolvers) {
      configurer.useVersionResolver(resolver);
    }

    if (apiVersionParser != null) {
      configurer.setVersionParser(apiVersionParser);
    }

    if (apiVersionDeprecationHandler != null) {
      configurer.setDeprecationHandler(apiVersionDeprecationHandler);
    }
    configurers.configureApiVersioning(configurer);
  }

  private void configureApiVersioningUse(ApiVersionConfigurer configurer, Use use) {
    if (use.header != null) {
      configurer.useRequestHeader(use.header);
    }
    if (use.requestParameter != null) {
      configurer.useRequestParam(use.requestParameter);
    }
    if (use.pathSegment != null) {
      configurer.usePathSegment(use.pathSegment);
    }
    for (var entry : use.mediaTypeParameter.entrySet()) {
      configurer.useMediaTypeParameter(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Resources resources = webProperties.resources;
    if (resources.addDefaultMappings) {
      addResourceHandler(registry, resources.webjarsPathPattern, "classpath:/META-INF/resources/webjars/");
      addResourceHandler(registry, resources.staticPathPattern, resources.staticLocations);
    }
    else {
      logger.debug("Default resource handling disabled");
    }
    // User maybe override
    configurers.addResourceHandlers(registry);
  }

  private void addResourceHandler(ResourceHandlerRegistry registry, String pattern, String... locations) {
    if (registry.hasMappingForPattern(pattern)) {
      return;
    }
    Resources resourceProperties = webProperties.resources;

    ResourceHandlerRegistration registration = registry.addResourceHandler(pattern);
    registration.addResourceLocations(locations);
    registration.setCachePeriod(getSeconds(resourceProperties.cache.period));
    registration.setCacheControl(resourceProperties.cache.getHttpCacheControl());
    registration.setUseLastModified(resourceProperties.cache.useLastModified);
    customizeResourceHandlerRegistration(registration);
  }

  private @Nullable Integer getSeconds(@Nullable Duration cachePeriod) {
    return cachePeriod != null ? (int) cachePeriod.getSeconds() : null;
  }

  private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
    if (registrationCustomizer != null) {
      registrationCustomizer.customize(registration);
    }
  }

  @Lazy
  @Component
  @ConditionalOnEnabledResourceChain
  static ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(
          WebProperties webProperties) {
    return new ResourceHandlerRegistrationCustomizer(webProperties.resources);
  }

  static class ResourceHandlerRegistrationCustomizer {

    private final Resources resources;

    ResourceHandlerRegistrationCustomizer(Resources resourceProperties) {
      this.resources = resourceProperties;
    }

    public void customize(ResourceHandlerRegistration registration) {
      Resources.Chain properties = resources.chain;
      configureResourceChain(properties, registration.resourceChain(properties.cache));
    }

    private void configureResourceChain(Resources.Chain properties, ResourceChainRegistration chain) {
      Strategy strategy = properties.strategy;
      if (properties.compressed) {
        chain.addResolver(new EncodedResourceResolver());
      }
      if (strategy.fixed.enabled || strategy.content.enabled) {
        chain.addResolver(getVersionResourceResolver(strategy));
      }
    }

    @SuppressWarnings("NullAway")
    private ResourceResolver getVersionResourceResolver(Strategy properties) {
      VersionResourceResolver resolver = new VersionResourceResolver();
      if (properties.fixed.enabled) {
        String version = properties.fixed.version;
        String[] paths = properties.fixed.paths;
        resolver.addFixedVersionStrategy(version, paths);
      }
      if (properties.content.enabled) {
        resolver.addContentVersionStrategy(properties.content.paths);
      }
      return resolver;
    }

  }

}
