/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.jetty;

import org.assertj.core.api.Assertions;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.framework.web.server.Http2;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.test.junit.DisabledOnOs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Andy Wilkinson
 */
class SslServerCustomizerTests {

  @Test
  @SuppressWarnings("rawtypes")
  void whenHttp2IsNotEnabledServerConnectorHasSslAndHttpConnectionFactories() {
    Server server = createCustomizedServer();
    assertThat(server.getConnectors()).hasSize(1);
    List<ConnectionFactory> factories = new ArrayList<>(server.getConnectors()[0].getConnectionFactories());
    assertThat(factories).extracting((factory) -> (Class) factory.getClass())
            .containsExactly(SslConnectionFactory.class, HttpConnectionFactory.class);
  }

  @Test
  @SuppressWarnings("rawtypes")
  @DisabledOnOs(os = OS.LINUX, architecture = "aarch64",
                disabledReason = "conscrypt doesn't support Linux aarch64, see https://github.com/google/conscrypt/issues/1051")
  void whenHttp2IsEnabledServerConnectorsHasSslAlpnH2AndHttpConnectionFactories() {
    Http2 http2 = new Http2();
    http2.setEnabled(true);
    Server server = createCustomizedServer(http2);
    assertThat(server.getConnectors()).hasSize(1);
    List<ConnectionFactory> factories = new ArrayList<>(server.getConnectors()[0].getConnectionFactories());
    assertThat(factories).extracting((factory) -> (Class) factory.getClass()).containsExactly(
            SslConnectionFactory.class, ALPNServerConnectionFactory.class, HTTP2ServerConnectionFactory.class,
            HttpConnectionFactory.class);
  }

  @Test
  @DisabledOnOs(os = OS.LINUX, architecture = "aarch64",
                disabledReason = "conscrypt doesn't support Linux aarch64, see https://github.com/google/conscrypt/issues/1051")
  void alpnConnectionFactoryHasNullDefaultProtocolToAllowNegotiationToHttp11() {
    Http2 http2 = new Http2();
    http2.setEnabled(true);
    Server server = createCustomizedServer(http2);
    assertThat(server.getConnectors()).hasSize(1);
    List<ConnectionFactory> factories = new ArrayList<>(server.getConnectors()[0].getConnectionFactories());
    assertThat(((ALPNServerConnectionFactory) factories.get(1)).getDefaultProtocol()).isNull();
  }

  @Test
  void configureSslWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException() {
    Ssl ssl = new Ssl();
    SslServerCustomizer customizer = new SslServerCustomizer(null, ssl, null, null);
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> customizer.configureSsl(new SslContextFactory.Server(), ssl, null))
            .satisfies((ex) -> {
              assertThat(ex).isInstanceOf(WebServerException.class);
              assertThat(ex).hasMessageContaining("Could not load key store 'null'");
            });
  }

  private Server createCustomizedServer() {
    return createCustomizedServer(new Http2());
  }

  private Server createCustomizedServer(Http2 http2) {
    Ssl ssl = new Ssl();
    ssl.setKeyStore("classpath:test.jks");
    return createCustomizedServer(ssl, http2);
  }

  private Server createCustomizedServer(Ssl ssl, Http2 http2) {
    Server server = new Server();
    new SslServerCustomizer(new InetSocketAddress(0), ssl, null, http2).customize(server);
    return server;
  }

}
