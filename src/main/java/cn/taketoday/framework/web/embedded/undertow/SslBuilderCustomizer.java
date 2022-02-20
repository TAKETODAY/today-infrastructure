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

package cn.taketoday.framework.web.embedded.undertow;

import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.SslConfigurationValidator;
import cn.taketoday.framework.web.server.SslStoreProvider;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.util.ResourceUtils;
import io.undertow.Undertow;

/**
 * {@link UndertowBuilderCustomizer} that configures SSL on the given builder instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 */
class SslBuilderCustomizer implements UndertowBuilderCustomizer {

  private final int port;

  private final InetAddress address;

  private final Ssl ssl;

  private final SslStoreProvider sslStoreProvider;

  SslBuilderCustomizer(int port, InetAddress address, Ssl ssl, SslStoreProvider sslStoreProvider) {
    this.port = port;
    this.address = address;
    this.ssl = ssl;
    this.sslStoreProvider = sslStoreProvider;
  }

  @Override
  public void customize(Undertow.Builder builder) {
    try {
      SSLContext sslContext = SSLContext.getInstance(this.ssl.getProtocol());
      sslContext.init(getKeyManagers(this.ssl, this.sslStoreProvider),
              getTrustManagers(this.ssl, this.sslStoreProvider), null);
      builder.addHttpsListener(this.port, getListenAddress(), sslContext);
      builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, getSslClientAuthMode(this.ssl));
      if (this.ssl.getEnabledProtocols() != null) {
        builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(this.ssl.getEnabledProtocols()));
      }
      if (this.ssl.getCiphers() != null) {
        builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of(this.ssl.getCiphers()));
      }
    }
    catch (NoSuchAlgorithmException | KeyManagementException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private String getListenAddress() {
    if (this.address == null) {
      return "0.0.0.0";
    }
    return this.address.getHostAddress();
  }

  private SslClientAuthMode getSslClientAuthMode(Ssl ssl) {
    if (ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
      return SslClientAuthMode.REQUIRED;
    }
    if (ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
      return SslClientAuthMode.REQUESTED;
    }
    return SslClientAuthMode.NOT_REQUESTED;
  }

  private KeyManager[] getKeyManagers(Ssl ssl, SslStoreProvider sslStoreProvider) {
    try {
      KeyStore keyStore = getKeyStore(ssl, sslStoreProvider);
      SslConfigurationValidator.validateKeyAlias(keyStore, ssl.getKeyAlias());
      KeyManagerFactory keyManagerFactory = KeyManagerFactory
              .getInstance(KeyManagerFactory.getDefaultAlgorithm());
      char[] keyPassword = (ssl.getKeyPassword() != null) ? ssl.getKeyPassword().toCharArray() : null;
      if (keyPassword == null && ssl.getKeyStorePassword() != null) {
        keyPassword = ssl.getKeyStorePassword().toCharArray();
      }
      keyManagerFactory.init(keyStore, keyPassword);
      if (ssl.getKeyAlias() != null) {
        return getConfigurableAliasKeyManagers(ssl, keyManagerFactory.getKeyManagers());
      }
      return keyManagerFactory.getKeyManagers();
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private KeyManager[] getConfigurableAliasKeyManagers(Ssl ssl, KeyManager[] keyManagers) {
    for (int i = 0; i < keyManagers.length; i++) {
      if (keyManagers[i] instanceof X509ExtendedKeyManager) {
        keyManagers[i] = new ConfigurableAliasKeyManager((X509ExtendedKeyManager) keyManagers[i],
                ssl.getKeyAlias());
      }
    }
    return keyManagers;
  }

  private KeyStore getKeyStore(Ssl ssl, SslStoreProvider sslStoreProvider) throws Exception {
    if (sslStoreProvider != null) {
      return sslStoreProvider.getKeyStore();
    }
    return loadKeyStore(ssl.getKeyStoreType(), ssl.getKeyStoreProvider(), ssl.getKeyStore(),
            ssl.getKeyStorePassword());
  }

  private TrustManager[] getTrustManagers(Ssl ssl, SslStoreProvider sslStoreProvider) {
    try {
      KeyStore store = getTrustStore(ssl, sslStoreProvider);
      TrustManagerFactory trustManagerFactory = TrustManagerFactory
              .getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(store);
      return trustManagerFactory.getTrustManagers();
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private KeyStore getTrustStore(Ssl ssl, SslStoreProvider sslStoreProvider) throws Exception {
    if (sslStoreProvider != null) {
      return sslStoreProvider.getTrustStore();
    }
    return loadTrustStore(ssl.getTrustStoreType(), ssl.getTrustStoreProvider(), ssl.getTrustStore(),
            ssl.getTrustStorePassword());
  }

  private KeyStore loadKeyStore(String type, String provider, String resource, String password) throws Exception {
    return loadStore(type, provider, resource, password);
  }

  private KeyStore loadTrustStore(String type, String provider, String resource, String password) throws Exception {
    if (resource == null) {
      return null;
    }
    return loadStore(type, provider, resource, password);
  }

  private KeyStore loadStore(String type, String provider, String resource, String password) throws Exception {
    type = (type != null) ? type : "JKS";
    KeyStore store = (provider != null) ? KeyStore.getInstance(type, provider) : KeyStore.getInstance(type);
    try {
      URL url = ResourceUtils.getURL(resource);
      try (InputStream stream = url.openStream()) {
        store.load(stream, (password != null) ? password.toCharArray() : null);
      }
      return store;
    }
    catch (Exception ex) {
      throw new WebServerException("Could not load key store '" + resource + "'", ex);
    }
  }

  /**
   * {@link X509ExtendedKeyManager} that supports custom alias configuration.
   */
  private static class ConfigurableAliasKeyManager extends X509ExtendedKeyManager {

    private final X509ExtendedKeyManager keyManager;

    private final String alias;

    ConfigurableAliasKeyManager(X509ExtendedKeyManager keyManager, String alias) {
      this.keyManager = keyManager;
      this.alias = alias;
    }

    @Override
    public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
      return this.keyManager.chooseEngineClientAlias(strings, principals, sslEngine);
    }

    @Override
    public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
      if (this.alias == null) {
        return this.keyManager.chooseEngineServerAlias(s, principals, sslEngine);
      }
      return this.alias;
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
      return this.keyManager.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
      return this.keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
      return this.keyManager.getCertificateChain(alias);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
      return this.keyManager.getClientAliases(keyType, issuers);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
      return this.keyManager.getPrivateKey(alias);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
      return this.keyManager.getServerAliases(keyType, issuers);
    }

  }

}
