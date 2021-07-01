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

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.condition.ConditionalOnClass;
import cn.taketoday.web.handler.NotFoundRequestAdapter;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.registry.HandlerMethodRegistry;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.resolver.ParameterResolvers;
import cn.taketoday.web.view.ResultHandler;
import cn.taketoday.web.view.ResultHandlers;

/**
 *
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
  ParameterResolvers parameterResolvers() {
    return new ParameterResolvers();
  }

  @MissingBean
  @ConditionalOnClass("com.fasterxml.jackson.databind.ObjectMapper")
  JacksonConfiguration jacksonConfiguration() {
    return new JacksonConfiguration();
  }

  /**
   * default {@link ResultHandler} registry
   */
  @MissingBean
  ResultHandlers resultHandlers() {
    return new ResultHandlers();
  }

}
