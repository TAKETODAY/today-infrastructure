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

package cn.taketoday.web.server.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.server.Ssl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

import static io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import static io.netty.handler.ssl.SslProvider.JDK;
import static io.netty.handler.ssl.SslProvider.OPENSSL;

/**
 * HTTPS netty channel initializer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/6 21:16
 */
final class SSLNettyChannelInitializer extends NettyChannelInitializer {

  @Nullable
  private final HashMap<String, SslContext> serverNameSslContexts;

  private final long handshakeTimeout;

  private final ClientAuth clientAuth;

  private final boolean http2Enabled;

  private volatile SslContext sslContext;

  public SSLNettyChannelInitializer(ChannelHandler channelHandler, @Nullable ChannelConfigurer channelConfigurer,
          boolean http2Enabled, Ssl ssl, SslBundle sslBundle, Map<String, SslBundle> serverNameSslBundles) {
    super(channelHandler, channelConfigurer);
    this.http2Enabled = http2Enabled;
    this.handshakeTimeout = ssl.handshakeTimeout.toMillis();
    this.clientAuth = Ssl.ClientAuth.map(ssl.clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
    this.sslContext = createSslContext(sslBundle);
    this.serverNameSslContexts = createServerNameSSLContexts(serverNameSslBundles);
  }

  @Override
  protected void preInitChannel(@Nullable ChannelConfigurer configurer, final Channel ch) {
    if (configurer != null) {
      configurer.initChannel(ch);
    }
    if (serverNameSslContexts != null) {
      SniHandler sniHandler = new SniHandler((hostname, promise) ->
              promise.setSuccess(serverNameSslContexts.getOrDefault(hostname, sslContext)), handshakeTimeout);
      ch.pipeline().addLast("SNI-handler", sniHandler);
    }
    else {
      SslHandler sslHandler = createSslHandler(ch);
      sslHandler.setHandshakeTimeoutMillis(handshakeTimeout);
      ch.pipeline().addLast("SSL-handler", sslHandler);
    }
  }

  public void updateSSLBundle(@Nullable String serverName, SslBundle sslBundle) {
    logger.debug("SSL Bundle has been updated, reloading SSL configuration");
    if (serverName == null) {
      this.sslContext = createSslContext(sslBundle);
    }
    else if (serverNameSslContexts != null) {
      serverNameSslContexts.put(serverName, createSslContext(sslBundle));
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

  /**
   * Create an {@link SslContext} for a given {@link SslBundle}.
   *
   * @param ssl the {@link SslBundle} to use
   * @return an {@link SslContext} instance
   */
  private SslContext createSslContext(SslBundle ssl) {
    SslOptions options = ssl.getOptions();
    try {
      return SslContextBuilder.forServer(ssl.getManagers().getKeyManagerFactory())
              .protocols(options.getEnabledProtocols())
              .sslProvider(getSslProvider())
              .ciphers(getCiphers(options), SupportedCipherSuiteFilter.INSTANCE)
              .clientAuth(clientAuth)
              .applicationProtocolConfig(http2Enabled ? new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
                      SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1) : null)
              .build();
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  @Nullable
  private List<String> getCiphers(SslOptions options) {
    if (ObjectUtils.isNotEmpty(options.getCiphers())) {
      return Arrays.asList(options.getCiphers());
    }
    if (http2Enabled) {
      return Http2SecurityUtil.CIPHERS;
    }
    return null;
  }

  private SslProvider getSslProvider() {
    return http2Enabled ? (SslProvider.isAlpnSupported(OPENSSL) ? OPENSSL : JDK)
            : (OpenSsl.isAvailable() ? OPENSSL : JDK);
  }

}
