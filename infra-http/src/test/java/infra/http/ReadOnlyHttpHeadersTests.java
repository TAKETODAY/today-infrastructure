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

package infra.http;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 12:51
 */
class ReadOnlyHttpHeadersTests {

  @Test
  void emptyInstanceIsSingleton() {
    ReadOnlyHttpHeaders empty1 = ReadOnlyHttpHeaders.EMPTY;
    ReadOnlyHttpHeaders empty2 = ReadOnlyHttpHeaders.EMPTY;
    assertThat(empty1).isSameAs(empty2);
  }

  @Test
  void constructorWithHeaders() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/plain");
    headers.add("Accept", "application/json");

    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable(headers));
    assertThat(readOnlyHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(readOnlyHeaders.getAccept()).containsExactly(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON);
  }

  @Test
  void getReturnsUnmodifiableList() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Custom-Header", "value1");
    headers.add("Custom-Header", "value2");

    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable(headers));
    List<String> values = readOnlyHeaders.get("Custom-Header");

    assertThat(values).isNotNull();
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> values.add("value3"));
  }

  @Test
  void getWithNonExistentHeaderReturnsNull() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(new DefaultHttpHeaders());
    assertThat(readOnlyHeaders.get("Non-Existent")).isNull();
  }

  @Test
  void getContentTypeUsesCaching() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Content-Type", "application/json;charset=UTF-8");

    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable(headers));
    MediaType contentType1 = readOnlyHeaders.getContentType();
    MediaType contentType2 = readOnlyHeaders.getContentType();

    assertThat(contentType1).isNotNull();
    assertThat(contentType1).isSameAs(contentType2);
  }

  @Test
  void getAcceptUsesCaching() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Accept", "text/plain");
    headers.add("Accept", "application/json");

    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable(headers));
    List<MediaType> accept1 = readOnlyHeaders.getAccept();
    List<MediaType> accept2 = readOnlyHeaders.getAccept();

    assertThat(accept1).isNotEmpty();
    assertThat(accept1).isSameAs(accept2);
  }

  @Test
  void addThrowsUnsupportedOperationException() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.add("Custom-Header", "value"));
  }

  @Test
  void addAllWithCollectionThrowsUnsupportedOperationException() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.add("Custom-Header", List.of("value1", "value2")));
  }

  @Test
  void setOrRemoveThrowsUnsupportedOperationException() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.setOrRemove("Custom-Header", "value"));
  }

  @Test
  void setOrRemove() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.setOrRemove("Custom-Header", List.of("value")));

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.setOrRemove("Custom-Header", new String[] { "value" }));
  }

  @Test
  void setHeader() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.setHeader("Custom-Header", "value"));
  }

  @Test
  void setAllThrowsUnsupportedOperationException() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    Map<String, List<String>> values = Map.of("Custom-Header", List.of("value"));
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.setAll(values));
  }

  @Test
  void setThrowsUnsupportedOperationException() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.set("Custom-Header", List.of("value")));
  }

  @Test
  void removeThrowsUnsupportedOperationException() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> readOnlyHeaders.remove("Custom-Header"));
  }

  @Test
  void clearThrowsUnsupportedOperationException() {
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable());
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(readOnlyHeaders::clear);
  }

  @Test
  void clearContentHeadersDoesNothing() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Content-Type", "application/json");
    headers.add("Content-Length", "100");

    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable(headers));
    readOnlyHeaders.clearContentHeaders(); // Should not throw exception

    assertThat(readOnlyHeaders.getContentType()).isNotNull();
    assertThat(readOnlyHeaders.getContentLength()).isEqualTo(100);
  }

  @Test
  void asWritableReturnsWritableCopy() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("Custom-Header", "value");

    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable(headers));
    HttpHeaders writableHeaders = readOnlyHeaders.asWritable();

    assertThat(writableHeaders).isNotSameAs(readOnlyHeaders);
    assertThat(writableHeaders.get("Custom-Header")).containsExactly("value");

    // Verify the returned instance is actually writable
    writableHeaders.add("Another-Header", "another-value");
    assertThat(writableHeaders.get("Another-Header")).containsExactly("another-value");
  }

  @Test
  void asReadOnlyReturnsSameInstance() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    ReadOnlyHttpHeaders readOnlyHeaders = new ReadOnlyHttpHeaders(HttpHeaders.forWritable(headers));
    HttpHeaders result = readOnlyHeaders.asReadOnly();

    assertThat(result).isSameAs(readOnlyHeaders);
  }

}