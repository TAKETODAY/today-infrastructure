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

import java.util.List;

/**
 * A page-numbered slice of results without a total row or page count.
 * The presence of a next slice is determined by fetching one additional row.
 *
 * @param rows the rows in the requested slice
 * @param pageNumber the one-based page number
 * @param pageSize the requested number of rows per slice
 * @param hasNext whether another slice exists
 * @param <T> the type of each row
 * @since 5.0
 */
public record Slice<T>(List<T> rows, int pageNumber, int pageSize, boolean hasNext) {

  public Slice {
    if (pageNumber < 1 || pageSize < 1) {
      throw new IllegalArgumentException("Page number and page size must be positive");
    }
    rows = List.copyOf(rows);
  }

  /**
   * Whether a previous slice can be requested.
   *
   * @return {@code true} when the requested page number is greater than one
   */
  public boolean hasPrevious() {
    return pageNumber > 1;
  }
}
