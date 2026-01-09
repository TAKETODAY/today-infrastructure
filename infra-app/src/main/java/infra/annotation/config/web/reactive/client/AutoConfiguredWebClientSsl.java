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

package infra.annotation.config.web.reactive.client;

import java.util.function.Consumer;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.http.client.reactive.ClientHttpConnector;
import infra.web.client.reactive.WebClient;

/**
 * An auto-configured {@link WebClientSsl} implementation.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AutoConfiguredWebClientSsl implements WebClientSsl {

  private final ClientHttpConnectorFactory<?> clientHttpConnectorFactory;

  private final SslBundles sslBundles;

  AutoConfiguredWebClientSsl(ClientHttpConnectorFactory<?> clientHttpConnectorFactory, SslBundles sslBundles) {
    this.clientHttpConnectorFactory = clientHttpConnectorFactory;
    this.sslBundles = sslBundles;
  }

  @Override
  public Consumer<WebClient.Builder> fromBundle(String bundleName) {
    return fromBundle(this.sslBundles.getBundle(bundleName));
  }

  @Override
  public Consumer<WebClient.Builder> fromBundle(SslBundle bundle) {
    return builder -> {
      ClientHttpConnector connector = clientHttpConnectorFactory.createClientHttpConnector(bundle);
      builder.clientConnector(connector);
    };
  }

}
