/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.origin;

import cn.taketoday.lang.Nullable;

/**
 * An interface that may be implemented by an object that can lookup {@link Origin}
 * information from a given key. Can be used to add origin support to existing classes.
 *
 * @param <K> the lookup key type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface OriginLookup<K> {

  /**
   * Return the origin of the given key or {@code null} if the origin cannot be
   * determined.
   *
   * @param key the key to lookup
   * @return the origin of the key or {@code null}
   */
  @Nullable
  Origin getOrigin(K key);

  /**
   * Return {@code true} if this lookup is immutable and has contents that will never
   * change.
   *
   * @return if the lookup is immutable
   */
  default boolean isImmutable() {
    return false;
  }

  /**
   * Return the implicit prefix that is applied when performing a lookup or {@code null}
   * if no prefix is used. Prefixes can be used to disambiguate keys that would
   * otherwise clash. For example, if multiple applications are running on the same
   * machine a different prefix can be set on each application to ensure that different
   * environment variables are used.
   *
   * @return the prefix applied by the lookup class or {@code null}.
   */
  @Nullable
  default String getPrefix() {
    return null;
  }

  /**
   * Attempt to lookup the origin from the given source. If the source is not a
   * {@link OriginLookup} or if an exception occurs during lookup then {@code null} is
   * returned.
   *
   * @param source the source object
   * @param key the key to lookup
   * @param <K> the key type
   * @return an {@link Origin} or {@code null}
   */
  @SuppressWarnings("unchecked")
  @Nullable
  static <K> Origin getOrigin(Object source, K key) {
    if (!(source instanceof OriginLookup)) {
      return null;
    }
    try {
      return ((OriginLookup<K>) source).getOrigin(key);
    }
    catch (Throwable ex) {
      return null;
    }
  }

}
