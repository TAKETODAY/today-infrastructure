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
import infra.mock.api.http.Part;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockMultipartFile;
import infra.mock.web.MockMultipartHttpMockRequest;
import infra.mock.web.MockPart;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockMultipartHttpRequestBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class MockMultipartHttpRequestBuilderTests {

  @Test
  void addFileAndParts() throws Exception {
    MockMultipartHttpMockRequest mockRequest =
            (MockMultipartHttpMockRequest) new MockMultipartHttpRequestBuilder("/upload")
                    .file(new MockMultipartFile("file", "test.txt", "text/plain", "Test".getBytes(UTF_8)))
                    .part(new MockPart("name", "value".getBytes(UTF_8)))
                    .buildRequest(new MockContextImpl());

    assertThat(mockRequest.getMultipartRequest().getFileMap()).containsOnlyKeys("file");
    assertThat(mockRequest.getParameterMap()).containsOnlyKeys("name");
    assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly("name");
  }

  @Test
    // gh-26261, gh-26400
  void addFileWithoutFilename() throws Exception {
    MockPart jsonPart = new MockPart("data", "{\"node\":\"node\"}".getBytes(UTF_8));
    jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    MockMultipartHttpMockRequest mockRequest =
            (MockMultipartHttpMockRequest) new MockMultipartHttpRequestBuilder("/upload")
                    .file(new MockMultipartFile("file", "Test".getBytes(UTF_8)))
                    .part(jsonPart)
                    .buildRequest(new MockContextImpl());

    assertThat(mockRequest.getMultipartRequest().getFileMap()).containsOnlyKeys("file");
    assertThat(mockRequest.getParameterMap()).hasSize(1);
    assertThat(mockRequest.getParameter("data")).isEqualTo("{\"node\":\"node\"}");
    assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly("data");
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
