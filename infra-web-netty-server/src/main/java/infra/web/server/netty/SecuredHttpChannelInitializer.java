/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslOptions;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ExceptionUtils;
import infra.util.ObjectUtils;
import infra.web.server.Ssl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
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
 * Secure HTTP channel initializer for handling HTTPS connections.
 * This class extends {@link HttpChannelInitializer} and provides
 * SSL/TLS support along with HTTP/2 protocol negotiation.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/6 21:16
 */
final class SecuredHttpChannelInitializer extends HttpChannelInitializer {

  private static final Logger logger = LoggerFactory.getLogger(SecuredHttpChannelInitializer.class);

  private final @Nullable HashMap<String, SslContext> serverNameSslContexts;

  private final long handshakeTimeout;

  private final ClientAuth clientAuth;

  private volatile SslContext sslContext;

  public SecuredHttpChannelInitializer(ChannelHandler httpTrafficHandler, HttpDecoderConfig config,
          @Nullable ChannelConfigurer configurer, boolean http2Enabled, Ssl ssl, SslBundle sslBundle,
          Map<String, SslBundle> serverNameSslBundles, Http2FrameCodecFactory http2FrameCodecFactory) {
    super(httpTrafficHandler, http2Enabled, configurer, config, http2FrameCodecFactory);
    this.handshakeTimeout = ssl.handshakeTimeout.toMillis();
    this.clientAuth = Ssl.ClientAuth.map(ssl.clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE);
    this.sslContext = createSslContext(sslBundle);
    this.serverNameSslContexts = createServerNameSSLContexts(serverNameSslBundles);
  }

  @Override
  protected void configureHttp11Channel(Channel ch) {
    configureSecuredChannel(ch);
    super.configureHttp11Channel(ch);
  }

  @Override
  protected void configureHttp11OrH2Channel(Channel ch) {
    configureSecuredChannel(ch);
    ch.pipeline().addLast(new SecuredNegotiationHandler(ch));
  }

  private void configureSecuredChannel(Channel ch) {
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

  private @Nullable HashMap<String, SslContext> createServerNameSSLContexts(Map<String, SslBundle> serverNameSslBundles) {
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
    SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(ssl.getManagers().getKeyManagerFactory())
            .protocols(options.getEnabledProtocols())
            .sslProvider(getSslProvider())
            .ciphers(getCiphers(options), SupportedCipherSuiteFilter.INSTANCE)
            .clientAuth(clientAuth)
            .applicationProtocolConfig(http2Enabled ? new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1) : null);

    return ExceptionUtils.sneakyThrow(sslContextBuilder::build);
  }

  private @Nullable List<String> getCiphers(SslOptions options) {
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

  private final class SecuredNegotiationHandler extends ApplicationProtocolNegotiationHandler {

    private final Channel ch;

    public SecuredNegotiationHandler(Channel ch) {
      super(ApplicationProtocolNames.HTTP_1_1);
      this.ch = ch;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) {
      if (logger.isDebugEnabled()) {
        logger.debug("Negotiated application-level protocol [{}]", protocol);
      }

      if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        ch.pipeline()
                .addLast(HttpCodec, createHttp2FrameCodec())
                .addLast(H2MultiplexHandler, new Http2MultiplexHandler(new H2StreamCodec()));
      }
      else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
        SecuredHttpChannelInitializer.super.configureHttp11Channel(ch);
      }
      else {
        throw new IllegalStateException("Unknown protocol: " + protocol);
      }
    }

    @Override
    public boolean isSharable() {
      return false;
    }

  }

  final class H2StreamCodec extends ChannelInitializer<Channel> {

    @Override
    public boolean isSharable() {
      return true;
    }

    @Override
    protected void initChannel(Channel ch) {
      addH2StreamHandlers(ch);
    }
  }

}
