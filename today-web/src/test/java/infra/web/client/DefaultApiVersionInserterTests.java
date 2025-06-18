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
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-API-Version").build();
    HttpHeaders headers = HttpHeaders.forWritable();

    inserter.insertVersion("v1", headers);

    assertThat(headers.getFirst("X-API-Version")).isEqualTo("v1");
  }

  @Test
  void versionInsertedAsQueryParam() {
    ApiVersionInserter inserter = ApiVersionInserter.forQueryParam("version").build();
    URI uri = URI.create("https://api.example.com/users");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users?version=v1");
  }

  @Test
  void versionInsertedAsPathSegment() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(0).build();
    URI uri = URI.create("https://api.example.com/users");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/v1/users");
  }

  @Test
  void versionInsertedWithCustomFormatter() {
    ApiVersionFormatter formatter = version -> "version-" + version;
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-Version")
            .withVersionFormatter(formatter)
            .build();
    HttpHeaders headers = HttpHeaders.forWritable();

    inserter.insertVersion(1, headers);

    assertThat(headers.getFirst("X-Version")).isEqualTo("version-1");
  }

  @Test
  void multipleVersionInsertionPoints() {
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-Version")
            .fromQueryParam("ver")
            .fromPathSegment(1)
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
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(5).build();
    URI uri = URI.create("https://api.example.com/users");

    assertThatThrownBy(() -> inserter.insertVersion("v1", uri))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot insert version");
  }

  @Test
  void noInsertionPointConfigured() {
    assertThatThrownBy(() -> ApiVersionInserter.forHeader(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Expected 'header', 'queryParam', or 'pathSegmentIndex' to be configured");
  }

  @Test
  void preservesExistingQueryParams() {
    ApiVersionInserter inserter = ApiVersionInserter.forQueryParam("version").build();
    URI uri = URI.create("https://api.example.com/users?sort=asc");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users?sort=asc&version=v1");
  }

  @Test
  void versionInsertedAsMiddlePathSegment() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(1).build();
    URI uri = URI.create("https://api.example.com/users/details/info");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/users/v1/details/info");
  }

  @Test
  void complexUriWithMultipleVersions() {
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-Version")
            .fromQueryParam("api-version")
            .fromPathSegment(0)
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
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(0).build();
    URI uri = URI.create("https://api.example.com");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/v1");
  }

  @Test
  void versionFormatterReturningNull() {
    ApiVersionFormatter formatter = version -> null;
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-Version")
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
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-Version")
            .withVersionFormatter(formatter)
            .build();

    HttpHeaders headers = HttpHeaders.forWritable();
    inserter.insertVersion(null, headers);

    assertThat(headers.getFirst("X-Version")).isEqualTo("latest");
  }

  @Test
  void emptyStringVersion() {
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-Version")
            .fromQueryParam("ver")
            .fromPathSegment(0)
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
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(0).build();
    URI uri = URI.create("https://api.example.com/users?q=test#fragment");

    URI result = inserter.insertVersion("v1", uri);

    assertThat(result.toString()).isEqualTo("https://api.example.com/v1/users?q=test#fragment");
  }

}