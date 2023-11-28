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

package cn.taketoday.framework.web.embedded.netty;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.framework.web.server.Http2;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SslServerCustomizer implements ReactorNettyServerCustomizer {

  private static final Logger logger = LoggerFactory.getLogger(SslServerCustomizer.class);

  @Nullable
  private final Http2 http2;

  private final ClientAuth clientAuth;

  private volatile SslProvider sslProvider;

  public SslServerCustomizer(@Nullable Http2 http2,
          @Nullable Ssl.ClientAuth clientAuth, SslBundle sslBundle) {
    this.http2 = http2;
    this.clientAuth = Ssl.ClientAuth.map(clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
    this.sslProvider = createSslProvider(sslBundle);
  }

  @Override
  public HttpServer apply(HttpServer server) {
    return server.secure(this::applySecurity);
  }

  private void applySecurity(SslContextSpec spec) {
    spec.sslContext(this.sslProvider.getSslContext())
            .setSniAsyncMappings((domainName, promise) -> promise.setSuccess(this.sslProvider));
  }

  void updateSslBundle(SslBundle sslBundle) {
    logger.debug("SSL Bundle has been updated, reloading SSL configuration");
    this.sslProvider = createSslProvider(sslBundle);
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
    AbstractProtocolSslContextSpec<?> sslContextSpec =
            (this.http2 != null && this.http2.isEnabled())
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
