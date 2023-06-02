/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.web.reactive.client;

import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.net.ssl.SSLException;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslManagerBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.http.client.reactive.ReactorClientHttpConnector;
import cn.taketoday.http.client.reactive.ReactorResourceFactory;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.ThrowingConsumer;
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

  private final Supplier<Stream<ReactorNettyHttpClientMapper>> mappers;

  ReactorClientHttpConnectorFactory(ReactorResourceFactory reactorResourceFactory) {
    this(reactorResourceFactory, Stream::empty);
  }

  ReactorClientHttpConnectorFactory(ReactorResourceFactory reactorResourceFactory,
          Supplier<Stream<ReactorNettyHttpClientMapper>> mappers) {
    this.reactorResourceFactory = reactorResourceFactory;
    this.mappers = mappers;
  }

  @Override
  public ReactorClientHttpConnector createClientHttpConnector(@Nullable SslBundle sslBundle) {
    ReactorNettyHttpClientMapper mapper = this.mappers.get()
            .reduce((before, after) -> (client) -> after.configure(before.configure(client)))
            .orElse(client -> client);
    if (sslBundle != null) {
      mapper = new SslConfigurer(sslBundle)::configure;
    }
    return new ReactorClientHttpConnector(this.reactorResourceFactory, mapper::configure);

  }

  /**
   * Configures the Netty {@link HttpClient} with SSL.
   */
  private static class SslConfigurer {

    private final SslBundle sslBundle;

    SslConfigurer(SslBundle sslBundle) {
      this.sslBundle = sslBundle;
    }

    HttpClient configure(HttpClient httpClient) {
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
