/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.persistence;

import infra.lang.Nullable;

/**
 * Interface for pagination information. Provides methods to retrieve details
 * about the current page and the number of items to be returned.
 *
 * <p>This interface is typically used in scenarios where data needs to be
 * retrieved in chunks or pages, such as database queries or API responses.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 *   // Create a Pageable instance for the second page with 10 items per page
 *   Pageable pageable = Pageable.of(2, 10);
 *
 *   // Retrieve the page number and page size
 *   int pageNumber = pageable.pageNumber(); // Returns 2
 *   int pageSize = pageable.pageSize();     // Returns 10
 *
 *   // Calculate the offset for database queries
 *   int offset = pageable.offset();         // Returns 10
 *
 *   // Use the Pageable instance in a query
 *   List<Item> items = repository.findItems(pageable);
 * }</pre>
 *
 * <p>The {@link #offset()} method calculates the starting index for the current
 * page based on the page number and page size. This is useful for database
 * queries that require an offset.
 *
 * <p>The {@link #pageSize(int max)} method allows you to enforce a maximum limit
 * on the page size, ensuring that the returned number of items does not exceed
 * the specified maximum.
 *
 * <p>The static factory method {@link #of(int pageNumber, int pageSize)} provides
 * a convenient way to create a {@code Pageable} instance. Additionally, the
 * {@link #unwrap(Object)} method can be used to safely cast an object to a
 * {@code Pageable} if applicable.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Page
 * @since 4.0 2024/3/31 15:09
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
