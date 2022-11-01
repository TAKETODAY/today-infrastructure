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

package cn.taketoday.annotation.config.web.client;

import java.util.List;

import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.client.config.RestTemplateBuilder;
import cn.taketoday.web.client.config.RestTemplateCustomizer;
import cn.taketoday.web.client.config.RestTemplateRequestCustomizer;

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
    if (httpMessageConverters != null) {
      builder = builder.messageConverters(httpMessageConverters.getConverters());
    }

    if (CollectionUtils.isNotEmpty(restTemplateCustomizers)) {
      builder = builder.customizers(restTemplateCustomizers);
    }

    if (CollectionUtils.isNotEmpty(restTemplateRequestCustomizers)) {
      builder = builder.requestCustomizers(restTemplateRequestCustomizers);
    }

    return builder;
  }

}
