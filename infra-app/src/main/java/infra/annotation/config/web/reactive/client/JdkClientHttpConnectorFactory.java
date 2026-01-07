/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;

import javax.net.ssl.SSLParameters;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslOptions;
import infra.http.client.reactive.JdkClientHttpConnector;

/**
 * {@link ClientHttpConnectorFactory} for {@link JdkClientHttpConnector}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JdkClientHttpConnectorFactory implements ClientHttpConnectorFactory<JdkClientHttpConnector> {

  @Override
  public JdkClientHttpConnector createClientHttpConnector(@Nullable SslBundle sslBundle) {
    Builder builder = HttpClient.newBuilder();
    if (sslBundle != null) {
      SslOptions options = sslBundle.getOptions();
      builder.sslContext(sslBundle.createSslContext());
      SSLParameters parameters = new SSLParameters();
      parameters.setCipherSuites(options.getCiphers());
      parameters.setProtocols(options.getEnabledProtocols());
      builder.sslParameters(parameters);
    }
    return new JdkClientHttpConnector(builder.build());
  }

}
