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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.framework.web.MockPkcs11Security;
import cn.taketoday.framework.web.MockPkcs11SecurityProvider;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServerSslBundle;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.web.servlet.DirtiesUrlFactories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link SslConnectorCustomizer}
 *
 * @author Brian Clozel
 */

@MockPkcs11Security
@DirtiesUrlFactories
@ExtendWith(OutputCaptureExtension.class)
class SslConnectorCustomizerTests {
  private final Logger logger = LoggerFactory.getLogger(SslConnectorCustomizerTests.class);

  private Tomcat tomcat;

  @BeforeEach
  void setup() {
    this.tomcat = new Tomcat();
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setPort(0);
    this.tomcat.setConnector(connector);
  }

  @AfterEach
  void stop() throws Exception {
    System.clearProperty("javax.net.ssl.trustStorePassword");
    this.tomcat.stop();
  }

  @Test
  void sslCiphersConfiguration() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyStore("classpath:test.jks");
    ssl.setKeyStorePassword("secret");
    ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
    Connector connector = this.tomcat.getConnector();
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, connector, ssl.getClientAuth());
    customizer.customize(WebServerSslBundle.get(ssl));
    this.tomcat.start();
    SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler().findSslHostConfigs();
    assertThat(sslHostConfigs[0].getCiphers()).isEqualTo("ALPHA:BRAVO:CHARLIE");
  }

  @Test
  void sslEnabledMultipleProtocolsConfiguration() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
    ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
    Connector connector = this.tomcat.getConnector();
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, connector, ssl.getClientAuth());
    customizer.customize(WebServerSslBundle.get(ssl));
    this.tomcat.start();
    SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
    assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
    assertThat(sslHostConfig.getEnabledProtocols()).containsExactlyInAnyOrder("TLSv1.1", "TLSv1.2");
  }

  @Test
  void sslEnabledProtocolsConfiguration() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setEnabledProtocols(new String[] { "TLSv1.2" });
    ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });
    Connector connector = this.tomcat.getConnector();
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, connector, ssl.getClientAuth());
    customizer.customize(WebServerSslBundle.get(ssl));
    this.tomcat.start();
    SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
    assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
    assertThat(sslHostConfig.getEnabledProtocols()).containsExactly("TLSv1.2");
  }

  @Test
  void customizeWhenSslIsEnabledWithNoKeyStoreAndNotPkcs11ThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> {
      SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, this.tomcat.getConnector(), Ssl.ClientAuth.NONE);
      customizer.customize(WebServerSslBundle.get(new Ssl()));
    }).withMessageContaining("SSL is enabled but no trust material is configured");
  }

  @Test
  void customizeWhenSslIsEnabledWithPkcs11AndKeyStoreThrowsException() {
    Ssl ssl = new Ssl();
    ssl.setKeyStoreType("PKCS11");
    ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
    ssl.setKeyStore("src/test/resources/test.jks");
    ssl.setKeyPassword("password");
    assertThatIllegalStateException().isThrownBy(() -> {
      SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, this.tomcat.getConnector(), ssl.getClientAuth());
      customizer.customize(WebServerSslBundle.get(ssl));
    }).withMessageContaining("must be empty or null for PKCS11 hardware key stores");
  }

  @Test
  void customizeWhenSslIsEnabledWithPkcs11AndKeyStoreProvider() {
    Ssl ssl = new Ssl();
    ssl.setKeyStoreType("PKCS11");
    ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
    ssl.setKeyStorePassword("1234");
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(logger, this.tomcat.getConnector(), ssl.getClientAuth());
    assertThatNoException().isThrownBy(() -> customizer.customize(WebServerSslBundle.get(ssl)));
  }

  private KeyStore loadStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    Resource resource = new ClassPathResource("test.jks");
    try (InputStream stream = resource.getInputStream()) {
      keyStore.load(stream, "secret".toCharArray());
      return keyStore;
    }
  }

}
