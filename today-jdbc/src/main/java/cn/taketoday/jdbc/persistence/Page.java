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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.core.style.ToStringBuilder;

/**
 * Page holder
 *
 * @param <T> content type
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
   * @param pageable page params
   * @param total total rows count
   * @param rows rows data
   */
  public Page(Pageable pageable, Number total, List<T> rows) {
    this(total, pageable.pageNumber(), pageable.pageSize(), rows);
  }

  public Page(Number total, int pageNumber, int limit, List<T> rows) {
    // set basic params
    this.totalRows = total;
    this.limit = limit;
    this.rows = rows;
    this.totalPages = (int) ((total.longValue() - 1) / limit + 1);

    // automatic correction based on the current number of the wrong input
    if (pageNumber >= 1) {
      this.pageNumber = Math.min(pageNumber, this.totalPages);
    }
    else {
      this.pageNumber = 1;
    }

    this.firstPage = this.pageNumber == 1;
    this.lastPage = this.totalPages == this.pageNumber && pageNumber != 1;
    // and the determination of pageNum boundaries
    this.hasPrevPage = pageNumber != 1;
    this.hasNextPage = pageNumber != totalPages;
    this.nextPage = hasNextPage ? (pageNumber + 1) : 1;
    this.prevPage = hasPrevPage ? (pageNumber - 1) : 1;
  }

  //

  public <R> Page<R> mapRows(Function<? super T, ? extends R> mapper) {
    return withRows(rows.stream().map(mapper).collect(Collectors.toList()));
  }

  public <R> R map(Function<Page<T>, R> mapper) {
    return mapper.apply(this);
  }

  public Page<T> peek(Consumer<T> consumer) {
    if (rows != null) {
      for (T row : rows) {
        consumer.accept(row);
      }
    }
    return this;
  }

  public <E> Page<E> withRows(List<E> rows) {
    return new Page<>(totalRows, pageNumber, limit, rows);
  }

  //

  public int getPageNumber() {
    return pageNumber;
  }

  public int getLimit() {
    return limit;
  }

  public int getPrevPage() {
    return prevPage;
  }

  public int getNextPage() {
    return nextPage;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public Number getTotalRows() {
    return totalRows;
  }

  public List<T> getRows() {
    return rows;
  }

  public boolean isFirstPage() {
    return firstPage;
  }

  public boolean isLastPage() {
    return lastPage;
  }

  public boolean isHasPrevPage() {
    return hasPrevPage;
  }

  public boolean isHasNextPage() {
    return hasNextPage;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
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