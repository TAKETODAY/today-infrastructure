/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.mock.web;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.FileCopyUtils;
import infra.util.ObjectUtils;
import infra.web.multipart.MultipartFile;
import infra.web.multipart.MultipartRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class MockMultipartHttpRequestTests {

  @Test
  void mockMultipartHttpMockRequestWithByteArray() throws IOException {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    assertThat(request.getFileNames().hasNext()).isFalse();
    assertThat(request.getFile("file1")).isNull();
    assertThat(request.getFile("file2")).isNull();
    assertThat(request.getFileMap().isEmpty()).isTrue();

    request.addFile(new MockMultipartFile("file1", "myContent1".getBytes()));
    request.addFile(new MockMultipartFile("file2", "myOrigFilename", "text/plain", "myContent2".getBytes()));
    doTestMultipartHttpRequest(request);
  }

  @Test
  void mockMultipartHttpMockRequestWithInputStream() throws IOException {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addFile(new MockMultipartFile("file1", new ByteArrayInputStream("myContent1".getBytes())));
    request.addFile(new MockMultipartFile("file2", "myOrigFilename", "text/plain", new ByteArrayInputStream(
            "myContent2".getBytes())));
    doTestMultipartHttpRequest(request);
  }

  @Test
  void mockMultiPartHttpMockRequestWithMixedData() {
    MockMultipartHttpMockRequest request = new MockMultipartHttpMockRequest();
    request.addFile(new MockMultipartFile("file", "myOrigFilename", MediaType.TEXT_PLAIN_VALUE, "myContent2".getBytes()));

    MockPart metadataPart = new MockPart("metadata", "{\"foo\": \"bar\"}".getBytes());
    metadataPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    request.addPart(metadataPart);

    HttpHeaders fileHttpHeaders = request.getMultipartHeaders("file");
    assertThat(fileHttpHeaders).isNotNull();
    assertThat(fileHttpHeaders.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);

    HttpHeaders dataHttpHeaders = request.getMultipartHeaders("metadata");
    assertThat(dataHttpHeaders).isNotNull();
    assertThat(dataHttpHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  private void doTestMultipartHttpRequest(MultipartRequest request) throws IOException {
    Set<String> fileNames = new HashSet<>();
    Iterator<String> fileIter = request.getFileNames();
    while (fileIter.hasNext()) {
      fileNames.add(fileIter.next());
    }
    assertThat(fileNames.size()).isEqualTo(2);
    assertThat(fileNames.contains("file1")).isTrue();
    assertThat(fileNames.contains("file2")).isTrue();
    MultipartFile file1 = request.getFile("file1");
    MultipartFile file2 = request.getFile("file2");
    Map<String, MultipartFile> fileMap = request.getFileMap();
    List<String> fileMapKeys = new ArrayList<>(fileMap.keySet());
    assertThat(fileMapKeys.size()).isEqualTo(2);
    assertThat(fileMap.get("file1")).isEqualTo(file1);
    assertThat(fileMap.get("file2")).isEqualTo(file2);

    assertThat(file1.getName()).isEqualTo("file1");
    assertThat(file1.getOriginalFilename()).isEqualTo("");
    assertThat(file1.getContentType()).isNull();
    assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(), file1.getBytes())).isTrue();
    assertThat(ObjectUtils.nullSafeEquals("myContent1".getBytes(),
            FileCopyUtils.copyToByteArray(file1.getInputStream()))).isTrue();
    assertThat(file2.getName()).isEqualTo("file2");
    assertThat(file2.getOriginalFilename()).isEqualTo("myOrigFilename");
    assertThat(file2.getContentType()).isEqualTo("text/plain");
    assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(), file2.getBytes())).isTrue();
    assertThat(ObjectUtils.nullSafeEquals("myContent2".getBytes(),
            FileCopyUtils.copyToByteArray(file2.getInputStream()))).isTrue();
  }

}
