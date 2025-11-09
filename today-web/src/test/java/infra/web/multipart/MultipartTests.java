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

import infra.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 10:37
 */
class MultipartTests {

  @Test
  void testGetContentTypeDefaultImplementation() {
    Multipart multipart = mock(Multipart.class);
    when(multipart.getContentType()).thenCallRealMethod();

    assertThat(multipart.getContentType()).isNull();
  }

  @Test
  void testIsFormField() {
    Multipart multipart = mock(Multipart.class);
    when(multipart.isFormField()).thenReturn(true);

    assertThat(multipart.isFormField()).isTrue();
  }

  @Test
  void testGetName() {
    Multipart multipart = mock(Multipart.class);
    when(multipart.getName()).thenReturn("test-part");

    assertThat(multipart.getName()).isEqualTo("test-part");
  }

  @Test
  void testGetValue() {
    Multipart multipart = mock(Multipart.class);
    when(multipart.getValue()).thenReturn("test-value");

    assertThat(multipart.getValue()).isEqualTo("test-value");
  }

  @Test
  void testGetHeaders() {
    Multipart multipart = mock(Multipart.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(multipart.getHeaders()).thenReturn(headers);

    assertThat(multipart.getHeaders()).isEqualTo(headers);
  }

  @Test
  void testGetBytes() throws IOException {
    Multipart multipart = mock(Multipart.class);
    byte[] content = "test content".getBytes();
    when(multipart.getBytes()).thenReturn(content);

    assertThat(multipart.getBytes()).isEqualTo(content);
  }

  @Test
  void testCleanup() throws IOException {
    Multipart multipart = mock(Multipart.class);
    doNothing().when(multipart).cleanup();

    assertThatNoException().isThrownBy(multipart::cleanup);
    verify(multipart).cleanup();
  }
}