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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.http.client.reactive.JettyClientHttpConnector;
import cn.taketoday.http.client.reactive.JettyResourceFactory;
import cn.taketoday.lang.Nullable;

/**
 * {@link ClientHttpConnectorFactory} for {@link JettyClientHttpConnector}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JettyClientHttpConnectorFactory implements ClientHttpConnectorFactory<JettyClientHttpConnector> {

  private final JettyResourceFactory jettyResourceFactory;

  JettyClientHttpConnectorFactory(JettyResourceFactory jettyResourceFactory) {
    this.jettyResourceFactory = jettyResourceFactory;
  }

  @Override
  public JettyClientHttpConnector createClientHttpConnector(@Nullable SslBundle sslBundle) {
    SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    if (sslBundle != null) {
      SslOptions options = sslBundle.getOptions();
      if (options.getCiphers() != null) {
        sslContextFactory.setIncludeCipherSuites(options.getCiphers());
        sslContextFactory.setExcludeCipherSuites();
      }
      if (options.getEnabledProtocols() != null) {
        sslContextFactory.setIncludeProtocols(options.getEnabledProtocols());
        sslContextFactory.setExcludeProtocols();
      }
      sslContextFactory.setSslContext(sslBundle.createSslContext());
    }
    ClientConnector connector = new ClientConnector();
    connector.setSslContextFactory(sslContextFactory);
    HttpClientTransportOverHTTP transport = new HttpClientTransportOverHTTP(connector);
    HttpClient httpClient = new HttpClient(transport);
    return new JettyClientHttpConnector(httpClient, this.jettyResourceFactory);
  }

}
