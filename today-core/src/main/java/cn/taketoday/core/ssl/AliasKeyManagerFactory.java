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

package cn.taketoday.core.ssl;

import java.net.Socket;
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
import javax.net.ssl.X509ExtendedKeyManager;

/**
 * {@link KeyManagerFactory} that allows a configurable key alias to be used. Due to the
 * fact that the actual calls to retrieve the key by alias are done at request time the
 * approach is to wrap the actual key managers with a {@link AliasX509ExtendedKeyManager}.
 * The actual SPI has to be wrapped as well due to the fact that
 * {@link KeyManagerFactory#getKeyManagers()} is final.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class AliasKeyManagerFactory extends KeyManagerFactory {

  AliasKeyManagerFactory(KeyManagerFactory delegate, String alias, String algorithm) {
    super(new AliasKeyManagerFactorySpi(delegate, alias), delegate.getProvider(), algorithm);
  }

  /**
   * {@link KeyManagerFactorySpi} that allows a configurable key alias to be used.
   */
  private static final class AliasKeyManagerFactorySpi extends KeyManagerFactorySpi {

    private final KeyManagerFactory delegate;

    private final String alias;

    private AliasKeyManagerFactorySpi(KeyManagerFactory delegate, String alias) {
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

    private AliasX509ExtendedKeyManager wrap(X509ExtendedKeyManager keyManager) {
      return new AliasX509ExtendedKeyManager(keyManager, this.alias);
    }

  }

  /**
   * {@link X509ExtendedKeyManager} that allows a configurable key alias to be used.
   */
  static final class AliasX509ExtendedKeyManager extends X509ExtendedKeyManager {

    private final X509ExtendedKeyManager delegate;

    private final String alias;

    private AliasX509ExtendedKeyManager(X509ExtendedKeyManager keyManager, String alias) {
      this.delegate = keyManager;
      this.alias = alias;
    }

    @Override
    public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
      return this.delegate.chooseEngineClientAlias(strings, principals, sslEngine);
    }

    @Override
    public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
      return this.alias;
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
