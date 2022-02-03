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

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.FunctionHandlerRegistry;
import cn.taketoday.web.registry.HandlerRegistry;
import cn.taketoday.web.registry.ResourceHandlerRegistry;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.validation.WebValidator;
import cn.taketoday.web.handler.ReturnValueHandler;
import cn.taketoday.web.view.template.AbstractTemplateRenderer;

/**
 * @author TODAY <br>
 * 2019-05-17 17:46
 */
public class CompositeWebMvcConfiguration implements WebMvcConfiguration {

  private final List<WebMvcConfiguration> webMvcConfigurations;

  public CompositeWebMvcConfiguration(List<WebMvcConfiguration> webMvcConfigurations) {
    AnnotationAwareOrderComparator.sort(webMvcConfigurations);
    this.webMvcConfigurations = webMvcConfigurations;
  }

  @Override
  public void configureTemplateRenderer(AbstractTemplateRenderer renderer) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureTemplateRenderer(renderer);
    }
  }

  @Override
  public void configureResourceHandler(ResourceHandlerRegistry registry) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureResourceHandler(registry);
    }
  }

  @Override
  public void configureParameterResolving(List<ParameterResolvingStrategy> resolvingStrategies) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureParameterResolving(resolvingStrategies);
    }
  }

  @Override
  public void configureParameterResolving(
          ParameterResolvingRegistry resolversRegistry, List<ParameterResolvingStrategy> customizedStrategies) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureParameterResolving(resolversRegistry, customizedStrategies);
    }
  }

  @Override
  public void configureResultHandler(List<ReturnValueHandler> resultResolvers) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureResultHandler(resultResolvers);
    }
  }

  @Override
  public void configureMultipart(MultipartConfiguration config) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureMultipart(config);
    }
  }

  @Override
  public void configureInitializer(List<WebApplicationInitializer> initializers) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureInitializer(initializers);
    }
  }

  @Override
  public <T> void configureTemplateLoader(List<T> loaders) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureTemplateLoader(loaders);
    }
  }

  @Override
  public void configureViewController(ViewControllerHandlerRegistry viewControllerHandlerRegistry) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureViewController(viewControllerHandlerRegistry);
    }
  }

  @Override
  public void configureFunctionHandler(FunctionHandlerRegistry functionHandlerRegistry) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureFunctionHandler(functionHandlerRegistry);
    }
  }

  @Override
  public void configureHandlerAdapter(List<HandlerAdapter> adapters) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureHandlerAdapter(adapters);
    }
  }

  @Override
  public void configureHandlerRegistry(List<HandlerRegistry> handlerRegistries) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureHandlerRegistry(handlerRegistries);
    }
  }

  @Override
  public void configureExceptionHandlers(final List<HandlerExceptionHandler> handlers) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureExceptionHandlers(handlers);
    }
  }

  @Override
  public void configureValidators(WebValidator validator) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureValidators(validator);
    }
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    for (WebMvcConfiguration webMvcConfiguration : getWebMvcConfigurations()) {
      webMvcConfiguration.configureContentNegotiation(configurer);
    }
  }

  /**
   * Get all {@link WebMvcConfiguration} beans
   */
  public List<WebMvcConfiguration> getWebMvcConfigurations() {
    return webMvcConfigurations;
  }
}
