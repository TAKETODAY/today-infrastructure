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
import cn.taketoday.lang.Nullable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

/**
 * HTTPS netty channel initializer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/6 21:16
 */
final class SSLNettyChannelInitializer extends NettyChannelInitializer {

  @Nullable
  private final HashMap<String, SslContext> serverNameSslProviders;

  private final long handshakeTimeout;

  private final ClientAuth clientAuth;

  private final boolean http2Enabled;

  private volatile SslContext sslContext;

  public SSLNettyChannelInitializer(ChannelHandler channelHandler, boolean http2Enabled,
          Ssl ssl, SslBundle sslBundle, Map<String, SslBundle> serverNameSslBundles) {
    super(channelHandler);
    this.http2Enabled = http2Enabled;
    this.handshakeTimeout = ssl.handshakeTimeout.toMillis();
    this.serverNameSslProviders = createServerNameSSLContexts(serverNameSslBundles);
    this.clientAuth = Ssl.ClientAuth.map(ssl.clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
    
    this.sslContext = createSslContext(sslBundle);
  }

  @Override
  protected void initChannel(final Channel ch) {
    if (serverNameSslProviders != null) {
      SniHandler sniHandler = new SniHandler((hostname, promise) ->
              promise.setSuccess(serverNameSslProviders.getOrDefault(hostname, sslContext)), handshakeTimeout);
      ch.pipeline().addLast("SNI-handler", sniHandler);
    }
    else {
      SslHandler sslHandler = createSslHandler(ch);
      sslHandler.setHandshakeTimeoutMillis(handshakeTimeout);
      ch.pipeline().addLast("SSL-handler", sslHandler);
    }
    super.initChannel(ch);
  }

  public void updateSSLBundle(@Nullable String serverName, SslBundle sslBundle) {
    logger.debug("SSL Bundle has been updated, reloading SSL configuration");
    if (serverName == null) {
      this.sslContext = createSslContext(sslBundle);
    }
    else if (serverNameSslProviders != null) {
      serverNameSslProviders.put(serverName, createSslContext(sslBundle));
    }
  }

  private SslHandler createSslHandler(Channel ch) {
    if (ch.remoteAddress() instanceof InetSocketAddress sniInfo) {
      return sslContext.newHandler(ch.alloc(), sniInfo.getHostString(), sniInfo.getPort());
    }
    return sslContext.newHandler(ch.alloc());
  }

  @Nullable
  private HashMap<String, SslContext> createServerNameSSLContexts(Map<String, SslBundle> serverNameSslBundles) {
    if (serverNameSslBundles.isEmpty()) {
      return null;
    }
    HashMap<String, SslContext> serverNameSslProviders = new HashMap<>(serverNameSslBundles.size());
    for (Map.Entry<String, SslBundle> entry : serverNameSslBundles.entrySet()) {
      serverNameSslProviders.put(entry.getKey(), createSslContext(entry.getValue()));
    }
    return serverNameSslProviders;
  }

  private SslContext createSslContext(SslBundle ssl) {
    return NettySSLBuilder.createSslContext(http2Enabled, clientAuth, ssl);
  }

}
