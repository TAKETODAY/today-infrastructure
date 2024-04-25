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

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.apache.hc.core5.reactor.ssl.SSLSessionVerifier;

import javax.net.ssl.SSLContext;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.http.client.reactive.HttpComponentsClientHttpConnector;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpConnectorFactory} for {@link HttpComponentsClientHttpConnector}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class HttpComponentsClientHttpConnectorFactory implements ClientHttpConnectorFactory<HttpComponentsClientHttpConnector> {

  @Override
  public HttpComponentsClientHttpConnector createClientHttpConnector(@Nullable SslBundle sslBundle) {
    HttpAsyncClientBuilder builder = HttpAsyncClients.custom();
    if (sslBundle != null) {
      SslOptions options = sslBundle.getOptions();
      SSLContext sslContext = sslBundle.createSslContext();
      SSLSessionVerifier sessionVerifier = (endpoint, sslEngine) -> {
        if (options.getCiphers() != null) {
          sslEngine.setEnabledCipherSuites(options.getCiphers());
        }
        if (options.getEnabledProtocols() != null) {
          sslEngine.setEnabledProtocols(options.getEnabledProtocols());
        }
        return null;
      };
      BasicClientTlsStrategy tlsStrategy = new BasicClientTlsStrategy(sslContext, sessionVerifier);
      AsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
              .setTlsStrategy(tlsStrategy)
              .build();
      builder.setConnectionManager(connectionManager);
    }
    return new HttpComponentsClientHttpConnector(builder.build());
  }

}
