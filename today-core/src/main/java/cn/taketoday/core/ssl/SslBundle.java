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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * A bundle of trust material that can be used to establish an SSL connection.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public interface SslBundle {

  /**
   * The default protocol to use.
   */
  String DEFAULT_PROTOCOL = "TLS";

  /**
   * Return the {@link SslStoreBundle} that can be used to access this bundle's key and
   * trust stores.
   *
   * @return the {@code SslStoreBundle} instance for this bundle
   */
  SslStoreBundle getStores();

  /**
   * Return a reference to the key that should be used for this bundle or
   * {@link SslBundleKey#NONE}.
   *
   * @return a reference to the SSL key that should be used
   */
  SslBundleKey getKey();

  /**
   * Return {@link SslOptions} that should be applied when establishing the SSL
   * connection.
   *
   * @return the options that should be applied
   */
  SslOptions getOptions();

  /**
   * Return the protocol to use when establishing the connection. Values should be
   * supported by {@link SSLContext#getInstance(String)}.
   *
   * @return the SSL protocol
   * @see SSLContext#getInstance(String)
   */
  String getProtocol();

  /**
   * Return the {@link SslManagerBundle} that can be used to access this bundle's
   * {@link KeyManager key} and {@link TrustManager trust} managers.
   *
   * @return the {@code SslManagerBundle} instance for this bundle
   */
  SslManagerBundle getManagers();

  /**
   * Factory method to create a new {@link SSLContext} for this bundle.
   *
   * @return a new {@link SSLContext} instance
   */
  default SSLContext createSslContext() {
    return getManagers().createSslContext(getProtocol());
  }

  /**
   * Factory method to create a new {@link SslBundle} instance.
   *
   * @param stores the stores or {@code null}
   * @return a new {@link SslBundle} instance
   */
  static SslBundle of(SslStoreBundle stores) {
    return of(stores, null, null);
  }

  /**
   * Factory method to create a new {@link SslBundle} instance.
   *
   * @param stores the stores or {@code null}
   * @param key the key or {@code null}
   * @return a new {@link SslBundle} instance
   */
  static SslBundle of(@Nullable SslStoreBundle stores, @Nullable SslBundleKey key) {
    return of(stores, key, null);
  }

  /**
   * Factory method to create a new {@link SslBundle} instance.
   *
   * @param stores the stores or {@code null}
   * @param key the key or {@code null}
   * @param options the options or {@code null}
   * @return a new {@link SslBundle} instance
   */
  static SslBundle of(@Nullable SslStoreBundle stores, @Nullable SslBundleKey key,
          @Nullable SslOptions options) {
    return of(stores, key, options, null);
  }

  /**
   * Factory method to create a new {@link SslBundle} instance.
   *
   * @param stores the stores or {@code null}
   * @param key the key or {@code null}
   * @param options the options or {@code null}
   * @param protocol the protocol or {@code null}
   * @return a new {@link SslBundle} instance
   */
  static SslBundle of(@Nullable SslStoreBundle stores, @Nullable SslBundleKey key,
          @Nullable SslOptions options, @Nullable String protocol) {
    return of(stores, key, options, protocol, null);
  }

  /**
   * Factory method to create a new {@link SslBundle} instance.
   *
   * @param stores the stores or {@code null}
   * @param key the key or {@code null}
   * @param options the options or {@code null}
   * @param protocol the protocol or {@code null}
   * @param managers the managers or {@code null}
   * @return a new {@link SslBundle} instance
   */
  static SslBundle of(@Nullable SslStoreBundle stores, @Nullable SslBundleKey key,
          @Nullable SslOptions options, @Nullable String protocol, @Nullable SslManagerBundle managers) {
    SslManagerBundle managersToUse = (managers != null) ? managers : SslManagerBundle.from(stores, key);
    return new SslBundle() {

      @Override
      public SslStoreBundle getStores() {
        return (stores != null) ? stores : SslStoreBundle.NONE;
      }

      @Override
      public SslBundleKey getKey() {
        return (key != null) ? key : SslBundleKey.NONE;
      }

      @Override
      public SslOptions getOptions() {
        return (options != null) ? options : SslOptions.NONE;
      }

      @Override
      public String getProtocol() {
        return StringUtils.isBlank(protocol) ? DEFAULT_PROTOCOL : protocol;
      }

      @Override
      public SslManagerBundle getManagers() {
        return managersToUse;
      }

    };
  }

}
