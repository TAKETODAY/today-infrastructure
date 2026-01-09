/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

  void setHttpMessageConvertersCustomizers(@Nullable List<ClientHttpMessageConvertersCustomizer> httpMessageConvertersCustomizers) {
    this.httpMessageConvertersCustomizers = httpMessageConvertersCustomizers;
  }

  void setRestTemplateCustomizers(@Nullable List<RestTemplateCustomizer> restTemplateCustomizers) {
    this.restTemplateCustomizers = restTemplateCustomizers;
  }

  void setRestTemplateRequestCustomizers(@Nullable List<RestTemplateRequestCustomizer<?>> restTemplateRequestCustomizers) {
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
    if (ObjectUtils.isNotEmpty(customizers)) {
      return method.apply(builder, customizers);
    }
    return builder;
  }

}
