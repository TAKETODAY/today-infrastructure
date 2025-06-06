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

package infra.web.client;

/**
 * Contract to format the API version for a request.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DefaultApiVersionInserter.Builder#withVersionFormatter(ApiVersionFormatter)
 * @since 5.0
 */
@FunctionalInterface
public interface ApiVersionFormatter {

  /**
   * Format the given version Object into a String value.
   *
   * @param version the version to format
   * @return the final String version to use
   */
  String formatVersion(Object version);

}
