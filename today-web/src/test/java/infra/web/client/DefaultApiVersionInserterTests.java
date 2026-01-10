/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.client;

import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/4 18:50
 */
class DefaultApiVersionInserterTests {

  @Test
  void versionInsertedAsHeader() {
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-API-Version");
    HttpHeaders headers = HttpHeaders.forWritable();

    inserter.insertVersion("v1", headers);

    assertThat(headers.getFirst("X-API-Version")).isEqualTo("v1");
  }

  @Test
  void versionInsertedAsQueryParam() {
    ApiVersionInserter inserter = ApiVersionInserter.forQueryParam("version");
    URI uri = URI.create("https://api.example.com/users");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users?version=v1");
  }

  @Test
  void versionInsertedAsPathSegment() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(0);
    URI uri = URI.create("https://api.example.com/users");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/v1/users");
  }

  @Test
  void versionInsertedWithCustomFormatter() {
    ApiVersionFormatter formatter = version -> "version-" + version;
    ApiVersionInserter inserter = ApiVersionInserter.builder()
            .useHeader("X-Version")
            .withVersionFormatter(formatter)
            .build();
    HttpHeaders headers = HttpHeaders.forWritable();

    inserter.insertVersion(1, headers);

    assertThat(headers.getFirst("X-Version")).isEqualTo("version-1");
  }

  @Test
  void multipleVersionInsertionPoints() {
    ApiVersionInserter inserter = ApiVersionInserter.builder().useHeader("X-Version")
            .useQueryParam("ver")
            .usePathSegment(1)
            .build();

    URI uri = URI.create("https://api.example.com/users/123");
    HttpHeaders headers = HttpHeaders.forWritable();

    URI result = inserter.insertVersion("v2", uri);
    inserter.insertVersion("v2", headers);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users/v2/123?ver=v2");
    assertThat(headers.getFirst("X-Version")).isEqualTo("v2");
  }

  @Test
  void invalidPathSegmentIndex() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(5);
    URI uri = URI.create("https://api.example.com/users");

    assertThatThrownBy(() -> inserter.insertVersion("v1", uri))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot insert version");
  }

  @Test
  void noInsertionPointConfigured() {
    assertThatThrownBy(() -> ApiVersionInserter.forHeader(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Expected 'header', 'queryParam', 'mediaTypeParam', or 'pathSegmentIndex' to be configured");
  }

  @Test
  void preservesExistingQueryParams() {
    ApiVersionInserter inserter = ApiVersionInserter.forQueryParam("version");
    URI uri = URI.create("https://api.example.com/users?sort=asc");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users?sort=asc&version=v1");
  }

  @Test
  void versionInsertedAsMiddlePathSegment() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(1);
    URI uri = URI.create("https://api.example.com/users/details/info");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users/v1/details/info");
  }

  @Test
  void complexUriWithMultipleVersions() {
    ApiVersionInserter inserter = ApiVersionInserter.builder().useHeader("X-Version")
            .useQueryParam("api-version")
            .usePathSegment(0)
            .build();

    URI uri = URI.create("https://api.example.com/users?page=1&sort=desc");
    HttpHeaders headers = HttpHeaders.forWritable();

    URI result = inserter.insertVersion("2.0", uri);
    inserter.insertVersion("2.0", headers);

    assertThat(result.toString())
            .isEqualTo("https://api.example.com/2.0/users?page=1&sort=desc&api-version=2.0");
    assertThat(headers.getFirst("X-Version")).isEqualTo("2.0");
  }

  @Test
  void emptyPathSegments() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(0);
    URI uri = URI.create("https://api.example.com");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/v1");
  }

  @Test
  void versionFormatterReturningNull() {
    ApiVersionFormatter formatter = version -> null;
    ApiVersionInserter inserter = ApiVersionInserter.builder().useHeader("X-Version")
            .withVersionFormatter(formatter)
            .build();

    HttpHeaders headers = HttpHeaders.forWritable();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> inserter.insertVersion("v1", headers))
            .withMessage("value is required")
    ;

    assertThat(headers.getFirst("X-Version")).isNull();
  }

  @Test
  void nullVersionWithNonNullFormatter() {
    ApiVersionFormatter formatter = version -> version == null ? "latest" : version.toString();
    ApiVersionInserter inserter = ApiVersionInserter.builder().useHeader("X-Version")
            .withVersionFormatter(formatter)
            .build();

    HttpHeaders headers = HttpHeaders.forWritable();
    inserter.insertVersion(null, headers);

    assertThat(headers.getFirst("X-Version")).isEqualTo("latest");
  }

  @Test
  void emptyStringVersion() {
    ApiVersionInserter inserter = ApiVersionInserter.builder().useHeader("X-Version")
            .useQueryParam("ver")
            .usePathSegment(0)
            .build();

    URI uri = URI.create("https://api.example.com/users");
    HttpHeaders headers = HttpHeaders.forWritable();

    URI result = inserter.insertVersion("", uri);
    inserter.insertVersion("", headers);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users?ver=");
    assertThat(headers.getFirst("X-Version")).isEqualTo("");
  }

  @Test
  void uriWithFragmentAndQuery() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(0);
    URI uri = URI.create("https://api.example.com/users?q=test#fragment");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/v1/users?q=test#fragment");
  }

}