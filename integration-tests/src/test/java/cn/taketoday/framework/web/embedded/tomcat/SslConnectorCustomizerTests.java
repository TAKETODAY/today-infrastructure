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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.testsupport.system.CapturedOutput;
import cn.taketoday.framework.testsupport.system.OutputCaptureExtension;
import cn.taketoday.framework.web.embedded.tomcat.SslConnectorCustomizer;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.SslStoreProvider;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslConnectorCustomizer}
 *
 * @author Brian Clozel
 */
@ExtendWith(OutputCaptureExtension.class)
class SslConnectorCustomizerTests {

  private Tomcat tomcat;

  private Connector connector;

  @BeforeEach
  void setup() {
    this.tomcat = new Tomcat();
    this.connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    this.connector.setPort(0);
    this.tomcat.setConnector(this.connector);
  }

  @AfterEach
  void stop() throws Exception {
    System.clearProperty("javax.net.ssl.trustStorePassword");
    ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance", null);
    ReflectionTestUtils.setField(URL.class, "factory", null);
    this.tomcat.stop();
  }

  @Test
  void sslCiphersConfiguration() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyStore("test.jks");
    ssl.setKeyStorePassword("secret");
    ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
    Connector connector = this.tomcat.getConnector();
    customizer.customize(connector);
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
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
    Connector connector = this.tomcat.getConnector();
    customizer.customize(connector);
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
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, null);
    Connector connector = this.tomcat.getConnector();
    customizer.customize(connector);
    this.tomcat.start();
    SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
    assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
    assertThat(sslHostConfig.getEnabledProtocols()).containsExactly("TLSv1.2");
  }

  @Test
  void customizeWhenSslStoreProviderProvidesOnlyKeyStoreShouldUseDefaultTruststore() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setTrustStore("src/test/resources/test.jks");
    SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
    KeyStore keyStore = loadStore();
    given(sslStoreProvider.getKeyStore()).willReturn(keyStore);
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, sslStoreProvider);
    Connector connector = this.tomcat.getConnector();
    customizer.customize(connector);
    this.tomcat.start();
    SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
    SSLHostConfig sslHostConfigWithDefaults = new SSLHostConfig();
    assertThat(sslHostConfig.getTruststoreFile()).isEqualTo(sslHostConfigWithDefaults.getTruststoreFile());
    Set<SSLHostConfigCertificate> certificates = sslHostConfig.getCertificates();
    assertThat(certificates).hasSize(1);
    assertThat(certificates.iterator().next().getCertificateKeystore()).isEqualTo(keyStore);
  }

  @Test
  void customizeWhenSslStoreProviderProvidesOnlyTrustStoreShouldUseDefaultKeystore() throws Exception {
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setKeyStore("src/test/resources/test.jks");
    SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
    KeyStore trustStore = loadStore();
    given(sslStoreProvider.getTrustStore()).willReturn(trustStore);
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, sslStoreProvider);
    Connector connector = this.tomcat.getConnector();
    customizer.customize(connector);
    this.tomcat.start();
    SSLHostConfig sslHostConfig = connector.getProtocolHandler().findSslHostConfigs()[0];
    sslHostConfig.getCertificates(true);
    SSLHostConfig sslHostConfigWithDefaults = new SSLHostConfig();
    sslHostConfigWithDefaults.getCertificates(true);
    assertThat(sslHostConfig.getTruststore()).isEqualTo(trustStore);
    System.out.println(sslHostConfig.getCertificates(false).stream()
            .map(SSLHostConfigCertificate::getCertificateFile).collect(Collectors.toList()));
    System.out.println(sslHostConfigWithDefaults.getCertificates(false).stream()
            .map(SSLHostConfigCertificate::getCertificateFile).collect(Collectors.toList()));
  }

  @Test
  void customizeWhenSslStoreProviderPresentShouldIgnorePasswordFromSsl(CapturedOutput output) throws Exception {
    System.setProperty("javax.net.ssl.trustStorePassword", "trustStoreSecret");
    Ssl ssl = new Ssl();
    ssl.setKeyPassword("password");
    ssl.setKeyStorePassword("secret");
    SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
    given(sslStoreProvider.getTrustStore()).willReturn(loadStore());
    given(sslStoreProvider.getKeyStore()).willReturn(loadStore());
    SslConnectorCustomizer customizer = new SslConnectorCustomizer(ssl, sslStoreProvider);
    Connector connector = this.tomcat.getConnector();
    customizer.customize(connector);
    this.tomcat.start();
    assertThat(connector.getState()).isEqualTo(LifecycleState.STARTED);
    assertThat(output).doesNotContain("Password verification failed");
  }

  @Test
  void customizeWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException() {
    assertThatExceptionOfType(WebServerException.class)
            .isThrownBy(() -> new SslConnectorCustomizer(new Ssl(), null).customize(this.tomcat.getConnector()))
            .withMessageContaining("Could not load key store 'null'");
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
