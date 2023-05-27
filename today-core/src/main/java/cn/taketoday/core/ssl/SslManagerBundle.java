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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A bundle of key and trust managers that can be used to establish an SSL connection.
 * Instances are usually created {@link #from(SslStoreBundle, SslBundleKey) from} an
 * {@link SslStoreBundle}.
 *
 * @author Scott Frederick
 * @see SslStoreBundle
 * @see SslBundle#getManagers()
 * @since 4.0
 */
public interface SslManagerBundle {

  /**
   * Return the {@code KeyManager} instances used to establish identity.
   *
   * @return the key managers
   */
  default KeyManager[] getKeyManagers() {
    return getKeyManagerFactory().getKeyManagers();
  }

  /**
   * Return the {@code KeyManagerFactory} used to establish identity.
   *
   * @return the key manager factory
   */
  KeyManagerFactory getKeyManagerFactory();

  /**
   * Return the {@link TrustManager} instances used to establish trust.
   *
   * @return the trust managers
   */
  default TrustManager[] getTrustManagers() {
    return getTrustManagerFactory().getTrustManagers();
  }

  /**
   * Return the {@link TrustManagerFactory} used to establish trust.
   *
   * @return the trust manager factory
   */
  TrustManagerFactory getTrustManagerFactory();

  /**
   * Factory method to create a new {@link SSLContext} for the {@link #getKeyManagers()
   * key managers} and {@link #getTrustManagers() trust managers} managed by this
   * instance.
   *
   * @param protocol the standard name of the SSL protocol. See
   * {@link SSLContext#getInstance(String)}
   * @return a new {@link SSLContext} instance
   */
  default SSLContext createSslContext(String protocol) {
    try {
      SSLContext sslContext = SSLContext.getInstance(protocol);
      sslContext.init(getKeyManagers(), getTrustManagers(), null);
      return sslContext;
    }
    catch (Exception ex) {
      throw new IllegalStateException("Could not load SSL context: " + ex.getMessage(), ex);
    }
  }

  /**
   * Factory method to create a new {@link SslManagerBundle} instance.
   *
   * @param keyManagerFactory the key manager factory
   * @param trustManagerFactory the trust manager factory
   * @return a new {@link SslManagerBundle} instance
   */
  static SslManagerBundle of(KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory) {
    Assert.notNull(keyManagerFactory, "KeyManagerFactory must not be null");
    Assert.notNull(trustManagerFactory, "TrustManagerFactory must not be null");
    return new SslManagerBundle() {

      @Override
      public KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
      }

      @Override
      public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
      }

    };
  }

  /**
   * Factory method to create a new {@link SslManagerBundle} backed by the given
   * {@link SslBundle} and {@link SslBundleKey}.
   *
   * @param storeBundle the SSL store bundle
   * @param key the key reference
   * @return a new {@link SslManagerBundle} instance
   */
  static SslManagerBundle from(@Nullable SslStoreBundle storeBundle, @Nullable SslBundleKey key) {
    return new DefaultSslManagerBundle(storeBundle, key);
  }

}
