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

package infra.web.service.config;

import org.jspecify.annotations.Nullable;

import infra.app.ssl.SslBundles;
import infra.beans.factory.ObjectProvider;
import infra.core.Ordered;
import infra.http.HttpHeaders;
import infra.http.client.HttpClientSettings;
import infra.http.client.config.HttpClientSettingsPropertyMapper;
import infra.http.client.reactive.ClientHttpConnectorBuilder;
import infra.util.StringUtils;
import infra.web.reactive.client.WebClient;
import infra.web.service.registry.HttpServiceGroup;
import infra.web.service.support.WebClientHttpServiceGroupConfigurer;

/**
 * A {@link WebClientHttpServiceGroupConfigurer} that configures the group and its
 * underlying {@link WebClient} using {@link HttpServiceClientProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class PropertiesWebClientHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

  private final HttpServiceClientProperties properties;

  private final HttpClientSettingsPropertyMapper clientSettingsPropertyMapper;

  private final ClientHttpConnectorBuilder<?> clientConnectorBuilder;

  PropertiesWebClientHttpServiceGroupConfigurer(@Nullable ClassLoader classLoader,
          HttpServiceClientProperties properties, @Nullable SslBundles sslBundles,
          ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
          @Nullable HttpClientSettings httpClientSettings) {
    this.properties = properties;
    this.clientSettingsPropertyMapper = new HttpClientSettingsPropertyMapper(sslBundles, httpClientSettings);
    this.clientConnectorBuilder = clientConnectorBuilder
            .getIfAvailable(() -> ClientHttpConnectorBuilder.detect(classLoader));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void configureGroups(Groups<WebClient.Builder> groups) {
    groups.forEachClient(this::configureClient);
  }

  private void configureClient(HttpServiceGroup group, WebClient.Builder builder) {
    HttpClientProperties clientProperties = this.properties.get(group.name());
    HttpClientSettings clientSettings = this.clientSettingsPropertyMapper.map(clientProperties);
    builder.clientConnector(this.clientConnectorBuilder.build(clientSettings));
    if (clientProperties != null) {
      if (clientProperties.baseUrl != null) {
        builder.baseURI(clientProperties.baseUrl);
      }
      if (!clientProperties.defaultHeader.isEmpty()) {
        builder.defaultHeaders(HttpHeaders.copyOf(clientProperties.defaultHeader));
      }

      if (StringUtils.hasText(clientProperties.apiVersion.defaultVersion)) {
        builder.defaultApiVersion(clientProperties.apiVersion.defaultVersion);
      }

      builder.apiVersionInserter(PropertiesApiVersionInserter.create(clientProperties.apiVersion.insert));
    }
  }

}
