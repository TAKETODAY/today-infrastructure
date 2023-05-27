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

package cn.taketoday.framework.web.embedded.undertow;

import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;

import java.net.InetAddress;

import javax.net.ssl.SSLContext;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.framework.web.server.Ssl.ClientAuth;
import cn.taketoday.lang.Nullable;
import io.undertow.Undertow;

/**
 * {@link UndertowBuilderCustomizer} that configures SSL on the given builder instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SslBuilderCustomizer implements UndertowBuilderCustomizer {

  private final int port;

  @Nullable
  private final InetAddress address;

  private final ClientAuth clientAuth;

  private final SslBundle sslBundle;

  SslBuilderCustomizer(int port, @Nullable InetAddress address, ClientAuth clientAuth, SslBundle sslBundle) {
    this.port = port;
    this.address = address;
    this.clientAuth = clientAuth;
    this.sslBundle = sslBundle;
  }

  @Override
  public void customize(Undertow.Builder builder) {
    SslOptions options = this.sslBundle.getOptions();
    SSLContext sslContext = this.sslBundle.createSslContext();
    builder.addHttpsListener(this.port, getListenAddress(), sslContext);
    builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE,
            ClientAuth.map(this.clientAuth, SslClientAuthMode.NOT_REQUESTED,
                    SslClientAuthMode.REQUESTED, SslClientAuthMode.REQUIRED));
    if (options.getEnabledProtocols() != null) {
      builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(options.getEnabledProtocols()));
    }
    if (options.getCiphers() != null) {
      builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of(options.getCiphers()));
    }
  }

  private String getListenAddress() {
    if (this.address == null) {
      return "0.0.0.0";
    }
    return this.address.getHostAddress();
  }

}
