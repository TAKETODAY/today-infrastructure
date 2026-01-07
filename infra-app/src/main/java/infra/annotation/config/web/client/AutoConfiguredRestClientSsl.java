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

import java.util.function.Consumer;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundles;
import infra.http.client.ClientHttpRequestFactory;
import infra.web.client.RestClient;
import infra.http.client.config.ClientHttpRequestFactoryBuilder;
import infra.http.client.config.HttpClientSettings;

/**
 * An auto-configured {@link RestClientSsl} implementation.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AutoConfiguredRestClientSsl implements RestClientSsl {

  private final ClientHttpRequestFactoryBuilder<?> builder;

  private final HttpClientSettings settings;

  private final SslBundles sslBundles;

  AutoConfiguredRestClientSsl(ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder,
          HttpClientSettings settings, SslBundles sslBundles) {
    this.builder = clientHttpRequestFactoryBuilder;
    this.settings = settings;
    this.sslBundles = sslBundles;
  }

  @Override
  public Consumer<RestClient.Builder> fromBundle(String bundleName) {
    return fromBundle(sslBundles.getBundle(bundleName));
  }

  @Override
  public Consumer<RestClient.Builder> fromBundle(SslBundle bundle) {
    return builder -> builder.requestFactory(requestFactory(bundle));
  }

  private ClientHttpRequestFactory requestFactory(SslBundle bundle) {
    return builder.build(settings.withSslBundle(bundle));
  }

}
