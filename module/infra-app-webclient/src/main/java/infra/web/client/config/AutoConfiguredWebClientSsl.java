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

package infra.web.client.config;

import java.util.function.Consumer;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.http.client.config.HttpClientSettings;
import infra.http.client.config.reactive.ClientHttpConnectorBuilder;
import infra.http.client.reactive.ClientHttpConnector;
import infra.web.client.reactive.WebClient;

/**
 * An auto-configured {@link WebClientSsl} implementation.
 *
 * @author Phillip Webb
 */
class AutoConfiguredWebClientSsl implements WebClientSsl {

  private final ClientHttpConnectorBuilder<?> connectorBuilder;

  private final HttpClientSettings settings;

  private final SslBundles sslBundles;

  AutoConfiguredWebClientSsl(ClientHttpConnectorBuilder<?> connectorBuilder,
          HttpClientSettings settings, SslBundles sslBundles) {
    this.connectorBuilder = connectorBuilder;
    this.settings = settings;
    this.sslBundles = sslBundles;
  }

  @Override
  public Consumer<WebClient.Builder> fromBundle(String bundleName) {
    return fromBundle(this.sslBundles.getBundle(bundleName));
  }

  @Override
  public Consumer<WebClient.Builder> fromBundle(SslBundle bundle) {
    return (builder) -> {
      HttpClientSettings settings = this.settings.withSslBundle(bundle);
      ClientHttpConnector connector = this.connectorBuilder.build(settings);
      builder.clientConnector(connector);
    };
  }

}
