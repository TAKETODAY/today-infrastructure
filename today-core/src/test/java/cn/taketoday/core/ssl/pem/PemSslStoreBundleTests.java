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

import java.security.KeyStore;
import java.util.function.Consumer;

import cn.taketoday.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemSslStoreBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class PemSslStoreBundleTests {

  @Test
  void whenNullStores() {
    PemSslStoreBundle bundle = new PemSslStoreBundle(null, null);
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

  private Consumer<KeyStore> storeContainingCert(String keyAlias) {
    return storeContainingCert(KeyStore.getDefaultType(), keyAlias);
  }

  private Consumer<KeyStore> storeContainingCert(String keyStoreType, String keyAlias) {
    return ThrowingConsumer.of((keyStore) -> {
      assertThat(keyStore).isNotNull();
      assertThat(keyStore.getType()).isEqualTo(keyStoreType);
      assertThat(keyStore.containsAlias(keyAlias)).isTrue();
      assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
      assertThat(keyStore.getKey(keyAlias, new char[] {})).isNull();
    });
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias) {
    return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias);
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias) {
    return ThrowingConsumer.of((keyStore) -> {
      assertThat(keyStore).isNotNull();
      assertThat(keyStore.getType()).isEqualTo(keyStoreType);
      assertThat(keyStore.containsAlias(keyAlias)).isTrue();
      assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
      assertThat(keyStore.getKey(keyAlias, new char[] {})).isNotNull();
    });
  }

}
