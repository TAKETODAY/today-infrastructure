/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.config;

import java.util.List;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.ReturnValueHandlerManager;

/**
 * A subclass of {@code WebMvcConfigurationSupport} that detects and delegates
 * to all beans of type {@link WebMvcConfigurer} allowing them to customize the
 * configuration provided by {@code WebMvcConfigurationSupport}. This is the
 * class actually imported by {@link EnableWebMvc @EnableWebMvc}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 15:33
 */
@DisableDependencyInjection
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport {

  private final CompositeWebMvcConfigurer configurers = new CompositeWebMvcConfigurer();

  public DelegatingWebMvcConfiguration(List<WebMvcConfigurer> configurers) {
    if (CollectionUtils.isNotEmpty(configurers)) {
      this.configurers.addWebMvcConfigurers(configurers);
    }
  }

  @Override
  protected void configurePathMatch(PathMatchConfigurer configurer) {
    this.configurers.configurePathMatch(configurer);
  }

  @Override
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    this.configurers.configureContentNegotiation(configurer);
  }

  @Override
  protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    this.configurers.configureAsyncSupport(configurer);
  }

  @Override
  protected void addFormatters(FormatterRegistry registry) {
    this.configurers.addFormatters(registry);
  }

  @Override
  protected void addInterceptors(InterceptorRegistry registry) {
    this.configurers.addInterceptors(registry);
  }

  @Override
  protected void addResourceHandlers(ResourceHandlerRegistry registry) {
    this.configurers.addResourceHandlers(registry);
  }

  @Override
  protected void addCorsMappings(CorsRegistry registry) {
    this.configurers.addCorsMappings(registry);
  }

  @Override
  protected void addViewControllers(ViewControllerRegistry registry) {
    this.configurers.addViewControllers(registry);
  }

  @Override
  protected void configureViewResolvers(ViewResolverRegistry registry) {
    this.configurers.configureViewResolvers(registry);
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
  protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    this.configurers.configureMessageConverters(converters);
  }

  @Override
  protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    this.configurers.extendMessageConverters(converters);
  }

  @Override
  protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    configurers.configureExceptionHandlers(handlers);
  }

  @Override
  protected void configureErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
    configurers.addErrorResponseInterceptors(interceptors);
  }

  @Override
  @Nullable
  protected Validator getValidator() {
    return configurers.getValidator();
  }

}
