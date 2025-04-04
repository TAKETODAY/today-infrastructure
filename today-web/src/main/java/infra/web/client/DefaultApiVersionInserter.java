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
import java.util.ArrayList;
import java.util.List;

import infra.http.HttpHeaders;
import infra.lang.Assert;
import infra.web.util.UriComponentsBuilder;

/**
 * Default implementation of {@link ApiVersionInserter} to insert the version
 * into a request header, query parameter, or the URL path.
 *
 * <p>Use {@link #builder()} to create an instance.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class DefaultApiVersionInserter implements ApiVersionInserter {

  @Nullable
  private final String header;

  @Nullable
  private final String queryParam;

  @Nullable
  private final Integer pathSegmentIndex;

  private final ApiVersionFormatter versionFormatter;

  private DefaultApiVersionInserter(@Nullable String header, @Nullable String queryParam,
          @Nullable Integer pathSegmentIndex, @Nullable ApiVersionFormatter formatter) {
    Assert.isTrue(header != null || queryParam != null || pathSegmentIndex != null,
            "Expected 'header', 'queryParam', or 'pathSegmentIndex' to be configured");

    this.header = header;
    this.queryParam = queryParam;
    this.pathSegmentIndex = pathSegmentIndex;
    this.versionFormatter = formatter != null ? formatter : Object::toString;
  }

  @Override
  public URI insertVersion(Object version, URI uri) {
    if (this.queryParam == null && this.pathSegmentIndex == null) {
      return uri;
    }
    String formattedVersion = this.versionFormatter.formatVersion(version);
    UriComponentsBuilder builder = UriComponentsBuilder.forURI(uri);
    if (this.queryParam != null) {
      builder.queryParam(this.queryParam, formattedVersion);
    }
    if (this.pathSegmentIndex != null) {
      var pathSegments = new ArrayList<>(builder.build().getPathSegments());
      assertPathSegmentIndex(this.pathSegmentIndex, pathSegments.size(), uri);
      pathSegments.add(this.pathSegmentIndex, formattedVersion);
      builder.replacePath(null);
      for (String segment : pathSegments) {
        builder.pathSegment(segment);
      }
    }
    return builder.build().toURI();
  }

  private void assertPathSegmentIndex(Integer index, int pathSegmentsSize, URI uri) {
    if (index > pathSegmentsSize) {
      throw new IllegalStateException("Cannot insert version into '%s' at path segment index %d".formatted(uri.getPath(), index));
    }
  }

  @Override
  public void insertVersion(Object version, HttpHeaders headers) {
    if (this.header != null) {
      headers.set(this.header, this.versionFormatter.formatVersion(version));
    }
  }

  /**
   * Create a builder for an inserter that sets a header.
   *
   * @param header the name of a header to hold the version
   */
  public static Builder fromHeader(@Nullable String header) {
    return new Builder(header, null, null);
  }

  /**
   * Create a builder for an inserter that sets a query parameter.
   *
   * @param queryParam the name of a query parameter to hold the version
   */
  public static Builder fromQueryParam(@Nullable String queryParam) {
    return new Builder(null, queryParam, null);
  }

  /**
   * Create a builder for an inserter that inserts a path segment.
   *
   * @param pathSegmentIndex the index of the path segment to hold the version
   */
  public static Builder fromPathSegment(@Nullable Integer pathSegmentIndex) {
    return new Builder(null, null, pathSegmentIndex);
  }

  /**
   * Create a builder.
   */
  public static Builder builder() {
    return new Builder(null, null, null);
  }

  /**
   * A builder for {@link DefaultApiVersionInserter}.
   */
  public static final class Builder {

    @Nullable
    private String header;

    @Nullable
    private String queryParam;

    @Nullable
    private Integer pathSegmentIndex;

    @Nullable
    private ApiVersionFormatter versionFormatter;

    private Builder(@Nullable String header, @Nullable String queryParam, @Nullable Integer pathSegmentIndex) {
      this.header = header;
      this.queryParam = queryParam;
      this.pathSegmentIndex = pathSegmentIndex;
    }

    /**
     * Configure the inserter to set a header.
     *
     * @param header the name of the header to hold the version
     */
    public Builder fromHeader(@Nullable String header) {
      this.header = header;
      return this;
    }

    /**
     * Configure the inserter to set a query parameter.
     *
     * @param queryParam the name of the query parameter to hold the version
     */
    public Builder fromQueryParam(@Nullable String queryParam) {
      this.queryParam = queryParam;
      return this;
    }

    /**
     * Configure the inserter to insert a path segment.
     *
     * @param pathSegmentIndex the index of the path segment to hold the version
     */
    public Builder fromPathSegment(@Nullable Integer pathSegmentIndex) {
      this.pathSegmentIndex = pathSegmentIndex;
      return this;
    }

    /**
     * Format the version Object into a String using the given {@link ApiVersionFormatter}.
     * <p>By default, the version is formatted with {@link Object#toString()}.
     *
     * @param versionFormatter the formatter to use
     */
    public Builder withVersionFormatter(ApiVersionFormatter versionFormatter) {
      this.versionFormatter = versionFormatter;
      return this;
    }

    /**
     * Build the inserter.
     */
    public ApiVersionInserter build() {
      return new DefaultApiVersionInserter(
              this.header, this.queryParam, this.pathSegmentIndex, this.versionFormatter);
    }
  }

}
