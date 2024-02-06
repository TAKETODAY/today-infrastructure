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

package cn.taketoday.annotation.config.ssl;

import org.junit.jupiter.api.Test;

import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Set;
import java.util.function.Consumer;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PropertiesSslBundle}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class PropertiesSslBundleTests {

  private static final char[] EMPTY_KEY_PASSWORD = new char[] {};

  @Test
  void pemPropertiesAreMappedToSslBundle() throws Exception {
    PemSslBundleProperties properties = new PemSslBundleProperties();
    properties.getKey().setAlias("alias");
    properties.getKey().setPassword("secret");
    properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
    properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
    properties.getKeystore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/rsa-cert.pem");
    properties.getKeystore().setPrivateKey("classpath:cn/taketoday/annotation/config/ssl/rsa-key.pem");
    properties.getKeystore().setPrivateKeyPassword(null);
    properties.getKeystore().setType("PKCS12");
    properties.getTruststore()
            .setCertificate("classpath:cn/taketoday/annotation/config/ssl/ed25519-cert.pem");
    properties.getTruststore()
            .setPrivateKey("classpath:cn/taketoday/annotation/config/ssl/ed25519-key.pem");
    properties.getTruststore().setPrivateKeyPassword("secret");
    properties.getTruststore().setType("PKCS12");
    SslBundle sslBundle = PropertiesSslBundle.get(properties);
    assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
    assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
    assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
    assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
    assertThat(sslBundle.getStores()).isNotNull();
    Certificate certificate = sslBundle.getStores().getKeyStore().getCertificate("alias");
    assertThat(certificate).isNotNull();
    assertThat(certificate.getType()).isEqualTo("X.509");
    Key key = sslBundle.getStores().getKeyStore().getKey("alias", "secret".toCharArray());
    assertThat(key).isNotNull();
    assertThat(key.getAlgorithm()).isEqualTo("RSA");
    certificate = sslBundle.getStores().getTrustStore().getCertificate("ssl");
    assertThat(certificate).isNotNull();
    assertThat(certificate.getType()).isEqualTo("X.509");
  }

  @Test
  void jksPropertiesAreMappedToSslBundle() {
    JksSslBundleProperties properties = new JksSslBundleProperties();
    properties.getKey().setAlias("alias");
    properties.getKey().setPassword("secret");
    properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
    properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
    properties.getKeystore().setPassword("secret");
    properties.getKeystore().setProvider("SUN");
    properties.getKeystore().setType("JKS");
    properties.getKeystore().setLocation("classpath:cn/taketoday/annotation/config/ssl/keystore.jks");
    properties.getTruststore().setPassword("secret");
    properties.getTruststore().setProvider("SUN");
    properties.getTruststore().setType("PKCS12");
    properties.getTruststore().setLocation("classpath:cn/taketoday/annotation/config/ssl/keystore.pkcs12");
    SslBundle sslBundle = PropertiesSslBundle.get(properties);
    assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
    assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
    assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
    assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
    assertThat(sslBundle.getStores()).isNotNull();
    assertThat(sslBundle.getStores()).extracting("keyStoreDetails")
            .extracting("location", "password", "provider", "type")
            .containsExactly("classpath:cn/taketoday/annotation/config/ssl/keystore.jks", "secret", "SUN",
                    "JKS");
    KeyStore trustStore = sslBundle.getStores().getTrustStore();
    assertThat(trustStore.getType()).isEqualTo("PKCS12");
    assertThat(trustStore.getProvider().getName()).isEqualTo("SUN");
  }

  @Test
  void getWithPemSslBundlePropertiesWhenVerifyKeyStoreAgainstSingleCertificateWithMatchCreatesBundle() {
    PemSslBundleProperties properties = new PemSslBundleProperties();
    properties.getKeystore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/key1.crt");
    properties.getKeystore().setPrivateKey("classpath:cn/taketoday/annotation/config/ssl/key1.pem");
    properties.getKeystore().setVerifyKeys(true);
    properties.getKey().setAlias("test-alias");
    SslBundle bundle = PropertiesSslBundle.get(properties);
    assertThat(bundle.getStores().getKeyStore()).satisfies(storeContainingCertAndKey("test-alias"));
  }

  @Test
  void getWithPemSslBundlePropertiesWhenVerifyKeyStoreAgainstCertificateChainWithMatchCreatesBundle() {
    PemSslBundleProperties properties = new PemSslBundleProperties();
    properties.getKeystore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/key2-chain.crt");
    properties.getKeystore().setPrivateKey("classpath:cn/taketoday/annotation/config/ssl/key2.pem");
    properties.getKeystore().setVerifyKeys(true);
    properties.getKey().setAlias("test-alias");
    SslBundle bundle = PropertiesSslBundle.get(properties);
    assertThat(bundle.getStores().getKeyStore()).satisfies(storeContainingCertAndKey("test-alias"));
  }

  @Test
  void getWithPemSslBundlePropertiesWhenVerifyKeyStoreWithNoMatchThrowsException() {
    PemSslBundleProperties properties = new PemSslBundleProperties();
    properties.getKeystore().setCertificate("classpath:cn/taketoday/annotation/config/ssl/key2.crt");
    properties.getKeystore().setPrivateKey("classpath:cn/taketoday/annotation/config/ssl/key1.pem");
    properties.getKeystore().setVerifyKeys(true);
    properties.getKey().setAlias("test-alias");
    assertThatIllegalStateException().isThrownBy(() -> PropertiesSslBundle.get(properties))
            .withMessageContaining("Private key in keystore matches none of the certificates");
  }

  private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias) {
    return ThrowingConsumer.of((keyStore) -> {
      assertThat(keyStore).isNotNull();
      assertThat(keyStore.getType()).isEqualTo(KeyStore.getDefaultType());
      assertThat(keyStore.containsAlias(keyAlias)).isTrue();
      assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
      assertThat(keyStore.getKey(keyAlias, EMPTY_KEY_PASSWORD)).isNotNull();
    });
  }

}
