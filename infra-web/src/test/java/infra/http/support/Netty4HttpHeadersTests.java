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
import java.util.Collection;
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
    nettyHeaders.add("Custom-Header", null);

    assertThat(underlyingHeaders.contains("Custom-Header")).isFalse();
  }

  @Test
  void addAll_shouldAddMultipleValues() {
    List<String> values = Arrays.asList("value1", "value2", "value3");
    nettyHeaders.addAll("Custom-Header", values);

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
  void containsKey_withExistingHeader_shouldReturnTrue() {
    underlyingHeaders.add("Header1", "value1");
    boolean contains = nettyHeaders.containsKey("Header1");
    assertThat(contains).isTrue();
  }

  @Test
  void containsKey_withNonStringKey_shouldReturnFalse() {
    boolean contains = nettyHeaders.containsKey(123);
    assertThat(contains).isFalse();
  }

  @Test
  void containsKey_withNonExistentHeader_shouldReturnFalse() {
    boolean contains = nettyHeaders.containsKey("Non-Existent");
    assertThat(contains).isFalse();
  }

  @Test
  void containsValue_withExistingValue_shouldReturnTrue() {
    underlyingHeaders.add("Header1", "value1");
    boolean contains = nettyHeaders.containsValue("value1");
    assertThat(contains).isTrue();
  }

  @Test
  void containsValue_withNonStringValue_shouldReturnFalse() {
    boolean contains = nettyHeaders.containsValue(123);
    assertThat(contains).isFalse();
  }

  @Test
  void containsValue_withNonExistentValue_shouldReturnFalse() {
    boolean contains = nettyHeaders.containsValue("non-existent");
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
  void get_withNonStringKey_shouldReturnNull() {
    List<String> result = nettyHeaders.get(123);
    assertThat(result).isNull();
  }

  @Test
  void get_withNonExistentHeader_shouldReturnNull() {
    List<String> result = nettyHeaders.get("Non-Existent");
    assertThat(result).isNull();
  }

  @Test
  void put_shouldReplaceHeaderValues() {
    underlyingHeaders.add("Header1", "old-value");
    List<String> newValues = Arrays.asList("new-value1", "new-value2");

    List<String> previous = nettyHeaders.put("Header1", newValues);

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
  void remove_withNonStringKey_shouldReturnNull() {
    List<String> result = nettyHeaders.remove(123);
    assertThat(result).isNull();
  }

  @Test
  void putAll_shouldAddAllHeaders() {
    Map<String, List<String>> headersToAdd = new HashMap<>();
    headersToAdd.put("Header1", Arrays.asList("value1", "value2"));
    headersToAdd.put("Header2", Arrays.asList("value3"));

    nettyHeaders.putAll(headersToAdd);

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
  void keySet_shouldReturnHeaderNames() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header2", "value2");

    Set<String> keySet = nettyHeaders.keySet();
    assertThat(keySet).containsExactlyInAnyOrder("Header1", "Header2");
  }

  @Test
  void keySet_iterator_shouldAllowRemoval() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header2", "value2");

    Set<String> keySet = nettyHeaders.keySet();
    Iterator<String> iterator = keySet.iterator();
    iterator.next();
    iterator.remove();

    assertThat(underlyingHeaders.size()).isEqualTo(1);
  }

  @Test
  void values_shouldReturnAllHeaderValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    Collection<List<String>> values = nettyHeaders.values();
    assertThat(values).hasSize(2);
    assertThat(values).contains(Arrays.asList("value1", "value2"));
    assertThat(values).contains(Arrays.asList("value3"));
  }

  @Test
  void entrySet_shouldReturnHeaderEntries() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    Set<Map.Entry<String, List<String>>> entrySet = new HashSet<>(nettyHeaders.entrySet());
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
  void entrySet_iterator_shouldAllowSetValue() {
    underlyingHeaders.add("Header1", "old-value");

    Set<Map.Entry<String, List<String>>> entrySet = nettyHeaders.entrySet();
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
}
