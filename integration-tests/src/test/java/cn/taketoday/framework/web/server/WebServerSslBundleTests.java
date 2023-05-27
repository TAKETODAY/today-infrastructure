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

package cn.taketoday.framework.web.server;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.KeyStore;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.core.ssl.SslStoreBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link WebServerSslBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
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
    ssl.setKeyStorePassword("secret");
    ssl.setKeyStoreType("PKCS11");
    ssl.setKeyPassword("password");
    ssl.setClientAuth(Ssl.ClientAuth.NONE);
    SslBundle bundle = WebServerSslBundle.get(ssl);
    assertThat(bundle).isNotNull();
    assertThat(bundle.getStores().getKeyStorePassword()).isEqualTo("secret");
    assertThat(bundle.getKey().getPassword()).isEqualTo("password");
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

  private KeyStore loadStore() throws Exception {
    Resource resource = new ClassPathResource("test.p12");
    try (InputStream stream = resource.getInputStream()) {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(stream, "secret".toCharArray());
      return keyStore;
    }
  }

}
