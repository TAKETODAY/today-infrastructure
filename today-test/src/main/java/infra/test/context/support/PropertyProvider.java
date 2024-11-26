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

package infra.test.context.support;

import infra.lang.Nullable;

/**
 * Strategy for providing named properties &mdash; for example, for looking up
 * key-value pairs in a generic fashion.
 *
 * <p>Primarily intended for use within the framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@FunctionalInterface
public interface PropertyProvider {

  /**
   * Get the value of the named property.
   *
   * @param name the name of the property to retrieve
   * @return the value of the property or {@code null} if not found
   */
  @Nullable
  String get(String name);

}
