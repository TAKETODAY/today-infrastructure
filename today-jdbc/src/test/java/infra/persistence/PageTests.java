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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/13 20:39
 */
class PageTests {

  @Test
  void shouldCreatePageWithValidData() {
    Pageable pageable = Pageable.of(1, 10);
    List<String> rows = List.of("row1", "row2", "row3");
    Page<String> page = new Page<>(pageable, 100, rows);

    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.getLimit()).isEqualTo(10);
    assertThat(page.getTotalRows()).isEqualTo(100);
    assertThat(page.getTotalPages()).isEqualTo(10);
    assertThat(page.getRows()).containsExactly("row1", "row2", "row3");
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.isLastPage()).isFalse();
    assertThat(page.hasPrevPage()).isFalse();
    assertThat(page.hasNextPage()).isTrue();
    assertThat(page.getPrevPage()).isEqualTo(1);
    assertThat(page.getNextPage()).isEqualTo(2);
  }

  @Test
  void shouldHandleLastPageCorrectly() {
    Pageable pageable = Pageable.of(10, 10);
    List<String> rows = List.of("row1", "row2", "row3");
    Page<String> page = new Page<>(pageable, 93, rows);

    assertThat(page.getPageNumber()).isEqualTo(10);
    assertThat(page.isLastPage()).isTrue();
    assertThat(page.hasNextPage()).isFalse();
    assertThat(page.getNextPage()).isEqualTo(1);
    assertThat(page.hasPrevPage()).isTrue();
    assertThat(page.getPrevPage()).isEqualTo(9);
  }

  @Test
  void shouldHandlePageNumberCorrection() {
    Pageable pageable = Pageable.of(15, 10); // Page number exceeds total pages
    List<String> rows = List.of("row1", "row2");
    Page<String> page = new Page<>(pageable, 145, rows); // 15 total pages

    assertThat(page.getPageNumber()).isEqualTo(15);
    assertThat(page.isLastPage()).isTrue();
  }

  @Test
  void shouldHandleInvalidPageNumber() {
    List<String> rows = List.of("row1", "row2");
    Page<String> page = new Page<>(100, 0, 10, rows); // Invalid page number

    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.isFirstPage()).isTrue();
  }

  @Test
  void shouldMapRowsToNewType() {
    Pageable pageable = Pageable.of(1, 10);
    List<Integer> rows = List.of(1, 2, 3);
    Page<Integer> page = new Page<>(pageable, 100, rows);

    Page<String> stringPage = page.mapRows(String::valueOf);

    assertThat(stringPage.getRows()).containsExactly("1", "2", "3");
    assertThat(stringPage.getPageNumber()).isEqualTo(page.getPageNumber());
    assertThat(stringPage.getLimit()).isEqualTo(page.getLimit());
    assertThat(stringPage.getTotalRows()).isEqualTo(page.getTotalRows());
  }

  @Test
  void shouldApplyConsumerToRows() {
    Pageable pageable = Pageable.of(1, 10);
    List<String> rows = List.of("row1", "row2", "row3");
    Page<String> page = new Page<>(pageable, 100, rows);

    StringBuilder result = new StringBuilder();
    page.peek(result::append);

    assertThat(result.toString()).isEqualTo("row1row2row3");
  }

  @Test
  void shouldCreateNewPageWithDifferentRows() {
    Pageable pageable = Pageable.of(1, 10);
    List<String> originalRows = List.of("row1", "row2");
    Page<String> originalPage = new Page<>(pageable, 100, originalRows);

    List<Integer> newRows = List.of(1, 2, 3);
    Page<Integer> newPage = originalPage.withRows(newRows);

    assertThat(newPage.getRows()).containsExactly(1, 2, 3);
    assertThat(newPage.getPageNumber()).isEqualTo(originalPage.getPageNumber());
    assertThat(newPage.getLimit()).isEqualTo(originalPage.getLimit());
    assertThat(newPage.getTotalRows()).isEqualTo(originalPage.getTotalRows());
  }

  @Test
  void shouldMapPageToDifferentType() {
    Pageable pageable = Pageable.of(1, 10);
    List<String> rows = List.of("row1", "row2");
    Page<String> page = new Page<>(pageable, 100, rows);

    String result = page.map(p -> p.getRows().toString() + "-page-" + p.getPageNumber());

    assertThat(result).isEqualTo("[row1, row2]-page-1");
  }

  @Test
  void shouldHandleEmptyRows() {
    Pageable pageable = Pageable.of(1, 10);
    List<String> rows = List.of();
    Page<String> page = new Page<>(pageable, 0, rows);

    assertThat(page.getRows()).isEmpty();
    assertThat(page.getTotalPages()).isEqualTo(1);
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.isLastPage()).isTrue();
  }

  @Test
  void shouldCalculateTotalPagesCorrectly() {
    // Test with exact division
    Page<String> page1 = new Page<>(100, 1, 10, List.of());
    assertThat(page1.getTotalPages()).isEqualTo(10);

    // Test with remainder
    Page<String> page2 = new Page<>(101, 1, 10, List.of());
    assertThat(page2.getTotalPages()).isEqualTo(11);

    // Test with zero total
    Page<String> page3 = new Page<>(0, 1, 10, List.of());
    assertThat(page3.getTotalPages()).isEqualTo(1);
  }

  @Test
  void shouldHandleNullRowsInConstructor() {
    Page<String> page = new Page<>(100, 1, 10, null);

    assertThat(page.getRows()).isEmpty();
    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.getLimit()).isEqualTo(10);
    assertThat(page.getTotalRows()).isEqualTo(100);
  }

  @Test
  void shouldHandleEdgeCaseWhereTotalRowsIsZero() {
    Page<String> page = new Page<>(0, 1, 10, List.of());

    assertThat(page.getTotalPages()).isEqualTo(1);
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.isLastPage()).isTrue();
    assertThat(page.hasPrevPage()).isFalse();
    assertThat(page.hasNextPage()).isFalse();
  }

  @Test
  void shouldHandleSingleItemOnSinglePage() {
    Page<String> page = new Page<>(1, 1, 10, List.of("only"));

    assertThat(page.getTotalPages()).isEqualTo(1);
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.isLastPage()).isTrue();
    assertThat(page.hasPrevPage()).isFalse();
    assertThat(page.hasNextPage()).isFalse();
    assertThat(page.getRows()).containsExactly("only");
  }

  @Test
  void shouldReturnSameInstanceAfterPeekOperation() {
    Page<String> originalPage = new Page<>(100, 1, 10, List.of("row1", "row2"));
    Page<String> returnedPage = originalPage.peek(s -> { });

    assertThat(returnedPage).isSameAs(originalPage);
  }

  @Test
  void shouldCorrectlyIdentifyFirstAndLastPageWhenOnlyOnePageExists() {
    Page<String> page = new Page<>(5, 1, 10, List.of("row1", "row2"));

    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.isLastPage()).isTrue();
    assertThat(page.getTotalPages()).isEqualTo(1);
  }

  @Test
  void shouldHandleLargePageNumberThatExceedsTotalPages() {
    Page<String> page = new Page<>(50, 100, 10, List.of("row1", "row2"));

    assertThat(page.getPageNumber()).isEqualTo(5); // Should be corrected to totalPages
    assertThat(page.isLastPage()).isTrue();
  }

  @Test
  void shouldCreateEqualPagesWithSameProperties() {
    Page<String> page1 = new Page<>(100, 2, 10, List.of("row1", "row2"));
    Page<String> page2 = new Page<>(100, 2, 10, List.of("row1", "row2"));

    assertThat(page1).isEqualTo(page1);
    assertThat(page1).isEqualTo(page2);
    assertThat(page1.hashCode()).isEqualTo(page2.hashCode());
  }

  @Test
  void shouldCreateUnequalPagesWithDifferentProperties() {
    Page<String> page1 = new Page<>(100, 1, 10, List.of("row1", "row2"));
    Page<String> page2 = new Page<>(100, 2, 10, List.of("row1", "row2"));

    assertThat(page1).isNotEqualTo(page2);
  }

  @Test
  void shouldGenerateProperToString() {
    Page<String> page = new Page<>(100, 1, 10, List.of("row1", "row2"));
    String toString = page.toString();

    assertThat(toString).contains("pageNumber = 1");
    assertThat(toString).contains("limit = 10");
    assertThat(toString).contains("totalRows = 100");
    assertThat(toString).contains("rows = list['row1', 'row2']");
  }

  @Test
  void shouldHandlePageableConstructor() {
    Pageable pageable = Pageable.of(3, 20);
    List<String> rows = List.of("item1", "item2");
    Page<String> page = new Page<>(pageable, 100, rows);

    assertThat(page.getPageNumber()).isEqualTo(3);
    assertThat(page.getLimit()).isEqualTo(20);
    assertThat(page.getTotalRows()).isEqualTo(100);
    assertThat(page.getRows()).containsExactly("item1", "item2");
  }

  @Test
  void shouldHandlePageNumberLessThanOne() {
    Page<String> page = new Page<>(50, -1, 10, List.of("row1"));

    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.isFirstPage()).isTrue();
  }

  @Test
  void shouldHandleZeroLimit() {
    assertThatThrownBy(() -> new Page<>(50, 1, 0, List.of("row1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("limit must great than 0");
  }

  @Test
  void shouldHandleNavigationOnFirstPage() {
    Page<String> page = new Page<>(100, 1, 10, List.of("row1", "row2"));

    assertThat(page.getPageNumber()).isEqualTo(1);
    assertThat(page.isFirstPage()).isTrue();
    assertThat(page.hasPrevPage()).isFalse();
    assertThat(page.getPrevPage()).isEqualTo(1);
    assertThat(page.hasNextPage()).isTrue();
    assertThat(page.getNextPage()).isEqualTo(2);
  }

  @Test
  void shouldHandleNavigationOnMiddlePage() {
    Page<String> page = new Page<>(100, 5, 10, List.of("row1", "row2"));

    assertThat(page.getPageNumber()).isEqualTo(5);
    assertThat(page.isFirstPage()).isFalse();
    assertThat(page.isLastPage()).isFalse();
    assertThat(page.hasPrevPage()).isTrue();
    assertThat(page.getPrevPage()).isEqualTo(4);
    assertThat(page.hasNextPage()).isTrue();
    assertThat(page.getNextPage()).isEqualTo(6);
  }

  @Test
  void shouldMapEmptyRows() {
    Page<String> page = new Page<>(100, 1, 10, List.of());
    Page<Integer> mappedPage = page.mapRows(String::length);

    assertThat(mappedPage.getRows()).isEmpty();
    assertThat(mappedPage.getPageNumber()).isEqualTo(page.getPageNumber());
    assertThat(mappedPage.getLimit()).isEqualTo(page.getLimit());
  }

  @Test
  void shouldMapWithNullMapper() {
    Page<String> page = new Page<>(100, 1, 10, List.of("test"));

    assertThatThrownBy(() -> page.mapRows(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldPeekWithEmptyRows() {
    Page<String> page = new Page<>(100, 1, 10, List.of());
    StringBuilder result = new StringBuilder();

    Page<String> returnedPage = page.peek(result::append);

    assertThat(result.toString()).isEmpty();
    assertThat(returnedPage).isSameAs(page);
  }

  @Test
  void shouldPeekWithNullConsumer() {
    Page<String> page = new Page<>(100, 1, 10, List.of("test"));

    assertThatThrownBy(() -> page.peek(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldCreateNewPageWithNullRows() {
    Page<String> originalPage = new Page<>(100, 1, 10, List.of("row1", "row2"));
    Page<Integer> newPage = originalPage.withRows(null);

    assertThat(newPage.getRows()).isEmpty();
    assertThat(newPage.getPageNumber()).isEqualTo(originalPage.getPageNumber());
    assertThat(newPage.getLimit()).isEqualTo(originalPage.getLimit());
    assertThat(newPage.getTotalRows()).isEqualTo(originalPage.getTotalRows());
  }

  @Test
  void shouldHandleEqualsWithNull() {
    Page<String> page = new Page<>(100, 1, 10, List.of("row1"));

    assertThat(page).isNotEqualTo(null);
  }

  @Test
  void shouldHandleEqualsWithDifferentType() {
    Page<String> page = new Page<>(100, 1, 10, List.of("row1"));

    assertThat(page).isNotEqualTo("not a page");
  }

}