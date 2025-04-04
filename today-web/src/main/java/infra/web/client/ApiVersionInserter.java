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

import java.net.URI;

import infra.http.HttpHeaders;

/**
 * Contract to determine how to insert an API version into the URI or headers
 * of a request.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public interface ApiVersionInserter {

  /**
   * Insert the version into the URI.
   * <p>The default implementation returns the supplied URI unmodified.
   *
   * @param version the version to insert
   * @param uri the URI for the request
   * @return the updated URI, or the original URI unmodified
   */
  default URI insertVersion(Object version, URI uri) {
    return uri;
  }

  /**
   * Insert the version into the request headers.
   * <p>The default implementation does not modify the supplied headers.
   *
   * @param version the version to insert
   * @param headers the request headers
   */
  default void insertVersion(Object version, HttpHeaders headers) {
  }

}
