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

import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CertificateFileSslStoreProvider}.
 *
 * @author Scott Frederick
 */
class CertificateFileSslStoreProviderTests {

  @Test
  void fromSslWhenNullReturnsNull() {
    assertThat(CertificateFileSslStoreProvider.from(null)).isNull();
  }

  @Test
  void fromSslWhenDisabledReturnsNull() {
    assertThat(CertificateFileSslStoreProvider.from(new Ssl())).isNull();
  }

  @Test
  void fromSslWithCertAndKeyReturnsStoreProvider() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setEnabled(true);
    ssl.setCertificate("classpath:test-cert.pem");
    ssl.setCertificatePrivateKey("classpath:test-key.pem");
    SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
    assertThat(storeProvider).isNotNull();
    assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "today-web");
    assertThat(storeProvider.getTrustStore()).isNull();
  }

  @Test
  void fromSslWithCertAndKeyAndTrustCertReturnsStoreProvider() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setEnabled(true);
    ssl.setCertificate("classpath:test-cert.pem");
    ssl.setCertificatePrivateKey("classpath:test-key.pem");
    ssl.setTrustCertificate("classpath:test-cert.pem");
    SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
    assertThat(storeProvider).isNotNull();
    assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "today-web");
    assertStoreContainsCert(storeProvider.getTrustStore(), KeyStore.getDefaultType(), "today-web-0");
  }

  @Test
  void fromSslWithCertAndKeyAndTrustCertAndTrustKeyReturnsStoreProvider() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setEnabled(true);
    ssl.setCertificate("classpath:test-cert.pem");
    ssl.setCertificatePrivateKey("classpath:test-key.pem");
    ssl.setTrustCertificate("classpath:test-cert.pem");
    ssl.setTrustCertificatePrivateKey("classpath:test-key.pem");
    SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
    assertThat(storeProvider).isNotNull();
    assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "today-web");
    assertStoreContainsCertAndKey(storeProvider.getTrustStore(), KeyStore.getDefaultType(), "today-web");
  }

  @Test
  void fromSslWithKeyAliasReturnsStoreProvider() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setEnabled(true);
    ssl.setKeyAlias("test-alias");
    ssl.setCertificate("classpath:test-cert.pem");
    ssl.setCertificatePrivateKey("classpath:test-key.pem");
    ssl.setTrustCertificate("classpath:test-cert.pem");
    ssl.setTrustCertificatePrivateKey("classpath:test-key.pem");
    SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
    assertThat(storeProvider).isNotNull();
    assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "test-alias");
    assertStoreContainsCertAndKey(storeProvider.getTrustStore(), KeyStore.getDefaultType(), "test-alias");
  }

  @Test
  void fromSslWithStoreTypeReturnsStoreProvider() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setEnabled(true);
    ssl.setKeyStoreType("PKCS12");
    ssl.setTrustStoreType("PKCS12");
    ssl.setCertificate("classpath:test-cert.pem");
    ssl.setCertificatePrivateKey("classpath:test-key.pem");
    ssl.setTrustCertificate("classpath:test-cert.pem");
    ssl.setTrustCertificatePrivateKey("classpath:test-key.pem");
    SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
    assertThat(storeProvider).isNotNull();
    assertStoreContainsCertAndKey(storeProvider.getKeyStore(), "PKCS12", "today-web");
    assertStoreContainsCertAndKey(storeProvider.getTrustStore(), "PKCS12", "today-web");
  }

  private void assertStoreContainsCertAndKey(KeyStore keyStore, String keyStoreType, String keyAlias)
          throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
    assertThat(keyStore).isNotNull();
    assertThat(keyStore.getType()).isEqualTo(keyStoreType);
    assertThat(keyStore.containsAlias(keyAlias)).isTrue();
    assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
    assertThat(keyStore.getKey(keyAlias, new char[] {})).isNotNull();
  }

  private void assertStoreContainsCert(KeyStore keyStore, String keyStoreType, String keyAlias)
          throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
    assertThat(keyStore).isNotNull();
    assertThat(keyStore.getType()).isEqualTo(keyStoreType);
    assertThat(keyStore.containsAlias(keyAlias)).isTrue();
    assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
    assertThat(keyStore.getKey(keyAlias, new char[] {})).isNull();
  }

}
