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

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 15:58
 */
class ApiVersionInserterTests {

  @Test
  void defaultInsertVersionReturnsUnmodifiedUri() {
    ApiVersionInserter inserter = new ApiVersionInserter() { };
    URI originalUri = URI.create("http://example.com/api/resource");

    URI result = inserter.insertVersion("1.0", originalUri);

    assertThat(result).isSameAs(originalUri);
  }

  @Test
  void defaultInsertVersionDoesNotModifyHeaders() {
    ApiVersionInserter inserter = new ApiVersionInserter() { };
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Content-Type", "application/json");

    inserter.insertVersion("1.0", headers);

    assertThat(headers.size()).isEqualTo(1);
    assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
  }

  @Test
  void forHeaderCreatesHeaderInserter() {
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("API-Version");
    HttpHeaders headers = HttpHeaders.forWritable();

    inserter.insertVersion("1.0", headers);

    assertThat(headers.getFirst("API-Version")).isEqualTo("1.0");
  }

  @Test
  void forQueryParamCreatesQueryParamInserter() {
    ApiVersionInserter inserter = ApiVersionInserter.forQueryParam("version");
    URI originalUri = URI.create("http://example.com/api/resource");

    URI result = inserter.insertVersion("1.0", originalUri);

    assertThat(result.toString()).isEqualTo("http://example.com/api/resource?version=1.0");
  }

  @Test
  void forMediaTypeParamCreatesMediaTypeParamInserter() {
    ApiVersionInserter inserter = ApiVersionInserter.forMediaTypeParam("v");
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Content-Type", "application/json");

    inserter.insertVersion("1.0", headers);

    assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json;v=1.0");
  }

  @Test
  void forPathSegmentCreatesPathSegmentInserter() {
    ApiVersionInserter inserter = ApiVersionInserter.forPathSegment(1);
    URI originalUri = URI.create("http://example.com/api/resource");

    URI result = inserter.insertVersion("1.0", originalUri);

    assertThat(result.toString()).isEqualTo("http://example.com/api/1.0/resource");
  }

  @Test
  void builderWithHeader() {
    ApiVersionInserter inserter = ApiVersionInserter.builder()
            .useHeader("X-API-Version")
            .build();

    HttpHeaders headers = HttpHeaders.forWritable();
    inserter.insertVersion("2.0", headers);

    assertThat(headers.getFirst("X-API-Version")).isEqualTo("2.0");
  }

  @Test
  void builderWithQueryParam() {
    ApiVersionInserter inserter = ApiVersionInserter.builder()
            .useQueryParam("api_version")
            .build();

    URI originalUri = URI.create("http://example.com/api/resource");
    URI result = inserter.insertVersion("2.0", originalUri);

    assertThat(result.toString()).isEqualTo("http://example.com/api/resource?api_version=2.0");
  }

  @Test
  void builderWithMediaTypeParam() {
    ApiVersionInserter inserter = ApiVersionInserter.builder()
            .useMediaTypeParam("version")
            .build();

    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("Content-Type", "application/xml");
    inserter.insertVersion("2.0", headers);

    assertThat(headers.getFirst("Content-Type")).isEqualTo("application/xml;version=2.0");
  }

  @Test
  void builderWithPathSegment() {
    ApiVersionInserter inserter = ApiVersionInserter.builder()
            .usePathSegment(0)
            .build();

    URI originalUri = URI.create("http://example.com/api/resource");
    URI result = inserter.insertVersion("2.0", originalUri);

    assertThat(result.toString()).isEqualTo("http://example.com/2.0/api/resource");
  }

  @Test
  void builderWithCustomVersionFormatter() {
    ApiVersionInserter inserter = ApiVersionInserter.builder()
            .useHeader("API-Version")
            .withVersionFormatter(version -> "v" + version.toString())
            .build();

    HttpHeaders headers = HttpHeaders.forWritable();
    inserter.insertVersion("1.0", headers);

    assertThat(headers.getFirst("API-Version")).isEqualTo("v1.0");
  }

  @Test
  void builderWithMultipleStrategies() {
    ApiVersionInserter inserter = ApiVersionInserter.builder()
            .useHeader("API-Version")
            .useQueryParam("version")
            .build();

    HttpHeaders headers = HttpHeaders.forWritable();
    inserter.insertVersion("1.0", headers);

    URI originalUri = URI.create("http://example.com/api/resource");
    URI result = inserter.insertVersion("1.0", originalUri);

    assertThat(headers.getFirst("API-Version")).isEqualTo("1.0");
    assertThat(result.toString()).isEqualTo("http://example.com/api/resource?version=1.0");
  }

}