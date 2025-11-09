/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core.ssl;

import org.jspecify.annotations.Nullable;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import infra.lang.Assert;

/**
 * A bundle of key and trust managers that can be used to establish an SSL connection.
 * Instances are usually created {@link #from(SslStoreBundle, SslBundleKey) from} an
 * {@link SslStoreBundle}.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
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
    Assert.notNull(keyManagerFactory, "KeyManagerFactory is required");
    Assert.notNull(trustManagerFactory, "TrustManagerFactory is required");
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

  /**
   * Factory method to create a new {@link SslManagerBundle} using the given
   * {@link TrustManagerFactory} and the default {@link KeyManagerFactory}.
   *
   * @param trustManagerFactory the trust manager factory
   * @return a new {@link SslManagerBundle} instance
   * @since 5.0
   */
  static SslManagerBundle from(TrustManagerFactory trustManagerFactory) {
    Assert.notNull(trustManagerFactory, "'trustManagerFactory' is required");
    KeyManagerFactory defaultKeyManagerFactory = createDefaultKeyManagerFactory();
    return of(defaultKeyManagerFactory, trustManagerFactory);
  }

  /**
   * Factory method to create a new {@link SslManagerBundle} using the given
   * {@link TrustManager TrustManagers} and the default {@link KeyManagerFactory}.
   *
   * @param trustManagers the trust managers to use
   * @return a new {@link SslManagerBundle} instance
   * @since 5.0
   */
  static SslManagerBundle from(TrustManager... trustManagers) {
    Assert.notNull(trustManagers, "'trustManagers' is required");
    KeyManagerFactory defaultKeyManagerFactory = createDefaultKeyManagerFactory();
    TrustManagerFactory defaultTrustManagerFactory = createDefaultTrustManagerFactory();
    return of(defaultKeyManagerFactory, FixedTrustManagerFactory.of(defaultTrustManagerFactory, trustManagers));
  }

  private static TrustManagerFactory createDefaultTrustManagerFactory() {
    String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
    TrustManagerFactory trustManagerFactory;
    try {
      trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
      trustManagerFactory.init((KeyStore) null);
    }
    catch (NoSuchAlgorithmException | KeyStoreException ex) {
      throw new IllegalStateException(
              "Unable to create TrustManagerFactory for default '%s' algorithm".formatted(defaultAlgorithm), ex);
    }
    return trustManagerFactory;
  }

  private static KeyManagerFactory createDefaultKeyManagerFactory() {
    String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
    KeyManagerFactory keyManagerFactory;
    try {
      keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
      keyManagerFactory.init(null, null);
    }
    catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException ex) {
      throw new IllegalStateException(
              "Unable to create KeyManagerFactory for default '%s' algorithm".formatted(defaultAlgorithm), ex);
    }
    return keyManagerFactory;
  }

}
