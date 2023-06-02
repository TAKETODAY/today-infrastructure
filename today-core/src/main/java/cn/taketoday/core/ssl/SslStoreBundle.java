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

import cn.taketoday.lang.Nullable;

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
  static SslStoreBundle of(@Nullable KeyStore keyStore,
          @Nullable String keyStorePassword, @Nullable KeyStore trustStore) {
    return new SslStoreBundle() {

      @Override
      public KeyStore getKeyStore() {
        return keyStore;
      }

      @Override
      public KeyStore getTrustStore() {
        return trustStore;
      }

      @Override
      public String getKeyStorePassword() {
        return keyStorePassword;
      }

    };
  }

}
