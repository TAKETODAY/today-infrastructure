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

package infra.core.ssl;

import org.jspecify.annotations.Nullable;

import java.security.KeyStore;
import java.security.KeyStoreException;

import infra.util.StringUtils;

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
