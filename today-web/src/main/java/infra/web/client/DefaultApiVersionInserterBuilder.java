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

import infra.lang.Nullable;

/**
 * Default implementation of {@link ApiVersionInserter.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ApiVersionInserter#forHeader(String)
 * @see ApiVersionInserter#forQueryParam(String)
 * @see ApiVersionInserter#forPathSegment(Integer)
 * @since 5.0
 */
final class DefaultApiVersionInserterBuilder implements ApiVersionInserter.Builder {

  @Nullable
  private String header;

  @Nullable
  private String queryParam;

  @Nullable
  private String mediaTypeParam;

  @Nullable
  private Integer pathSegmentIndex;

  @Nullable
  private ApiVersionFormatter versionFormatter;

  DefaultApiVersionInserterBuilder(@Nullable String header, @Nullable String queryParam,
          @Nullable String mediaTypeParam, @Nullable Integer pathSegmentIndex) {

    this.header = header;
    this.queryParam = queryParam;
    this.mediaTypeParam = mediaTypeParam;
    this.pathSegmentIndex = pathSegmentIndex;
  }

  /**
   * Configure the inserter to set a header.
   *
   * @param header the name of the header to hold the version
   */
  @Override
  public ApiVersionInserter.Builder useHeader(@Nullable String header) {
    this.header = header;
    return this;
  }

  @Override
  public ApiVersionInserter.Builder useQueryParam(@Nullable String queryParam) {
    this.queryParam = queryParam;
    return this;
  }

  @Override
  public ApiVersionInserter.Builder useMediaTypeParam(@Nullable String param) {
    this.mediaTypeParam = param;
    return this;
  }

  @Override
  public ApiVersionInserter.Builder usePathSegment(@Nullable Integer pathSegmentIndex) {
    this.pathSegmentIndex = pathSegmentIndex;
    return this;
  }

  @Override
  public ApiVersionInserter.Builder withVersionFormatter(ApiVersionFormatter versionFormatter) {
    this.versionFormatter = versionFormatter;
    return this;
  }

  public ApiVersionInserter build() {
    return new DefaultApiVersionInserter(
            this.header, this.queryParam, this.mediaTypeParam, this.pathSegmentIndex,
            this.versionFormatter);
  }

}
