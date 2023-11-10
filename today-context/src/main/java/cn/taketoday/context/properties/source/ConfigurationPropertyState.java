/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.source;

import java.util.function.Predicate;

import cn.taketoday.lang.Assert;

/**
 * The state of content from a {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public enum ConfigurationPropertyState {

  /**
   * The {@link ConfigurationPropertySource} has at least one matching
   * {@link ConfigurationProperty}.
   */
  PRESENT,

  /**
   * The {@link ConfigurationPropertySource} has no matching
   * {@link ConfigurationProperty ConfigurationProperties}.
   */
  ABSENT,

  /**
   * It's not possible to determine if {@link ConfigurationPropertySource} has matching
   * {@link ConfigurationProperty ConfigurationProperties} or not.
   */
  UNKNOWN;

  /**
   * Search the given iterable using a predicate to determine if content is
   * {@link #PRESENT} or {@link #ABSENT}.
   *
   * @param <T> the data type
   * @param source the source iterable to search
   * @param predicate the predicate used to test for presence
   * @return {@link #PRESENT} if the iterable contains a matching item, otherwise
   * {@link #ABSENT}.
   */
  static <T> ConfigurationPropertyState search(Iterable<T> source, Predicate<T> predicate) {
    Assert.notNull(source, "Source is required");
    Assert.notNull(predicate, "Predicate is required");
    for (T item : source) {
      if (predicate.test(item)) {
        return PRESENT;
      }
    }
    return ABSENT;
  }

}
