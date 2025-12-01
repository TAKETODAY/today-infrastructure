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

package infra.web.multipart.support;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import infra.test.util.ReflectionTestUtils;
import infra.util.MultiValueMap;
import infra.web.multipart.Part;
import infra.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 10:56
 */
class AbstractMultipartRequestTests {

  @Test
  void getFileNamesReturnsIteratorOfFileNames() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.forLinkedHashMap();
    multipartFiles.add("file1", mock(MultipartFile.class));
    multipartFiles.add("file2", mock(MultipartFile.class));

    when(multipartRequest.getFileNames()).thenCallRealMethod();
    when(multipartRequest.getMultipartFiles()).thenReturn(multipartFiles);

    Iterator<String> fileNames = multipartRequest.getFileNames();

    assertThat(fileNames).isNotNull();
    assertThat(fileNames.hasNext()).isTrue();
  }

  @Test
  void getFileReturnsFirstMultipartFileByName() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.forLinkedHashMap();
    MultipartFile expectedFile = mock(MultipartFile.class);
    multipartFiles.add("test-file", expectedFile);

    when(multipartRequest.getFile("test-file")).thenCallRealMethod();
    when(multipartRequest.getMultipartFiles()).thenReturn(multipartFiles);

    MultipartFile result = multipartRequest.getFile("test-file");

    assertThat(result).isEqualTo(expectedFile);
  }

  @Test
  void getFileReturnsNullWhenFileNotFound() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.forLinkedHashMap();

    when(multipartRequest.getFile("nonexistent")).thenCallRealMethod();
    when(multipartRequest.getMultipartFiles()).thenReturn(multipartFiles);

    MultipartFile result = multipartRequest.getFile("nonexistent");

    assertThat(result).isNull();
  }

  @Test
  void getFilesReturnsAllMultipartFilesByName() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.forLinkedHashMap();
    MultipartFile file1 = mock(MultipartFile.class);
    MultipartFile file2 = mock(MultipartFile.class);
    multipartFiles.add("test-file", file1);
    multipartFiles.add("test-file", file2);

    when(multipartRequest.getFiles("test-file")).thenCallRealMethod();
    when(multipartRequest.getMultipartFiles()).thenReturn(multipartFiles);

    List<MultipartFile> result = multipartRequest.getFiles("test-file");

    assertThat(result).containsExactly(file1, file2);
  }

  @Test
  void getFileMapReturnsSingleValueMap() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, MultipartFile> multipartFiles = MultiValueMap.forLinkedHashMap();
    MultipartFile file1 = mock(MultipartFile.class);
    MultipartFile file2 = mock(MultipartFile.class);
    multipartFiles.add("file1", file1);
    multipartFiles.add("file2", file2);

    when(multipartRequest.getFileMap()).thenCallRealMethod();
    when(multipartRequest.getMultipartFiles()).thenReturn(multipartFiles);

    Map<String, MultipartFile> result = multipartRequest.getFileMap();

    assertThat(result).hasSize(2);
    assertThat(result.get("file1")).isEqualTo(file1);
    assertThat(result.get("file2")).isEqualTo(file2);
  }

  @Test
  void getMultipartFilesInitializesFromMultipartData() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, Part> parts = MultiValueMap.forLinkedHashMap();
    MultipartFile multipart1 = mock(MultipartFile.class);
    Part part2 = mock(Part.class);
    when(multipart1.isFormField()).thenReturn(false);
    when(part2.isFormField()).thenReturn(true);
    parts.add("file1", multipart1);
    parts.add("field1", part2);

    when(multipartRequest.getMultipartFiles()).thenCallRealMethod();
    when(multipartRequest.multipartData()).thenReturn(parts);

    MultiValueMap<String, MultipartFile> result = multipartRequest.getMultipartFiles();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst("file1")).isEqualTo(multipart1);
  }

  @Test
  void multipartDataReturnsParsedRequest() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, Part> parsedParts = MultiValueMap.forLinkedHashMap();

    when(multipartRequest.multipartData()).thenCallRealMethod();
    when(multipartRequest.parseRequest()).thenReturn(parsedParts);

    MultiValueMap<String, Part> result = multipartRequest.multipartData();

    assertThat(result).isSameAs(parsedParts);
  }

  @Test
  void isResolvedReturnsTrueWhenPartsAreInitialized() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);
    MultiValueMap<String, Part> parsedParts = MultiValueMap.forLinkedHashMap();

    ReflectionTestUtils.setField(multipartRequest, "parts", parsedParts);

    when(multipartRequest.isResolved()).thenCallRealMethod();

    assertThat(multipartRequest.isResolved()).isTrue();
  }

  @Test
  void isResolvedReturnsFalseWhenPartsAreNotInitialized() {
    AbstractMultipartRequest multipartRequest = mock(AbstractMultipartRequest.class);

    when(multipartRequest.isResolved()).thenCallRealMethod();

    assertThat(multipartRequest.isResolved()).isFalse();
  }

}