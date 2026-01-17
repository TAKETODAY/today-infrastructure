/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.client;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.web.util.UriComponentsBuilder;

/**
 * Default implementation of {@link ApiVersionInserter}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see DefaultApiVersionInserterBuilder
 * @since 5.0
 */
public final class DefaultApiVersionInserter implements ApiVersionInserter {

  @Nullable
  private final String header;

  @Nullable
  private final String queryParam;

  @Nullable
  private final String mediaTypeParam;

  @Nullable
  private final Integer pathSegmentIndex;

  private final ApiVersionFormatter versionFormatter;

  DefaultApiVersionInserter(@Nullable String header, @Nullable String queryParam,
          @Nullable String mediaTypeParam, @Nullable Integer pathSegmentIndex,
          @Nullable ApiVersionFormatter formatter) {
    Assert.isTrue(header != null || queryParam != null || mediaTypeParam != null || pathSegmentIndex != null,
            "Expected 'header', 'queryParam', 'mediaTypeParam', or 'pathSegmentIndex' to be configured");

    this.header = header;
    this.queryParam = queryParam;
    this.mediaTypeParam = mediaTypeParam;
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
      String formattedVersion = this.versionFormatter.formatVersion(version);
      headers.set(this.header, formattedVersion);
    }
    if (this.mediaTypeParam != null) {
      MediaType contentType = headers.getContentType();
      if (contentType != null) {
        Map<String, String> params = new LinkedHashMap<>(contentType.getParameters());
        params.put(this.mediaTypeParam, this.versionFormatter.formatVersion(version));
        contentType = new MediaType(contentType, params);
        headers.setContentType(contentType);
      }
    }
  }

}
