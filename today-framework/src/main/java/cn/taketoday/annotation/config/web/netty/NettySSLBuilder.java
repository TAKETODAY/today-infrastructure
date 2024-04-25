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
import java.util.Arrays;
import java.util.List;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
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
   * Create an {@link SslContext} for a given {@link SslBundle}.
   *
   * @param ssl the {@link SslBundle} to use
   * @return an {@link SslContext} instance
   */
  public static SslContext createSslContext(boolean http2Enabled, ClientAuth clientAuth, SslBundle ssl) {
    SslOptions options = ssl.getOptions();
    try {
      return SslContextBuilder.forServer(ssl.getManagers().getKeyManagerFactory())
              .protocols(options.getEnabledProtocols())
              .sslProvider(getSslProvider(http2Enabled))
              .ciphers(getCiphers(http2Enabled, options), SupportedCipherSuiteFilter.INSTANCE)
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
  private static List<String> getCiphers(boolean http2Enabled, SslOptions options) {
    if (ObjectUtils.isNotEmpty(options.getCiphers())) {
      return Arrays.asList(options.getCiphers());
    }
    if (http2Enabled) {
      return Http2SecurityUtil.CIPHERS;
    }
    return null;
  }

  private static SslProvider getSslProvider(boolean http2Enabled) {
    return http2Enabled ? (SslProvider.isAlpnSupported(OPENSSL) ? OPENSSL : JDK)
            : (OpenSsl.isAvailable() ? OPENSSL : JDK);
  }

}
