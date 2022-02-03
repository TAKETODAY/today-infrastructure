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

import cn.taketoday.core.io.Resource;
import cn.taketoday.web.annotation.Multipart;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.ViewController;
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
import cn.taketoday.web.view.template.TemplateRenderer;

/**
 * @author TODAY 2019-05-17 17:46
 */
public interface WebMvcConfiguration {

  /**
   * Configure {@link ParameterResolvingStrategy}
   *
   * @param customizedStrategies {@link ParameterResolvingStrategy} registry
   */
  default void configureParameterResolving(List<ParameterResolvingStrategy> customizedStrategies) { }

  /**
   * Configure {@link ParameterResolvingStrategy}
   * <p>
   * user can add {@link ParameterResolvingStrategy} to {@code resolvingStrategies} or
   * use {@link ParameterResolvingRegistry#getCustomizedStrategies()} or
   * use {@link ParameterResolvingRegistry#getDefaultStrategies()}
   * </p>
   *
   * @param customizedStrategies {@link ParameterResolvingStrategy} registry
   * @see WebApplicationLoader#configureParameterResolving(List, WebMvcConfiguration)
   * @see ParameterResolvingRegistry#getCustomizedStrategies()
   * @see ParameterResolvingRegistry#getDefaultStrategies()
   * @since 4.0
   */
  default void configureParameterResolving(
          ParameterResolvingRegistry registry, List<ParameterResolvingStrategy> customizedStrategies) {
    configureParameterResolving(customizedStrategies);
  }

  /**
   * Configure {@link ReturnValueHandler}
   *
   * @param returnValueHandlers {@link ReturnValueHandler} registry
   */
  default void configureResultHandler(List<ReturnValueHandler> returnValueHandlers) { }

  /**
   * Configure {@link TemplateRenderer}
   *
   * @param renderer {@link TemplateRenderer} instance
   */
  default void configureTemplateRenderer(AbstractTemplateRenderer renderer) { }

  /**
   * Configure static {@link Resource}
   *
   * @param registry {@link ResourceHandlerRegistry}
   */
  default void configureResourceHandler(ResourceHandlerRegistry registry) { }

  /**
   * Configure {@link Multipart}
   *
   * @param config {@link MultipartConfiguration}
   */
  default void configureMultipart(MultipartConfiguration config) { }

  /**
   * Configure WebApplicationInitializer
   *
   * @param initializers WebApplicationInitializer register
   */
  default void configureInitializer(List<WebApplicationInitializer> initializers) { }

  /**
   * Configure Freemarker's {@link freemarker.cache.TemplateLoader} s
   *
   * @param loaders {@link freemarker.cache.TemplateLoader}
   * beans in application context
   * @since 2.3.7
   */
  default <T> void configureTemplateLoader(List<T> loaders) { }

  /**
   * Configure {@link ViewController} s
   *
   * @param registry {@link ViewControllerHandlerRegistry}
   * @since 2.3.7
   */
  default void configureViewController(ViewControllerHandlerRegistry registry) { }

  /**
   * Configure Function Handler
   *
   * @param registry {@link FunctionHandlerRegistry}
   * @since 2.3.7
   */
  default void configureFunctionHandler(FunctionHandlerRegistry registry) { }

  /**
   * Configure {@link HandlerAdapter}
   *
   * @param adapters {@link HandlerAdapter}s
   * @since 2.3.7
   */
  default void configureHandlerAdapter(List<HandlerAdapter> adapters) { }

  /**
   * Configure {@link HandlerRegistry}
   *
   * @param handlerRegistries {@link HandlerRegistry}s
   * @since 2.3.7
   */
  default void configureHandlerRegistry(List<HandlerRegistry> handlerRegistries) { }

  /**
   * Configure {@link HandlerExceptionHandler}
   *
   * @param handlers HandlerExceptionHandlers
   * @since 3.0
   */
  default void configureExceptionHandlers(List<HandlerExceptionHandler> handlers) { }

  /**
   * Configure {@link WebValidator}
   *
   * @param validator list of validators
   * @since 3.0
   */
  default void configureValidators(WebValidator validator) { }

  /**
   * Configure content negotiation options.
   *
   * @since 4.0
   */
  default void configureContentNegotiation(ContentNegotiationConfigurer configurer) { }
}
