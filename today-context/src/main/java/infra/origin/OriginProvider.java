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

package infra.origin;

import infra.lang.Nullable;

/**
 * Interface to provide access to the origin of an item.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Origin
 * @since 4.0
 */
@FunctionalInterface
public interface OriginProvider {

  /**
   * Return the source origin or {@code null} if the origin is not known.
   *
   * @return the origin or {@code null}
   */
  @Nullable
  Origin getOrigin();

}
