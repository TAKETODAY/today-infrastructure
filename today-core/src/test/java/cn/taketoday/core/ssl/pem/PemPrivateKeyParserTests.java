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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.List;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

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
    PrivateKey privateKey = parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
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
    PrivateKey privateKey = parse(read("cn/taketoday/core/ssl/pkcs1/" + file));
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
    assertThatIllegalStateException()
            .isThrownBy(() -> parse(read("cn/taketoday/core/ssl/pkcs1/" + file)))
            .withMessageContaining("Error loading private key file")
            .withCauseInstanceOf(IllegalStateException.class)
            .havingCause()
            .withMessageContaining("Unsupported private key format");
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
    PrivateKey privateKey = parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
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
    assertThatIllegalStateException()
            .isThrownBy(() -> PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs8/" + file)))
            .withMessageContaining("Error loading private key file")
            .withCauseInstanceOf(IllegalStateException.class)
            .havingCause()
            .withMessageContaining("Unrecognized private key format");
  }

  @ParameterizedTest
  // @formatter:off
  @ValueSource(strings = {
          "ed448.key",
          "ed25519.key"
  })
    // @formatter:on
  void shouldParseEdDsaPkcs8(String file) throws IOException {
    PrivateKey privateKey = parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
    assertThat(privateKey).isNotNull();
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo("EdDSA");
  }

  @ParameterizedTest
  // @formatter:off
  @ValueSource(strings = {
          "x448.key",
          "x25519.key"
  })
    // @formatter:on
  void shouldParseXdhPkcs8(String file) throws IOException {
    PrivateKey privateKey = parse(read("cn/taketoday/core/ssl/pkcs8/" + file));
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
    PrivateKey privateKey = parse(read("cn/taketoday/core/ssl/sec1/" + file));
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
    assertThatIllegalStateException()
            .isThrownBy(() -> PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/sec1/" + file)))
            .withMessageContaining("Error loading private key file")
            .withCauseInstanceOf(IllegalStateException.class)
            .havingCause()
            .withMessageContaining("Unrecognized private key format");
  }

  @Test
  void parseWithNonKeyTextWillReturnEmptyArray() throws Exception {
    assertThat(PemPrivateKeyParser.parse(read("test-banner.txt"))).isEmpty();
  }

  @ParameterizedTest
  // @formatter:off
  @CsvSource({
          "dsa-aes-128-cbc.key,				DSA",
          "rsa-aes-256-cbc.key,				RSA",
          "prime256v1-aes-256-cbc.key,		EC",
          "ed25519-aes-256-cbc.key,			EdDSA",
          "x448-aes-256-cbc.key,				XDH"
  })
    // @formatter:on
  void shouldParseEncryptedPkcs8(String file, String algorithm) throws IOException {
    // Created with:
    // openssl pkcs8 -topk8 -in <input file> -out <output file> -v2 <algorithm>
    // -passout pass:test
    // where <algorithm> is aes128 or aes256
    String content = read("cn/taketoday/core/ssl/pkcs8/" + file);
    List<PrivateKey> privateKeys = PemPrivateKeyParser.parse(content, "test");
    assertThat(privateKeys).isNotEmpty();
    PrivateKey privateKey = privateKeys.get(0);
    assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
  }

  @Test
  void shouldNotParseEncryptedPkcs8NotUsingAes() {
    // Created with:
    // openssl pkcs8 -topk8 -in rsa.key -out rsa-des-ede3-cbc.key -v2 des3 -passout
    // pass:test
    assertThatIllegalStateException()
            .isThrownBy(() -> PemPrivateKeyParser
                    .parse(read("cn/taketoday/core/ssl/pkcs8/rsa-des-ede3-cbc.key"), "test"))
            .isInstanceOf(IllegalStateException.class)
            .withMessageContaining("Error decrypting private key");
  }

  @Test
  void shouldNotParseEncryptedPkcs8NotUsingPbkdf2() {
    // Created with:
    // openssl pkcs8 -topk8 -in rsa.key -out rsa-des-ede3-cbc.key -scrypt -passout
    // pass:test
    assertThatIllegalStateException()
            .isThrownBy(() -> PemPrivateKeyParser
                    .parse(read("cn/taketoday/core/ssl/pkcs8/rsa-scrypt.key"), "test"))
            .withMessageContaining("Error decrypting private key");
  }

  @Test
  void shouldNotParseEncryptedSec1() throws Exception {
    // created with:
    // openssl ecparam -genkey -name prime256v1 | openssl ec -aes-128-cbc -out
    // prime256v1-aes-128-cbc.key
    assertThat(PemPrivateKeyParser
            .parse(read("cn/taketoday/core/ssl/sec1/prime256v1-aes-128-cbc.key"), "test")).isEmpty();
  }

  @Test
  void shouldNotParseEncryptedPkcs1() throws Exception {
    // created with:
    // openssl genrsa -aes-256-cbc -out rsa-aes-256-cbc.key
    assertThat(PemPrivateKeyParser.parse(read("cn/taketoday/core/ssl/pkcs1/rsa-aes-256-cbc.key"),
            "test"))
            .isEmpty();
  }

  private PrivateKey parse(String key) {
    List<PrivateKey> keys = PemPrivateKeyParser.parse(key);
    return (!ObjectUtils.isEmpty(keys)) ? keys.get(0) : null;
  }

  private String read(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }

}
