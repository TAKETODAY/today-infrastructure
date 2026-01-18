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

package infra.http.service.config;

import org.jspecify.annotations.Nullable;

import infra.app.ssl.SslBundles;
import infra.beans.factory.ObjectProvider;
import infra.core.Ordered;
import infra.http.HttpHeaders;
import infra.http.client.ClientHttpRequestFactoryBuilder;
import infra.http.client.HttpClientSettings;
import infra.http.client.config.HttpClientSettingsProperties;
import infra.http.client.config.HttpClientSettingsPropertyMapper;
import infra.http.service.registry.HttpServiceGroup;
import infra.http.service.support.RestClientHttpServiceGroupConfigurer;
import infra.util.StringUtils;
import infra.web.client.RestClient;

/**
 * A {@link RestClientHttpServiceGroupConfigurer} that configures the group and its
 * underlying {@link RestClient} using {@link HttpClientSettingsProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class PropertiesRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

  /**
   * The default order for the PropertiesRestClientHttpServiceGroupConfigurer.
   */
  private static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

  private final HttpServiceClientProperties properties;

  private final HttpClientSettingsPropertyMapper clientSettingsPropertyMapper;

  private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

  PropertiesRestClientHttpServiceGroupConfigurer(@Nullable ClassLoader classLoader,
          HttpServiceClientProperties properties, @Nullable SslBundles sslBundles,
          ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder,
          @Nullable HttpClientSettings httpClientSettings) {
    this.properties = properties;
    this.clientSettingsPropertyMapper = new HttpClientSettingsPropertyMapper(sslBundles, httpClientSettings);
    this.requestFactoryBuilder = requestFactoryBuilder
            .getIfAvailable(() -> ClientHttpRequestFactoryBuilder.detect(classLoader));
  }

  @Override
  public int getOrder() {
    return DEFAULT_ORDER;
  }

  @Override
  public void configureGroups(Groups<RestClient.Builder> groups) {
    groups.forEachClient(this::configureClient);
  }

  private void configureClient(HttpServiceGroup group, RestClient.Builder builder) {
    HttpClientProperties clientProperties = this.properties.get(group.name());
    HttpClientSettings clientSettings = this.clientSettingsPropertyMapper.map(clientProperties);
    builder.requestFactory(this.requestFactoryBuilder.build(clientSettings));

    if (clientProperties != null) {
      if (clientProperties.baseUri != null) {
        builder.baseURI(clientProperties.baseUri);
      }
      if (!clientProperties.defaultHeader.isEmpty()) {
        builder.defaultHeaders(HttpHeaders.copyOf(clientProperties.defaultHeader));
      }

      if (StringUtils.hasText(clientProperties.apiVersion.defaultVersion)) {
        builder.defaultApiVersion(clientProperties.apiVersion.defaultVersion);
      }

      builder.apiVersionInserter(clientProperties.apiVersion.createApiVersionInserter());
    }

  }

}
