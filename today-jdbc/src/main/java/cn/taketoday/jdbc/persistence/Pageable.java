/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.persistence;

import cn.taketoday.core.style.ToStringBuilder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 15:09
 */
public interface Pageable {

  /**
   * Returns the current page to be returned.
   *
   * @return the current page to be returned.
   */
  int current();

  /**
   * Returns the number of items to be returned.
   *
   * @return the number of items of that page
   */
  int size();

  default int offset() {
    return (current() - 1) * size();
  }

  default int offset(int max) {
    return (current() - 1) * size(max);
  }

  default int size(int max) {
    return Math.min(size(), max);
  }

  static Simple of(int size, int current) {
    return new Simple(size, current);
  }

  record Simple(int size, int current) implements Pageable {

    @Override
    public String toString() {
      return ToStringBuilder.from(this)
              .append("size", size)
              .append("current", current)
              .toString();
    }
  }

}
