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

package cn.taketoday.framework.web.embedded.netty;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.framework.web.server.Http2;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.lang.Nullable;
import io.netty.handler.ssl.ClientAuth;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;

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

  @Nullable
  private final Http2 http2;

  @Nullable
  private final Ssl.ClientAuth clientAuth;

  private final SslBundle sslBundle;

  public SslServerCustomizer(@Nullable Http2 http2,
          @Nullable Ssl.ClientAuth clientAuth, SslBundle sslBundle) {
    this.http2 = http2;
    this.clientAuth = clientAuth;
    this.sslBundle = sslBundle;
  }

  @Override
  public HttpServer apply(HttpServer server) {
    AbstractProtocolSslContextSpec<?> sslContextSpec = createSslContextSpec();
    return server.secure((spec) -> spec.sslContext(sslContextSpec));
  }

  protected AbstractProtocolSslContextSpec<?> createSslContextSpec() {
    var sslContextSpec
            = Http2.isEnabled(http2)
              ? Http2SslContextSpec.forServer(this.sslBundle.getManagers().getKeyManagerFactory())
              : Http11SslContextSpec.forServer(this.sslBundle.getManagers().getKeyManagerFactory());

    sslContextSpec.configure(builder -> {
      SslOptions options = this.sslBundle.getOptions();
      builder.trustManager(this.sslBundle.getManagers().getTrustManagerFactory())
              .protocols(options.getEnabledProtocols())
              .ciphers(SslOptions.asSet(options.getCiphers()))
              .clientAuth(Ssl.ClientAuth.map(this.clientAuth,
                      ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE));
    });
    return sslContextSpec;
  }

}
