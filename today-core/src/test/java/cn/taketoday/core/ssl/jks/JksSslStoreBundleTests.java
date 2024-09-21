/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core.ssl.jks;

import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.util.function.Consumer;

import cn.taketoday.core.ssl.MockPkcs11Security;
import cn.taketoday.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JksSslStoreBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
@MockPkcs11Security
class JksSslStoreBundleTests {

  @Test
  void whenNullStores() {
    JksSslStoreDetails keyStoreDetails = null;
    JksSslStoreDetails trustStoreDetails = null;
    JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).isNull();
    assertThat(bundle.getKeyStorePassword()).isNull();
    assertThat(bundle.getTrustStore()).isNull();
  }

  @Test
  void whenStoresHaveNoValues() {
    JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation(null);
    JksSslStoreDetails trustStoreDetails = JksSslStoreDetails.forLocation(null);
    JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).isNull();
    assertThat(bundle.getKeyStorePassword()).isNull();
    assertThat(bundle.getTrustStore()).isNull();
  }

  @Test
  void whenTypePKCS11AndLocationGetKeyStoreThrowsException() {
    JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails("PKCS11", null, "test.jks", null);
    JksSslStoreBundle jksSslStoreBundle = new JksSslStoreBundle(keyStoreDetails, null);
    assertThatIllegalStateException().isThrownBy(jksSslStoreBundle::getKeyStore)
            .withMessageContaining(
                    "Unable to create key store: Location is 'test.jks', but must be empty or null for PKCS11 hardware key stores");
  }

  @Test
  void whenHasKeyStoreLocation() {
    JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation("classpath:test.jks")
            .withPassword("secret");
    JksSslStoreDetails trustStoreDetails = null;
    JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "password"));
  }

  @Test
  void getTrustStoreWithLocations() {
    JksSslStoreDetails keyStoreDetails = null;
    JksSslStoreDetails trustStoreDetails = JksSslStoreDetails.forLocation("classpath:test.jks")
            .withPassword("secret");
    JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("test-alias", "password"));
  }

  @Test
  void whenHasKeyStoreType() {
    JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails("jks", null, "classpath:test.jks", "secret");
    JksSslStoreDetails trustStoreDetails = null;
    JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("jks", "test-alias", "password"));
  }

  @Test
  void whenHasTrustStoreType() {
    JksSslStoreDetails keyStoreDetails = null;
    JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails("jks", null, "classpath:test.jks", "secret");
    JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
    assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("jks", "test-alias", "password"));
  }

  @Test
  void whenHasKeyStoreProvider() {
    JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, "com.example.KeyStoreProvider",
            "classpath:test.jks", "secret");
    JksSslStoreBundle jksSslStoreBundle = new JksSslStoreBundle(keyStoreDetails, null);
    assertThatIllegalStateException().isThrownBy(jksSslStoreBundle::getKeyStore)
            .withMessageContaining("com.example.KeyStoreProvider");
  }

  @Test
  void whenHasTrustStoreProvider() {
    JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails(null, "com.example.KeyStoreProvider",
            "classpath:test.jks", "secret");
    JksSslStoreBundle jksSslStoreBundle = new JksSslStoreBundle(null, trustStoreDetails);
    assertThatIllegalStateException().isThrownBy(jksSslStoreBundle::getTrustStore)
            .withMessageContaining("com.example.KeyStoreProvider");
  }

  @Test
  void storeCreationIsLazy() {
    JksSslStoreDetails details = new JksSslStoreDetails(null, null, "does-not-exist", null);
    JksSslStoreBundle bundle = new JksSslStoreBundle(details, details);
    assertThatIllegalStateException().isThrownBy(bundle::getKeyStore);
    assertThatIllegalStateException().isThrownBy(bundle::getTrustStore);
  }

  @Test
  void invalidLocationThrowsException() {
    JksSslStoreDetails trustStoreDetails = JksSslStoreDetails.forLocation("does-not-exist.p12");
    JksSslStoreBundle jksSslStoreBundle = new JksSslStoreBundle(null, trustStoreDetails);
    assertThatIllegalStateException().isThrownBy(jksSslStoreBundle::getTrustStore)
            .withMessageContaining("trust store")
            .withMessageContaining("does-not-exist.p12");
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias, String keyPassword) {
    return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias, keyPassword);
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias, String keyPassword) {
    return ThrowingConsumer.of((keyStore) -> {
      assertThat(keyStore).isNotNull();
      assertThat(keyStore.getType()).isEqualTo(keyStoreType);
      assertThat(keyStore.containsAlias(keyAlias)).isTrue();
      assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
      assertThat(keyStore.getKey(keyAlias, keyPassword.toCharArray())).isNotNull();
    });
  }

}
