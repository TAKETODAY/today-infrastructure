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

package cn.taketoday.persistence;

import cn.taketoday.lang.Nullable;

/**
 * for page query
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/31 15:09
 * @see Page
 */
public interface Pageable {

  /**
   * Returns the current page to be returned.
   *
   * @return the current page to be returned.
   */
  int pageNumber();

  /**
   * Returns the number of items to be returned.
   *
   * @return the number of items of that page
   */
  int pageSize();

  /**
   * Returns the number of offset in database
   *
   * @return the number of offset in database
   */
  default int offset() {
    return (pageNumber() - 1) * pageSize();
  }

  /**
   * Returns the number of offset in database
   *
   * @return the number of offset in database
   */
  default int offset(int max) {
    return (pageNumber() - 1) * pageSize(max);
  }

  /**
   * Returns the number of items with max limit to be returned.
   *
   * @return the number of items with max limit of that page
   */
  default int pageSize(int max) {
    return Math.min(pageSize(), max);
  }

  /**
   * Create Simple pageable instance
   *
   * @param pageSize page size
   * @param pageNumber current page number
   */
  static Pageable of(int pageNumber, int pageSize) {
    return new SimplePageable(pageNumber, pageSize);
  }

  /**
   * unwrap
   */
  @Nullable
  static Pageable unwrap(@Nullable Object source) {
    return source instanceof Pageable pageable ? pageable : null;
  }

}
