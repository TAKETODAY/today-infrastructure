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

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.EnableDependencyInjection;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnWebApplication;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.expression.ExpressionProcessor;
import cn.taketoday.lang.Component;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.config.jackson.JacksonAutoConfiguration;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.NotFoundRequestAdapter;
import cn.taketoday.web.handler.method.DefaultExceptionHandler;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.HandlerMethodRegistry;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlers;
import cn.taketoday.web.view.template.DefaultTemplateRenderer;
import cn.taketoday.web.view.template.TemplateRenderer;

/**
 * Web MVC auto configuration
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

  /**
   * default {@link MultipartConfiguration} bean
   */
  @Lazy
  @Component
  @EnableDependencyInjection
  @Props(prefix = "multipart.")
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(MultipartConfiguration.class)
  MultipartConfiguration multipartConfiguration() {
    return new MultipartConfiguration();
  }

  /**
   * default {@link NotFoundRequestAdapter} to handle request-url not found
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(NotFoundRequestAdapter.class)
  NotFoundRequestAdapter notFoundRequestAdapter() {
    return new NotFoundRequestAdapter();
  }

  /**
   * core {@link cn.taketoday.web.registry.HandlerRegistry} to register handler
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  HandlerMethodRegistry handlerMethodRegistry() {
    return new HandlerMethodRegistry();
  }

  /**
   * default {@link ParameterResolvingStrategy} registry
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  ParameterResolvingRegistry parameterResolvingRegistry(
          WebApplicationContext context, MultipartConfiguration multipartConfig) {
    ParameterResolvingRegistry registry = new ParameterResolvingRegistry();
    registry.setApplicationContext(context);
    registry.setMultipartConfig(multipartConfig);
    registry.setMessageConverters(getMessageConverters());
    // @since 3.0
    registry.registerDefaultParameterResolvers();
    return registry;
  }

  /**
   * default {@link ReturnValueHandler} registry
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  ReturnValueHandlers returnValueHandlers(WebApplicationContext context) {
    ReturnValueHandlers resultHandlers = new ReturnValueHandlers();
    resultHandlers.setApplicationContext(context);
    resultHandlers.setMessageConverters(getMessageConverters());
    resultHandlers.initHandlers();
    resultHandlers.registerDefaultHandlers();
    return resultHandlers;
  }

  /**
   * default {@link HandlerExceptionHandler}
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(HandlerExceptionHandler.class)
  DefaultExceptionHandler defaultExceptionHandler() {
    return new DefaultExceptionHandler();
  }

  @Component
  @Props(prefix = "web.mvc.view.")
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean(TemplateRenderer.class)
  DefaultTemplateRenderer templateRenderer(ResourceLoader resourceLoader) {
    DefaultTemplateRenderer renderer = new DefaultTemplateRenderer(
            ExpressionProcessor.getSharedInstance().getManager());
    renderer.setResourceLoader(resourceLoader);
    return renderer;
  }
}
