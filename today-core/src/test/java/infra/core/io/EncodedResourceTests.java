/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author TODAY 2021/3/9 20:11
 */
class EncodedResourceTests {

  private static final String UTF8 = "UTF-8";
  private static final String UTF16 = "UTF-16";
  private static final Charset UTF8_CS = Charset.forName(UTF8);
  private static final Charset UTF16_CS = Charset.forName(UTF16);

  private final Resource resource = new DescriptiveResource("test");

  @Test
  void equalsWithNullOtherObject() {
    assertThat(new EncodedResource(resource).equals(null)).isFalse();
  }

  @Test
  void equalsWithSameEncoding() {
    EncodedResource er1 = new EncodedResource(resource, UTF8);
    EncodedResource er2 = new EncodedResource(resource, UTF8);
    assertThat(er2).isEqualTo(er1);
  }

  @Test
  void equalsWithDifferentEncoding() {
    EncodedResource er1 = new EncodedResource(resource, UTF8);
    EncodedResource er2 = new EncodedResource(resource, UTF16);
    assertThat(er2).isNotEqualTo(er1);
  }

  @Test
  void equalsWithSameCharset() {
    EncodedResource er1 = new EncodedResource(resource, UTF8_CS);
    EncodedResource er2 = new EncodedResource(resource, UTF8_CS);
    assertThat(er2).isEqualTo(er1);
  }

  @Test
  void equalsWithDifferentCharset() {
    EncodedResource er1 = new EncodedResource(resource, UTF8_CS);
    EncodedResource er2 = new EncodedResource(resource, UTF16_CS);
    assertThat(er2).isNotEqualTo(er1);
  }

  @Test
  void equalsWithEncodingAndCharset() {
    EncodedResource er1 = new EncodedResource(resource, UTF8);
    EncodedResource er2 = new EncodedResource(resource, UTF8_CS);
    assertThat(er2).isNotEqualTo(er1);
  }

  @Test
  void getReaderWithDefaultCharset() throws IOException {
    EncodedResource resource = new EncodedResource(new ByteArrayResource(new byte[] {}));
    Reader reader = resource.getReader();
    assertThat(reader).isNotNull();
  }

  @Test
  void getReaderWithSpecificCharset() throws IOException {
    EncodedResource resource = new EncodedResource(new ByteArrayResource(new byte[] { 'h', 'e', 'l', 'l' }), StandardCharsets.UTF_8);
    Reader reader = resource.getReader();
    assertThat(reader).isNotNull();
  }

  @Test
  void requiresReaderReturnsFalseWhenNoEncodingSpecified() {
    EncodedResource resource = new EncodedResource(new DescriptiveResource("test"));
    assertThat(resource.requiresReader()).isFalse();
  }

  @Test
  void requiresReaderReturnsTrueWhenEncodingSpecified() {
    EncodedResource resource = new EncodedResource(new DescriptiveResource("test"), "UTF-8");
    assertThat(resource.requiresReader()).isTrue();
  }

  @Test
  void getContentAsStringUsesDefaultCharsetWhenNoneSpecified() throws IOException {
    String content = "test content";
    Resource mockResource = mock(Resource.class);
    when(mockResource.getContentAsString(Charset.defaultCharset())).thenReturn(content);

    EncodedResource resource = new EncodedResource(mockResource);
    assertThat(resource.getContentAsString()).isEqualTo(content);
  }

  @Test
  void hashCodeReturnsUnderlyingResourceHashCode() {
    Resource testResource = new DescriptiveResource("test");
    EncodedResource resource = new EncodedResource(testResource);
    assertThat(resource.hashCode()).isEqualTo(testResource.hashCode());
  }

  @Test
  void constructorThrowsExceptionForNullResource() {
    assertThrows(IllegalArgumentException.class, () -> new EncodedResource(null));
  }

  @Test
  void getContentAsStringWithSpecificCharset() throws IOException {
    String content = "测试内容";
    Resource mockResource = mock(Resource.class);
    when(mockResource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(content);

    EncodedResource resource = new EncodedResource(mockResource, StandardCharsets.UTF_8);
    assertThat(resource.getContentAsString()).isEqualTo(content);
  }

  @Test
  void getContentAsStringWithSpecificEncoding() throws IOException {
    String content = "测试内容";
    Resource mockResource = mock(Resource.class);
    when(mockResource.getContentAsString(StandardCharsets.UTF_8)).thenReturn(content);

    EncodedResource resource = new EncodedResource(mockResource, "UTF-8");
    assertThat(resource.getContentAsString()).isEqualTo(content);

    assertThat(resource.getEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void getInputStreamReturnsSameAsUnderlyingResource() throws IOException {
    byte[] content = "test".getBytes();
    Resource mockResource = mock(Resource.class);
    InputStream expected = new ByteArrayInputStream(content);
    when(mockResource.getInputStream()).thenReturn(expected);

    EncodedResource resource = new EncodedResource(mockResource);
    assertThat(resource.getInputStream()).isSameAs(expected);
  }

  @Test
  void toStringReturnsUnderlyingResourceToString() {
    Resource mockResource = mock(Resource.class);
    String expected = "MockResource";
    when(mockResource.toString()).thenReturn(expected);

    EncodedResource resource = new EncodedResource(mockResource);
    assertThat(resource.toString()).isEqualTo(expected);
  }

  @Test
  void equalsWithSameResourceDifferentEncoding() {
    Resource testResource = new DescriptiveResource("test");
    EncodedResource resource1 = new EncodedResource(testResource, StandardCharsets.UTF_8);
    EncodedResource resource2 = new EncodedResource(testResource, "UTF-8");
    assertThat(resource1).isNotEqualTo(resource2);
    assertThat(resource2.getEncoding()).isNotEqualTo(resource1.getEncoding());
    assertThat(resource2.getCharset()).isNotEqualTo(resource1.getCharset());
  }

  @Test
  void getResourceReturnsOriginalResource() {
    Resource testResource = new DescriptiveResource("test");
    EncodedResource resource = new EncodedResource(testResource);
    assertThat(resource.getResource()).isSameAs(testResource);
  }

  @Test
  void getReaderWithInvalidEncodingThrowsException() {
    EncodedResource resource = new EncodedResource(new DescriptiveResource("test"), "INVALID");
    assertThrows(IOException.class, resource::getReader);
  }

}
