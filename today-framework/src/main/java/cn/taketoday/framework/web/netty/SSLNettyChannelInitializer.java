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

package cn.taketoday.framework.web.netty;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.annotation.config.web.netty.NettySSLBuilder;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import io.netty.channel.Channel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

/**
 * HTTPS netty channel initializer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/6 21:16
 */
final class SSLNettyChannelInitializer extends NettyChannelInitializer {

  private final Map<String, SslContext> serverNameSslProviders;

  private final ClientAuth clientAuth;

  private final boolean http2Enabled;

  private volatile SslContext sslContext;

  public SSLNettyChannelInitializer(NettyChannelHandler channelHandler, boolean http2Enabled,
          @Nullable Ssl.ClientAuth clientAuth, SslBundle sslBundle, Map<String, SslBundle> serverNameSslBundles) {
    super(channelHandler);
    Assert.notNull(sslBundle, "sslBundle is required");
    this.http2Enabled = http2Enabled;
    this.sslContext = createSslContext(sslBundle);
    this.serverNameSslProviders = createServerNameSSLContexts(serverNameSslBundles);
    this.clientAuth = Ssl.ClientAuth.map(clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
  }

  @Override
  protected void initChannel(final Channel ch) {
    SslHandler sslHandler;
    if (ch.remoteAddress() instanceof InetSocketAddress sniInfo) {
      sslHandler = sslContext.newHandler(ch.alloc(), sniInfo.getHostString(), sniInfo.getPort());
    }
    else {
      sslHandler = sslContext.newHandler(ch.alloc());
    }

    ch.pipeline().addLast("ssl-handler", sslHandler);
    super.initChannel(ch);
  }

  void updateSSLBundle(@Nullable String serverName, SslBundle sslBundle) {
    logger.debug("SSL Bundle has been updated, reloading SSL configuration");
    if (serverName == null) {
      this.sslContext = createSslContext(sslBundle);
    }
    else {
      this.serverNameSslProviders.put(serverName, createSslContext(sslBundle));
    }
  }

  private Map<String, SslContext> createServerNameSSLContexts(Map<String, SslBundle> serverNameSslBundles) {
    HashMap<String, SslContext> serverNameSslProviders = new HashMap<>();
    for (Map.Entry<String, SslBundle> entry : serverNameSslBundles.entrySet()) {
      serverNameSslProviders.put(entry.getKey(), createSslContext(entry.getValue()));
    }
    return serverNameSslProviders;
  }

  private SslContext createSslContext(SslBundle ssl) {
    return NettySSLBuilder.createSslContext(http2Enabled, clientAuth, ssl);
  }

}
