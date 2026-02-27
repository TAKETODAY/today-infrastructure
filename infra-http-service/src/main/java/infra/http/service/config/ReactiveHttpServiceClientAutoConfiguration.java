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

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.ResourceLoader;
import infra.core.ssl.SslBundles;
import infra.http.client.HttpClientSettings;
import infra.http.reactive.client.ClientHttpConnectorBuilder;
import infra.http.reactive.client.config.ReactiveHttpClientAutoConfiguration;
import infra.http.service.registry.HttpServiceProxyRegistry;
import infra.http.service.support.WebClientAdapter;
import infra.stereotype.Component;
import infra.web.reactive.client.WebClient;
import infra.web.reactive.client.WebClientCustomizer;
import infra.web.reactive.client.config.WebClientAutoConfiguration;

/**
 * AutoConfiguration for Infra reactive HTTP Service Clients backed by {@link WebClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 5.0
 */
@DisableDIAutoConfiguration(after = { ReactiveHttpClientAutoConfiguration.class, WebClientAutoConfiguration.class })
@ConditionalOnClass(WebClientAdapter.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
public final class ReactiveHttpServiceClientAutoConfiguration {
  @Component
  static HttpServiceClientProperties httpServiceClientProperties(ConfigurableEnvironment environment) {
    return HttpServiceClientProperties.bind(environment);
  }

  @Component
  static PropertiesWebClientHttpServiceGroupConfigurer webClientPropertiesHttpServiceGroupConfigurer(
          ResourceLoader resourceLoader, HttpServiceClientProperties properties,
          ObjectProvider<SslBundles> sslBundles, ObjectProvider<ClientHttpConnectorBuilder<?>> clientConnectorBuilder,
          ObjectProvider<HttpClientSettings> httpClientSettings) {
    return new PropertiesWebClientHttpServiceGroupConfigurer(resourceLoader.getClassLoader(), properties,
            sslBundles.getIfAvailable(), clientConnectorBuilder, httpClientSettings.getIfAvailable());
  }

  @Component
  static WebClientCustomizerHttpServiceGroupConfigurer webClientCustomizerHttpServiceGroupConfigurer(
          ObjectProvider<WebClientCustomizer> customizers) {
    return new WebClientCustomizerHttpServiceGroupConfigurer(customizers);
  }

}
