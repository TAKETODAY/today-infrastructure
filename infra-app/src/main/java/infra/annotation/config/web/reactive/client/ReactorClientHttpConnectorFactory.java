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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

import infra.beans.factory.ObjectProvider;
import infra.core.ssl.SslBundle;
import infra.core.ssl.SslManagerBundle;
import infra.core.ssl.SslOptions;
import infra.http.client.ReactorResourceFactory;
import infra.http.client.reactive.ReactorClientHttpConnector;
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
