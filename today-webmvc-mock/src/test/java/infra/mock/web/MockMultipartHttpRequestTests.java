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

package infra.mock.web;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.FileCopyUtils;
import infra.util.ObjectUtils;
import infra.web.multipart.MultipartRequest;
import infra.web.multipart.Part;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class MockMultipartHttpRequestTests {

  @Test
  void mockMultipartHttpMockRequestWithByteArray() throws IOException {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartRequest multipartRequest = request.getMultipartRequest();
    assertThat(multipartRequest.getPartNames().iterator().hasNext()).isFalse();
    assertThat(multipartRequest.getPart("file1")).isNull();
    assertThat(multipartRequest.getPart("file2")).isNull();
    assertThat(multipartRequest.getParts().isEmpty()).isTrue();

    request.addPart(new MockMemoryFilePart("file1", "myContent1".getBytes()));
    request.addPart(new MockMemoryFilePart("file2", "myOrigFilename", "text/plain", "myContent2".getBytes()));
    doTestMultipartHttpRequest(multipartRequest);
  }

  @Test
  void mockMultipartHttpMockRequestWithInputStream() throws IOException {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    MultipartRequest multipartRequest = request.getMultipartRequest();

    request.addPart(new MockMemoryFilePart("file1", new ByteArrayInputStream("myContent1".getBytes())));
    request.addPart(new MockMemoryFilePart("file2", "myOrigFilename", "text/plain", new ByteArrayInputStream(
            "myContent2".getBytes())));
    doTestMultipartHttpRequest(multipartRequest);
  }

  @Test
  void mockMultiPartHttpMockRequestWithMixedData() {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addPart(new MockMemoryFilePart("file", "myOrigFilename", MediaType.TEXT_PLAIN_VALUE, "myContent2".getBytes()));

    MockMemoryPart metadataPart = new MockMemoryPart("metadata", "{\"foo\": \"bar\"}".getBytes());
    metadataPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    request.addPart(metadataPart);

    HttpHeaders fileHttpHeaders = request.getMultipartRequest().getHeaders("file");
    assertThat(fileHttpHeaders).isNotNull();
    assertThat(fileHttpHeaders.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);

    HttpHeaders dataHttpHeaders = request.getMultipartRequest().getHeaders("metadata");
    assertThat(dataHttpHeaders).isNotNull();
    assertThat(dataHttpHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  private void doTestMultipartHttpRequest(MultipartRequest request) throws IOException {
    Set<String> fileNames = new HashSet<>();
    for (String string : request.getPartNames()) {
      fileNames.add(string);
    }
    assertThat(fileNames.size()).isEqualTo(2);
    assertThat(fileNames.contains("file1")).isTrue();
    assertThat(fileNames.contains("file2")).isTrue();
    Part file1 = request.getPart("file1");
    Part file2 = request.getPart("file2");
    Map<String, Part> fileMap = request.getParts().toSingleValueMap();
    List<String> fileMapKeys = new ArrayList<>(fileMap.keySet());
    assertThat(fileMapKeys.size()).isEqualTo(2);
    assertThat(fileMap.get("file1")).isEqualTo(file1);
    assertThat(fileMap.get("file2")).isEqualTo(file2);

    assertThat(file1.getName()).isEqualTo("file1");
    assertThat(file1.getOriginalFilename()).isEqualTo("");
    assertThat(file1.getContentType()).isNull();
    assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(), file1.getContentAsByteArray())).isTrue();
    assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(),
            FileCopyUtils.copyToByteArray(file1.getInputStream()))).isTrue();
    assertThat(file2.getName()).isEqualTo("file2");
    assertThat(file2.getOriginalFilename()).isEqualTo("myOrigFilename");
    assertThat(file2.getContentTypeAsString()).isEqualTo("text/plain");
    assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(), file2.getContentAsByteArray())).isTrue();
    assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(),
            FileCopyUtils.copyToByteArray(file2.getInputStream()))).isTrue();
  }

}
