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

package infra.web.multipart;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;

import infra.core.io.Resource;
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
class PartTests {

  @Test
  void testIsFormField() {
    Part part = mock(Part.class);
    when(part.isFormField()).thenReturn(true);

    assertThat(part.isFormField()).isTrue();
  }

  @Test
  void testGetName() {
    Part part = mock(Part.class);
    when(part.getName()).thenReturn("test-part");

    assertThat(part.getName()).isEqualTo("test-part");
  }

  @Test
  void testGetValue() throws IOException {
    Part part = mock(Part.class);
    when(part.getContentAsString()).thenReturn("test-value");

    assertThat(part.getContentAsString()).isEqualTo("test-value");
  }

  @Test
  void testGetHeaders() {
    Part part = mock(Part.class);
    HttpHeaders headers = mock(HttpHeaders.class);
    when(part.getHeaders()).thenReturn(headers);

    assertThat(part.getHeaders()).isEqualTo(headers);
  }

  @Test
  void testGetBytes() throws IOException {
    Part part = mock(Part.class);
    byte[] content = "test content".getBytes();
    when(part.getContentAsByteArray()).thenReturn(content);

    assertThat(part.getContentAsByteArray()).isEqualTo(content);
  }

  @Test
  void testCleanup() throws IOException {
    Part part = mock(Part.class);
    doNothing().when(part).cleanup();

    assertThatNoException().isThrownBy(part::cleanup);
    verify(part).cleanup();
  }

  @Test
  void testToStringMethod() {
    Part part = mock(Part.class);

    when(part.toString()).thenReturn("MockPart");
    when(part.getName()).thenReturn("test-name");

    assertThat(part.toString()).isEqualTo("MockPart");
  }

  @Test
  void getHeaderReturnsFirstHeaderValue() {
    Part part = mock(Part.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "text/plain");
    headers.add("Content-Type", "charset=utf-8");
    when(part.getHeaders()).thenReturn(headers);
    when(part.getHeader("Content-Type")).thenCallRealMethod();

    String headerValue = part.getHeader("Content-Type");

    assertThat(headerValue).isEqualTo("text/plain");
  }

  @Test
  void getHeaderReturnsNullWhenNotFound() {
    Part part = mock(Part.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(part.getHeaders()).thenReturn(headers);

    String headerValue = part.getHeader("Non-Existent-Header");

    assertThat(headerValue).isNull();
  }

  @Test
  void getHeadersReturnsAllValuesForName() {
    Part part = mock(Part.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept", "text/html");
    headers.add("Accept", "application/json");
    headers.add("Accept", "application/xml");
    when(part.getHeaders()).thenReturn(headers);
    when(part.getHeaders("Accept")).thenCallRealMethod();

    Collection<String> headerValues = part.getHeaders("Accept");

    assertThat(headerValues).containsExactly("text/html", "application/json", "application/xml");
  }

  @Test
  void getHeadersReturnsEmptyCollectionWhenNotFound() {
    Part part = mock(Part.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(part.getHeaders()).thenReturn(headers);

    Collection<String> headerValues = part.getHeaders("Non-Existent-Header");

    assertThat(headerValues).isEmpty();
  }

  @Test
  void getHeaderNamesReturnsAllHeaderNames() {
    Part part = mock(Part.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "text/plain");
    headers.add("Content-Length", "100");
    headers.add("Cache-Control", "no-cache");
    when(part.getHeaders()).thenReturn(headers);
    when(part.getHeaderNames()).thenCallRealMethod();

    Collection<String> headerNames = part.getHeaderNames();

    assertThat(headerNames).containsExactlyInAnyOrder("Content-Type", "Content-Length", "Cache-Control");
  }

  @Test
  void getBodyReturnsSameAsStream() throws IOException {
    Part part = mock(Part.class);
    java.io.InputStream inputStream = mock(java.io.InputStream.class);
    when(part.getInputStream()).thenReturn(inputStream);
    when(part.getBody()).thenCallRealMethod();

    java.io.InputStream body = part.getBody();
    assertThat(body).isSameAs(inputStream);
  }

  @Test
  void getContentTypeStringReturnsNullWhenContentTypeIsNull() {
    Part part = mock(Part.class);
    when(part.getContentType()).thenReturn(null);

    String contentTypeString = part.getContentTypeAsString();

    assertThat(contentTypeString).isNull();
  }

  @Test
  void getResourceReturnsPartResource() {
    Part part = mock(Part.class);
    when(part.getResource()).thenCallRealMethod();
    Resource resource = part.getResource();

    assertThat(resource).isInstanceOf(PartResource.class);
  }

}