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
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleKey;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.core.ssl.SslStoreBundle;
import cn.taketoday.framework.web.server.Ssl.ClientAuth;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures SSL support on the given connector.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class SslConnectorCustomizer implements TomcatConnectorCustomizer {

  @Nullable
  private final ClientAuth clientAuth;

  private final SslBundle sslBundle;

  SslConnectorCustomizer(@Nullable ClientAuth clientAuth, SslBundle sslBundle) {
    this.clientAuth = clientAuth;
    this.sslBundle = sslBundle;
  }

  @Override
  public void customize(Connector connector) {
    ProtocolHandler handler = connector.getProtocolHandler();
    Assert.state(handler instanceof AbstractHttp11JsseProtocol,
            "To use SSL, the connector's protocol handler must be an AbstractHttp11JsseProtocol subclass");
    configureSsl((AbstractHttp11JsseProtocol<?>) handler);
    connector.setScheme("https");
    connector.setSecure(true);
  }

  /**
   * Configure Tomcat's {@link AbstractHttp11JsseProtocol} for SSL.
   *
   * @param protocol the protocol
   */
  void configureSsl(AbstractHttp11JsseProtocol<?> protocol) {
    SslBundleKey key = this.sslBundle.getKey();
    SslStoreBundle stores = this.sslBundle.getStores();
    SslOptions options = this.sslBundle.getOptions();
    protocol.setSSLEnabled(true);
    SSLHostConfig sslHostConfig = new SSLHostConfig();
    sslHostConfig.setHostName(protocol.getDefaultSSLHostConfigName());
    sslHostConfig.setSslProtocol(this.sslBundle.getProtocol());
    protocol.addSslHostConfig(sslHostConfig);
    configureSslClientAuth(sslHostConfig);
    SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.UNDEFINED);
    String keystorePassword = (stores.getKeyStorePassword() != null) ? stores.getKeyStorePassword() : "";
    certificate.setCertificateKeystorePassword(keystorePassword);
    if (key.getPassword() != null) {
      certificate.setCertificateKeyPassword(key.getPassword());
    }
    if (key.getAlias() != null) {
      certificate.setCertificateKeyAlias(key.getAlias());
    }
    sslHostConfig.addCertificate(certificate);
    if (options.getCiphers() != null) {
      String ciphers = StringUtils.arrayToCommaDelimitedString(options.getCiphers());
      sslHostConfig.setCiphers(ciphers);
    }
    configureEnabledProtocols(protocol);
    configureSslStoreProvider(protocol, sslHostConfig, certificate);
  }

  private void configureEnabledProtocols(AbstractHttp11JsseProtocol<?> protocol) {
    SslOptions options = this.sslBundle.getOptions();
    if (options.getEnabledProtocols() != null) {
      String enabledProtocols = StringUtils.arrayToDelimitedString(options.getEnabledProtocols(), "+");
      for (SSLHostConfig sslHostConfig : protocol.findSslHostConfigs()) {
        sslHostConfig.setProtocols(enabledProtocols);
      }
    }
  }

  private void configureSslClientAuth(SSLHostConfig config) {
    config.setCertificateVerification(ClientAuth.map(this.clientAuth, "none", "optional", "required"));
  }

  protected void configureSslStoreProvider(AbstractHttp11JsseProtocol<?> protocol, SSLHostConfig sslHostConfig,
          SSLHostConfigCertificate certificate) {
    Assert.isInstanceOf(Http11NioProtocol.class, protocol,
            "SslStoreProvider can only be used with Http11NioProtocol");
    try {
      SslStoreBundle stores = this.sslBundle.getStores();
      if (stores.getKeyStore() != null) {
        certificate.setCertificateKeystore(stores.getKeyStore());
      }
      if (stores.getTrustStore() != null) {
        sslHostConfig.setTrustStore(stores.getTrustStore());
      }
    }
    catch (Exception ex) {
      throw new IllegalStateException("Could not load store: " + ex.getMessage(), ex);
    }
  }

}
