/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.config.annotation;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.format.FormatterRegistry;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.util.CollectionUtils;
import infra.validation.Validator;
import infra.web.ErrorResponse;
import infra.web.HandlerExceptionHandler;
import infra.web.HandlerMapping;
import infra.web.bind.resolver.ParameterResolvingRegistry;
import infra.web.bind.resolver.ParameterResolvingStrategies;
import infra.web.handler.ReturnValueHandlerManager;

/**
 * A {@link WebMvcConfigurer} that delegates to one or more others.
 *
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
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.addResourceHandlers(registry);
    }
  }

  @Override
  public void configureParameterResolving(ParameterResolvingStrategies resolvingStrategies) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureParameterResolving(resolvingStrategies);
    }
  }

  @Override
  public void configureParameterResolving(
          ParameterResolvingRegistry resolversRegistry, ParameterResolvingStrategies customizedStrategies) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureParameterResolving(resolversRegistry, customizedStrategies);
    }
  }

  @Override
  public void modifyReturnValueHandlerManager(ReturnValueHandlerManager manager) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.modifyReturnValueHandlerManager(manager);
    }
  }

  @Override
  public void configureHandlerRegistry(List<HandlerMapping> handlerRegistries) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureHandlerRegistry(handlerRegistries);
    }
  }

  @Override
  public void configureExceptionHandlers(final List<HandlerExceptionHandler> handlers) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureExceptionHandlers(handlers);
    }
  }

  @Override
  public void extendExceptionHandlers(List<HandlerExceptionHandler> handlers) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.extendExceptionHandlers(handlers);
    }
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureContentNegotiation(configurer);
    }
  }

  @Override
  public void configureViewResolvers(ViewResolverRegistry registry) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureViewResolvers(registry);
    }
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configurePathMatch(configurer);
    }
  }

  @Override
  public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureMessageConverters(builder);
    }
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureMessageConverters(converters);
    }
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.extendMessageConverters(converters);
    }
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.addFormatters(registry);
    }
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.addCorsMappings(registry);
    }
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.addViewControllers(registry);
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
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.addInterceptors(registry);
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
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureAsyncSupport(configurer);
    }
  }

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    for (WebMvcConfigurer delegate : getWebMvcConfigurers()) {
      delegate.configureApiVersioning(configurer);
    }
  }

  /**
   * Get all {@link WebMvcConfigurer} beans
   */
  public List<WebMvcConfigurer> getWebMvcConfigurers() {
    return webMvcConfigurers;
  }

}
