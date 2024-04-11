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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/11 14:56
 */
class PageableTests {

  @Test
  void pageNumber() {
    assertThat(Pageable.of(1, 10).pageNumber()).isEqualTo(1);
    assertThat(Pageable.of(3, 10).pageNumber()).isEqualTo(3);
    assertThat(Pageable.of(4, 10).pageNumber()).isEqualTo(4);
    assertThat(Pageable.of(4, 1).pageNumber()).isEqualTo(4);
  }

  @Test
  void pageSize() {
    assertThat(Pageable.of(1, 10).pageSize()).isEqualTo(10);
    assertThat(Pageable.of(1, 10).pageSize(5)).isEqualTo(5);
    assertThat(Pageable.of(2, 10).pageSize(5)).isEqualTo(5);
  }

  @Test
  void offset() {
    assertThat(Pageable.of(1, 10).offset()).isEqualTo(0);
    assertThat(Pageable.of(2, 10).offset()).isEqualTo(10);
    assertThat(Pageable.of(1, 10).offset(5)).isEqualTo(0);

    assertThat(Pageable.of(2, 10).offset(5)).isEqualTo(5);
    assertThat(Pageable.of(3, 10).offset(5)).isEqualTo(10);
  }

  @Test
  void of() {
    assertThat(Pageable.of(1, 10)).isEqualTo(new SimplePageable(1, 10));
  }

  @Test
  void unwrap() {
    assertThat(Pageable.unwrap(1)).isNull();
    assertThat(Pageable.unwrap(null)).isNull();
    assertThat(Pageable.unwrap(Pageable.of(1, 10))).isNotNull();
  }

  @Test
  void toString_() {
    assertThat(Pageable.of(1, 10).toString()).endsWith("pageNumber = 1, pageSize = 10]");
  }

}