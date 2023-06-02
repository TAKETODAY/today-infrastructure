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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.net.ssl.SSLEngine;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Configuration options that should be applied when establishing an SSL connection.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SslBundle#getOptions()
 * @since 4.0
 */
public interface SslOptions {

  /**
   * {@link SslOptions} that returns {@code null} results.
   */
  SslOptions NONE = of((Set<String>) null, (Set<String>) null);

  /**
   * Return if any SSL options have been specified.
   *
   * @return {@code true} if SSL options have been specified
   */
  default boolean isSpecified() {
    return (getCiphers() != null) && (getEnabledProtocols() != null);
  }

  /**
   * Return the ciphers that can be used or an empty set. The cipher names in this set
   * should be compatible with those supported by
   * {@link SSLEngine#getSupportedCipherSuites()}.
   *
   * @return the ciphers that can be used or {@code null}
   */
  @Nullable
  String[] getCiphers();

  /**
   * Return the protocols that should be enabled or an empty set. The protocols names in
   * this set should be compatible with those supported by
   * {@link SSLEngine#getSupportedProtocols()}.
   *
   * @return the protocols to enable or {@code null}
   */
  @Nullable
  String[] getEnabledProtocols();

  /**
   * Factory method to create a new {@link SslOptions} instance.
   *
   * @param ciphers the ciphers
   * @param enabledProtocols the enabled protocols
   * @return a new {@link SslOptions} instance
   */
  static SslOptions of(@Nullable String[] ciphers, @Nullable String[] enabledProtocols) {
    return new SslOptions() {

      @Nullable
      @Override
      public String[] getCiphers() {
        return ciphers;
      }

      @Nullable
      @Override
      public String[] getEnabledProtocols() {
        return enabledProtocols;
      }

    };
  }

  /**
   * Factory method to create a new {@link SslOptions} instance.
   *
   * @param ciphers the ciphers
   * @param enabledProtocols the enabled protocols
   * @return a new {@link SslOptions} instance
   */
  static SslOptions of(@Nullable Set<String> ciphers, @Nullable Set<String> enabledProtocols) {
    return of(toArray(ciphers), toArray(enabledProtocols));
  }

  /**
   * Helper method that provides a null-safe way to convert a {@code String[]} to a
   * {@link Collection} for client libraries to use.
   *
   * @param array the array to convert
   * @return a collection or {@code null}
   */
  @Nullable
  static Set<String> asSet(@Nullable String[] array) {
    return array != null ? Collections.unmodifiableSet(CollectionUtils.newLinkedHashSet(array)) : null;
  }

  @Nullable
  private static String[] toArray(@Nullable Collection<String> collection) {
    return collection != null ? StringUtils.toStringArray(collection) : null;
  }

}
