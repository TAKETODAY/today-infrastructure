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

package infra.http.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.http.DefaultHttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 00:19
 */
class Netty4HttpHeadersTests {

  private Netty4HttpHeaders nettyHeaders;

  private io.netty.handler.codec.http.HttpHeaders underlyingHeaders;

  @BeforeEach
  void setUp() {
    underlyingHeaders = new DefaultHttpHeaders();
    nettyHeaders = new Netty4HttpHeaders(underlyingHeaders);
  }

  @Test
  void getFirst_shouldReturnFirstHeaderValue() {
    underlyingHeaders.add("Content-Type", "application/json");
    underlyingHeaders.add("Content-Type", "text/plain");

    String result = nettyHeaders.getFirst("Content-Type");
    assertThat(result).isEqualTo("application/json");
  }

  @Test
  void getFirst_withNonExistentHeader_shouldReturnNull() {
    String result = nettyHeaders.getFirst("Non-Existent");
    assertThat(result).isNull();
  }

  @Test
  void add_shouldAddHeader() {
    nettyHeaders.add("Custom-Header", "value1");

    assertThat(underlyingHeaders.get("Custom-Header")).isEqualTo("value1");
  }

  @Test
  void add_withNullValue_shouldNotAddHeader() {
    nettyHeaders.add("Custom-Header", (String) null);

    assertThat(underlyingHeaders.contains("Custom-Header")).isFalse();
  }

  @Test
  void addAll_shouldAddMultipleValues() {
    List<String> values = Arrays.asList("value1", "value2", "value3");
    nettyHeaders.add("Custom-Header", values);

    List<String> result = underlyingHeaders.getAll("Custom-Header");
    assertThat(result).containsExactly("value1", "value2", "value3");
  }

  @Test
  void setHeader_shouldReplaceExistingHeader() {
    underlyingHeaders.add("Custom-Header", "old-value");
    nettyHeaders.setHeader("Custom-Header", "new-value");

    List<String> result = underlyingHeaders.getAll("Custom-Header");
    assertThat(result).containsExactly("new-value");
  }

