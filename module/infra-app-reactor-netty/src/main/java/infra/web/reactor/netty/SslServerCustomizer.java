/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.reactor.netty;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslOptions;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.server.Ssl;
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
  private SslProvider.GenericSslContextSpec<?> createSslContextSpec(SslBundle sslBundle) {
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
