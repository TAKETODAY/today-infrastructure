/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.annotation.config.web.client;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import infra.annotation.config.http.ClientHttpMessageConvertersCustomizer;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.util.ObjectUtils;
import infra.web.client.config.RestTemplateBuilder;
import infra.web.client.config.RestTemplateCustomizer;
import infra.web.client.config.RestTemplateRequestCustomizer;

/**
 * Configure {@link RestTemplateBuilder} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 16:14
 */
public final class RestTemplateBuilderConfigurer {

  private @Nullable ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

  private @Nullable HttpClientSettings clientSettings;

  private @Nullable List<ClientHttpMessageConvertersCustomizer> httpMessageConvertersCustomizers;

  private @Nullable List<RestTemplateCustomizer> restTemplateCustomizers;

  private @Nullable List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers;

  void setRequestFactoryBuilder(@Nullable ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder) {
    this.requestFactoryBuilder = requestFactoryBuilder;
  }

  void setClientSettings(@Nullable HttpClientSettings clientSettings) {
    this.clientSettings = clientSettings;
  }

  void setHttpMessageConvertersCustomizers(
          @Nullable List<ClientHttpMessageConvertersCustomizer> httpMessageConvertersCustomizers) {
    this.httpMessageConvertersCustomizers = httpMessageConvertersCustomizers;
  }

  void setRestTemplateCustomizers(@Nullable List<RestTemplateCustomizer> restTemplateCustomizers) {
    this.restTemplateCustomizers = restTemplateCustomizers;
  }

  void setRestTemplateRequestCustomizers(
          @Nullable List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
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
    if (this.requestFactoryBuilder != null) {
      builder = builder.requestFactoryBuilder(this.requestFactoryBuilder);
    }
    if (this.clientSettings != null) {
      builder = builder.clientSettings(this.clientSettings);
    }
    if (this.httpMessageConvertersCustomizers != null) {
      ClientBuilder clientBuilder = HttpMessageConverters.forClient();
      for (ClientHttpMessageConvertersCustomizer customizer : httpMessageConvertersCustomizers) {
        customizer.customize(clientBuilder);
      }
      builder = builder.messageConverters(clientBuilder.asList());
    }
    builder = addCustomizers(builder, this.restTemplateCustomizers, RestTemplateBuilder::customizers);
    builder = addCustomizers(builder, this.restTemplateRequestCustomizers, RestTemplateBuilder::requestCustomizers);
    return builder;
  }

  private <T> RestTemplateBuilder addCustomizers(RestTemplateBuilder builder, @Nullable List<T> customizers,
          BiFunction<RestTemplateBuilder, Collection<T>, RestTemplateBuilder> method) {
    if (!ObjectUtils.isEmpty(customizers)) {
      return method.apply(builder, customizers);
    }
    return builder;
  }

}
