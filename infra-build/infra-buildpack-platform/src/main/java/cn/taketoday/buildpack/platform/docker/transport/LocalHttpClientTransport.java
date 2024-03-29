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

import com.sun.jna.Platform;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import cn.taketoday.buildpack.platform.docker.configuration.ResolvedDockerHost;
import cn.taketoday.buildpack.platform.socket.DomainSocket;
import cn.taketoday.buildpack.platform.socket.NamedPipeSocket;

/**
 * {@link HttpClientTransport} that talks to local Docker.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class LocalHttpClientTransport extends HttpClientTransport {

  private static final String DOCKER_SCHEME = "docker";

  private static final int DEFAULT_DOCKER_PORT = 2376;

  private static final HttpHost LOCAL_DOCKER_HOST = new HttpHost(DOCKER_SCHEME, "localhost", DEFAULT_DOCKER_PORT);

  private LocalHttpClientTransport(HttpClient client, HttpHost host) {
    super(client, host);
  }

  static LocalHttpClientTransport create(ResolvedDockerHost dockerHost) {
    HttpClientBuilder builder = HttpClients.custom();
    builder.setConnectionManager(new LocalConnectionManager(dockerHost.getAddress()));
    builder.setRoutePlanner(new LocalRoutePlanner());
    HttpHost host = new HttpHost(DOCKER_SCHEME, dockerHost.getAddress());
    return new LocalHttpClientTransport(builder.build(), host);
  }

  /**
   * {@link HttpClientConnectionManager} for local Docker.
   */
  private static class LocalConnectionManager extends BasicHttpClientConnectionManager {

    LocalConnectionManager(String host) {
      super(getRegistry(host), null, null, new LocalDnsResolver());
    }

    private static Registry<ConnectionSocketFactory> getRegistry(String host) {
      RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.create();
      builder.register(DOCKER_SCHEME, new LocalConnectionSocketFactory(host));
      return builder.build();
    }

  }

  /**
   * {@link DnsResolver} that ensures only the loopback address is used.
   */
  private static class LocalDnsResolver implements DnsResolver {

    private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
      return new InetAddress[] { LOOPBACK };
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
      return LOOPBACK.getCanonicalHostName();
    }

  }

  /**
   * {@link ConnectionSocketFactory} that connects to the local Docker domain socket or
   * named pipe.
   */
  private static class LocalConnectionSocketFactory implements ConnectionSocketFactory {

    private final String host;

    LocalConnectionSocketFactory(String host) {
      this.host = host;
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
      if (Platform.isWindows()) {
        return NamedPipeSocket.get(this.host);
      }
      return DomainSocket.get(this.host);
    }

    @Override
    public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host,
            InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
            throws IOException {
      return socket;
    }

  }

  /**
   * {@link HttpRoutePlanner} for local Docker.
   */
  private static class LocalRoutePlanner implements HttpRoutePlanner {

    @Override
    public HttpRoute determineRoute(HttpHost target, HttpContext context) {
      return new HttpRoute(LOCAL_DOCKER_HOST);
    }

  }

}
