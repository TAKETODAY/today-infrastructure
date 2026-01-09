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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import infra.core.style.ToStringBuilder;
import infra.lang.Assert;

/**
 * Represents a paginated result set containing a subset of data from a larger dataset.
 * This class provides information about the current page, total pages, row count,
 * and navigation options such as previous and next pages. It also supports operations
 * like mapping rows to a different type or applying actions to each row.
 *
 * <p><b>Usage Example:</b></p>
 *
 * <pre>{@code
 * // Create a pageable object with page number 2 and page size 10
 * Pageable pageable = Pageable.of(2, 10);
 *
 * // Simulate a database query result
 * List<String> allRows = List.of("row1", "row2", "row3", ..., "row100");
 * Number totalRows = allRows.size();
 * List<String> currentPageRows = allRows.subList(pageable.offset(), pageable.offset() + pageable.pageSize());
 *
 * // Create a Page instance
 * Page<String> page = new Page<>(pageable, totalRows, currentPageRows);
 *
 * // Access page information
 * System.out.println("Current Page: " + page.getPageNumber());
 * System.out.println("Total Pages: " + page.getTotalPages());
 * System.out.println("Has Next Page: " + page.isHasNextPage());
 *
 * // Map rows to uppercase
 * Page<String> upperCasePage = page.mapRows(String::toUpperCase);
 *
 * // Print mapped rows
 * upperCasePage.getRows().forEach(System.out::println);
 * }</pre>
 *
 * <p>This class is immutable and thread-safe. To create a new instance with modified
 * rows, use the {@link #withRows(List)} method.</p>
 *
 * @param <T> the type of elements in the rows list
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Page<T> {

  /**
   * current pageNum number
   */
  private final int pageNumber;

  /**
   * How many pages per pageNum
   */
  private final int limit;

  /**
   * prev pageNum number
   */
  private final int prevPage;

  /**
   * next pageNum number
   */
  private final int nextPage;

  /**
   * total pageNum count
   */
  private final int totalPages;

  /**
   * total row count
   */
  private final Number totalRows;

  /**
   * row list
   */
  private final List<T> rows;

  /**
   * is first pageNum
   */
  private final boolean firstPage;

  /**
   * is last pageNum
   */
  private final boolean lastPage;

  /**
   * has prev pageNum
   */
  private final boolean hasPrevPage;

  /**
   * has next pageNum
   */
  private final boolean hasNextPage;

  /**
   * Constructs a new {@code Page} instance with the specified pagination details.
   * <p>This constructor is typically used to represent a paginated result set,
   * including the total number of elements, the current page number, the page size,
   * and the rows (data) for the current page.
   *
   * @param pageable the {@link Pageable} object containing pagination information,
   * such as page number and page size
   * @param total the total number of elements across all pages. This value is
   * typically retrieved from the database or another data source
   * @param rows the list of elements representing the data for the current page
   */
  public Page(Pageable pageable, Number total, @Nullable List<T> rows) {
    this(total, pageable.pageNumber(), pageable.pageSize(), rows);
  }

  /**
   * Constructs a new {@code Page} object representing a paginated result set.
   * <p>This constructor initializes the pagination properties based on the provided
   * parameters and performs automatic correction for invalid input values.
   *
   * @param total the total number of rows in the dataset. Must not be null.
   * @param pageNumber the current page number requested by the user.
   * If less than 1, it will default to 1.
   * @param limit the maximum number of rows per page. Must be greater than 0.
   * @param rows the list of rows to be displayed on the current page.
   * Can be empty but must not be null.
   */
  public Page(Number total, int pageNumber, int limit, @Nullable List<T> rows) {
    Assert.isTrue(limit > 0, "limit must great than 0");
    // set basic params
    this.totalRows = total;
    this.limit = limit;
    this.rows = rows == null ? Collections.emptyList() : rows;
    this.totalPages = (int) ((total.longValue() - 1) / limit + 1);

    // automatic correction based on the current number of the wrong input
    if (pageNumber >= 1) {
      this.pageNumber = Math.min(pageNumber, this.totalPages);
    }
    else {
      this.pageNumber = 1;
    }

    this.firstPage = this.pageNumber == 1;
    this.lastPage = this.totalPages == this.pageNumber;
    // and the determination of pageNum boundaries
    this.hasPrevPage = pageNumber != 1;
    this.hasNextPage = pageNumber != totalPages;
    this.nextPage = hasNextPage ? (pageNumber + 1) : 1;
    this.prevPage = hasPrevPage ? (pageNumber - 1) : 1;
  }

  //

  /**
   * Maps the rows of this {@code Page} to a new type using the provided mapping function.
   *
   * <p>This method applies the given {@code mapper} function to each row in the current page,
   * transforming the rows into a new type. The resulting {@code Page} retains the original
   * pagination metadata (e.g., page number, total rows) but contains the transformed rows.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Transform a Page<User> to a Page<UserDto>
   *   Page<User> userPage = ...;
   *
   *   Page<UserDto> userDtoPage = userPage.mapRows(user ->
   *     new UserDto(user.getName(), user.getEmail())
   *   );
   * }</pre>
   *
   * <p>The example above demonstrates how to convert a {@code Page} of {@code User} objects
   * into a {@code Page} of {@code UserDto} objects by applying a mapping function to each row.
   *
   * @param <R> the type of the transformed rows
   * @param mapper a function that transforms each row of type {@code T} into a new type {@code R}.
   * Must not be null.
   * @return a new {@code Page} containing the transformed rows, with the same pagination metadata
   * as the original page.
   */
  public <R> Page<R> mapRows(Function<? super T, ? extends R> mapper) {
    return withRows(rows.stream().map(mapper).collect(Collectors.toList()));
  }

  /**
   * Transforms this {@code Page} instance using the provided mapping function.
   *
   * <p>This method applies the given {@code mapper} function to the current {@code Page}
   * instance and returns the result. It is useful for converting a {@code Page} into
   * another type or transforming its structure while retaining the context of the page.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Transform a Page<User> to a DTO representation
   *   Page<User> userPage = ...;
   *
   *   PageDto dto = userPage.map(page -> {
   *     List<UserDto> userDtos = page.getRows().stream()
   *       .map(user -> new UserDto(user.getName(), user.getEmail()))
   *       .toList();
   *     return new PageDto(page.getPageNumber(), page.getTotalPages(), userDtos);
   *   });
   * }</pre>
   *
   * @param <R> the type of the result returned by the mapping function
   * @param mapper a function that takes the current {@code Page} and produces a result
   * of type {@code R}. Must not be null.
   * @return the result of applying the {@code mapper} function to this {@code Page}.
   */
  public <R> R map(Function<Page<T>, R> mapper) {
    return mapper.apply(this);
  }

  /**
   * Performs an action on each row of the current page without modifying the rows.
   *
   * <p>This method allows you to apply a {@link Consumer} function to each row in the
   * {@code rows} list of this {@code Page}. The original pagination metadata and rows
   * remain unchanged, and the method returns the same {@code Page} instance for chaining.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Print each user's name in the current page
   *   Page<User> userPage = ...;
   *
   *   userPage.peek(user -> {
   *     System.out.println("User: " + user.getName());
   *   });
   * }</pre>
   *
   * <p>In the example above, the {@code peek} method is used to print the name of each
   * user in the current page. The {@code userPage} instance remains unmodified after
   * the operation.
   *
   * @param consumer a non-null {@link Consumer} that defines the action to be performed
   * on each row in the current page. If {@code rows} is null or empty,
   * the consumer will not be invoked.
   * @return the same {@code Page} instance, allowing for method chaining.
   */
  public Page<T> peek(Consumer<T> consumer) {
    for (T row : rows) {
      consumer.accept(row);
    }
    return this;
  }

  /**
   * Creates a new {@code Page} instance with the specified rows, while retaining
   * the existing pagination metadata (total rows, page number, and limit).
   *
   * <p>This method is useful when you need to replace the rows of a {@code Page}
   * with a new set of data without altering the pagination context.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Create a new Page with updated rows
   *   List<String> originalRows = Arrays.asList("row1", "row2");
   *   Page<String> originalPage = new Page<>(100, 1, 10, originalRows);
   *
   *   List<String> newRows = Arrays.asList("newRow1", "newRow2");
   *   Page<String> updatedPage = originalPage.withRows(newRows);
   *
   *   System.out.println(updatedPage.getRows());
   *   // Output: [newRow1, newRow2]
   * }</pre>
   *
   * @param <E> the type of elements in the rows list
   * @param rows the new list of rows to be included in the page. Can be empty or null.
   * @return a new {@code Page} instance containing the specified rows and the same
   * pagination metadata as the original page.
   */
  public <E> Page<E> withRows(List<E> rows) {
    return new Page<>(totalRows, pageNumber, limit, rows);
  }

  //

  /**
   * Returns the current page number.
   *
   * <p>This method retrieves the page number of the current {@code Page} instance.
   * The page number is typically used in pagination to identify the position of the
   * current subset of data within a larger dataset.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Assuming a Page instance with pageNumber = 3
   *   Page<User> userPage = new Page<>(100, 3, 10, userList);
   *
   *   int currentPage = userPage.getPageNumber();
   *   System.out.println("Current Page: " + currentPage);
   *   // Output: Current Page: 3
   * }</pre>
   *
   * @return the current page number, which is a positive integer starting from 1.
   */
  public int getPageNumber() {
    return pageNumber;
  }

  /**
   * Returns the maximum number of rows allowed per page.
   *
   * <p>This method retrieves the limit, which defines the maximum number of rows that can be
   * included in a single page. This value is typically used in pagination to control the size
   * of each page.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Assuming a Page instance with limit = 10
   *   Page<User> userPage = new Page<>(100, 1, 10, userList);
   *
   *   int limit = userPage.getLimit();
   *   System.out.println("Limit per page: " + limit);
   *   // Output: Limit per page: 10
   * }</pre>
   *
   * @return the maximum number of rows allowed per page, which is a positive integer.
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Returns the previous page number relative to the current page.
   *
   * <p>This method retrieves the page number of the previous page in the pagination sequence.
   * If the current page is the first page, this method typically returns the first page number
   * (usually 1), depending on the implementation. It is useful for navigating backward in a
   * paginated dataset.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre>{@code
   *   // Assuming a Page instance with pageNumber = 3
   *   Page<User> userPage = new Page<>(100, 3, 10, userList);
   *
   *   int prevPage = userPage.getPrevPage();
   *   System.out.println("Previous Page: " + prevPage);
   *   // Output: Previous Page: 2
   *
   *   // If the current page is the first page
   *   Page<User> firstPage = new Page<>(100, 1, 10, userList);
   *
   *   int prevPageWhenFirst = firstPage.getPrevPage();
   *   System.out.println("Previous Page when on First Page: " + prevPageWhenFirst);
   *   // Output: Previous Page when on First Page: 1
   * }</pre>
   *
   * @return the page number of the previous page, or the first page number if the current page
   * is already the first page.
   */
  public int getPrevPage() {
    return prevPage;
  }

  /**
   * Returns the next page number to be used in pagination.
   * This method retrieves the value of the {@code nextPage} variable,
   * which is typically updated based on the current state of pagination.
   *
   * <p>Example usage:
   * <pre>{@code
   * Paginator paginator = new Paginator();
   * paginator.setNextPage(5);
   *
   * int nextPageNumber = paginator.getNextPage();
   * System.out.println("Next page: " + nextPageNumber);
   * }</pre>
   *
   * @return the next page number as an integer value
   */
  public int getNextPage() {
    return nextPage;
  }

  /**
   * Returns the total number of pages available in the current context.
   * This method is typically used in scenarios where data is paginated,
   * such as displaying a list of items across multiple pages.
   *
   * @return the total number of pages as an integer value
   */
  public int getTotalPages() {
    return totalPages;
  }

  /**
   * Returns the total number of rows in the dataset or table.
   *
   * <p>
   * This method is useful when you need to determine the size of the data for
   * processing or display purposes. For example, it can be used to validate the
   * amount of data retrieved or to configure pagination settings.
   *
   * @return the total number of rows as a {@link Number}, or {@code null} if the
   * information is not available or has not been set.
   */
  public Number getTotalRows() {
    return totalRows;
  }

  /**
   * Returns the list of rows stored in this object.
   *
   * This method is typically used to retrieve all the rows that have been
   * previously set or initialized within the object. The returned list is
   * a direct reference to the internal storage, so modifications to the list
   * may affect the internal state of the object.
   *
   * <p>Example usage:
   * <pre>{@code
   *  List<String> rows = table.getRows();
   *  for (String row : rows) {
   *    System.out.println(row);
   *  }
   * }</pre>
   * If no rows have been set, this method will return an empty list.
   *
   * @return the list of rows stored in this object. If no rows are present,
   * an empty list is returned.
   */
  public List<T> getRows() {
    return rows;
  }

  /**
   * Checks if the current instance represents the first page.
   *
   * <p>This method returns a boolean value indicating whether the current
   * object is associated with the first page. It is useful in scenarios where
   * pagination logic needs to be implemented, such as in web applications or
   * document processing systems.</p>
   *
   * @return true if the current instance represents the first page,
   * false otherwise.
   */
  public boolean isFirstPage() {
    return firstPage;
  }

  /**
   * Checks if the current page is the last page.
   *
   * <p>This method is useful when implementing pagination logic. For example,
   * it can be used to determine whether to display a "Next" button in a
   * user interface.
   *
   * @return true if the current page is the last page, false otherwise
   */
  public boolean isLastPage() {
    return lastPage;
  }

  /**
   * Checks if there is a previous page available.
   *
   * <p>This method returns a boolean value indicating whether the current context
   * has a previous page. It is typically used in pagination scenarios to enable
   * or disable navigation controls.
   *
   * @return true if a previous page exists, false otherwise
   */
  public boolean hasPrevPage() {
    return hasPrevPage;
  }

  /**
   * Checks if there is a next page available.
   *
   * <p>This method returns the value of the internal flag `hasNextPage`,
   * which indicates whether additional content or data exists for
   * pagination purposes. It is typically used in scenarios where data
   * is loaded dynamically, such as in web applications or APIs with
   * paginated responses.
   *
   * @return true if there is a next page available; false otherwise.
   */
  public boolean hasNextPage() {
    return hasNextPage;
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("pageNumber", pageNumber)
            .append("limit", limit)
            .append("prevPage", prevPage)
            .append("nextPage", nextPage)
            .append("totalPages", totalPages)
            .append("totalRows", totalRows)
            .append("isFirstPage", firstPage)
            .append("isLastPage", lastPage)
            .append("hasPrevPage", hasPrevPage)
            .append("hasNextPage", hasNextPage)
            .append("rows", rows)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Page<?> page) {
      return pageNumber == page.pageNumber
              && limit == page.limit
              && prevPage == page.prevPage
              && nextPage == page.nextPage
              && totalPages == page.totalPages
              && lastPage == page.lastPage
              && firstPage == page.firstPage
              && hasPrevPage == page.hasPrevPage
              && hasNextPage == page.hasNextPage
              && Objects.equals(totalRows, page.totalRows)
              && Objects.equals(rows, page.rows);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageNumber, limit, prevPage,
            nextPage, totalPages, totalRows, rows, firstPage,
            lastPage, hasPrevPage, hasNextPage);
  }

}