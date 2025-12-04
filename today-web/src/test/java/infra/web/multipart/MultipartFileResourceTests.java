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

package infra.web.multipart;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 10:24
 */
class MultipartFileResourceTests {

  @Test
  void testConstructor() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource).isNotNull();
  }

  @Test
  void testExists() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource.exists()).isTrue();
  }

  @Test
  void testIsOpen() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource.isOpen()).isTrue();
  }

  @Test
  void testContentLength() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getContentLength()).thenReturn(1024L);
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource.contentLength()).isEqualTo(1024L);
  }

  @Test
  void testGetName() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource.getName()).isEqualTo("test.txt");
  }

  @Test
  void testGetInputStream() throws IOException, IllegalStateException {
    MultipartFile multipartFile = mock(MultipartFile.class);
    InputStream inputStream = mock(InputStream.class);
    when(multipartFile.getInputStream()).thenReturn(inputStream);
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource.getInputStream()).isEqualTo(inputStream);
  }

  @Test
  void testToString() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getName()).thenReturn("test-file");
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource.toString()).isEqualTo("MultipartFile resource [test-file]");
  }

  @Test
  void testEquals() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    MultipartFileResource resource1 = new MultipartFileResource(multipartFile);
    MultipartFileResource resource2 = new MultipartFileResource(multipartFile);

    assertThat(resource1).isEqualTo(resource2);
    assertThat(resource1).isEqualTo(resource1);
    assertThat(resource1).isNotEqualTo(new Object());
  }

  @Test
  void testHashCode() {
    MultipartFile multipartFile = mock(MultipartFile.class);
    MultipartFileResource resource = new MultipartFileResource(multipartFile);

    assertThat(resource.hashCode()).isEqualTo(multipartFile.hashCode());
  }

}