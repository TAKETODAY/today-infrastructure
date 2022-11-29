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

package cn.taketoday.web.config;

import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.handler.ReturnValueHandlerManager;
import cn.taketoday.web.registry.ViewControllerHandlerMapping;

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
@Configuration(proxyBeanMethods = false)
public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport {

  private final CompositeWebMvcConfigurer configurers = new CompositeWebMvcConfigurer();

  @Autowired(required = false)
  public void setConfigurers(List<WebMvcConfigurer> configurers) {
    if (!CollectionUtils.isEmpty(configurers)) {
      this.configurers.addWebMvcConfiguration(configurers);
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
  protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    this.configurers.configureDefaultServletHandling(configurer);
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
  protected void configureViewController(ViewControllerHandlerMapping registry) {
    configurers.configureViewController(registry);
  }

  @Override
  @Nullable
  protected Validator getValidator() {
    return this.configurers.getValidator();
  }

  @Override
  protected void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    configurers.configureExceptionHandlers(handlers);
  }
}
