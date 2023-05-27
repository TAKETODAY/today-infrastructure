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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.net.InetSocketAddress;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.framework.web.server.Http2;
import cn.taketoday.framework.web.server.Ssl.ClientAuth;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link JettyServerCustomizer} that configures SSL on the given Jetty server instance.
 *
 * @author Brian Clozel
 * @author Olivier Lamy
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SslServerCustomizer implements JettyServerCustomizer {

  @Nullable
  private final Http2 http2;

  private final InetSocketAddress address;

  private final ClientAuth clientAuth;

  private final SslBundle sslBundle;

  SslServerCustomizer(@Nullable Http2 http2, InetSocketAddress address, ClientAuth clientAuth, SslBundle sslBundle) {
    this.address = address;
    this.clientAuth = clientAuth;
    this.sslBundle = sslBundle;
    this.http2 = http2;
  }

  @Override
  public void customize(Server server) {
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setEndpointIdentificationAlgorithm(null);
    configureSsl(sslContextFactory, this.clientAuth);
    ServerConnector connector = createConnector(server, sslContextFactory);
    server.setConnectors(new Connector[] { connector });
  }

  private ServerConnector createConnector(Server server, SslContextFactory.Server sslContextFactory) {
    HttpConfiguration config = new HttpConfiguration();
    config.setSendServerVersion(false);
    config.setSecureScheme("https");
    config.setSecurePort(this.address.getPort());
    config.addCustomizer(new SecureRequestCustomizer());
    ServerConnector connector = createServerConnector(server, sslContextFactory, config);
    connector.setPort(this.address.getPort());
    connector.setHost(this.address.getHostString());
    return connector;
  }

  private ServerConnector createServerConnector(Server server, SslContextFactory.Server sslContextFactory,
          HttpConfiguration config) {
    if (!Http2.isEnabled(http2)) {
      return createHttp11ServerConnector(config, sslContextFactory, server);
    }
    Assert.state(isJettyAlpnPresent(),
            "An 'org.eclipse.jetty:jetty-alpn-*-server' dependency is required for HTTP/2 support.");
    Assert.state(isJettyHttp2Present(),
            "The 'org.eclipse.jetty.http2:http2-server' dependency is required for HTTP/2 support.");
    return createHttp2ServerConnector(config, sslContextFactory, server);
  }

  private ServerConnector createHttp11ServerConnector(HttpConfiguration config,
          SslContextFactory.Server sslContextFactory, Server server) {
    SslConnectionFactory sslConnectionFactory = createSslConnectionFactory(
            sslContextFactory, HttpVersion.HTTP_1_1.asString());
    HttpConnectionFactory connectionFactory = new HttpConnectionFactory(config);
    return new SslValidatingServerConnector(this.sslBundle.getKey(), sslContextFactory, server,
            sslConnectionFactory, connectionFactory);
  }

  private SslConnectionFactory createSslConnectionFactory(SslContextFactory.Server sslContextFactory,
          String protocol) {
    try {
      return new SslConnectionFactory(sslContextFactory, protocol);
    }
    catch (NoSuchMethodError ex) {
      // Jetty 10
      try {
        return SslConnectionFactory.class.getConstructor(SslContextFactory.Server.class, String.class)
                .newInstance(sslContextFactory, protocol);
      }
      catch (Exception ex2) {
        throw new RuntimeException(ex2);
      }
    }
  }

  private boolean isJettyAlpnPresent() {
    return ClassUtils.isPresent("org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory", null);
  }

  private boolean isJettyHttp2Present() {
    return ClassUtils.isPresent("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory", null);
  }

  private ServerConnector createHttp2ServerConnector(HttpConfiguration config,
          SslContextFactory.Server sslContextFactory, Server server) {
    HttpConnectionFactory http = new HttpConnectionFactory(config);
    HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(config);
    ALPNServerConnectionFactory alpn = createAlpnServerConnectionFactory();
    sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
    if (isConscryptPresent()) {
      sslContextFactory.setProvider("Conscrypt");
    }
    SslConnectionFactory sslConnectionFactory = createSslConnectionFactory(sslContextFactory, alpn.getProtocol());
    return new SslValidatingServerConnector(this.sslBundle.getKey(), sslContextFactory, server,
            sslConnectionFactory, alpn, h2, http);
  }

  private ALPNServerConnectionFactory createAlpnServerConnectionFactory() {
    try {
      return new ALPNServerConnectionFactory();
    }
    catch (IllegalStateException ex) {
      throw new IllegalStateException(
              "An 'org.eclipse.jetty:jetty-alpn-*-server' dependency is required for HTTP/2 support.", ex);
    }
  }

  private boolean isConscryptPresent() {
    return ClassUtils.isPresent("org.conscrypt.Conscrypt", null)
            && ClassUtils.isPresent("org.eclipse.jetty.alpn.conscrypt.server.ConscryptServerALPNProcessor", null);
  }

  /**
   * Configure the SSL connection.
   *
   * @param factory the Jetty {@link Server SslContextFactory.Server}.
   * @param clientAuth the client authentication mode
   */
  protected void configureSsl(SslContextFactory.Server factory, ClientAuth clientAuth) {
    SslBundleKey key = this.sslBundle.getKey();
    SslOptions options = this.sslBundle.getOptions();
    SslStoreBundle stores = this.sslBundle.getStores();
    factory.setProtocol(this.sslBundle.getProtocol());
    configureSslClientAuth(factory, clientAuth);
    if (stores.getKeyStorePassword() != null) {
      factory.setKeyStorePassword(stores.getKeyStorePassword());
    }
    factory.setCertAlias(key.getAlias());
    if (options.getCiphers() != null) {
      factory.setIncludeCipherSuites(options.getCiphers());
      factory.setExcludeCipherSuites();
    }
    if (options.getEnabledProtocols() != null) {
      factory.setIncludeProtocols(options.getEnabledProtocols());
      factory.setExcludeProtocols();
    }
    try {
      if (key.getPassword() != null) {
        factory.setKeyManagerPassword(key.getPassword());
      }
      factory.setKeyStore(stores.getKeyStore());
      factory.setTrustStore(stores.getTrustStore());
    }
    catch (Exception ex) {
      throw new IllegalStateException("Unable to set SSL store: " + ex.getMessage(), ex);
    }
  }

  private void configureSslClientAuth(SslContextFactory.Server factory, ClientAuth clientAuth) {
    factory.setWantClientAuth(clientAuth == ClientAuth.WANT || clientAuth == ClientAuth.NEED);
    factory.setNeedClientAuth(clientAuth == ClientAuth.NEED);
  }

  /**
   * A {@link ServerConnector} that validates the ssl key alias on server startup.
   */
  static class SslValidatingServerConnector extends ServerConnector {

    private final SslBundleKey key;
    private final SslContextFactory sslContextFactory;

    SslValidatingServerConnector(SslBundleKey key, SslContextFactory sslContextFactory, Server server,
            SslConnectionFactory sslConnectionFactory, HttpConnectionFactory connectionFactory) {
      super(server, sslConnectionFactory, connectionFactory);
      this.key = key;
      this.sslContextFactory = sslContextFactory;
    }

    SslValidatingServerConnector(SslBundleKey keyAlias, SslContextFactory sslContextFactory, Server server,
            ConnectionFactory... factories) {
      super(server, factories);
      this.key = keyAlias;
      this.sslContextFactory = sslContextFactory;
    }

    @Override
    protected void doStart() throws Exception {
      super.doStart();
      this.key.assertContainsAlias(this.sslContextFactory.getKeyStore());
    }

  }

}
