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

package cn.taketoday.framework.web.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;

import cn.taketoday.framework.web.server.SslConfigurationValidator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SslConfigurationValidator}.
 *
 * @author Chris Bono
 */

class SslConfigurationValidatorTests {

  private static final String VALID_ALIAS = "test-alias";

  private static final String INVALID_ALIAS = "test-alias-5150";

  private KeyStore keyStore;

  @BeforeEach
  void loadKeystore() throws Exception {
    this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (InputStream stream = new FileInputStream("src/test/resources/test.jks")) {
      this.keyStore.load(stream, "secret".toCharArray());
    }
  }

  @Test
  void validateKeyAliasWhenAliasFoundShouldNotFail() {
    SslConfigurationValidator.validateKeyAlias(this.keyStore, VALID_ALIAS);
  }

  @Test
  void validateKeyAliasWhenNullAliasShouldNotFail() {
    SslConfigurationValidator.validateKeyAlias(this.keyStore, null);
  }

  @Test
  void validateKeyAliasWhenEmptyAliasShouldNotFail() {
    SslConfigurationValidator.validateKeyAlias(this.keyStore, "");
  }

  @Test
  void validateKeyAliasWhenAliasNotFoundShouldThrowException() {
    assertThatThrownBy(() -> SslConfigurationValidator.validateKeyAlias(this.keyStore, INVALID_ALIAS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Keystore does not contain specified alias '" + INVALID_ALIAS + "'");
  }

  @Test
  void validateKeyAliasWhenKeyStoreThrowsExceptionOnContains() throws KeyStoreException {
    KeyStore uninitializedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    assertThatThrownBy(() -> SslConfigurationValidator.validateKeyAlias(uninitializedKeyStore, "alias"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not determine if keystore contains alias 'alias'");
  }

}
