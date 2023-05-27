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
import java.security.KeyStoreException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * A reference to a single key obtained via {@link SslBundle}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface SslBundleKey {

  /**
   * {@link SslBundleKey} that returns no values.
   */
  SslBundleKey NONE = of(null, null);

  /**
   * Return the password that should be used to access the key or {@code null} if no
   * password is required.
   *
   * @return the key password
   */
  @Nullable
  String getPassword();

  /**
   * Return the alias of the key or {@code null} if the key has no alias.
   *
   * @return the key alias
   */
  @Nullable
  String getAlias();

  /**
   * Assert that the alias is contained in the given keystore.
   *
   * @param keyStore the keystore to check
   */
  default void assertContainsAlias(@Nullable KeyStore keyStore) {
    String alias = getAlias();
    if (StringUtils.isNotEmpty(alias) && keyStore != null) {
      try {
        if (!keyStore.containsAlias(alias)) {
          throw new IllegalStateException("Keystore does not contain alias '%s'".formatted(alias));
        }
      }
      catch (KeyStoreException ex) {
        throw new IllegalStateException(
                String.format("Could not determine if keystore contains alias '%s'", alias), ex);
      }
    }
  }

  /**
   * Factory method to create a new {@link SslBundleKey} instance.
   *
   * @param password the password used to access the key
   * @return a new {@link SslBundleKey} instance
   */
  static SslBundleKey of(String password) {
    return of(password, null);
  }

  /**
   * Factory method to create a new {@link SslBundleKey} instance.
   *
   * @param password the password used to access the key
   * @param alias the alias of the key
   * @return a new {@link SslBundleKey} instance
   */
  static SslBundleKey of(@Nullable String password, @Nullable String alias) {
    return new SslBundleKey() {

      @Override
      @Nullable
      public String getPassword() {
        return password;
      }

      @Override
      @Nullable
      public String getAlias() {
        return alias;
      }

    };
  }

}
