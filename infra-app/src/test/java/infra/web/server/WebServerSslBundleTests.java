/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.server;

import org.junit.jupiter.api.Test;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslBundleKey;
import infra.core.ssl.SslOptions;
import infra.core.ssl.SslStoreBundle;
import infra.web.MockPkcs11Security;
import infra.web.MockPkcs11SecurityProvider;
import infra.web.server.WebServerSslBundle.WebServerSslStoreBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
    ssl.enabled = (false);
    assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
            .withMessage("SSL is not enabled");
  }

  @Test
  void whenFromJksProperties() {
    Ssl ssl = new Ssl();
    ssl.keyStore = ("classpath:test.p12");
    ssl.keyStorePassword = ("secret");
    ssl.keyStoreType = ("PKCS12");
    ssl.trustStore = ("classpath:test.p12");
    ssl.trustStorePassword = ("secret");
    ssl.trustStoreType = ("PKCS12");
    ssl.keyPassword = ("password");
    ssl.keyAlias = ("alias");
    ssl.clientAuth = (Ssl.ClientAuth.NONE);
    ssl.ciphers = new String[] { "ONE", "TWO", "THREE" };
    ssl.enabledProtocols = new String[] { "TLSv1.1", "TLSv1.2" };
    ssl.protocol = ("TestProtocol");
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
    ssl.keyStoreType = ("PKCS11");
    ssl.keyStoreProvider = (MockPkcs11SecurityProvider.NAME);
    ssl.keyStore = ("src/test/resources/test.jks");
    ssl.keyPassword = ("password");
    ssl.clientAuth = (Ssl.ClientAuth.NONE);
    assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
            .withMessageContaining("must be empty or null for PKCS11 hardware key stores");
  }

  @Test
  void whenFromPkcs11Properties() {
    Ssl ssl = new Ssl();
    ssl.keyStoreType = ("PKCS11");
    ssl.keyStoreProvider = (MockPkcs11SecurityProvider.NAME);
    ssl.trustStoreType = ("PKCS11");
    ssl.trustStoreProvider = (MockPkcs11SecurityProvider.NAME);
    ssl.keyPassword = ("password");
    ssl.clientAuth = (Ssl.ClientAuth.NONE);
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
    ssl.certificate = ("classpath:test-cert.pem");
    ssl.certificatePrivateKey = ("classpath:test-key.pem");
    ssl.trustCertificate = ("classpath:test-cert-chain.pem");
    ssl.keyStoreType = ("PKCS12");
    ssl.trustStoreType = ("PKCS12");
    ssl.keyPassword = ("password");
    ssl.clientAuth = (Ssl.ClientAuth.NONE);
    ssl.ciphers = new String[] { "ONE", "TWO", "THREE" };
    ssl.enabledProtocols = new String[] { "TLSv1.1", "TLSv1.2" };
    ssl.protocol = ("TLSv1.1");
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
    ssl.certificate = ("classpath:test-cert.pem");
    ssl.certificatePrivateKey = ("classpath:test-key.pem");
    ssl.keyStoreType = ("PKCS12");
    ssl.keyPassword = ("password");
    ssl.trustStore = ("classpath:test.p12");
    ssl.trustStorePassword = ("secret");
    ssl.trustStoreType = ("PKCS12");
    ssl.clientAuth = (Ssl.ClientAuth.NONE);
    ssl.ciphers = (new String[] { "ONE", "TWO", "THREE" });
    ssl.enabledProtocols = (new String[] { "TLSv1.1", "TLSv1.2" });
    ssl.protocol = ("TLSv1.1");
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
    ssl.keyStore = ("classpath:test.p12");
    ssl.keyStoreType = ("PKCS12");
    ssl.keyPassword = ("password");
    ssl.trustCertificate = ("classpath:test-cert-chain.pem");
    ssl.trustStorePassword = ("secret");
    ssl.trustStoreType = ("PKCS12");
    ssl.clientAuth = (Ssl.ClientAuth.NONE);
    ssl.ciphers = (new String[] { "ONE", "TWO", "THREE" });
    ssl.enabledProtocols = (new String[] { "TLSv1.1", "TLSv1.2" });
    ssl.protocol = ("TLSv1.1");
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
  void getWithNullSslThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(null))
            .withMessage("SSL is not enabled");
  }

  @Test
  void getWithBundleNameAndNullSslBundlesThrowsException() {
    Ssl ssl = new Ssl();
    ssl.bundle = "test-bundle";

    assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl, null))
            .withMessage("SSL bundle 'test-bundle' was requested but no SslBundles instance was provided");
  }

  @Test
  void getWithValidSslBundleReturnsBundle() {
    Ssl ssl = new Ssl();
    ssl.keyStore = "classpath:test.p12";
    ssl.keyStorePassword = "secret";
    ssl.keyPassword = "password";

    SslBundle bundle = WebServerSslBundle.get(ssl);

    assertThat(bundle).isNotNull();
    assertThat(bundle.getKey().getPassword()).isEqualTo("password");
    assertThat(bundle.getStores().getKeyStorePassword()).isEqualTo("secret");
  }

  @Test
  void hasPemKeyStorePropertiesReturnsTrueWhenCertificateAndPrivateKeyAreSet() {
    Ssl ssl = new Ssl();
    ssl.certificate = "classpath:test-cert.pem";
    ssl.certificatePrivateKey = "classpath:test-key.pem";

    boolean result = WebServerSslBundle.hasPemKeyStoreProperties(ssl);

    assertThat(result).isTrue();
  }

  @Test
  void hasPemKeyStorePropertiesReturnsFalseWhenCertificateOrPrivateKeyIsNull() {
    Ssl ssl1 = new Ssl();
    ssl1.certificate = null;
    ssl1.certificatePrivateKey = "classpath:test-key.pem";

    Ssl ssl2 = new Ssl();
    ssl2.certificate = "classpath:test-cert.pem";
    ssl2.certificatePrivateKey = null;

    boolean result1 = WebServerSslBundle.hasPemKeyStoreProperties(ssl1);
    boolean result2 = WebServerSslBundle.hasPemKeyStoreProperties(ssl2);

    assertThat(result1).isFalse();
    assertThat(result2).isFalse();
  }

  @Test
  void hasPemTrustStorePropertiesReturnsTrueWhenTrustCertificateIsSet() {
    Ssl ssl = new Ssl();
    ssl.trustCertificate = "classpath:test-cert.pem";

    boolean result = WebServerSslBundle.hasPemTrustStoreProperties(ssl);

    assertThat(result).isTrue();
  }

  @Test
  void hasPemTrustStorePropertiesReturnsFalseWhenTrustCertificateIsNull() {
    Ssl ssl = new Ssl();
    ssl.trustCertificate = null;

    boolean result = WebServerSslBundle.hasPemTrustStoreProperties(ssl);

    assertThat(result).isFalse();
  }

  @Test
  void hasJksKeyStorePropertiesReturnsTrueWhenKeyStoreIsSet() {
    Ssl ssl = new Ssl();
    ssl.keyStore = "classpath:test.p12";

    boolean result = WebServerSslBundle.hasJksKeyStoreProperties(ssl);

    assertThat(result).isTrue();
  }

  @Test
  void hasJksKeyStorePropertiesReturnsTrueWhenKeyStoreTypeIsPkcs11() {
    Ssl ssl = new Ssl();
    ssl.keyStoreType = "PKCS11";

    boolean result = WebServerSslBundle.hasJksKeyStoreProperties(ssl);

    assertThat(result).isTrue();
  }

  @Test
  void hasJksTrustStorePropertiesReturnsTrueWhenTrustStoreIsSet() {
    Ssl ssl = new Ssl();
    ssl.trustStore = "classpath:test.p12";

    boolean result = WebServerSslBundle.hasJksTrustStoreProperties(ssl);

    assertThat(result).isTrue();
  }

  @Test
  void hasJksTrustStorePropertiesReturnsTrueWhenTrustStoreTypeIsPkcs11() {
    Ssl ssl = new Ssl();
    ssl.trustStoreType = "PKCS11";

    boolean result = WebServerSslBundle.hasJksTrustStoreProperties(ssl);

    assertThat(result).isTrue();
  }

  @Test
  void createStoreBundleCreatesBundleWithKeyStoreAndTrustStore() {
    Ssl ssl = new Ssl();
    ssl.keyStore = "classpath:test.p12";
    ssl.keyStorePassword = "secret";
    ssl.trustStore = "classpath:test.p12";
    ssl.trustStorePassword = "secret";

    // This test verifies that the method doesn't throw exceptions
    assertThatNoException().isThrownBy(() -> {
      SslStoreBundle bundle = WebServerSslBundle.createStoreBundle(ssl);
      assertThat(bundle).isNotNull();
      assertThat(bundle.getKeyStore()).isNotNull();
      assertThat(bundle.getTrustStore()).isNotNull();
    });
  }

  @Test
  void webServerSslStoreBundleConstructorThrowsExceptionWhenNoStores() {
    assertThatIllegalStateException().isThrownBy(() ->
                    new WebServerSslStoreBundle(null, null, null))
            .withMessage("SSL is enabled but no trust material is configured");
  }

  @Test
  void webServerSslStoreBundleReturnsCorrectValues() {
    Ssl ssl = new Ssl();
    ssl.keyStore = "classpath:test.p12";
    ssl.keyStorePassword = "secret";
    ssl.trustStore = "classpath:test.p12";

    SslStoreBundle storeBundle = WebServerSslBundle.createStoreBundle(ssl);
    WebServerSslStoreBundle bundle =
            new WebServerSslStoreBundle(
                    storeBundle.getKeyStore(),
                    storeBundle.getTrustStore(),
                    ssl.keyStorePassword);

    assertThat(bundle.getKeyStore()).isNotNull();
    assertThat(bundle.getTrustStore()).isNotNull();
    assertThat(bundle.getKeyStorePassword()).isEqualTo("secret");
  }

  @Test
  void toStringReturnsFormattedString() {
    Ssl ssl = new Ssl();
    ssl.keyStore = "classpath:test.p12";
    ssl.keyStorePassword = "secret";
    ssl.keyPassword = "password";
    ssl.protocol = "TLS";

    SslBundle bundle = WebServerSslBundle.get(ssl);

    String result = bundle.toString();
    assertThat(result).contains("WebServerSslBundle");
    assertThat(result).contains("protocol = 'TLS'");
  }

}
