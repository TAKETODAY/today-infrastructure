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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.annotation.config.web.netty.NettySSLBuilder;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.lang.Nullable;
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
public class SslServerCustomizer implements ReactorNettyServerCustomizer {

  private static final Log logger = LogFactory.getLog(SslServerCustomizer.class);

  private final boolean isHttp2Enabled;

  private final ClientAuth clientAuth;

  private volatile SslProvider sslProvider;

  private final HashMap<String, SslProvider> serverNameSslProviders;

  public SslServerCustomizer(boolean isHttp2Enabled, @Nullable Ssl.ClientAuth clientAuth,
          SslBundle sslBundle, Map<String, SslBundle> serverNameSslBundles) {
    this.isHttp2Enabled = isHttp2Enabled;
    this.clientAuth = Ssl.ClientAuth.map(clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
    this.sslProvider = createSslProvider(sslBundle);
    this.serverNameSslProviders = createServerNameSslProviders(serverNameSslBundles);
  }

  @Override
  public HttpServer apply(HttpServer server) {
    return server.secure(this::applySecurity);
  }

  private void applySecurity(SslContextSpec spec) {
    spec.sslContext(this.sslProvider.getSslContext()).setSniAsyncMappings((serverName, promise) -> {
      SslProvider provider = (serverName != null) ? this.serverNameSslProviders.get(serverName) : this.sslProvider;
      return promise.setSuccess(provider);
    });
  }

  void updateSslBundle(@Nullable String serverName, SslBundle sslBundle) {
    logger.debug("SSL Bundle has been updated, reloading SSL configuration");
    if (serverName == null) {
      this.sslProvider = createSslProvider(sslBundle);
    }
    else {
      this.serverNameSslProviders.put(serverName, createSslProvider(sslBundle));
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
  protected final AbstractProtocolSslContextSpec<?> createSslContextSpec(SslBundle sslBundle) {
    AbstractProtocolSslContextSpec<?> sslContextSpec = isHttp2Enabled
            ? Http2SslContextSpec.forServer(sslBundle.getManagers().getKeyManagerFactory())
            : Http11SslContextSpec.forServer(sslBundle.getManagers().getKeyManagerFactory());
    return sslContextSpec.configure(builder -> {
      builder.trustManager(sslBundle.getManagers().getTrustManagerFactory());
      SslOptions options = sslBundle.getOptions();
      builder.protocols(options.getEnabledProtocols());
      builder.ciphers(SslOptions.asSet(options.getCiphers()));
      builder.clientAuth(this.clientAuth);
    });
  }

}
