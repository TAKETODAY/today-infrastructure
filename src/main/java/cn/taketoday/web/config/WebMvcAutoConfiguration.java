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

import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.lang.Component;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.method.DefaultExceptionHandler;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.NotFoundRequestAdapter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.HandlerMethodRegistry;
import cn.taketoday.web.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlers;

/**
 * Web MVC auto configuration
 * <p>
 * config framework
 * </p>
 */
@Configuration(proxyBeanMethods = false)
@DisableAllDependencyInjection
public class WebMvcAutoConfiguration {

  /**
   * default {@link MultipartConfiguration} bean
   */
  @Lazy
  @Component
  @Props(prefix = "multipart.")
  @ConditionalOnMissingBean(MultipartConfiguration.class)
  MultipartConfiguration multipartConfiguration() {
    return new MultipartConfiguration();
  }

  /**
   * default {@link NotFoundRequestAdapter} to handle request-url not found
   */
  @Component
  @ConditionalOnMissingBean(NotFoundRequestAdapter.class)
  NotFoundRequestAdapter notFoundRequestAdapter() {
    return new NotFoundRequestAdapter();
  }

  /**
   * core {@link cn.taketoday.web.registry.HandlerRegistry} to register handler
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnMissingBean
  HandlerMethodRegistry handlerMethodRegistry() {
    return new HandlerMethodRegistry();
  }

  /**
   * default {@link ParameterResolvingStrategy} registry
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  ParameterResolvingRegistry parameterResolvers(WebApplicationContext context) {
    final ParameterResolvingRegistry resolversRegistry = new ParameterResolvingRegistry();
    resolversRegistry.setApplicationContext(context);
    // @since 3.0
    resolversRegistry.registerDefaultParameterResolvers();
    return resolversRegistry;
  }

  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnClass("com.fasterxml.jackson.databind.ObjectMapper")
  JacksonConfiguration jacksonConfiguration() {
    return new JacksonConfiguration();
  }

  /**
   * default {@link ReturnValueHandler} registry
   */
  @Component
  @ConditionalOnMissingBean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  ReturnValueHandlers resultHandlers(WebApplicationContext context) {
    ReturnValueHandlers resultHandlers = new ReturnValueHandlers();
    resultHandlers.setApplicationContext(context);
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

}
