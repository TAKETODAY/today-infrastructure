/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.annotation.config.web.reactive.client;

import java.util.function.Consumer;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundles;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.web.client.reactive.WebClient;

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
