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
import java.security.KeyStoreException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslBundleKey}.
 *
 * @author Phillip Webb
 */
class SslBundleKeyTests {

  @Test
  void noneHasNoValues() {
    SslBundleKey keyReference = SslBundleKey.NONE;
    assertThat(keyReference.getPassword()).isNull();
    assertThat(keyReference.getAlias()).isNull();
  }

  @Test
  void ofCreatesWithPasswordSslKeyReference() {
    SslBundleKey keyReference = SslBundleKey.of("password");
    assertThat(keyReference.getPassword()).isEqualTo("password");
    assertThat(keyReference.getAlias()).isNull();
  }

  @Test
  void ofCreatesWithPasswordAndAliasSslKeyReference() {
    SslBundleKey keyReference = SslBundleKey.of("password", "alias");
    assertThat(keyReference.getPassword()).isEqualTo("password");
    assertThat(keyReference.getAlias()).isEqualTo("alias");
  }

  @Test
  void getKeyManagerFactoryWhenHasAliasNotInStoreThrowsException() throws Exception {
    KeyStore keyStore = mock(KeyStore.class);
    given(keyStore.containsAlias("alias")).willReturn(false);
    SslBundleKey key = SslBundleKey.of("secret", "alias");
    assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAlias(keyStore))
            .withMessage("Keystore does not contain alias 'alias'");
  }

  @Test
  void getKeyManagerFactoryWhenHasAliasNotDeterminedInStoreThrowsException() throws Exception {
    KeyStore keyStore = mock(KeyStore.class);
    given(keyStore.containsAlias("alias")).willThrow(KeyStoreException.class);
    SslBundleKey key = SslBundleKey.of("secret", "alias");
    assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAlias(keyStore))
            .withMessage("Could not determine if keystore contains alias 'alias'");
  }

}
