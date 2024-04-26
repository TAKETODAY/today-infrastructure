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

package cn.taketoday.framework.web.reactive.server.netty;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.handler.ssl.ClientAuth;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.SslProvider.SslContextSpec;

/**
 * {@link ReactorNettyServerCustomizer} that configures SSL for the given Reactor Netty server
 * instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Chris Bono
 * @author Cyril Dangerville
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class SslServerCustomizer implements ReactorNettyServerCustomizer {

  private static final Logger logger = LoggerFactory.getLogger(SslServerCustomizer.class);

  private final boolean http2Enabled;

  private final ClientAuth clientAuth;

  private final Duration handshakeTimeout;

  private volatile SslProvider sslProvider;

  private final HashMap<String, SslProvider> serverNameSslProviders;

  public SslServerCustomizer(boolean http2Enabled, Ssl ssl,
          SslBundle sslBundle, Map<String, SslBundle> serverNameSslBundles) {
    this.http2Enabled = http2Enabled;
    this.handshakeTimeout = ssl.handshakeTimeout;
    this.clientAuth = Ssl.ClientAuth.map(ssl.clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
    this.sslProvider = createSslProvider(sslBundle);
    this.serverNameSslProviders = createServerNameSslProviders(serverNameSslBundles);
  }

  @Override
  public HttpServer apply(HttpServer server) {
    return server.secure(this::applySecurity);
  }

  private void applySecurity(SslContextSpec spec) {
    var builder = spec.sslContext(sslProvider.getSslContext())
            .handshakeTimeout(handshakeTimeout);

    if (!serverNameSslProviders.isEmpty()) {
      builder.setSniAsyncMappings((serverName, promise) ->
              promise.setSuccess(serverNameSslProviders.getOrDefault(serverName, sslProvider)));
    }
  }

  void updateSslBundle(@Nullable String serverName, SslBundle sslBundle) {
    logger.debug("SSL Bundle has been updated, reloading SSL configuration");
    if (serverName == null) {
      sslProvider = createSslProvider(sslBundle);
    }
    else {
      serverNameSslProviders.put(serverName, createSslProvider(sslBundle));
    }
  }

  private HashMap<String, SslProvider> createServerNameSslProviders(Map<String, SslBundle> serverNameSslBundles) {
    HashMap<String, SslProvider> serverNameSslProviders = new HashMap<>();
    for (Map.Entry<String, SslBundle> entry : serverNameSslBundles.entrySet()) {
      serverNameSslProviders.put(entry.getKey(), createSslProvider(entry.getValue()));
    }
    return serverNameSslProviders;
  }

  private SslProvider createSslProvider(SslBundle sslBundle) {
    return SslProvider.builder().sslContext(createSslContextSpec(sslBundle)).build();
  }

  /**
   * Create an {@link AbstractProtocolSslContextSpec} for a given {@link SslBundle}.
   *
   * @param sslBundle the {@link SslBundle} to use
   * @return an {@link AbstractProtocolSslContextSpec} instance
   */
  private AbstractProtocolSslContextSpec<?> createSslContextSpec(SslBundle sslBundle) {
    AbstractProtocolSslContextSpec<?> sslContextSpec = http2Enabled
            ? Http2SslContextSpec.forServer(sslBundle.getManagers().getKeyManagerFactory())
            : Http11SslContextSpec.forServer(sslBundle.getManagers().getKeyManagerFactory());
    return sslContextSpec.configure(builder -> {
      SslOptions options = sslBundle.getOptions();
      builder.trustManager(sslBundle.getManagers().getTrustManagerFactory());
      builder.protocols(options.getEnabledProtocols());
      builder.ciphers(SslOptions.asSet(options.getCiphers()));
      builder.clientAuth(this.clientAuth);
    });
  }

}
