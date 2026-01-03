/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config;

import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLContext;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslOptions;
import infra.lang.Contract;

/**
 * Adapts {@link SslBundle} to an
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link DefaultClientTlsStrategy}.
 *
 * @author Phillip Webb
 */
final class HttpComponentsSslBundleTlsStrategy {

  private HttpComponentsSslBundleTlsStrategy() {
  }

  @Contract("!null -> !null")
  static @Nullable DefaultClientTlsStrategy get(@Nullable SslBundle sslBundle) {
    if (sslBundle == null) {
      return null;
    }
    SslOptions options = sslBundle.getOptions();
    SSLContext sslContext = sslBundle.createSslContext();
    return new DefaultClientTlsStrategy(sslContext, options.getEnabledProtocols(), options.getCiphers(), null,
            new DefaultHostnameVerifier());
  }

}
