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

package infra.core.style;

import org.jspecify.annotations.Nullable;

/**
 * Strategy that encapsulates value String styling algorithms
 * according to conventions.
 *
 * @author Keith Donald
 * @since 4.0
 */
public interface ValueStyler {

  /**
   * Style the given value, returning a String representation.
   *
   * @param value the Object value to style
   * @return the styled String
   */
  String style(@Nullable Object value);

}
