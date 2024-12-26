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

package infra.annotation.config.web.reactive.client;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

import infra.beans.factory.ObjectProvider;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslManagerBundle;
import infra.core.ssl.SslOptions;
import infra.http.client.ReactorResourceFactory;
import infra.http.client.reactive.ReactorClientHttpConnector;
import infra.lang.Nullable;
import infra.util.function.ThrowingConsumer;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

/**
 * {@link ClientHttpConnectorFactory} for {@link ReactorClientHttpConnector}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ReactorClientHttpConnectorFactory implements ClientHttpConnectorFactory<ReactorClientHttpConnector> {

  private final ReactorResourceFactory reactorResourceFactory;

  @Nullable
  private final ObjectProvider<ReactorNettyHttpClientMapper> mappersProvider;

  ReactorClientHttpConnectorFactory(ReactorResourceFactory reactorResourceFactory) {
    this(reactorResourceFactory, null);
  }

  ReactorClientHttpConnectorFactory(ReactorResourceFactory reactorResourceFactory,
          @Nullable ObjectProvider<ReactorNettyHttpClientMapper> mappersProvider) {
    this.reactorResourceFactory = reactorResourceFactory;
    this.mappersProvider = mappersProvider;
  }

  @Override
  public ReactorClientHttpConnector createClientHttpConnector(@Nullable SslBundle sslBundle) {
    List<ReactorNettyHttpClientMapper> mappers = new ArrayList<>();
    if (mappersProvider != null) {
      mappersProvider.addOrderedTo(mappers);
    }
    if (sslBundle != null) {
      mappers.add(new SslConfigurer(sslBundle));
    }
    return new ReactorClientHttpConnector(reactorResourceFactory,
            ReactorNettyHttpClientMapper.forCompose(mappers));
  }

  /**
   * Configures the Netty {@link HttpClient} with SSL.
   */
  private static class SslConfigurer implements ReactorNettyHttpClientMapper {

    private final SslBundle sslBundle;

    SslConfigurer(SslBundle sslBundle) {
      this.sslBundle = sslBundle;
    }

    @Override
    public HttpClient apply(HttpClient httpClient) {
      return httpClient.secure(ThrowingConsumer.of(this::customizeSsl).throwing(IllegalStateException::new));
    }

    private void customizeSsl(SslContextSpec spec) throws SSLException {
      SslOptions options = this.sslBundle.getOptions();
      SslManagerBundle managers = this.sslBundle.getManagers();
      SslContextBuilder builder = SslContextBuilder.forClient()
              .keyManager(managers.getKeyManagerFactory())
              .trustManager(managers.getTrustManagerFactory())
              .ciphers(SslOptions.asSet(options.getCiphers()))
              .protocols(options.getEnabledProtocols());
      spec.sslContext(builder.build());
    }

  }

}
