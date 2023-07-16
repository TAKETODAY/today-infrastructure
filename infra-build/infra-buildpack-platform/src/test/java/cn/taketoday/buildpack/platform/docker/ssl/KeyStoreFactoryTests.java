/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.ssl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KeyStoreFactory}.
 *
 * @author Scott Frederick
 */
class KeyStoreFactoryTests {

  private PemFileWriter fileWriter;

  @BeforeEach
  void setUp() throws IOException {
    this.fileWriter = new PemFileWriter();
  }

  @AfterEach
  void tearDown() throws IOException {
    this.fileWriter.cleanup();
  }

  @Test
  void createKeyStoreWithCertChain()
          throws IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
    Path certPath = this.fileWriter.writeFile("cert.pem", PemFileWriter.CA_CERTIFICATE, PemFileWriter.CERTIFICATE);
    KeyStore keyStore = KeyStoreFactory.create(certPath, null, "test-alias");
    assertThat(keyStore.containsAlias("test-alias-0")).isTrue();
    assertThat(keyStore.getCertificate("test-alias-0")).isNotNull();
    assertThat(keyStore.getKey("test-alias-0", new char[] {})).isNull();
    assertThat(keyStore.containsAlias("test-alias-1")).isTrue();
    assertThat(keyStore.getCertificate("test-alias-1")).isNotNull();
    assertThat(keyStore.getKey("test-alias-1", new char[] {})).isNull();
  }

  @Test
  void createKeyStoreWithCertChainAndRsaPrivateKey()
          throws IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
    Path certPath = this.fileWriter.writeFile("cert.pem", PemFileWriter.CA_CERTIFICATE, PemFileWriter.CERTIFICATE);
    Path keyPath = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_RSA_KEY);
    KeyStore keyStore = KeyStoreFactory.create(certPath, keyPath, "test-alias");
    assertThat(keyStore.containsAlias("test-alias")).isTrue();
    assertThat(keyStore.getCertificate("test-alias")).isNotNull();
    assertThat(keyStore.getKey("test-alias", new char[] {})).isNotNull();
  }

}
