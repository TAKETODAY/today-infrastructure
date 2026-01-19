/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.persistence;

import org.jspecify.annotations.Nullable;

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
   * Creates a new {@code Pageable} instance representing the pagination information.
   *
   * <p>This method is used to construct a {@code Pageable} object with the specified page number
   * and page size. The resulting object can be used to control pagination in data retrieval
   * operations, such as querying a database or fetching results from a service.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   *   // Create a Pageable instance for the 3rd page with 10 items per page
   *   Pageable pageable = Pageable.of(3, 10);
   *
   *   // Access the page number and page size
   *   System.out.println("Page Number: " + pageable.pageNumber());
   *   System.out.println("Page Size: " + pageable.pageSize());
   *
   *   // Use the Pageable instance in a query (example)
   *   List<Item> items = repository.findItems(pageable);
   * }</pre>
   *
   * @param pageNumber the one-based index of the current page (must be non-negative)
   * @param pageSize the number of items to include in each page (must be positive)
   * @return a new {@code Pageable} instance configured with the given page number and page size
   * @throws IllegalArgumentException if {@code pageNumber} is negative or {@code pageSize} is not positive
   */
  static Pageable of(int pageNumber, int pageSize) {
    return new SimplePageable(pageNumber, pageSize);
  }

  /**
   * Safely unwraps the given source object to a {@link Pageable} instance if applicable.
   *
   * <p>This method checks whether the provided source object is an instance of {@code Pageable}.
   * If the check passes, the object is cast to {@code Pageable} and returned. Otherwise, the
   * method returns {@code null}.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   *   Object obj = Pageable.of(2, 10);
   *
   *   // Attempt to unwrap the object to a Pageable
   *   Pageable pageable = Pageable.unwrap(obj);
   *
   *   if (pageable != null) {
   *     System.out.println("Page number: " + pageable.pageNumber());
   *     System.out.println("Page size: " + pageable.pageSize());
   *   }
   *   else {
   *     System.out.println("The object is not a Pageable instance.");
   *   }
   * }</pre>
   *
   * @param source the object to be unwrapped; may be {@code null}
   * @return the unwrapped {@code Pageable} instance if the source is a {@code Pageable},
   * or {@code null} if the source is not a {@code Pageable} or is {@code null}
   */
  static @Nullable Pageable unwrap(@Nullable Object source) {
    return source instanceof Pageable pageable ? pageable : null;
  }

}
