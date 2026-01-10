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

package infra.http.client.reactive;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 13:37
 */
class HttpComponentsHeadersTests {

  @Test
  void constructorWithHttpMessage() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers).isNotNull();
  }

  @Test
  void getFirstReturnsFirstHeaderValue() {
    HttpMessage message = mock(HttpMessage.class);
    Header header = mock(Header.class);
    when(header.getValue()).thenReturn("value1");
    when(message.getFirstHeader("test")).thenReturn(header);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.getFirst("test")).isEqualTo("value1");
  }

  @Test
  void getFirstReturnsNullWhenHeaderNotFound() {
    HttpMessage message = mock(HttpMessage.class);
    when(message.getFirstHeader("nonexistent")).thenReturn(null);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.getFirst("nonexistent")).isNull();
  }

  @Test
  void addAddsHeaderToMessage() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    headers.add("test", "value");

    verify(message).addHeader("test", "value");
  }

  @Test
  void addWithNullValue() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    headers.add("test", null);

    verify(message).addHeader("test", null);
  }

  @Test
  void setHeaderSetsHeaderOnMessage() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    headers.setHeader("test", "value");

    verify(message).setHeader("test", "value");
  }

  @Test
  void toSingleValueMapReturnsMapWithFirstValues() {
    HttpMessage message = mock(HttpMessage.class);
    Header[] headersArray = new Header[] {
            createMockHeader("test", "value1"),
            createMockHeader("test", "value2"),
            createMockHeader("another", "value3")
    };
    when(message.getHeaders()).thenReturn(headersArray);
    when(message.headerIterator()).thenReturn(Arrays.asList(headersArray).iterator());

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    Map<String, String> singleValueMap = headers.toSingleValueMap();

    assertThat(singleValueMap).containsEntry("test", "value1");
    assertThat(singleValueMap).containsEntry("another", "value3");
    assertThat(singleValueMap.size()).isEqualTo(2);
  }

  @Test
  void sizeReturnsNumberOfHeaders() {
    HttpMessage message = mock(HttpMessage.class);
    Header[] headersArray = new Header[] {
            createMockHeader("test1", "value1"),
            createMockHeader("test2", "value2")
    };
    when(message.getHeaders()).thenReturn(headersArray);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.size()).isEqualTo(2);
  }

  @Test
  void isEmptyReturnsTrueWhenNoHeaders() {
    HttpMessage message = mock(HttpMessage.class);
    when(message.getHeaders()).thenReturn(new Header[0]);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void containsKeyReturnsTrueForExistingHeader() {
    HttpMessage message = mock(HttpMessage.class);
    when(message.containsHeader("existing")).thenReturn(true);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.containsKey("existing")).isTrue();
  }

  @Test
  void containsKeyReturnsFalseForNonExistingHeader() {
    HttpMessage message = mock(HttpMessage.class);
    when(message.containsHeader("nonexistent")).thenReturn(false);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.containsKey("nonexistent")).isFalse();
  }

  @Test
  void containsKeyReturnsFalseForNonStringKey() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.containsKey(123)).isFalse();
  }

  @Test
  void containsValueReturnsTrueForExistingValue() {
    HttpMessage message = mock(HttpMessage.class);
    Header[] headersArray = new Header[] { createMockHeader("test", "existing-value") };
    when(message.getHeaders()).thenReturn(headersArray);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.containsValue("existing-value")).isTrue();
  }

  @Test
  void containsValueReturnsFalseForNonExistingValue() {
    HttpMessage message = mock(HttpMessage.class);
    Header[] headersArray = new Header[] { createMockHeader("test", "value") };
    when(message.getHeaders()).thenReturn(headersArray);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.containsValue("nonexistent-value")).isFalse();
  }

  @Test
  void containsValueReturnsFalseForNonStringValue() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.containsValue(123)).isFalse();
  }

  @Test
  void getReturnsListOfValuesForExistingHeader() {
    HttpMessage message = mock(HttpMessage.class);
    Header[] headersArray = new Header[] {
            createMockHeader("test", "value1"),
            createMockHeader("test", "value2")
    };
    when(message.getHeaders("test")).thenReturn(headersArray);
    when(message.containsHeader("test")).thenReturn(true);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    List<String> values = headers.get("test");

    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void getReturnsNullForNonExistingHeader() {
    HttpMessage message = mock(HttpMessage.class);
    when(message.containsHeader("nonexistent")).thenReturn(false);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.get("nonexistent")).isNull();
  }

  @Test
  void getReturnsNullForNonStringKey() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.get(123)).isNull();
  }

  @Test
  void removeReturnsNullForNonStringKey() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    assertThat(headers.remove(123)).isNull();
    verify(message, never()).removeHeaders(anyString());
  }

  @Test
  void putAllAddsAllHeadersFromMap() {
    HttpMessage message = mock(HttpMessage.class);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    Map<String, List<String>> map = Map.of(
            "header1", List.of("value1", "value2"),
            "header2", List.of("value3")
    );
    headers.putAll(map);

    verify(message).addHeader("header1", "value1");
    verify(message).addHeader("header1", "value2");
    verify(message).addHeader("header2", "value3");
  }

  @Test
  void clearRemovesAllHeaders() {
    HttpMessage message = mock(HttpMessage.class);
    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    headers.clear();

    verify(message).setHeaders();
  }

  @Test
  void keySetReturnsAllHeaderNames() {
    HttpMessage message = mock(HttpMessage.class);
    Header[] headersArray = new Header[] {
            createMockHeader("test1", "value1"),
            createMockHeader("test2", "value2"),
            createMockHeader("test1", "value3") // Duplicate name
    };
    when(message.getHeaders()).thenReturn(headersArray);

    HttpComponentsHeaders headers = new HttpComponentsHeaders(message);
    Set<String> keySet = headers.keySet();

    assertThat(keySet).containsExactlyInAnyOrder("test1", "test2");
  }

  private Header createMockHeader(String name, String value) {
    Header header = mock();
    when(header.getName()).thenReturn(name);
    when(header.getValue()).thenReturn(value);
    return header;
  }

}