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

    String contentTypeString = part.getContentTypeString();

    assertThat(contentTypeString).isNull();
  }

  @Test
  void getContentTypeStringReturnsStringRepresentation() {
    Part part = mock(Part.class);
    infra.http.MediaType mediaType = infra.http.MediaType.TEXT_PLAIN;
    when(part.getContentType()).thenReturn(mediaType);
    when(part.getContentTypeString()).thenCallRealMethod();

    String contentTypeString = part.getContentTypeString();

    assertThat(contentTypeString).isEqualTo("text/plain");
  }

  @Test
  void getResourceReturnsPartResource() {
    Part part = mock(Part.class);
    when(part.getResource()).thenCallRealMethod();
    Resource resource = part.getResource();

    assertThat(resource).isInstanceOf(PartResource.class);
  }

}