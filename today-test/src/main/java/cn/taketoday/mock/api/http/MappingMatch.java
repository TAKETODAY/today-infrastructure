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

package cn.taketoday.mock.api.http;

/**
 * <p>
 * Enumeration of Servlet mapping types.
 * </p>
 *
 * @since Servlet 4.0
 */
public enum MappingMatch {
  /**
   * <p>
   * This is used when the mapping was achieved with an exact match to the application's context root.
   * </p>
   */
  CONTEXT_ROOT,
  /**
   * <p>
   * This is used when the mapping was achieved with an exact match to the default servlet of the application, the
   * '{@code /}' character.
   * </p>
   */
  DEFAULT,
  /**
   * <p>
   * This is used when the mapping was achieved with an exact match to the incoming request.
   * </p>
   */
  EXACT,
  /**
   * <p>
   * This is used when the mapping was achieved using an extension, such as "{@code *.xhtml}".
   * </p>
   */
  EXTENSION,
  /**
   * <p>
   * This is used when the mapping was achieved using a path, such as "{@code /faces/*}".
   * </p>
   */
  PATH
}
