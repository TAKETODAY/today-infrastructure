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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.ErrorResponse;
import cn.taketoday.web.HandlerExceptionHandler;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategies;
import cn.taketoday.web.handler.ReturnValueHandlerManager;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-05-17 17:46
 */
public class CompositeWebMvcConfigurer implements WebMvcConfigurer {

  private final List<WebMvcConfigurer> webMvcConfigurers;

  public CompositeWebMvcConfigurer() {
    this(new ArrayList<>());
  }

  public CompositeWebMvcConfigurer(List<WebMvcConfigurer> webMvcConfigurers) {
    this.webMvcConfigurers = webMvcConfigurers;
  }

  public void addWebMvcConfigurers(List<WebMvcConfigurer> configurers) {
    if (CollectionUtils.isNotEmpty(configurers)) {
      webMvcConfigurers.addAll(configurers);
    }
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.addResourceHandlers(registry);
    }
  }

  @Override
  public void configureParameterResolving(ParameterResolvingStrategies resolvingStrategies) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureParameterResolving(resolvingStrategies);
    }
  }

  @Override
  public void configureParameterResolving(
          ParameterResolvingRegistry resolversRegistry, ParameterResolvingStrategies customizedStrategies) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureParameterResolving(resolversRegistry, customizedStrategies);
    }
  }

  @Override
  public void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.modifyReturnValueHandlerManager(manager);
    }
  }

  @Override
  public void configureHandlerRegistry(List<HandlerMapping> handlerRegistries) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureHandlerRegistry(handlerRegistries);
    }
  }

  @Override
  public void configureExceptionHandlers(final List<HandlerExceptionHandler> handlers) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureExceptionHandlers(handlers);
    }
  }

  @Override
  public void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.extendExceptionHandlers(handlers);
    }
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureContentNegotiation(configurer);
    }
  }

  @Override
  public void configureViewResolvers(ViewResolverRegistry registry) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureViewResolvers(registry);
    }
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configurePathMatch(configurer);
    }
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureMessageConverters(converters);
    }
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.extendMessageConverters(converters);
    }
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.addFormatters(registry);
    }
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.addCorsMappings(registry);
    }
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.addViewControllers(registry);
    }
  }

  @Override
  public void addErrorResponseInterceptors(List<ErrorResponse.Interceptor> interceptors) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.addErrorResponseInterceptors(interceptors);
    }
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.addInterceptors(registry);
    }
  }

  @Nullable
  @Override
  public Validator getValidator() {
    Validator selected = null;
    for (WebMvcConfigurer configurer : getWebMvcConfigurers()) {
      Validator validator = configurer.getValidator();
      if (validator != null) {
        if (selected != null) {
          throw new IllegalStateException(
                  "No unique Validator found: {%s, %s}".formatted(selected, validator));
        }
        selected = validator;
      }
    }
    return selected;
  }

  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    for (WebMvcConfigurer webMvcConfigurer : getWebMvcConfigurers()) {
      webMvcConfigurer.configureAsyncSupport(configurer);
    }
  }

  /**
   * Get all {@link WebMvcConfigurer} beans
   */
  public List<WebMvcConfigurer> getWebMvcConfigurers() {
    return webMvcConfigurers;
  }

}
