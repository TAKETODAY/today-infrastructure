/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.annotation.config.web.reactive.client;

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.apache.hc.core5.reactor.ssl.SSLSessionVerifier;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLContext;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslOptions;
import infra.http.client.reactive.HttpComponentsClientHttpConnector;

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
