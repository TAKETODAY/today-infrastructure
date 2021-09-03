/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.beans.Configuration;
import cn.taketoday.beans.Lazy;
import cn.taketoday.beans.MissingBean;
import cn.taketoday.context.Props;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.handler.DefaultExceptionHandler;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.NotFoundRequestAdapter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.HandlerMethodRegistry;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.resolver.ParameterResolvers;
import cn.taketoday.web.view.ReturnValueHandler;
import cn.taketoday.web.view.ReturnValueHandlers;

/**
 * Web MVC auto configuration
 * <p>
 * config framework
 * </p>
 */
@Configuration
public class WebMvcAutoConfiguration {

  /**
   * default {@link MultipartConfiguration} bean
   */
  @Lazy
  @MissingBean
  @Props(prefix = "multipart.")
  MultipartConfiguration multipartConfiguration() {
    return new MultipartConfiguration();
  }

  /**
   * default {@link NotFoundRequestAdapter} to handle request-url not found
   */
  @MissingBean(type = NotFoundRequestAdapter.class)
  NotFoundRequestAdapter notFoundRequestAdapter() {
    return new NotFoundRequestAdapter();
  }

  /**
   * core {@link cn.taketoday.web.registry.HandlerRegistry} to register handler
   */
  @MissingBean
  HandlerMethodRegistry handlerMethodRegistry() {
    return new HandlerMethodRegistry();
  }

  /**
   * default {@link ParameterResolver} registry
   */
  @MissingBean
  ParameterResolvers parameterResolvers(WebApplicationContext context) {
    final ParameterResolvers parameterResolvers = new ParameterResolvers();
    parameterResolvers.setApplicationContext(context);
    // @since 3.0
    parameterResolvers.registerDefaultParameterResolvers();
    return parameterResolvers;
  }

  @MissingBean
  @ConditionalOnClass("com.fasterxml.jackson.databind.ObjectMapper")
  JacksonConfiguration jacksonConfiguration() {
    return new JacksonConfiguration();
  }

  /**
   * default {@link ReturnValueHandler} registry
   */
  @MissingBean
  ReturnValueHandlers resultHandlers(WebApplicationContext context) {
    ReturnValueHandlers resultHandlers = new ReturnValueHandlers();
    resultHandlers.initHandlers(context);
    resultHandlers.registerDefaultResultHandlers();
    return resultHandlers;
  }

  /**
   * default {@link HandlerExceptionHandler}
   */
  @MissingBean(type = HandlerExceptionHandler.class)
  DefaultExceptionHandler defaultExceptionHandler() {
    return new DefaultExceptionHandler();
  }

}
