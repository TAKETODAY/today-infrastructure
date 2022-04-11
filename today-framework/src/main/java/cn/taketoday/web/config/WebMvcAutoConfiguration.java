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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.config.HttpMessageConverters;
import cn.taketoday.http.config.HttpMessageConvertersAutoConfiguration;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.config.WebProperties.Resources;
import cn.taketoday.web.config.WebProperties.Resources.Chain.Strategy;
import cn.taketoday.web.config.jackson.JacksonAutoConfiguration;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.i18n.AcceptHeaderLocaleResolver;
import cn.taketoday.web.i18n.FixedLocaleResolver;
import cn.taketoday.web.registry.FunctionHandlerRegistry;
import cn.taketoday.web.resource.EncodedResourceResolver;
import cn.taketoday.web.resource.ResourceResolver;
import cn.taketoday.web.resource.VersionResourceResolver;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.view.BeanNameViewResolver;
import cn.taketoday.web.view.ContentNegotiatingViewResolver;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.ViewResolver;

/**
 * Web MVC configuration
 * <p>
 * config framework
 * </p>
 */
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@EnableConfigurationProperties({ WebMvcProperties.class, WebProperties.class })
@Import({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class })
public class WebMvcAutoConfiguration extends WebMvcConfigurationSupport {

  private final BeanFactory beanFactory;
  private final WebProperties webProperties;
  private final WebMvcProperties mvcProperties;

  private final CompositeWebMvcConfiguration mvcConfiguration = new CompositeWebMvcConfiguration();

  @Nullable
  private final ResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer;

  private final ObjectProvider<HttpMessageConverters> messageConvertersProvider;

  public WebMvcAutoConfiguration(
          BeanFactory beanFactory,
          WebProperties webProperties,
          WebMvcProperties mvcProperties,
          ObjectProvider<ResourceHandlerRegistrationCustomizer> customizers,
          ObjectProvider<HttpMessageConverters> messageConvertersProvider) {
    this.beanFactory = beanFactory;
    this.mvcProperties = mvcProperties;
    this.webProperties = webProperties;
    this.messageConvertersProvider = messageConvertersProvider;
    this.resourceHandlerRegistrationCustomizer = customizers.getIfAvailable();
  }

  @Autowired(required = false)
  public void setMvcConfiguration(List<WebMvcConfiguration> mvcConfiguration) {
    if (CollectionUtils.isNotEmpty(mvcConfiguration)) {
      this.mvcConfiguration.addWebMvcConfiguration(mvcConfiguration);
    }
  }

  @Component
  @ConditionalOnMissingBean
  public InternalResourceViewResolver defaultViewResolver() {
    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
    resolver.setPrefix(this.mvcProperties.getView().getPrefix());
    resolver.setSuffix(this.mvcProperties.getView().getSuffix());
    return resolver;
  }

  @Component
  @ConditionalOnBean(View.class)
  @ConditionalOnMissingBean
  public BeanNameViewResolver beanNameViewResolver() {
    BeanNameViewResolver resolver = new BeanNameViewResolver();
    resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
    return resolver;
  }

  @Component
  @ConditionalOnBean(ViewResolver.class)
  @ConditionalOnMissingBean(name = "viewResolver", value = ContentNegotiatingViewResolver.class)
  public ContentNegotiatingViewResolver viewResolver(@Nullable ContentNegotiationManager contentNegotiationManager) {
    ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
    resolver.setContentNegotiationManager(contentNegotiationManager);
    // ContentNegotiatingViewResolver uses all the other view resolvers to locate
    // a view so it should have a high precedence
    resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return resolver;
  }

  @Bean
  @ConditionalOnMissingBean(name = LocaleResolver.BEAN_NAME)
  public LocaleResolver localeResolver() {
    if (this.webProperties.getLocaleResolver() == WebProperties.LocaleResolver.FIXED) {
      return new FixedLocaleResolver(this.webProperties.getLocale());
    }
    AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setDefaultLocale(this.webProperties.getLocale());
    return localeResolver;
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    messageConvertersProvider.ifAvailable(
            customConverters -> converters.addAll(customConverters.getConverters()));
    mvcConfiguration.configureMessageConverters(converters);
  }

  @Override
  protected void addFormatters(FormatterRegistry registry) {
    ApplicationConversionService.addBeans(registry, this.beanFactory);
    mvcConfiguration.addFormatters(registry);
  }

  @Override
  protected void addInterceptors(InterceptorRegistry registry) {
    mvcConfiguration.addInterceptors(registry);
  }

  @Override
  protected void addCorsMappings(CorsRegistry registry) {
    mvcConfiguration.addCorsMappings(registry);
  }

