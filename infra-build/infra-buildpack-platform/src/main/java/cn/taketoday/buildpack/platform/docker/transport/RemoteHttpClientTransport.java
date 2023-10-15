/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.transport;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.socket.LayeredConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import cn.taketoday.buildpack.platform.docker.configuration.DockerHost;
import cn.taketoday.buildpack.platform.docker.configuration.ResolvedDockerHost;
import cn.taketoday.buildpack.platform.docker.ssl.SslContextFactory;
import cn.taketoday.lang.Assert;

/**
 * {@link HttpClientTransport} that talks to a remote Docker.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class RemoteHttpClientTransport extends HttpClientTransport {

  private static final Timeout SOCKET_TIMEOUT = Timeout.of(30, TimeUnit.MINUTES);

  private RemoteHttpClientTransport(HttpClient client, HttpHost host) {
    super(client, host);
  }

  static RemoteHttpClientTransport createIfPossible(ResolvedDockerHost dockerHost) {
    return createIfPossible(dockerHost, new SslContextFactory());
  }

  static RemoteHttpClientTransport createIfPossible(ResolvedDockerHost dockerHost,
          SslContextFactory sslContextFactory) {
    if (!dockerHost.isRemote()) {
      return null;
    }
    try {
      return create(dockerHost, sslContextFactory, HttpHost.create(dockerHost.getAddress()));
    }
    catch (URISyntaxException ex) {
      return null;
    }
  }

  private static RemoteHttpClientTransport create(DockerHost host, SslContextFactory sslContextFactory,
          HttpHost tcpHost) {
    SocketConfig socketConfig = SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(SOCKET_TIMEOUT).build();
    PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
            .create()
            .setDefaultSocketConfig(socketConfig);
    if (host.isSecure()) {
      connectionManagerBuilder.setSSLSocketFactory(getSecureConnectionSocketFactory(host, sslContextFactory));
    }
    HttpClientBuilder builder = HttpClients.custom();
    builder.setConnectionManager(connectionManagerBuilder.build());
    String scheme = host.isSecure() ? "https" : "http";
    HttpHost httpHost = new HttpHost(scheme, tcpHost.getHostName(), tcpHost.getPort());
    return new RemoteHttpClientTransport(builder.build(), httpHost);
  }

  private static LayeredConnectionSocketFactory getSecureConnectionSocketFactory(DockerHost host,
          SslContextFactory sslContextFactory) {
    String directory = host.getCertificatePath();
    Assert.hasText(directory,
            () -> "Docker host TLS verification requires trust material location to be specified with certificate path");
    SSLContext sslContext = sslContextFactory.forDirectory(directory);
    return new SSLConnectionSocketFactory(sslContext);
  }

}
