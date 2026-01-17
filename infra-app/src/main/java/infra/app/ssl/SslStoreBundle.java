/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.ssl;

import org.jspecify.annotations.Nullable;

import java.security.KeyStore;

/**
 * A bundle of key and trust stores that can be used to establish an SSL connection.
 *
 * @author Scott Frederick
 * @see SslBundle#getStores()
 * @since 4.0
 */
public interface SslStoreBundle {

  /**
   * {@link SslStoreBundle} that returns {@code null} for each method.
   */
  SslStoreBundle NONE = of(null, null, null);

  /**
   * Return a key store generated from the trust material or {@code null}.
   *
   * @return the key store
   */
  @Nullable
  KeyStore getKeyStore();

  /**
   * Return the password for the key in the key store or {@code null}.
   *
   * @return the key password
   */
  @Nullable
  String getKeyStorePassword();

  /**
   * Return a trust store generated from the trust material or {@code null}.
   *
   * @return the trust store
   */
  @Nullable
  KeyStore getTrustStore();

  /**
   * Factory method to create a new {@link SslStoreBundle} instance.
   *
   * @param keyStore the key store or {@code null}
   * @param keyStorePassword the key store password or {@code null}
   * @param trustStore the trust store or {@code null}
   * @return a new {@link SslStoreBundle} instance
   */
  static SslStoreBundle of(@Nullable KeyStore keyStore, @Nullable String keyStorePassword, @Nullable KeyStore trustStore) {

    return new SslStoreBundle() {
      @Nullable
      @Override
      public KeyStore getKeyStore() {
        return keyStore;
      }

      @Nullable
      @Override
      public KeyStore getTrustStore() {
        return trustStore;
      }

      @Nullable
      @Override
      public String getKeyStorePassword() {
        return keyStorePassword;
      }

    };
  }

}