  @Test
  void toSingleValueMap_shouldReturnMapWithSingleValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    Map<String, String> result = nettyHeaders.toSingleValueMap();
    assertThat(result).containsEntry("Header1", "value1");
    assertThat(result).containsEntry("Header2", "value3");
    assertThat(result).hasSize(2);
  }

  @Test
  void size_shouldReturnNumberOfUniqueHeaderNames() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    int size = nettyHeaders.size();
    assertThat(size).isEqualTo(2);
  }

  @Test
  void isEmpty_whenNoHeaders_shouldReturnTrue() {
    boolean empty = nettyHeaders.isEmpty();
    assertThat(empty).isTrue();
  }

  @Test
  void isEmpty_whenHeadersPresent_shouldReturnFalse() {
    underlyingHeaders.add("Header1", "value1");
    boolean empty = nettyHeaders.isEmpty();
    assertThat(empty).isFalse();
  }

  @Test
  void containsHeader_withExisting_shouldReturnTrue() {
    underlyingHeaders.add("Header1", "value1");
    boolean contains = nettyHeaders.contains("Header1");
    assertThat(contains).isTrue();
  }

  @Test
  void containsHeader_withNonExistent_shouldReturnFalse() {
    boolean contains = nettyHeaders.contains("Non-Existent");
    assertThat(contains).isFalse();
  }

  @Test
  void get_withExistingHeader_shouldReturnAllValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");

    List<String> result = nettyHeaders.get("Header1");
    assertThat(result).containsExactly("value1", "value2");
  }

  @Test
  void get_withNonExistentHeader_shouldReturnNull() {
    List<String> result = nettyHeaders.get("Non-Existent");
    assertThat(result).isNull();
  }

  @Test
  void set_shouldReplaceHeaderValues() {
    underlyingHeaders.add("Header1", "old-value");
    List<String> newValues = Arrays.asList("new-value1", "new-value2");

    List<String> previous = nettyHeaders.set("Header1", newValues);

    assertThat(previous).containsExactly("old-value");
    assertThat(underlyingHeaders.getAll("Header1")).containsExactly("new-value1", "new-value2");
  }

  @Test
  void remove_withExistingHeader_shouldRemoveAndReturnValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");

    List<String> removed = nettyHeaders.remove("Header1");

    assertThat(removed).containsExactly("value1", "value2");
    assertThat(underlyingHeaders.contains("Header1")).isFalse();
  }

  @Test
  void setAll_shouldAddAllHeaders() {
    Map<String, List<String>> headersToAdd = new HashMap<>();
    headersToAdd.put("Header1", Arrays.asList("value1", "value2"));
    headersToAdd.put("Header2", Arrays.asList("value3"));

    nettyHeaders.setAll(headersToAdd);

    assertThat(underlyingHeaders.getAll("Header1")).containsExactly("value1", "value2");
    assertThat(underlyingHeaders.getAll("Header2")).containsExactly("value3");
  }

  @Test
  void clear_shouldRemoveAllHeaders() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header2", "value2");

    nettyHeaders.clear();

    assertThat(underlyingHeaders.isEmpty()).isTrue();
  }

  @Test
  void names_shouldReturnHeaderNames() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header2", "value2");

    Set<String> keySet = nettyHeaders.names();
    assertThat(keySet).containsExactlyInAnyOrder("Header1", "Header2");
  }

  @Test
  void names_iterator_shouldAllowRemoval() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header2", "value2");

    Set<String> keySet = nettyHeaders.names();
    Iterator<String> iterator = keySet.iterator();
    iterator.next();
    iterator.remove(); // keySet 只是一个视图，不影响原有 headers

    assertThat(underlyingHeaders.size()).isEqualTo(2);
  }

  @Test
  void entries_shouldReturnHeaderEntries() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    Set<Map.Entry<String, List<String>>> entrySet = new HashSet<>(nettyHeaders.entries());
    assertThat(entrySet).hasSize(2);

    boolean foundHeader1 = false;
    boolean foundHeader2 = false;

    for (Map.Entry<String, List<String>> entry : entrySet) {
      if ("Header1".equals(entry.getKey())) {
        assertThat(entry.getValue()).containsExactly("value1", "value2");
        foundHeader1 = true;
      }
      else if ("Header2".equals(entry.getKey())) {
        assertThat(entry.getValue()).containsExactly("value3");
        foundHeader2 = true;
      }
    }

    assertThat(foundHeader1).isTrue();
    assertThat(foundHeader2).isTrue();
  }

  @Test
  void entriesValue() {
    underlyingHeaders.add("Header1", "old-value");

    Set<Map.Entry<String, List<String>>> entrySet = nettyHeaders.entries();
    Iterator<Map.Entry<String, List<String>>> iterator = entrySet.iterator();
    Map.Entry<String, List<String>> entry = iterator.next();
    List<String> oldValue = entry.setValue(Arrays.asList("new-value"));

    assertThat(oldValue).containsExactly("old-value");
    assertThat(underlyingHeaders.getAll("Header1")).containsExactly("new-value");
  }

  @Test
  void constructor_withNullHeaders_shouldCreateInstance() {
    Netty4HttpHeaders headers = new Netty4HttpHeaders(null);
    assertThat(headers).isNotNull();
  }

  @Test
  void equals_shouldReturnTrueForEqualHeaders() {
    Netty4HttpHeaders headers1 = new Netty4HttpHeaders(new DefaultHttpHeaders());
    Netty4HttpHeaders headers2 = new Netty4HttpHeaders(new DefaultHttpHeaders());

    assertThat(headers1).isEqualTo(headers1);

    // 两个空头部应相等

    assertThat(headers1).isEqualTo(headers2);

    // 添加相同内容后应相等
    headers1.add("Content-Type", "application/json");
    headers1.add("X-Custom", "value");
    headers2.add("Content-Type", "application/json");
    headers2.add("X-Custom", "value");
    assertThat(headers1).isEqualTo(headers2);

    // 内容不同应不相等
    headers2.add("X-Custom", "different-value");
    assertThat(headers1).isNotEqualTo(headers2);

    // 与 null 比较应返回 false
    assertThat(headers1).isNotEqualTo(null);

    // 与不同类型的对象比较应返回 false
    assertThat(headers1).isNotEqualTo("not a header");
  }

  @Test
  void hashCode_shouldBeConsistentWithEquals() {
    Netty4HttpHeaders headers1 = new Netty4HttpHeaders(new DefaultHttpHeaders());
    Netty4HttpHeaders headers2 = new Netty4HttpHeaders(new DefaultHttpHeaders());

    // 两个空头部应相等，hashCode 也应相同
    assertThat(headers1.hashCode()).isEqualTo(headers2.hashCode());

    // 添加相同内容后应相等，hashCode 也应相同
    headers1.add("Content-Type", "application/json");
    headers1.add("X-Custom", "value");
    headers2.add("Content-Type", "application/json");
    headers2.add("X-Custom", "value");
    assertThat(headers1.hashCode()).isEqualTo(headers2.hashCode());

    // 内容不同应不相等，hashCode 通常也不同（不强制，但大概率不同）
    headers2.add("X-Custom", "different-value");
    assertThat(headers1).isNotEqualTo(headers2);
  }

  @Test
  void forEach() {
    Netty4HttpHeaders headers1 = new Netty4HttpHeaders(new DefaultHttpHeaders());
    Netty4HttpHeaders headers2 = new Netty4HttpHeaders(new DefaultHttpHeaders());
    headers1.add("Content-Type", "application/json");
    headers1.add("X-Custom", "value");

    headers1.forEach(headers2::add);
    assertThat(headers1).isEqualTo(headers2);

  }

}
