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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.jdbc.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.core.style.ToStringBuilder;

public class Page<T> {

  /**
   * current pageNum number
   */
  private int pageNum = 1;

  /**
   * How many pages per pageNum
   */
  private int limit = 10;

  /**
   * prev pageNum number
   */
  private int prevPage = 1;

  /**
   * next pageNum number
   */
  private int nextPage = 1;

  /**
   * total pageNum count
   */
  private int totalPages = 1;

  /**
   * total row count
   */
  private long totalRows = 0L;

  /**
   * row list
   */
  private List<T> rows;

  /**
   * is first pageNum
   */
  private boolean isFirstPage = false;

  /**
   * is last pageNum
   */
  private boolean isLastPage = false;

  /**
   * has prev pageNum
   */
  private boolean hasPrevPage = false;

  /**
   * has next pageNum
   */
  private boolean hasNextPage = false;

  /**
   * navigation pageNum number
   */
  private int navPages = 8;

  /**
   * all navigation pageNum number
   */
  private int[] navPageNums;

  public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
    Page<R> page = new Page<>(this.totalRows, this.pageNum, this.limit);
    if (null != rows) {
      page.setRows(rows.stream().map(mapper).collect(Collectors.toList()));
    }
    return page;
  }

  public Page<T> peek(Consumer<T> consumer) {
    if (null != rows) {
      this.rows = rows.stream().peek(consumer).collect(Collectors.toList());
    }
    return this;
  }

  public Page<T> navPages(int navPages) {
    // calculation of navigation pageNum after basic parameter setting
    this.calcNavigatePageNumbers(navPages);
    return this;
  }

  public Page() {
  }

  public Page(long total, int page, int limit) {
    init(total, page, limit);
  }

  private void init(long total, int pageNum, int limit) {
    // set basic params
    this.totalRows = total;
    this.limit = limit;
    this.totalPages = (int) ((this.totalRows - 1) / this.limit + 1);

    // automatic correction based on the current number of the wrong input
    if (pageNum < 1) {
      this.pageNum = 1;
    }
    else if (pageNum > this.totalPages) {
      this.pageNum = this.totalPages;
    }
    else {
      this.pageNum = pageNum;
    }

    // calculation of navigation pageNum after basic parameter setting
    this.calcNavigatePageNumbers(this.navPages);

    // and the determination of pageNum boundaries
    judgePageBoudary();
  }

  private void calcNavigatePageNumbers(int navPages) {
    // when the total number of pages is less than or equal to the number of navigation pages
    if (this.totalPages <= navPages) {
      navPageNums = new int[totalPages];
      for (int i = 0; i < totalPages; i++) {
        navPageNums[i] = i + 1;
      }
    }
    else {
      // when the total number of pages is greater than the number of navigation pages
      navPageNums = new int[navPages];
      int startNum = pageNum - navPages / 2;
      int endNum = pageNum + navPages / 2;
      if (startNum < 1) {
        startNum = 1;
        for (int i = 0; i < navPages; i++) {
          navPageNums[i] = startNum++;
        }
      }
      else if (endNum > totalPages) {
        endNum = totalPages;
        for (int i = navPages - 1; i >= 0; i--) {
          navPageNums[i] = endNum--;
        }
      }
      else {
        for (int i = 0; i < navPages; i++) {
          navPageNums[i] = startNum++;
        }
      }
    }
  }

  private void judgePageBoudary() {
    isFirstPage = pageNum == 1;
    isLastPage = pageNum == totalPages && pageNum != 1;
    hasPrevPage = pageNum != 1;
    hasNextPage = pageNum != totalPages;
    if (hasNextPage) {
      nextPage = pageNum + 1;
    }
    if (hasPrevPage) {
      prevPage = pageNum - 1;
    }
  }

  //

  public int getPageNum() {
    return pageNum;
  }

  public void setPageNum(int pageNum) {
    this.pageNum = pageNum;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getPrevPage() {
    return prevPage;
  }

  public void setPrevPage(int prevPage) {
    this.prevPage = prevPage;
  }

  public int getNextPage() {
    return nextPage;
  }

  public void setNextPage(int nextPage) {
    this.nextPage = nextPage;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(int totalPages) {
    this.totalPages = totalPages;
  }

  public long getTotalRows() {
    return totalRows;
  }

  public void setTotalRows(long totalRows) {
    this.totalRows = totalRows;
  }

  public List<T> getRows() {
    return rows;
  }

  public void setRows(List<T> rows) {
    this.rows = rows;
  }

  public boolean isFirstPage() {
    return isFirstPage;
  }

  public void setFirstPage(boolean firstPage) {
    isFirstPage = firstPage;
  }

  public boolean isLastPage() {
    return isLastPage;
  }

  public void setLastPage(boolean lastPage) {
    isLastPage = lastPage;
  }

  public boolean isHasPrevPage() {
    return hasPrevPage;
  }

  public void setHasPrevPage(boolean hasPrevPage) {
    this.hasPrevPage = hasPrevPage;
  }

  public boolean isHasNextPage() {
    return hasNextPage;
  }

  public void setHasNextPage(boolean hasNextPage) {
    this.hasNextPage = hasNextPage;
  }

  public int getNavPages() {
    return navPages;
  }

  public void setNavPages(int navPages) {
    this.navPages = navPages;
  }

  public int[] getNavPageNums() {
    return navPageNums;
  }

  public void setNavPageNums(int[] navPageNums) {
    this.navPageNums = navPageNums;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("pageNum", pageNum)
            .append("limit", limit)
            .append("prevPage", prevPage)
            .append("nextPage", nextPage)
            .append("totalPages", totalPages)
            .append("totalRows", totalRows)
            .append("rows", rows)
            .append("isFirstPage", isFirstPage)
            .append("isLastPage", isLastPage)
            .append("hasPrevPage", hasPrevPage)
            .append("hasNextPage", hasNextPage)
            .append("navPages", navPages)
            .append("navPageNums", navPageNums)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Page<?> page) {
      return pageNum == page.pageNum
              && limit == page.limit
              && navPages == page.navPages
              && prevPage == page.prevPage
              && nextPage == page.nextPage
              && totalRows == page.totalRows
              && totalPages == page.totalPages
              && isLastPage == page.isLastPage
              && isFirstPage == page.isFirstPage
              && hasPrevPage == page.hasPrevPage
              && hasNextPage == page.hasNextPage
              && Objects.equals(rows, page.rows)
              && Arrays.equals(navPageNums, page.navPageNums);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(pageNum, limit, prevPage,
            nextPage, totalPages, totalRows, rows, isFirstPage,
            isLastPage, hasPrevPage, hasNextPage, navPages);
    result = 31 * result + Arrays.hashCode(navPageNums);
    return result;
  }
}