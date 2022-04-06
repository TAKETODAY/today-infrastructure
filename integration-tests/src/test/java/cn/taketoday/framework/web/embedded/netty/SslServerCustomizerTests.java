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

package cn.taketoday.framework.web.embedded.netty;

import org.junit.jupiter.api.Test;

import java.security.NoSuchProviderException;

import cn.taketoday.framework.web.embedded.netty.SslServerCustomizer;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServerException;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Andy Wilkinson
 * @author Raheela Aslam
 */
@SuppressWarnings("deprecation")
class SslServerCustomizerTests {

  @Test
  void keyStoreProviderIsUsedWhenCreatingKeyStore() {
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setKeyStoreProvider("com.example.KeyStoreProvider");
    SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
    assertThatIllegalStateException().isThrownBy(() -> customizer.getKeyManagerFactory(ssl, null))
            .withCauseInstanceOf(NoSuchProviderException.class)
            .withMessageContaining("com.example.KeyStoreProvider");
  }

  @Test
  void trustStoreProviderIsUsedWhenCreatingTrustStore() {
    Ssl ssl = new Ssl();
    ssl.setTrustStorePassword("password");
    ssl.setTrustStore("src/test/resources/test.jks");
    ssl.setTrustStoreProvider("com.example.TrustStoreProvider");
    SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
    assertThatIllegalStateException().isThrownBy(() -> customizer.getTrustManagerFactory(ssl, null))
            .withCauseInstanceOf(NoSuchProviderException.class)
            .withMessageContaining("com.example.TrustStoreProvider");
  }

  @Test
  void getKeyManagerFactoryWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException() {
    Ssl ssl = new Ssl();
    SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
    assertThatIllegalStateException().isThrownBy(() -> customizer.getKeyManagerFactory(ssl, null))
            .withCauseInstanceOf(WebServerException.class).withMessageContaining("Could not load key store 'null'");
  }

}
