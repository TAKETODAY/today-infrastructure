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
import infra.http.client.ClientHttpRequestFactoryBuilder;
import infra.http.client.HttpClientSettings;
import infra.http.client.config.ImperativeHttpClientAutoConfiguration;
import infra.http.service.registry.HttpServiceProxyRegistry;
import infra.http.service.support.RestClientAdapter;
import infra.stereotype.Component;
import infra.web.client.RestClient;
import infra.web.client.RestClientCustomizer;
import infra.web.client.config.RestClientAutoConfiguration;

/**
 * AutoConfiguration for Infra HTTP Service clients backed by {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@DisableDIAutoConfiguration(after = { ImperativeHttpClientAutoConfiguration.class, RestClientAutoConfiguration.class })
@ConditionalOnClass(RestClientAdapter.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
public final class HttpServiceClientAutoConfiguration {

  @Component
  static HttpServiceClientProperties httpServiceClientProperties(ConfigurableEnvironment environment) {
    return HttpServiceClientProperties.bind(environment);
  }

  @Component
  static PropertiesRestClientHttpServiceGroupConfigurer restClientPropertiesHttpServiceGroupConfigurer(
          ResourceLoader resourceLoader, HttpServiceClientProperties properties,
          ObjectProvider<SslBundles> sslBundles,
          ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder,
          ObjectProvider<HttpClientSettings> httpClientSettings) {
    return new PropertiesRestClientHttpServiceGroupConfigurer(resourceLoader.getClassLoader(), properties,
            sslBundles.getIfAvailable(), requestFactoryBuilder, httpClientSettings.getIfAvailable());
  }

  @Component
  static RestClientCustomizerHttpServiceGroupConfigurer restClientCustomizerHttpServiceGroupConfigurer(
          ObjectProvider<RestClientCustomizer> customizers) {
    return new RestClientCustomizerHttpServiceGroupConfigurer(customizers);
  }

}
