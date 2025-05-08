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

/**
 * An enumeration representing the sorting order for query results.
 *
 * <p>This enum defines two constants:
 * <ul>
 *   <li>{@code ASC}: Represents ascending order.</li>
 *   <li>{@code DESC}: Represents descending order.</li>
 * </ul>
 *
 * <p>It is typically used in query operations to specify the desired order
 * of the results. For example, it can be used in combination with sorting
 * annotations or methods to define how data should be ordered.
 */
public enum Order {
  ASC, DESC
}
