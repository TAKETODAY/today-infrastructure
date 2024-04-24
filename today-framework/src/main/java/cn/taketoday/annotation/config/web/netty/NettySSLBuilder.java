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

package cn.taketoday.annotation.config.web.netty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.framework.web.server.Http2;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

import static cn.taketoday.framework.web.server.Ssl.ClientAuth.map;
import static io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import static io.netty.handler.ssl.SslProvider.JDK;
import static io.netty.handler.ssl.SslProvider.OPENSSL;

/**
 * Internal using for creating {@link SslContext}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/24 15:09
 */
public abstract class NettySSLBuilder {

  /**
   * Create an {@link SslContext} for a given {@link Ssl}.
   *
   * @param ssl the {@link Ssl} to use
   * @return an {@link SslContext} instance
   */
  public static SslContext createSslContext(@Nullable Http2 http2, Ssl ssl) {
    Assert.state(ssl.publicKey.exists(), "publicKey not found");
    Assert.state(ssl.privateKey.exists(), "privateKey not found");

    try (InputStream publicKeyStream = ssl.publicKey.getInputStream();
            InputStream privateKeyStream = ssl.privateKey.getInputStream()) {
      return SslContextBuilder.forServer(publicKeyStream, privateKeyStream, ssl.keyPassword)
              .protocols(ssl.enabledProtocols)
              .sslProvider(getSslProvider(http2))
              .ciphers(getCiphers(http2, ssl), SupportedCipherSuiteFilter.INSTANCE)
              .clientAuth(map(ssl.clientAuth, ClientAuth.NONE, ClientAuth.OPTIONAL, ClientAuth.REQUIRE))
              .applicationProtocolConfig(Http2.isEnabled(http2) ? new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
                      SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1) : null)
              .build();
    }
    catch (IOException e) {
      throw new IllegalStateException("publicKey or publicKey resource I/O error", e);
    }
  }

  @Nullable
  private static List<String> getCiphers(@Nullable Http2 http2, Ssl ssl) {
    if (ObjectUtils.isNotEmpty(ssl.ciphers)) {
      return Arrays.asList(ssl.ciphers);
    }
    if (Http2.isEnabled(http2)) {
      return Http2SecurityUtil.CIPHERS;
    }
    return null;
  }

  private static SslProvider getSslProvider(@Nullable Http2 http2) {
    if (Http2.isEnabled(http2)) {
      return SslProvider.isAlpnSupported(OPENSSL) ? OPENSSL : JDK;
    }
    return OpenSsl.isAvailable() ? OPENSSL : JDK;
  }

}
