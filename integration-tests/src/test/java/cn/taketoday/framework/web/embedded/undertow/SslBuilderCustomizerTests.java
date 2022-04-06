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

package cn.taketoday.framework.web.embedded.undertow;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.security.NoSuchProviderException;

import javax.net.ssl.KeyManager;

import cn.taketoday.framework.web.embedded.undertow.SslBuilderCustomizer;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SslBuilderCustomizer}
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 */
class SslBuilderCustomizerTests {

  @Test
  void getKeyManagersWhenAliasIsNullShouldNotDecorate() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setKeyStore("src/test/resources/test.jks");
    SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
    KeyManager[] keyManagers = ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null);
    Class<?> name = Class.forName(
            "cn.taketoday.framework.web.embedded.undertow.SslBuilderCustomizer$ConfigurableAliasKeyManager");
    assertThat(keyManagers[0]).isNotInstanceOf(name);
  }

  @Test
  void keyStoreProviderIsUsedWhenCreatingKeyStore() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setKeyStoreProvider("com.example.KeyStoreProvider");
    SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
    assertThatIllegalStateException()
            .isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null))
            .withCauseInstanceOf(NoSuchProviderException.class)
            .withMessageContaining("com.example.KeyStoreProvider");
  }

  @Test
  void trustStoreProviderIsUsedWhenCreatingTrustStore() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setTrustStorePassword("password");
    ssl.setTrustStore("src/test/resources/test.jks");
    ssl.setTrustStoreProvider("com.example.TrustStoreProvider");
    SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
    assertThatIllegalStateException()
            .isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getTrustManagers", ssl, null))
            .withCauseInstanceOf(NoSuchProviderException.class)
            .withMessageContaining("com.example.TrustStoreProvider");
  }

  @Test
  void getKeyManagersWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException() throws Exception {
    Ssl ssl = new Ssl();
    SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
    assertThatIllegalStateException()
            .isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null))
            .withCauseInstanceOf(WebServerException.class).withMessageContaining("Could not load key store 'null'");
  }

}
