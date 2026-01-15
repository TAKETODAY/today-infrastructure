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

package infra.app.config.web.reactive.client;

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