  @Override
  protected void configureFunctionHandler(FunctionHandlerRegistry functionHandlerRegistry) {
    mvcConfiguration.configureFunctionHandler(functionHandlerRegistry);
  }

  @Override
  protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    mvcConfiguration.configureDefaultServletHandling(configurer);
  }

  @Override
  protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    mvcConfiguration.extendMessageConverters(converters);
  }

  @Override
  protected void addViewControllers(ViewControllerRegistry registry) {
    mvcConfiguration.addViewControllers(registry);
  }

  @Override
  protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    mvcConfiguration.configureExceptionHandlers(handlers);
  }

  @Override
  protected void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    mvcConfiguration.extendExceptionHandlers(handlers);
  }

  @Nullable
  @Override
  protected Validator getValidator() {
    return mvcConfiguration.getValidator();
  }

  @Override
  protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    mvcConfiguration.configureAsyncSupport(configurer);
  }

  @Override
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    WebMvcProperties.Contentnegotiation contentnegotiation = mvcProperties.getContentnegotiation();
    configurer.favorParameter(contentnegotiation.isFavorParameter());
    if (contentnegotiation.getParameterName() != null) {
      configurer.parameterName(contentnegotiation.getParameterName());
    }
    Map<String, MediaType> mediaTypes = mvcProperties.getContentnegotiation().getMediaTypes();
    mediaTypes.forEach(configurer::mediaType);

    mvcConfiguration.configureContentNegotiation(configurer);
  }

  @Override
  protected void configurePathMatch(PathMatchConfigurer configurer) {
    mvcConfiguration.configurePathMatch(configurer);
  }

  @Override
  protected void configureViewResolvers(ViewResolverRegistry registry) {
    mvcConfiguration.configureViewResolvers(registry);
  }

  @Override
  protected void modifyParameterResolvingRegistry(ParameterResolvingRegistry registry) {
    mvcConfiguration.configureParameterResolving(registry, registry.getCustomizedStrategies());
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Resources resourceProperties = webProperties.getResources();
    if (!resourceProperties.isAddMappings()) {
      log.debug("Default resource handling disabled");
      return;
    }
    addResourceHandler(registry, "/webjars/**", "classpath:/META-INF/resources/webjars/");
    addResourceHandler(registry, this.mvcProperties.getStaticPathPattern(), (registration) -> {
      registration.addResourceLocations(resourceProperties.getStaticLocations());
    });

    mvcConfiguration.addResourceHandlers(registry);
  }

  private void addResourceHandler(ResourceHandlerRegistry registry, String pattern, String... locations) {
    addResourceHandler(registry, pattern, (registration) -> registration.addResourceLocations(locations));
  }

  private void addResourceHandler(
          ResourceHandlerRegistry registry, String pattern,
          Consumer<ResourceHandlerRegistration> customizer) {
    if (registry.hasMappingForPattern(pattern)) {
      return;
    }
    Resources resourceProperties = webProperties.getResources();

    ResourceHandlerRegistration registration = registry.addResourceHandler(pattern);
    customizer.accept(registration);
    registration.setCachePeriod(getSeconds(resourceProperties.getCache().getPeriod()));
    registration.setCacheControl(resourceProperties.getCache().getCachecontrol().toHttpCacheControl());
    registration.setUseLastModified(resourceProperties.getCache().isUseLastModified());
    customizeResourceHandlerRegistration(registration);
  }

  private Integer getSeconds(Duration cachePeriod) {
    return (cachePeriod != null) ? (int) cachePeriod.getSeconds() : null;
  }

  private void customizeResourceHandlerRegistration(ResourceHandlerRegistration registration) {
    if (resourceHandlerRegistrationCustomizer != null) {
      resourceHandlerRegistrationCustomizer.customize(registration);
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnEnabledResourceChain
  static class ResourceChainCustomizerConfiguration {

    @Bean
    ResourceChainResourceHandlerRegistrationCustomizer resourceHandlerRegistrationCustomizer(WebProperties webProperties) {
      return new ResourceChainResourceHandlerRegistrationCustomizer(webProperties.getResources());
    }

  }

  interface ResourceHandlerRegistrationCustomizer {

    void customize(ResourceHandlerRegistration registration);

  }

  static class ResourceChainResourceHandlerRegistrationCustomizer implements ResourceHandlerRegistrationCustomizer {

    private final Resources resources;

    ResourceChainResourceHandlerRegistrationCustomizer(Resources resourceProperties) {
      this.resources = resourceProperties;
    }

    @Override
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
