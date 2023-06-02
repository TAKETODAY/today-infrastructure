/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet.request;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockMultipartFile;
import cn.taketoday.mock.web.MockMultipartHttpServletRequest;
import cn.taketoday.mock.web.MockPart;
import cn.taketoday.mock.web.MockServletContext;
import jakarta.servlet.http.Part;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockMultipartHttpServletRequestBuilder}.
 *
 * @author Rossen Stoyanchev
 */
public class MockMultipartHttpServletRequestBuilderTests {

  @Test
    // gh-26166
  void addFileAndParts() throws Exception {
    MockMultipartHttpServletRequest mockRequest =
            (MockMultipartHttpServletRequest) new MockMultipartHttpServletRequestBuilder("/upload")
                    .file(new MockMultipartFile("file", "test.txt", "text/plain", "Test".getBytes(UTF_8)))
                    .part(new MockPart("name", "value".getBytes(UTF_8)))
                    .buildRequest(new MockServletContext());

    assertThat(mockRequest.getFileMap()).containsOnlyKeys("file");
    assertThat(mockRequest.getParameterMap()).containsOnlyKeys("name");
    assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly("name");
  }

  @Test
    // gh-26261, gh-26400
  void addFileWithoutFilename() throws Exception {
    MockPart jsonPart = new MockPart("data", "{\"node\":\"node\"}".getBytes(UTF_8));
    jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    MockMultipartHttpServletRequest mockRequest =
            (MockMultipartHttpServletRequest) new MockMultipartHttpServletRequestBuilder("/upload")
                    .file(new MockMultipartFile("file", "Test".getBytes(UTF_8)))
                    .part(jsonPart)
                    .buildRequest(new MockServletContext());

    assertThat(mockRequest.getFileMap()).containsOnlyKeys("file");
    assertThat(mockRequest.getParameterMap()).hasSize(1);
    assertThat(mockRequest.getParameter("data")).isEqualTo("{\"node\":\"node\"}");
    assertThat(mockRequest.getParts()).extracting(Part::getName).containsExactly("data");
  }

  @Test
  void mergeAndBuild() {
    MockHttpServletRequestBuilder parent = new MockHttpServletRequestBuilder(HttpMethod.GET, "/");
    parent.characterEncoding("UTF-8");
    Object result = new MockMultipartHttpServletRequestBuilder("/fileUpload").merge(parent);

    assertThat(result).isNotNull();
    assertThat(result.getClass()).isEqualTo(MockMultipartHttpServletRequestBuilder.class);

    MockMultipartHttpServletRequestBuilder builder = (MockMultipartHttpServletRequestBuilder) result;
    MockHttpServletRequest request = builder.buildRequest(new MockServletContext());
    assertThat(request.getCharacterEncoding()).isEqualTo("UTF-8");
  }

}
