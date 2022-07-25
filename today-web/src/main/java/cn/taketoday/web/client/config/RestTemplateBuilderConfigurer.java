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

package cn.taketoday.web.client.config;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.util.CollectionUtils;

/**
 * Configure {@link RestTemplateBuilder} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 16:14
 */
public final class RestTemplateBuilderConfigurer {

  private HttpMessageConverters httpMessageConverters;

  private List<RestTemplateCustomizer> restTemplateCustomizers;

  private List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers;

  void setHttpMessageConverters(HttpMessageConverters httpMessageConverters) {
    this.httpMessageConverters = httpMessageConverters;
  }

  void setRestTemplateCustomizers(List<RestTemplateCustomizer> restTemplateCustomizers) {
    this.restTemplateCustomizers = restTemplateCustomizers;
  }

  void setRestTemplateRequestCustomizers(List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
    this.restTemplateRequestCustomizers = restTemplateRequestCustomizers;
  }

  /**
   * Configure the specified {@link RestTemplateBuilder}. The builder can be further
   * tuned and default settings can be overridden.
   *
   * @param builder the {@link RestTemplateBuilder} instance to configure
   * @return the configured builder
   */
  public RestTemplateBuilder configure(RestTemplateBuilder builder) {
    if (this.httpMessageConverters != null) {
      builder = builder.messageConverters(this.httpMessageConverters.getConverters());
    }
    builder = addCustomizers(builder, this.restTemplateCustomizers, RestTemplateBuilder::customizers);
    builder = addCustomizers(builder, this.restTemplateRequestCustomizers, RestTemplateBuilder::requestCustomizers);
    return builder;
  }

  private <T> RestTemplateBuilder addCustomizers(
          RestTemplateBuilder builder, List<T> customizers,
          BiFunction<RestTemplateBuilder, Collection<T>, RestTemplateBuilder> method) {
    if (CollectionUtils.isNotEmpty(customizers)) {
      return method.apply(builder, customizers);
    }
    return builder;
  }

}
