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

package cn.taketoday.framework.web.server;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.framework.web.MockPkcs11Security;
import cn.taketoday.framework.web.MockPkcs11SecurityProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link WebServerSslBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
@MockPkcs11Security
class WebServerSslBundleTests {

  @Test
  void whenSslDisabledThrowsException() {
    Ssl ssl = new Ssl();
    ssl.setEnabled(false);
    assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
            .withMessage("SSL is not enabled");
  }

  @Test
  void whenFromJksProperties() {
    Ssl ssl = new Ssl();
    ssl.setKeyStore("classpath:test.p12");
    ssl.setKeyStorePassword("secret");
    ssl.setKeyStoreType("PKCS12");
    ssl.setTrustStore("classpath:test.p12");
    ssl.setTrustStorePassword("secret");
    ssl.setTrustStoreType("PKCS12");
    ssl.setKeyPassword("password");
    ssl.setKeyAlias("alias");
    ssl.setClientAuth(Ssl.ClientAuth.NONE);
    ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
    ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
    ssl.setProtocol("TestProtocol");
    SslBundle bundle = WebServerSslBundle.get(ssl);
    assertThat(bundle).isNotNull();
    assertThat(bundle.getProtocol()).isEqualTo("TestProtocol");
    SslBundleKey key = bundle.getKey();
    assertThat(key.getPassword()).isEqualTo("password");
    assertThat(key.getAlias()).isEqualTo("alias");
    SslStoreBundle stores = bundle.getStores();
    assertThat(stores.getKeyStorePassword()).isEqualTo("secret");
    assertThat(stores.getKeyStore()).isNotNull();
    assertThat(stores.getTrustStore()).isNotNull();
    SslOptions options = bundle.getOptions();
    assertThat(options.getCiphers()).containsExactly("ONE", "TWO", "THREE");
    assertThat(options.getEnabledProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
  }

  @Test
  void whenFromJksPropertiesWithPkcs11StoreType() {
    Ssl ssl = new Ssl();
    ssl.setKeyStoreType("PKCS11");
    ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setKeyPassword("password");
    ssl.setClientAuth(Ssl.ClientAuth.NONE);
    assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
            .withMessageContaining("must be empty or null for PKCS11 hardware key stores");
  }

  @Test
  void whenFromPkcs11Properties() {
    Ssl ssl = new Ssl();
    ssl.setKeyStoreType("PKCS11");
    ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
    ssl.setTrustStoreType("PKCS11");
    ssl.setTrustStoreProvider(MockPkcs11SecurityProvider.NAME);
    ssl.setKeyPassword("password");
    ssl.setClientAuth(Ssl.ClientAuth.NONE);
    SslBundle bundle = WebServerSslBundle.get(ssl);
    assertThat(bundle).isNotNull();
    assertThat(bundle.getProtocol()).isEqualTo("TLS");
    SslBundleKey key = bundle.getKey();
    assertThat(key.getPassword()).isEqualTo("password");
    SslStoreBundle stores = bundle.getStores();
    assertThat(stores.getKeyStore()).isNotNull();
    assertThat(stores.getTrustStore()).isNotNull();
  }

  @Test
  void whenFromPemProperties() {
    Ssl ssl = new Ssl();
    ssl.setCertificate("classpath:test-cert.pem");
    ssl.setCertificatePrivateKey("classpath:test-key.pem");
    ssl.setTrustCertificate("classpath:test-cert-chain.pem");
    ssl.setKeyStoreType("PKCS12");
    ssl.setTrustStoreType("PKCS12");
    ssl.setKeyPassword("password");
    ssl.setClientAuth(Ssl.ClientAuth.NONE);
    ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
    ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
    ssl.setProtocol("TLSv1.1");
    SslBundle bundle = WebServerSslBundle.get(ssl);
    assertThat(bundle).isNotNull();
    SslBundleKey key = bundle.getKey();
    assertThat(key.getAlias()).isNull();
    assertThat(key.getPassword()).isEqualTo("password");
    SslStoreBundle stores = bundle.getStores();
    assertThat(stores.getKeyStorePassword()).isNull();
    assertThat(stores.getKeyStore()).isNotNull();
    assertThat(stores.getTrustStore()).isNotNull();
    SslOptions options = bundle.getOptions();
    assertThat(options.getCiphers()).containsExactly("ONE", "TWO", "THREE");
    assertThat(options.getEnabledProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
  }

  @Test
  void whenMissingPropertiesThrowsException() {
    Ssl ssl = new Ssl();
    assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
            .withMessageContaining("SSL is enabled but no trust material is configured");
  }

  @Test
  void whenPemKeyStoreAndJksTrustStoreProperties() {
    Ssl ssl = new Ssl();
    ssl.setCertificate("classpath:test-cert.pem");
    ssl.setCertificatePrivateKey("classpath:test-key.pem");
    ssl.setKeyStoreType("PKCS12");
    ssl.setKeyPassword("password");
    ssl.setTrustStore("classpath:test.p12");
    ssl.setTrustStorePassword("secret");
    ssl.setTrustStoreType("PKCS12");
    ssl.setClientAuth(Ssl.ClientAuth.NONE);
    ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
    ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
    ssl.setProtocol("TLSv1.1");
    SslBundle bundle = WebServerSslBundle.get(ssl);
    assertThat(bundle).isNotNull();
    SslBundleKey key = bundle.getKey();
    assertThat(key.getAlias()).isNull();
    assertThat(key.getPassword()).isEqualTo("password");
    SslStoreBundle stores = bundle.getStores();
    assertThat(stores.getKeyStorePassword()).isNull();
    assertThat(stores.getKeyStore()).isNotNull();
    assertThat(stores.getTrustStore()).isNotNull();
    SslOptions options = bundle.getOptions();
    assertThat(options.getCiphers()).containsExactly("ONE", "TWO", "THREE");
    assertThat(options.getEnabledProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
  }

  @Test
  void whenJksKeyStoreAndPemTrustStoreProperties() {
    Ssl ssl = new Ssl();
    ssl.setKeyStore("classpath:test.p12");
    ssl.setKeyStoreType("PKCS12");
    ssl.setKeyPassword("password");
    ssl.setTrustCertificate("classpath:test-cert-chain.pem");
    ssl.setTrustStorePassword("secret");
    ssl.setTrustStoreType("PKCS12");
    ssl.setClientAuth(Ssl.ClientAuth.NONE);
    ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
    ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
    ssl.setProtocol("TLSv1.1");
    SslBundle bundle = WebServerSslBundle.get(ssl);
    assertThat(bundle).isNotNull();
    SslBundleKey key = bundle.getKey();
    assertThat(key.getAlias()).isNull();
    assertThat(key.getPassword()).isEqualTo("password");
    SslStoreBundle stores = bundle.getStores();
    assertThat(stores.getKeyStorePassword()).isNull();
    assertThat(stores.getKeyStore()).isNotNull();
    assertThat(stores.getTrustStore()).isNotNull();
    SslOptions options = bundle.getOptions();
    assertThat(options.getCiphers()).containsExactly("ONE", "TWO", "THREE");
    assertThat(options.getEnabledProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
  }

}
