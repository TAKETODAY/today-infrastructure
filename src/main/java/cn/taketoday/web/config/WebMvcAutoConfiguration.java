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

import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnWebApplication;
import cn.taketoday.core.Ordered;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.config.jackson.JacksonAutoConfiguration;
import cn.taketoday.web.i18n.AcceptHeaderLocaleResolver;
import cn.taketoday.web.i18n.FixedLocaleResolver;
import cn.taketoday.web.registry.FunctionHandlerRegistry;
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
@ConditionalOnWebApplication
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Import(JacksonAutoConfiguration.class)
public class WebMvcAutoConfiguration extends WebMvcConfigurationSupport {

  private final WebProperties webProperties;
  private final WebMvcProperties mvcProperties;

  private final CompositeWebMvcConfiguration mvcConfiguration = new CompositeWebMvcConfiguration();

  public WebMvcAutoConfiguration(
          @Props(prefix = "web.mvc.") WebMvcProperties mvcProperties,
          @Props(prefix = "web.") WebProperties webProperties) {
    this.mvcProperties = mvcProperties;
    this.webProperties = webProperties;
  }

  @Autowired(required = false)
  public void setMvcConfiguration(List<WebMvcConfiguration> mvcConfiguration) {
    if (!CollectionUtils.isEmpty(mvcConfiguration)) {
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
  protected void addResourceHandlers(ResourceHandlerRegistry registry) {
    mvcConfiguration.configureResourceHandler(registry);
  }

  @Override
  protected void configureFunctionHandler(FunctionHandlerRegistry functionHandlerRegistry) {
    mvcConfiguration.configureFunctionHandler(functionHandlerRegistry);
  }

  @Override
  protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    mvcConfiguration.configureMessageConverters(converters);
  }

  @Override
  protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    mvcConfiguration.extendMessageConverters(converters);
  }

  @Override
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
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

}
