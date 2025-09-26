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

import org.jspecify.annotations.Nullable;

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

  /**
   * Create an inserter that sets a header.
   *
   * @param header the name of a header to hold the version
   */
  static ApiVersionInserter forHeader(@Nullable String header) {
    return new DefaultApiVersionInserterBuilder(header, null, null, null).build();
  }

  /**
   * Create an inserter that sets a query parameter.
   *
   * @param queryParam the name of a query parameter to hold the version
   */
  static ApiVersionInserter forQueryParam(@Nullable String queryParam) {
    return new DefaultApiVersionInserterBuilder(null, queryParam, null, null).build();
  }

  /**
   * Create an inserter to set a MediaType parameter on the "Content-Type" header.
   *
   * @param mediaTypeParam the name of the media type parameter to hold the version
   */
  static ApiVersionInserter forMediaTypeParam(@Nullable String mediaTypeParam) {
    return new DefaultApiVersionInserterBuilder(null, null, mediaTypeParam, null).build();
  }

  /**
   * Create an inserter that inserts a path segment.
   *
   * @param pathSegmentIndex the index of the path segment to hold the version
   */
  static ApiVersionInserter forPathSegment(@Nullable Integer pathSegmentIndex) {
    return new DefaultApiVersionInserterBuilder(null, null, null, pathSegmentIndex).build();
  }

  /**
   * Create a builder for an {@link ApiVersionInserter}.
   */
  static Builder builder() {
    return new DefaultApiVersionInserterBuilder(null, null, null, null);
  }

  /**
   * Builder for {@link ApiVersionInserter}.
   */
  interface Builder {

    /**
     * Configure the inserter to set a header.
     *
     * @param header the name of the header to hold the version
     */
    Builder useHeader(@Nullable String header);

    /**
     * Configure the inserter to set a query parameter.
     *
     * @param queryParam the name of the query parameter to hold the version
     */
    Builder useQueryParam(@Nullable String queryParam);

    /**
     * Create an inserter to set a MediaType parameter on the "Content-Type" header.
     *
     * @param param the name of the media type parameter to hold the version
     */
    Builder useMediaTypeParam(@Nullable String param);

    /**
     * Configure the inserter to insert a path segment.
     *
     * @param pathSegmentIndex the index of the path segment to hold the version
     */
    Builder usePathSegment(@Nullable Integer pathSegmentIndex);

    /**
     * Format the version Object into a String using the given {@link ApiVersionFormatter}.
     * <p>By default, the version is formatted with {@link Object#toString()}.
     *
     * @param versionFormatter the formatter to use
     */
    Builder withVersionFormatter(ApiVersionFormatter versionFormatter);

    /**
     * Build the {@link ApiVersionInserter} instance.
     */
    ApiVersionInserter build();

  }

}
