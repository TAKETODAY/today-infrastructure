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

package cn.taketoday.core.ssl.pem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PemPrivateKeyParser}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class PemPrivateKeyParserTests {

  @Test
  void parsePkcs8RsaKeyFile() throws Exception {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-rsa.pem"));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
  }

  @ParameterizedTest
  @ValueSource(strings = { "key-ec-nist-p256.pem", "key-ec-nist-p384.pem", "key-ec-prime256v1.pem",
          "key-ec-secp256r1.pem" })
  void parsePkcs8EcKeyFile(String fileName) throws Exception {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/" + fileName));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
  }

  @Test
  void parsePkcs8DsaKeyFile() throws Exception {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-dsa.pem"));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("DSA");
  }

  @Test
  void parsePkcs8Ed25519KeyFile() throws Exception {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-ec-ed25519.pem"));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EdDSA");
  }

  @Test
  void parsePkcs8KeyFileWithEcdsa() throws Exception {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/test-ec-key.pem"));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
  }

  @Test
  void parseWithNonKeyTextWillThrowException() {
    assertThatIllegalStateException().isThrownBy(() -> PemPrivateKeyParser.parse(read("test-banner.txt")));
  }

  private String read(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

}
