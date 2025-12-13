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

package infra.test.web.mock.request;

import org.junit.jupiter.api.Test;

import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockMemoryFilePart;
import infra.mock.web.MockMemoryPart;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.web.multipart.Part;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockMultipartHttpRequestBuilder}.
 *
 * @author Rossen Stoyanchev
 */
class MockMultipartHttpRequestBuilderTests {

  @Test
  void addFileAndParts() throws Exception {
    MockMultipartHttpMockRequest mockRequest =
            (MockMultipartHttpMockRequest) new MockMultipartHttpRequestBuilder("/upload")
                    .file(new MockMemoryFilePart("file", "test.txt", "text/plain", "Test".getBytes(UTF_8)))
                    .part(new MockMemoryPart("name", "value".getBytes(UTF_8)))
                    .buildRequest(new MockContextImpl());

    assertThat(mockRequest.getMultipartRequest().getParts()).containsOnlyKeys("file", "name");
    assertThat(mockRequest.getParameterMap()).containsOnlyKeys("name");
    assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly();
  }

  @Test
  void addFileWithoutFilename() throws Exception {
    MockMemoryPart jsonPart = new MockMemoryPart("data", "{\"node\":\"node\"}".getBytes(UTF_8));
    jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    MockMultipartHttpMockRequest mockRequest =
            (MockMultipartHttpMockRequest) new MockMultipartHttpRequestBuilder("/upload")
                    .file(new MockMemoryFilePart("file", "Test".getBytes(UTF_8)))
                    .part(jsonPart)
                    .buildRequest(new MockContextImpl());

    assertThat(mockRequest.getMultipartRequest().getParts().toSingleValueMap()).containsOnlyKeys("file", "data");
    assertThat(mockRequest.getParameterMap()).hasSize(1);
    assertThat(mockRequest.getParameter("data")).isEqualTo("{\"node\":\"node\"}");
    assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly();
  }

  @Test
  void mergeAndBuild() {
    MockHttpRequestBuilder parent = new MockHttpRequestBuilder(HttpMethod.GET, "/");
    parent.characterEncoding("UTF-8");
    Object result = new MockMultipartHttpRequestBuilder("/fileUpload").merge(parent);

    assertThat(result).isNotNull();
    assertThat(result.getClass()).isEqualTo(MockMultipartHttpRequestBuilder.class);

    MockMultipartHttpRequestBuilder builder = (MockMultipartHttpRequestBuilder) result;
    HttpMockRequestImpl request = builder.buildRequest(new MockContextImpl());
    assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
  }

}
