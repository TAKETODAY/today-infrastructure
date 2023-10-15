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

import java.security.KeyStore;
import java.util.function.Consumer;

import cn.taketoday.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PemSslStoreBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class PemSslStoreBundleTests {

  private static final char[] EMPTY_KEY_PASSWORD = new char[] {};

  @Test
  void whenNullStores() {
    PemSslStoreDetails keyStoreDetails = null;
    PemSslStoreDetails trustStoreDetails = null;
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).isNull();
    assertThat(bundle.getKeyStorePassword()).isNull();
    assertThat(bundle.getTrustStore()).isNull();
  }

  @Test
  void whenStoresHaveNoValues() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate(null);
    PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate(null);
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).isNull();
    assertThat(bundle.getKeyStorePassword()).isNull();
    assertThat(bundle.getTrustStore()).isNull();
  }

  @Test
  void whenHasKeyStoreDetailsCertAndKey() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreDetails trustStoreDetails = null;
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
    assertThat(bundle.getTrustStore()).isNull();
  }

  @Test
  void whenHasKeyStoreDetailsCertAndEncryptedKey() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/pkcs8/key-rsa-encrypted.pem")
            .withPrivateKeyPassword("test");
    PemSslStoreDetails trustStoreDetails = null;
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
    assertThat(bundle.getTrustStore()).isNull();
  }

  @Test
  void whenHasKeyStoreDetailsAndTrustStoreDetailsWithoutKey() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem");
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
    assertThat(bundle.getTrustStore()).satisfies(storeContainingCert("ssl-0"));
  }

  @Test
  void whenHasKeyStoreDetailsAndTrustStoreDetails() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
    assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("ssl"));
  }

  @Test
  void whenHasKeyStoreDetailsAndTrustStoreDetailsAndAlias() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails, "test-alias");
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias"));
    assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("test-alias"));
  }

  @Test
  void whenHasStoreType() {
    PemSslStoreDetails keyStoreDetails = new PemSslStoreDetails("PKCS12", "classpath:ssl/test-cert.pem",
            "classpath:ssl/test-key.pem");
    PemSslStoreDetails trustStoreDetails = new PemSslStoreDetails("PKCS12", "classpath:ssl/test-cert.pem",
            "classpath:ssl/test-key.pem");
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("PKCS12", "ssl"));
    assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("PKCS12", "ssl"));
  }

  @Test
  void whenHasKeyStoreDetailsAndTrustStoreDetailsAndKeyPassword() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:ssl/test-cert.pem")
            .withPrivateKey("classpath:ssl/test-key.pem");
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails, "test-alias", "keysecret");
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
    assertThat(bundle.getTrustStore())
            .satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
  }

  @Test
  void shouldVerifyKeysIfEnabled() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails
            .forCertificate("classpath:cn/taketoday/core/ssl/pem/key1.crt")
            .withPrivateKey("classpath:cn/taketoday/core/ssl/pem/key1.pem");
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, null, "test-alias", "keysecret", true);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
  }

  @Test
  void shouldVerifyKeysIfEnabledAndCertificateChainIsUsed() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails
            .forCertificate("classpath:cn/taketoday/core/ssl/pem/key2-chain.crt")
            .withPrivateKey("classpath:cn/taketoday/core/ssl/pem/key2.pem");
    PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, null, "test-alias", "keysecret", true);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
  }

  @Test
  void shouldFailIfVerifyKeysIsEnabledAndKeysDontMatch() {
    PemSslStoreDetails keyStoreDetails = PemSslStoreDetails
            .forCertificate("classpath:cn/taketoday/core/ssl/pem/key2.crt")
            .withPrivateKey("classpath:cn/taketoday/core/ssl/pem/key1.pem");
    assertThatIllegalStateException()
            .isThrownBy(() -> new PemSslStoreBundle(keyStoreDetails, null, null, null, true))
            .withMessageContaining("Private key matches none of the certificates");
  }

  private Consumer<KeyStore> storeContainingCert(String keyAlias) {
    return storeContainingCert(KeyStore.getDefaultType(), keyAlias);
  }

  private Consumer<KeyStore> storeContainingCert(String keyStoreType, String keyAlias) {
    return ThrowingConsumer.of((keyStore) -> {
      assertThat(keyStore).isNotNull();
      assertThat(keyStore.getType()).isEqualTo(keyStoreType);
      assertThat(keyStore.containsAlias(keyAlias)).isTrue();
      assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
      assertThat(keyStore.getKey(keyAlias, EMPTY_KEY_PASSWORD)).isNull();
    });
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias) {
    return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias);
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias) {
    return storeContainingCertAndKey(keyStoreType, keyAlias, EMPTY_KEY_PASSWORD);
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias, char[] keyPassword) {
    return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias, keyPassword);
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias, char[] keyPassword) {
    return ThrowingConsumer.of((keyStore) -> {
      assertThat(keyStore).isNotNull();
      assertThat(keyStore.getType()).isEqualTo(keyStoreType);
      assertThat(keyStore.containsAlias(keyAlias)).isTrue();
      assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
      assertThat(keyStore.getKey(keyAlias, keyPassword)).isNotNull();
    });
  }

}
