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

package cn.taketoday.framework.web.embedded.netty;

import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import cn.taketoday.framework.web.server.Http2;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.SslConfigurationValidator;
import cn.taketoday.framework.web.server.SslStoreProvider;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import io.netty.handler.ssl.ClientAuth;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;

/**
 * {@link NettyServerCustomizer} that configures SSL for the given Reactor Netty server
 * instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Chris Bono
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 * @deprecated this class is meant for internal use only.
 */
@Deprecated
public class SslServerCustomizer implements NettyServerCustomizer {

  private final Ssl ssl;

  @Nullable
  private final Http2 http2;

  @Nullable
  private final SslStoreProvider sslStoreProvider;

  public SslServerCustomizer(Ssl ssl, @Nullable Http2 http2, @Nullable SslStoreProvider sslStoreProvider) {
    this.ssl = ssl;
    this.http2 = http2;
    this.sslStoreProvider = sslStoreProvider;
  }

  @Override
  public HttpServer apply(HttpServer server) {
    AbstractProtocolSslContextSpec<?> sslContextSpec = createSslContextSpec();
    return server.secure(spec -> spec.sslContext(sslContextSpec));
  }

  protected AbstractProtocolSslContextSpec<?> createSslContextSpec() {
    AbstractProtocolSslContextSpec<?> sslContextSpec;
    if (this.http2 != null && this.http2.isEnabled()) {
      sslContextSpec = Http2SslContextSpec.forServer(getKeyManagerFactory(ssl, this.sslStoreProvider));
    }
    else {
      sslContextSpec = Http11SslContextSpec.forServer(getKeyManagerFactory(ssl, sslStoreProvider));
    }
    sslContextSpec.configure(builder -> {
      builder.trustManager(getTrustManagerFactory(this.ssl, this.sslStoreProvider));
      if (this.ssl.getEnabledProtocols() != null) {
        builder.protocols(this.ssl.getEnabledProtocols());
      }
      if (this.ssl.getCiphers() != null) {
        builder.ciphers(Arrays.asList(this.ssl.getCiphers()));
      }
      if (this.ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
        builder.clientAuth(ClientAuth.REQUIRE);
      }
      else if (this.ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
        builder.clientAuth(ClientAuth.OPTIONAL);
      }
    });
    return sslContextSpec;
  }

