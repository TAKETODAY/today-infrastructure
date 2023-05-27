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

package cn.taketoday.core.ssl;

import org.junit.jupiter.api.Test;

import java.security.KeyStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslStoreBundle}
 *
 * @author Phillip Webb
 */
class SslStoreBundleTests {

  @Test
  void noneReturnsEmptySslStoreBundle() {
    SslStoreBundle bundle = SslStoreBundle.NONE;
    assertThat(bundle.getKeyStore()).isNull();
    assertThat(bundle.getKeyStorePassword()).isNull();
    assertThat(bundle.getTrustStore()).isNull();
  }

  @Test
  void ofCreatesStoreBundle() {
    KeyStore keyStore = mock(KeyStore.class);
    String keyStorePassword = "secret";
    KeyStore trustStore = mock(KeyStore.class);
    SslStoreBundle bundle = SslStoreBundle.of(keyStore, keyStorePassword, trustStore);
    assertThat(bundle.getKeyStore()).isSameAs(keyStore);
    assertThat(bundle.getKeyStorePassword()).isEqualTo(keyStorePassword);
    assertThat(bundle.getTrustStore()).isSameAs(trustStore);
  }

}
