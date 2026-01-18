/*
 * Copyright 2012-present the original author or authors.
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

package infra.http.service.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.web.client.ApiVersionInserter;
import infra.http.service.config.ApiVersionProperties.Insert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApiVersionProperties}.
 *
 * @author Phillip Webb
 */
class ApiVersionPropertiesTests {

  @Test
  void getReturnsInserterBasedOnProperties() throws Exception {
    ApiVersionProperties apiVersionProperties = new ApiVersionProperties();
    Insert properties = apiVersionProperties.insert;
    properties.header = ("x-test");
    properties.queryParameter = ("v");
    properties.pathSegment = 1;
    properties.mediaTypeParameter = ("mtp");
    ApiVersionInserter inserter = apiVersionProperties.createApiVersionInserter();
    URI uri = new URI("https://example.com/foo/bar");
    assertThat(inserter.insertVersion("123", uri)).hasToString("https://example.com/foo/123/bar?v=123");
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.APPLICATION_JSON);
    inserter.insertVersion("123", headers);
    assertThat(headers.get("x-test")).containsExactly("123");
    MediaType contentType = headers.getContentType();
    assertThat(contentType).isNotNull();
    assertThat(contentType.getParameters()).containsEntry("mtp", "123");
  }

  @Test
  void getWhenNoPropertiesReturnsEmpty() {
    ApiVersionProperties apiVersionProperties = new ApiVersionProperties();
    ApiVersionInserter inserter = apiVersionProperties.createApiVersionInserter();
    assertThat(inserter).isNull();
  }

}
