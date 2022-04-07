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

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import java.io.FileNotFoundException;

import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.SslStoreProvider;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures SSL support on the given connector.
 *
 * @author Brian Clozel
 */
class SslConnectorCustomizer implements TomcatConnectorCustomizer {

  private final Ssl ssl;

  private final SslStoreProvider sslStoreProvider;

  SslConnectorCustomizer(Ssl ssl, SslStoreProvider sslStoreProvider) {
    Assert.notNull(ssl, "Ssl configuration should not be null");
    this.ssl = ssl;
    this.sslStoreProvider = sslStoreProvider;
  }

  @Override
  public void customize(Connector connector) {
    ProtocolHandler handler = connector.getProtocolHandler();
    Assert.state(handler instanceof AbstractHttp11JsseProtocol,
            "To use SSL, the connector's protocol handler must be an AbstractHttp11JsseProtocol subclass");
    configureSsl((AbstractHttp11JsseProtocol<?>) handler, this.ssl, this.sslStoreProvider);
    connector.setScheme("https");
    connector.setSecure(true);
  }

  /**
   * Configure Tomcat's {@link AbstractHttp11JsseProtocol} for SSL.
   *
   * @param protocol the protocol
   * @param ssl the ssl details
   * @param sslStoreProvider the ssl store provider
   */
  protected void configureSsl(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl, SslStoreProvider sslStoreProvider) {
    protocol.setSSLEnabled(true);
    SSLHostConfig sslHostConfig = new SSLHostConfig();
    sslHostConfig.setHostName(protocol.getDefaultSSLHostConfigName());
    sslHostConfig.setSslProtocol(ssl.getProtocol());
    protocol.addSslHostConfig(sslHostConfig);
    configureSslClientAuth(sslHostConfig, ssl);
    SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.UNDEFINED);
    if (ssl.getKeyStorePassword() != null) {
      certificate.setCertificateKeystorePassword(ssl.getKeyStorePassword());
    }
    if (ssl.getKeyPassword() != null) {
      certificate.setCertificateKeyPassword(ssl.getKeyPassword());
    }
    if (ssl.getKeyAlias() != null) {
      certificate.setCertificateKeyAlias(ssl.getKeyAlias());
    }
    sslHostConfig.addCertificate(certificate);
    String ciphers = StringUtils.arrayToCommaDelimitedString(ssl.getCiphers());
    if (StringUtils.hasText(ciphers)) {
      sslHostConfig.setCiphers(ciphers);
    }
    configureEnabledProtocols(protocol, ssl);
    if (sslStoreProvider != null) {
      configureSslStoreProvider(protocol, sslHostConfig, certificate, sslStoreProvider);
    }
    else {
      configureSslKeyStore(certificate, ssl);
      configureSslTrustStore(sslHostConfig, ssl);
    }
  }

  private void configureEnabledProtocols(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl) {
    if (ssl.getEnabledProtocols() != null) {
      for (SSLHostConfig sslHostConfig : protocol.findSslHostConfigs()) {
        sslHostConfig.setProtocols(StringUtils.arrayToCommaDelimitedString(ssl.getEnabledProtocols()));
      }
    }
  }

  private void configureSslClientAuth(SSLHostConfig config, Ssl ssl) {
    if (ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
      config.setCertificateVerification("required");
    }
    else if (ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
      config.setCertificateVerification("optional");
    }
  }

  protected void configureSslStoreProvider(
          AbstractHttp11JsseProtocol<?> protocol, SSLHostConfig sslHostConfig,
          SSLHostConfigCertificate certificate, SslStoreProvider sslStoreProvider) {
    Assert.isInstanceOf(Http11NioProtocol.class, protocol,
            "SslStoreProvider can only be used with Http11NioProtocol");
    try {
      if (sslStoreProvider.getKeyStore() != null) {
        certificate.setCertificateKeystore(sslStoreProvider.getKeyStore());
      }
      if (sslStoreProvider.getTrustStore() != null) {
        sslHostConfig.setTrustStore(sslStoreProvider.getTrustStore());
      }
    }
    catch (Exception ex) {
      throw new WebServerException("Could not load store: " + ex.getMessage(), ex);
    }
  }

  private void configureSslKeyStore(SSLHostConfigCertificate certificate, Ssl ssl) {
    try {
      certificate.setCertificateKeystoreFile(ResourceUtils.getURL(ssl.getKeyStore()).toString());
    }
    catch (Exception ex) {
      throw new WebServerException("Could not load key store '" + ssl.getKeyStore() + "'", ex);
    }
    if (ssl.getKeyStoreType() != null) {
      certificate.setCertificateKeystoreType(ssl.getKeyStoreType());
    }
    if (ssl.getKeyStoreProvider() != null) {
      certificate.setCertificateKeystoreProvider(ssl.getKeyStoreProvider());
    }
  }

  private void configureSslTrustStore(SSLHostConfig sslHostConfig, Ssl ssl) {
    if (ssl.getTrustStore() != null) {
      try {
        sslHostConfig.setTruststoreFile(ResourceUtils.getURL(ssl.getTrustStore()).toString());
      }
      catch (FileNotFoundException ex) {
        throw new WebServerException("Could not load trust store: " + ex.getMessage(), ex);
      }
    }
    if (ssl.getTrustStorePassword() != null) {
      sslHostConfig.setTruststorePassword(ssl.getTrustStorePassword());
    }
    if (ssl.getTrustStoreType() != null) {
      sslHostConfig.setTruststoreType(ssl.getTrustStoreType());
    }
    if (ssl.getTrustStoreProvider() != null) {
      sslHostConfig.setTruststoreProvider(ssl.getTrustStoreProvider());
    }
  }

}
