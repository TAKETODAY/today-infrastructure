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

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import cn.taketoday.lang.Nullable;

/**
 * Default implementation of {@link SslManagerBundle}.
 *
 * @author Scott Frederick
 * @see SslManagerBundle#from(SslStoreBundle, SslBundleKey)
 */
class DefaultSslManagerBundle implements SslManagerBundle {

  private final SslStoreBundle storeBundle;

  private final SslBundleKey key;

  DefaultSslManagerBundle(@Nullable SslStoreBundle storeBundle, @Nullable SslBundleKey key) {
    this.storeBundle = (storeBundle != null) ? storeBundle : SslStoreBundle.NONE;
    this.key = (key != null) ? key : SslBundleKey.NONE;
  }

  @Override
  public KeyManagerFactory getKeyManagerFactory() {
    try {
      KeyStore store = this.storeBundle.getKeyStore();
      this.key.assertContainsAlias(store);
      String alias = this.key.getAlias();
      String algorithm = KeyManagerFactory.getDefaultAlgorithm();
      KeyManagerFactory factory = getKeyManagerFactoryInstance(algorithm);
      if (alias != null) {
        factory = new AliasKeyManagerFactory(factory, alias, algorithm);
      }
      String password = this.key.getPassword();
      if (password == null) {
        password = storeBundle.getKeyStorePassword();
      }
      factory.init(store, (password != null) ? password.toCharArray() : null);
      return factory;
    }
    catch (RuntimeException ex) {
      throw ex;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Could not load key manager factory: " + ex.getMessage(), ex);
    }
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory() {
    try {
      KeyStore store = this.storeBundle.getTrustStore();
      String algorithm = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory factory = getTrustManagerFactoryInstance(algorithm);
      factory.init(store);
      return factory;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Could not load trust manager factory: " + ex.getMessage(), ex);
    }
  }

  protected KeyManagerFactory getKeyManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
    return KeyManagerFactory.getInstance(algorithm);
  }

  protected TrustManagerFactory getTrustManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
    return TrustManagerFactory.getInstance(algorithm);
  }

}
