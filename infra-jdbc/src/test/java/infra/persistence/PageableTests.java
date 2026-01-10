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