  KeyManagerFactory getKeyManagerFactory(Ssl ssl, @Nullable SslStoreProvider sslStoreProvider) {
    try {
      KeyStore keyStore = getKeyStore(ssl, sslStoreProvider);
      SslConfigurationValidator.validateKeyAlias(keyStore, ssl.getKeyAlias());
      KeyManagerFactory keyManagerFactory = (ssl.getKeyAlias() == null)
                                            ? KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                                            : new ConfigurableAliasKeyManagerFactory(ssl.getKeyAlias(),
                                                    KeyManagerFactory.getDefaultAlgorithm());
      char[] keyPassword = (ssl.getKeyPassword() != null) ? ssl.getKeyPassword().toCharArray() : null;
      if (keyPassword == null && ssl.getKeyStorePassword() != null) {
        keyPassword = ssl.getKeyStorePassword().toCharArray();
      }
      keyManagerFactory.init(keyStore, keyPassword);
      return keyManagerFactory;
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private KeyStore getKeyStore(Ssl ssl, @Nullable SslStoreProvider sslStoreProvider) throws Exception {
    if (sslStoreProvider != null) {
      return sslStoreProvider.getKeyStore();
    }
    return loadKeyStore(ssl.getKeyStoreType(), ssl.getKeyStoreProvider(), ssl.getKeyStore(),
            ssl.getKeyStorePassword());
  }

  TrustManagerFactory getTrustManagerFactory(Ssl ssl, @Nullable SslStoreProvider sslStoreProvider) {
    try {
      KeyStore store = getTrustStore(ssl, sslStoreProvider);
      TrustManagerFactory trustManagerFactory = TrustManagerFactory
              .getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(store);
      return trustManagerFactory;
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private KeyStore getTrustStore(Ssl ssl, @Nullable SslStoreProvider sslStoreProvider) throws Exception {
    if (sslStoreProvider != null) {
      return sslStoreProvider.getTrustStore();
    }
    return loadTrustStore(ssl.getTrustStoreType(), ssl.getTrustStoreProvider(), ssl.getTrustStore(),
            ssl.getTrustStorePassword());
  }

  private KeyStore loadKeyStore(String type, String provider, String resource, String password) throws Exception {

    return loadStore(type, provider, resource, password);
  }

  private KeyStore loadTrustStore(String type, String provider, @Nullable String resource, String password) throws Exception {
    if (resource == null) {
      return null;
    }
    return loadStore(type, provider, resource, password);
  }

  private KeyStore loadStore(@Nullable String type, @Nullable String provider, String resource, @Nullable String password) throws Exception {
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
   * A {@link KeyManagerFactory} that allows a configurable key alias to be used. Due to
   * the fact that the actual calls to retrieve the key by alias are done at request
   * time the approach is to wrap the actual key managers with a
   * {@link ConfigurableAliasKeyManager}. The actual SPI has to be wrapped as well due
   * to the fact that {@link KeyManagerFactory#getKeyManagers()} is final.
   */
  private static final class ConfigurableAliasKeyManagerFactory extends KeyManagerFactory {

    private ConfigurableAliasKeyManagerFactory(String alias, String algorithm) throws NoSuchAlgorithmException {
      this(KeyManagerFactory.getInstance(algorithm), alias, algorithm);
    }

    private ConfigurableAliasKeyManagerFactory(KeyManagerFactory delegate, String alias, String algorithm) {
      super(new ConfigurableAliasKeyManagerFactorySpi(delegate, alias), delegate.getProvider(), algorithm);
    }

  }

  private static final class ConfigurableAliasKeyManagerFactorySpi extends KeyManagerFactorySpi {

    private final KeyManagerFactory delegate;

    private final String alias;

    private ConfigurableAliasKeyManagerFactorySpi(KeyManagerFactory delegate, String alias) {
      this.delegate = delegate;
      this.alias = alias;
    }

    @Override
    protected void engineInit(KeyStore keyStore, char[] chars)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
      this.delegate.init(keyStore, chars);
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
            throws InvalidAlgorithmParameterException {
      throw new InvalidAlgorithmParameterException("Unsupported ManagerFactoryParameters");
    }

    @Override
    protected KeyManager[] engineGetKeyManagers() {
      return Arrays.stream(this.delegate.getKeyManagers())
              .filter(X509ExtendedKeyManager.class::isInstance)
              .map(X509ExtendedKeyManager.class::cast)
              .map(this::wrap)
              .toArray(KeyManager[]::new);
    }

    private ConfigurableAliasKeyManager wrap(X509ExtendedKeyManager keyManager) {
      return new ConfigurableAliasKeyManager(keyManager, this.alias);
    }

  }

  private static final class ConfigurableAliasKeyManager extends X509ExtendedKeyManager {

    private final X509ExtendedKeyManager delegate;

    @Nullable
    private final String alias;

    private ConfigurableAliasKeyManager(X509ExtendedKeyManager keyManager, @Nullable String alias) {
      this.delegate = keyManager;
      this.alias = alias;
    }

    @Override
    public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
      return this.delegate.chooseEngineClientAlias(strings, principals, sslEngine);
    }

    @Override
    public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
      return (this.alias != null) ? this.alias : this.delegate.chooseEngineServerAlias(s, principals, sslEngine);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
      return this.delegate.chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
      return this.delegate.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
      return this.delegate.getCertificateChain(alias);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
      return this.delegate.getClientAliases(keyType, issuers);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
      return this.delegate.getPrivateKey(alias);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
      return this.delegate.getServerAliases(keyType, issuers);
    }

  }

}
