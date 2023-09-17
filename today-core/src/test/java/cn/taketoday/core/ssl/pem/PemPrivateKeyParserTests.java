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

package cn.taketoday.core.ssl.pem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;

import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PemPrivateKeyParser}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class PemPrivateKeyParserTests {

  @ParameterizedTest
  // @formatter:off
  @CsvSource({
          "dsa.key,		DSA",
          "rsa.key,		RSA",
          "rsa-pss.key,	RSASSA-PSS"
  })
    // @formatter:on
  void shouldParseTraditionalPkcs8(String file, String algorithm) throws IOException {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
  }

  @ParameterizedTest
  // @formatter:off
  @CsvSource({
          "rsa.key,	RSA"
  })
    // @formatter:on
  void shouldParseTraditionalPkcs1(String file, String algorithm) throws IOException {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs1/" + file));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
  }

  @ParameterizedTest
  // @formatter:off
  @ValueSource(strings = {
          "dsa.key"
  })
    // @formatter:on
  void shouldNotParseUnsupportedTraditionalPkcs1(String file) {
    assertThatThrownBy(() -> PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs1/" + file)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error loading private key file")
            .hasCauseInstanceOf(IllegalStateException.class)
            .cause()
            .hasMessageContaining("Unrecognized private key format");
  }

  @ParameterizedTest
  // @formatter:off
  @CsvSource({
          "brainpoolP256r1.key,	brainpoolP256r1,	1.3.36.3.3.2.8.1.1.7",
          "brainpoolP320r1.key,	brainpoolP320r1,	1.3.36.3.3.2.8.1.1.9",
          "brainpoolP384r1.key,	brainpoolP384r1,	1.3.36.3.3.2.8.1.1.11",
          "brainpoolP512r1.key,	brainpoolP512r1,	1.3.36.3.3.2.8.1.1.13",
          "prime256v1.key,		secp256r1,			1.2.840.10045.3.1.7",
          "secp224r1.key,			secp224r1,			1.3.132.0.33",
          "secp256k1.key,			secp256k1,			1.3.132.0.10",
          "secp256r1.key,			secp256r1,			1.2.840.10045.3.1.7",
          "secp384r1.key,			secp384r1,			1.3.132.0.34",
          "secp521r1.key,			secp521r1,			1.3.132.0.35"
  })
    // @formatter:on
  void shouldParseEcPkcs8(String file, String curveName, String oid) throws IOException {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
    assertThat(privateKey).isInstanceOf(ECPrivateKey.class);
    ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
    assertThat(ecPrivateKey.getParams().toString()).contains(curveName).contains(oid);
  }

  @ParameterizedTest
  // @formatter:off
  @ValueSource(strings = {
          "brainpoolP256t1.key",
          "brainpoolP320t1.key",
          "brainpoolP384t1.key",
          "brainpoolP512t1.key"
  })
    // @formatter:on
  void shouldNotParseUnsupportedEcPkcs8(String file) {
    assertThatThrownBy(() -> PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs8/" + file)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error loading private key file")
            .hasCauseInstanceOf(IllegalStateException.class)
            .cause()
            .hasMessageContaining("Unrecognized private key format");
  }

  @EnabledForJreRange(min = JRE.JAVA_17, disabledReason = "EdDSA is only supported since Java 17")
  @ParameterizedTest
  // @formatter:off
  @ValueSource(strings = {
          "ed448.key",
          "ed25519.key"
  })
    // @formatter:on
  void shouldParseEdDsaPkcs8(String file) throws IOException {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EdDSA");
  }

  @EnabledForJreRange(min = JRE.JAVA_17, disabledReason = "XDH is only supported since Java 17")
  @ParameterizedTest
  // @formatter:off
  @ValueSource(strings = {
          "x448.key",
          "x25519.key"
  })
    // @formatter:on
  void shouldParseXdhPkcs8(String file) throws IOException {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("XDH");
  }

  @ParameterizedTest
  // @formatter:off
  @CsvSource({
          "brainpoolP256r1.key,	brainpoolP256r1,	1.3.36.3.3.2.8.1.1.7",
          "brainpoolP320r1.key,	brainpoolP320r1,	1.3.36.3.3.2.8.1.1.9",
          "brainpoolP384r1.key,	brainpoolP384r1,	1.3.36.3.3.2.8.1.1.11",
          "brainpoolP512r1.key,	brainpoolP512r1,	1.3.36.3.3.2.8.1.1.13",
          "prime256v1.key,		secp256r1,			1.2.840.10045.3.1.7",
          "secp224r1.key,			secp224r1,			1.3.132.0.33",
          "secp256k1.key,			secp256k1,			1.3.132.0.10",
          "secp256r1.key,			secp256r1,			1.2.840.10045.3.1.7",
          "secp384r1.key,			secp384r1,			1.3.132.0.34",
          "secp521r1.key,			secp521r1,			1.3.132.0.35"
  })
    // @formatter:on
  void shouldParseEcSec1(String file, String curveName, String oid) throws IOException {
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/sec1/" + file));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
    assertThat(privateKey).isInstanceOf(ECPrivateKey.class);
    ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
    assertThat(ecPrivateKey.getParams().toString()).contains(curveName).contains(oid);
  }

  @ParameterizedTest
  // @formatter:off
  @ValueSource(strings = {
          "brainpoolP256t1.key",
          "brainpoolP320t1.key",
          "brainpoolP384t1.key",
          "brainpoolP512t1.key"
  })
    // @formatter:on
  void shouldNotParseUnsupportedEcSec1(String file) {
    assertThatThrownBy(() -> PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/sec1/" + file)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Error loading private key file")
            .hasCauseInstanceOf(IllegalStateException.class)
            .cause()
            .hasMessageContaining("Unrecognized private key format");
  }

  @Test
  void parseWithNonKeyTextWillThrowException() {
    assertThatIllegalStateException().isThrownBy(() -> PemPrivateKeyParser.parse(read("test-banner.txt")));
  }

  @Test
  void parsePkcs8EncryptedRsaKeyFile() throws Exception {
    // created with:
    // openssl genpkey -aes-256-cbc -algorithm RSA \
    // -pkeyopt rsa_keygen_bits:4096 -out key-rsa-encrypted.key
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-rsa-encrypted.pem"), "test");
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
  }

  @Test
  void parsePkcs8EncryptedEcKeyFile() throws Exception {
    // created with:
    // openssl genpkey -aes-256-cbc -algorithm EC \
    // -pkeyopt ec_paramgen_curve:prime256v1 -out key-ec-encrypted.key
    PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-ec-encrypted.pem"), "test");
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
  }

  @Test
  void failParsingPkcs1EncryptedKeyFile() throws Exception {
    // created with:
    // openssl genrsa -aes-256-cbc -out key-rsa-encrypted.pem
    assertThatIllegalStateException()
            .isThrownBy(() -> PemPrivateKeyParser.parse(read("ssl/pkcs1/key-rsa-encrypted.pem"), "test"))
            .withMessageContaining("Unrecognized private key format");
  }

  @Test
  void failParsingEcEncryptedKeyFile() throws Exception {
    // created with:
    // openssl ecparam -genkey -name prime256v1 | openssl ec -aes-128-cbc -out
    // key-ec-prime256v1-encrypted.pem
    assertThatIllegalStateException()
            .isThrownBy(() -> PemPrivateKeyParser.parse(read("ssl/ec/key-ec-prime256v1-encrypted.pem"), "test"))
            .withMessageContaining("Unrecognized private key format");
  }

  private String read(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

}
