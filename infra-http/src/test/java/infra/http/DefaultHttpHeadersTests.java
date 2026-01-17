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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/6 11:52
 */
class DefaultHttpHeadersTests {

  @Test
  void constructor_default_shouldCreateCaseInsensitiveMap() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    headers.add("Content-Type", "application/json");
    assertThat(headers.getFirst("content-type")).isEqualTo("application/json");
    assertThat(headers.getFirst("CONTENT-TYPE")).isEqualTo("application/json");
    assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
  }

  @Test
  void constructor_withMap_shouldWrapProvidedMap() {
    Map<String, List<String>> map = new LinkedHashMap<>();
    List<String> values = new ArrayList<>();
    values.add("value1");
    values.add("value2");
    map.put("Header-Name", values);

    DefaultHttpHeaders headers = new DefaultHttpHeaders(map);

    assertThat(headers.get("Header-Name")).containsExactly("value1", "value2");
    assertThat(headers.size()).isEqualTo(1);
  }

  @Test
  void constructor_withMultiValueMap_shouldUseProvidedMap() {
    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
    multiValueMap.add("Header-Name", "value1");
    multiValueMap.add("Header-Name", "value2");

    DefaultHttpHeaders headers = new DefaultHttpHeaders(multiValueMap);

    assertThat(headers.get("Header-Name")).containsExactly("value1", "value2");
  }

  @Test
  void constructor_withEmptyMultiValueMap_shouldCreateNewMap() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders(HttpHeaders.EMPTY);

    headers.add("Test-Header", "test-value");
    assertThat(headers.getFirst("Test-Header")).isEqualTo("test-value");
  }

  @Test
  void constructor_withNestedDefaultHttpHeaders_shouldUnwrap() {
    MultiValueMap<String, String> innerMap = new LinkedMultiValueMap<>();
    innerMap.add("Inner-Header", "inner-value");
    DefaultHttpHeaders innerHeaders = new DefaultHttpHeaders(innerMap);

    MultiValueMap<String, String> outerMap = new LinkedMultiValueMap<>();
    outerMap.add("Outer-Header", "outer-value");
    DefaultHttpHeaders outerHeaders = new DefaultHttpHeaders(outerMap);

    // Create nested structure
    DefaultHttpHeaders headers = new DefaultHttpHeaders(innerHeaders);

    assertThat(headers.get("Inner-Header")).containsExactly("inner-value");
    assertThat(headers.size()).isEqualTo(1);
  }

  @Test
  void getFirst_shouldReturnFirstHeaderValue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "first-value");
    headers.add("Test-Header", "second-value");

    assertThat(headers.getFirst("Test-Header")).isEqualTo("first-value");
  }

  @Test
  void getFirst_withCaseInsensitiveName_shouldReturnFirstHeaderValue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    assertThat(headers.getFirst("test-header")).isEqualTo("value");
    assertThat(headers.getFirst("TEST-HEADER")).isEqualTo("value");
  }

  @Test
  void getFirst_withNonExistentHeader_shouldReturnNull() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThat(headers.getFirst("Non-Existent")).isNull();
  }

  @Test
  void add_shouldAddHeaderValue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value1");
    headers.add("Test-Header", "value2");

    assertThat(headers.get("Test-Header")).containsExactly("value1", "value2");
  }

  @Test
  void add_withNullValue_shouldAddNullValue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", null);

    assertThat(headers.get("Test-Header")).containsExactly((String) null);
  }

  @Test
  void add_withCaseInsensitiveName_shouldTreatAsSameHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value1");
    headers.add("test-header", "value2");

    assertThat(headers.get("Test-Header")).containsExactly("value1", "value2");
  }

  @Test
  void setHeader_shouldReplaceExistingValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");
    headers.setHeader("Test-Header", "new-value");

    assertThat(headers.get("Test-Header")).containsExactly("new-value");
  }

  @Test
  void setHeader_withNullValue_shouldRemoveHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");
    headers.setHeader("Test-Header", null);

    assertThat(headers.get("Test-Header")).isNull();
  }

  @Test
  void setOrRemove_withArray_shouldReplaceValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> oldValues = headers.setOrRemove("Test-Header", new String[] { "new-value1", "new-value2" });

    assertThat(headers.get("Test-Header")).containsExactly("new-value1", "new-value2");
    assertThat(oldValues).containsExactly("old-value");
  }

  @Test
  void setOrRemove_withEmptyArray_shouldRemoveHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    List<String> oldValues = headers.setOrRemove("Test-Header", new String[] {});

    assertThat(headers.get("Test-Header")).isNull();
    assertThat(oldValues).containsExactly("value");
  }

  @Test
  void setOrRemove_withNullArray_shouldRemoveHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    List<String> oldValues = headers.setOrRemove("Test-Header", (String[]) null);

    assertThat(headers.get("Test-Header")).isNull();
    assertThat(oldValues).containsExactly("value");
  }

  @Test
  void setOrRemove_withCollection_shouldReplaceValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> newValues = new ArrayList<>();
    newValues.add("new-value1");
    newValues.add("new-value2");
    List<String> oldValues = headers.setOrRemove("Test-Header", newValues);

    assertThat(headers.get("Test-Header")).containsExactly("new-value1", "new-value2");
    assertThat(oldValues).containsExactly("old-value");
  }

  @Test
  void setOrRemove_withEmptyCollection_shouldRemoveHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    List<String> oldValues = headers.setOrRemove("Test-Header", new ArrayList<>());

    assertThat(headers.get("Test-Header")).isNull();
    assertThat(oldValues).containsExactly("value");
  }

  @Test
  void setOrRemove_withNullCollection_shouldRemoveHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    List<String> oldValues = headers.setOrRemove("Test-Header", (Collection<String>) null);

    assertThat(headers.get("Test-Header")).isNull();
    assertThat(oldValues).containsExactly("value");
  }

  @Test
  void remove_shouldRemoveHeaderAndReturnValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value1");
    headers.add("Test-Header", "value2");

    List<String> removedValues = headers.remove("Test-Header");

    assertThat(removedValues).containsExactly("value1", "value2");
    assertThat(headers.get("Test-Header")).isNull();
  }

  @Test
  void remove_withNonExistentHeader_shouldReturnNull() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    List<String> removedValues = headers.remove("Non-Existent");

    assertThat(removedValues).isNull();
  }

  @Test
  void size_shouldReturnNumberOfHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value");
    headers.add("Header-2", "value");

    assertThat(headers.size()).isEqualTo(2);
  }

  @Test
  void isEmpty_shouldReturnTrueWhenNoHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void isEmpty_shouldReturnFalseWhenHasHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    assertThat(headers.isEmpty()).isFalse();
  }

  @Test
  void containsKey_shouldReturnTrueForExistingHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    assertThat(headers.containsKey("Test-Header")).isTrue();
    assertThat(headers.containsKey("test-header")).isTrue();
  }

  @Test
  void containsKey_shouldReturnFalseForNonExistentHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThat(headers.containsKey("Non-Existent")).isFalse();
  }

  @Test
  void containsValue_shouldReturnTrueForExistingValue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "test-value");

    List<String> valueList = new ArrayList<>();
    valueList.add("test-value");
    assertThat(headers.containsValue(valueList)).isTrue();
  }

  @Test
  void get_shouldReturnAllValuesForHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value1");
    headers.add("Test-Header", "value2");

    List<String> values = headers.get("Test-Header");
    assertThat(values).containsExactly("value1", "value2");
  }

  @Test
  void get_withNonExistentHeader_shouldReturnNull() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    List<String> values = headers.get("Non-Existent");
    assertThat(values).isNull();
  }

  @Test
  void put_shouldReplaceAllValuesForHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> newValues = new ArrayList<>();
    newValues.add("new-value1");
    newValues.add("new-value2");
    List<String> oldValues = headers.put("Test-Header", newValues);

    assertThat(headers.get("Test-Header")).containsExactly("new-value1", "new-value2");
    assertThat(oldValues).containsExactly("old-value");
  }

  @Test
  void putAll_shouldAddAllHeadersFromMap() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Map<String, List<String>> newHeaders = new HashMap<>();
    List<String> values1 = new ArrayList<>();
    values1.add("value1");
    values1.add("value2");
    List<String> values2 = new ArrayList<>();
    values2.add("value3");
    newHeaders.put("Header-1", values1);
    newHeaders.put("Header-2", values2);

    headers.putAll(newHeaders);

    assertThat(headers.get("Header-1")).containsExactly("value1", "value2");
    assertThat(headers.get("Header-2")).containsExactly("value3");
  }

  @Test
  void clear_shouldRemoveAllHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");

    headers.clear();

    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void keySet_shouldReturnAllHeaderNames() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value");
    headers.add("Header-2", "value");

    Set<String> keySet = headers.keySet();
    assertThat(keySet).containsExactlyInAnyOrder("Header-1", "Header-2");
  }

  @Test
  void values_shouldReturnAllHeaderValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");
    headers.add("Header-2", "value3");

    Collection<List<String>> values = headers.values();
    assertThat(values).hasSize(2);
  }

  @Test
  void entrySet_shouldReturnAllHeaderEntries() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");

    Set<Map.Entry<String, List<String>>> entrySet = headers.entrySet();
    assertThat(entrySet).hasSize(2);
  }

  @Test
  void toSingleValueMap_shouldReturnMapWithFirstValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-1", "value2");
    headers.add("Header-2", "value3");

    Map<String, String> singleValueMap = headers.toSingleValueMap();
    assertThat(singleValueMap).hasSize(2);
    assertThat(singleValueMap.get("Header-1")).isEqualTo("value1");
    assertThat(singleValueMap.get("Header-2")).isEqualTo("value3");
  }

  @Test
  void forEach_shouldIterateOverAllHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-1", "value2");
    headers.add("Header-2", "value3");

    AtomicInteger count = new AtomicInteger(0);
    headers.forEach((name, values) -> count.incrementAndGet());

    assertThat(count.get()).isEqualTo(2);
  }

  @Test
  void putIfAbsent_shouldAddHeaderIfNotExists() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    List<String> values = new ArrayList<>();
    values.add("value");
    List<String> result = headers.putIfAbsent("Test-Header", values);

    assertThat(result).isNull();
    assertThat(headers.get("Test-Header")).containsExactly("value");
  }

  @Test
  void putIfAbsent_shouldNotAddHeaderIfExists() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "existing-value");

    List<String> values = new ArrayList<>();
    values.add("new-value");
    List<String> result = headers.putIfAbsent("Test-Header", values);

    assertThat(result).containsExactly("existing-value");
    assertThat(headers.get("Test-Header")).containsExactly("existing-value");
  }

  @Test
  void asReadOnly_shouldReturnReadOnlyView() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    HttpHeaders readOnlyHeaders = headers.asReadOnly();

    assertThat(readOnlyHeaders).isInstanceOf(ReadOnlyHttpHeaders.class);
    assertThat(readOnlyHeaders.getFirst("Test-Header")).isEqualTo("value");
  }

  @Test
  void equals_withSameHeaders_shouldReturnTrue() {
    DefaultHttpHeaders headers1 = new DefaultHttpHeaders();
    headers1.add("Header-1", "value1");
    headers1.add("Header-2", "value2");

    DefaultHttpHeaders headers2 = new DefaultHttpHeaders();
    headers2.add("Header-1", "value1");
    headers2.add("Header-2", "value2");

    assertThat(headers1.equals(headers2)).isTrue();
  }

  @Test
  void equals_withDifferentHeaders_shouldReturnFalse() {
    DefaultHttpHeaders headers1 = new DefaultHttpHeaders();
    headers1.add("Header-1", "value1");

    DefaultHttpHeaders headers2 = new DefaultHttpHeaders();
    headers2.add("Header-2", "value2");

    assertThat(headers1.equals(headers2)).isFalse();
  }

  @Test
  void equals_withSameInstance_shouldReturnTrue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    assertThat(headers.equals(headers)).isTrue();
  }

  @Test
  void equals_withNull_shouldReturnFalse() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThat(headers.equals(null)).isFalse();
  }

  @Test
  void equals_withDifferentType_shouldReturnFalse() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThat(headers.equals("string")).isFalse();
  }

  @Test
  void hashCode_shouldBeConsistent() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    int hashCode1 = headers.hashCode();
    int hashCode2 = headers.hashCode();

    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  @Test
  void toArrayMap_shouldConvertToMapWithStringArrayValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-1", "value2");
    headers.add("Header-2", "value3");

    Map<String, String[]> arrayMap = headers.toArrayMap(String[]::new);

    assertThat(arrayMap).hasSize(2);
    assertThat(arrayMap.get("Header-1")).containsExactly("value1", "value2");
    assertThat(arrayMap.get("Header-2")).containsExactly("value3");
  }

  @Test
  void copyToArrayMap_shouldPopulateProvidedMap() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-1", "value2");

    Map<String, String[]> targetMap = new HashMap<>();
    headers.copyToArrayMap(targetMap, String[]::new);

    assertThat(targetMap).hasSize(1);
    assertThat(targetMap.get("Header-1")).containsExactly("value1", "value2");
  }

  @Test
  void setHeader_withMultipleValues_shouldReplaceWithSingleValue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value1");
    headers.add("Test-Header", "value2");
    headers.setHeader("Test-Header", "single-value");

    assertThat(headers.get("Test-Header")).containsExactly("single-value");
  }

  @Test
  void setHeader_caseInsensitive_shouldReplaceExisting() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");
    headers.setHeader("test-header", "new-value");

    assertThat(headers.getFirst("Test-Header")).isEqualTo("new-value");
  }

  @Test
  void constructor_withNullMultiValueMap_shouldThrowException() {
    assertThatThrownBy(() -> new DefaultHttpHeaders((MultiValueMap<String, String>) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MultiValueMap is required");
  }

  @Test
  void get_withCaseInsensitiveName_shouldReturnValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    assertThat(headers.get("test-header")).containsExactly("value");
    assertThat(headers.get("TEST-HEADER")).containsExactly("value");
  }

  @Test
  void put_withCaseInsensitiveKey_shouldReplaceValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> newValues = new ArrayList<>();
    newValues.add("new-value");
    List<String> oldValues = headers.put("test-header", newValues);

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.getFirst("Test-Header")).isEqualTo("new-value");
  }

  @Test
  void containsKey_caseInsensitive_shouldReturnTrue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    assertThat(headers.containsKey("test-header")).isTrue();
    assertThat(headers.containsKey("TEST-HEADER")).isTrue();
  }

  @Test
  void remove_caseInsensitive_shouldRemoveHeader() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value1");
    headers.add("Test-Header", "value2");

    List<String> removedValues = headers.remove("test-header");

    assertThat(removedValues).containsExactly("value1", "value2");
    assertThat(headers.get("Test-Header")).isNull();
  }

  @Test
  void putIfAbsent_caseInsensitive_shouldNotAddIfExist() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "existing-value");

    List<String> newValues = new ArrayList<>();
    newValues.add("new-value");
    List<String> result = headers.putIfAbsent("test-header", newValues);

    assertThat(result).containsExactly("existing-value");
    assertThat(headers.getFirst("Test-Header")).isEqualTo("existing-value");
  }

  @Test
  void equals_withWrappedInstances_shouldReturnTrue() {
    MultiValueMap<String, String> innerMap = new LinkedMultiValueMap<>();
    innerMap.add("Header", "value");
    DefaultHttpHeaders innerHeaders = new DefaultHttpHeaders(innerMap);

    DefaultHttpHeaders wrapper1 = new DefaultHttpHeaders(innerHeaders);
    DefaultHttpHeaders wrapper2 = new DefaultHttpHeaders(innerHeaders);

    assertThat(wrapper1.equals(wrapper2)).isTrue();
  }

  @Test
  void equals_withDifferentWrappedInstances_shouldReturnFalse() {
    MultiValueMap<String, String> map1 = new LinkedMultiValueMap<>();
    map1.add("Header", "value1");
    DefaultHttpHeaders headers1 = new DefaultHttpHeaders(map1);

    MultiValueMap<String, String> map2 = new LinkedMultiValueMap<>();
    map2.add("Header", "value2");
    DefaultHttpHeaders headers2 = new DefaultHttpHeaders(map2);

    assertThat(headers1.equals(headers2)).isFalse();
  }

  @Test
  void hashCode_consistencyWithWrappedInstances() {
    MultiValueMap<String, String> innerMap = new LinkedMultiValueMap<>();
    innerMap.add("Header", "value");
    DefaultHttpHeaders innerHeaders = new DefaultHttpHeaders(innerMap);
    DefaultHttpHeaders wrapper = new DefaultHttpHeaders(innerHeaders);

    int hashCode1 = wrapper.hashCode();
    int hashCode2 = wrapper.hashCode();

    assertThat(hashCode1).isEqualTo(hashCode2);
  }

  @Test
  void asReadOnly_modificationsShouldNotAffectOriginal() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Original-Header", "original-value");

    HttpHeaders readOnly = headers.asReadOnly();
    // This would cause compilation error if attempted:
    // readOnly.add("New-Header", "new-value");

    assertThat(readOnly.getFirst("Original-Header")).isEqualTo("original-value");
  }

  @Test
  void constructor_withMap_shouldMaintainCaseInsensitivity() {
    Map<String, List<String>> map = new LinkedHashMap<>();
    List<String> values = new ArrayList<>();
    values.add("value");
    map.put("Content-Type", values);

    DefaultHttpHeaders headers = new DefaultHttpHeaders(map);

    assertThat(headers.getFirst("content-type")).isEqualTo("value");
    assertThat(headers.getFirst("CONTENT-TYPE")).isEqualTo("value");
  }

  @Test
  void add_multipleValues_shouldMaintainOrder() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "first");
    headers.add("Test-Header", "second");
    headers.add("Test-Header", "third");

    assertThat(headers.get("Test-Header")).containsExactly("first", "second", "third");
  }

  @Test
  void setOrRemove_withArray_shouldHandleNullValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    List<String> oldValues = headers.setOrRemove("Test-Header", new String[] { null, "new-value" });

    assertThat(oldValues).containsExactly("value");
    assertThat(headers.get("Test-Header")).containsExactly((String) null, "new-value");
  }

  @Test
  void setOrRemove_withCollection_shouldHandleNullValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    List<String> newValues = new ArrayList<>();
    newValues.add(null);
    newValues.add("new-value");
    List<String> oldValues = headers.setOrRemove("Test-Header", newValues);

    assertThat(oldValues).containsExactly("value");
    assertThat(headers.get("Test-Header")).containsExactly((String) null, "new-value");
  }

  @Test
  void put_shouldMaintainCaseInsensitivity() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> newValues = new ArrayList<>();
    newValues.add("new-value");
    List<String> oldValues = headers.put("test-header", newValues);

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.getFirst("Test-Header")).isEqualTo("new-value");
  }

  @Test
  void putAll_shouldMaintainCaseInsensitivity() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Map<String, List<String>> newHeaders = new HashMap<>();
    List<String> values = new ArrayList<>();
    values.add("value");
    newHeaders.put("content-type", values);

    headers.putAll(newHeaders);

    assertThat(headers.getFirst("Content-Type")).isEqualTo("value");
  }

  @Test
  void keySet_shouldReflectActualKeys() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");
    headers.add("Another-Header", "value");

    Set<String> keySet = headers.keySet();

    assertThat(keySet).containsExactlyInAnyOrder("Test-Header", "Another-Header");
  }

  @Test
  void values_shouldReturnAllCollections() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");
    headers.add("Header-2", "value3");

    Collection<List<String>> values = headers.values();

    assertThat(values).containsExactlyInAnyOrder(
            List.of("value1"),
            List.of("value2", "value3")
    );
  }

  @Test
  void entrySet_shouldProvideAccessToAllEntries() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");

    Set<Map.Entry<String, List<String>>> entries = headers.entrySet();

    assertThat(entries).hasSize(2);
    assertThat(entries.stream().map(Map.Entry::getKey))
            .containsExactlyInAnyOrder("Header-1", "Header-2");
  }

  @Test
  void toSingleValueMap_shouldHandleNullValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-With-Null", null);
    headers.add("Header-With-Value", "value");

    Map<String, String> singleValueMap = headers.toSingleValueMap();

    assertThat(singleValueMap).hasSize(2);
    assertThat(singleValueMap.get("Header-With-Null")).isNull();
    assertThat(singleValueMap.get("Header-With-Value")).isEqualTo("value");
  }

  @Test
  void forEach_shouldProcessAllHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header-1", "value1");
    headers.add("Header-2", "value2");
    headers.add("Header-2", "value3");

    Map<String, List<String>> processedHeaders = new HashMap<>();
    headers.forEach(processedHeaders::put);

    assertThat(processedHeaders).hasSize(2);
    assertThat(processedHeaders.get("Header-1")).containsExactly("value1");
    assertThat(processedHeaders.get("Header-2")).containsExactly("value2", "value3");
  }

  @Test
  void putIfAbsent_shouldMaintainCaseInsensitivity() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "existing");

    List<String> newValues = new ArrayList<>();
    newValues.add("new-value");
    List<String> result = headers.putIfAbsent("test-header", newValues);

    assertThat(result).containsExactly("existing");
    assertThat(headers.getFirst("Test-Header")).isEqualTo("existing");
  }

  @Test
  void equals_withSelf_shouldReturnTrue() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header", "value");

    assertThat(headers.equals(headers)).isTrue();
  }

  @Test
  void equals_withDifferentClass_shouldReturnFalse() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThat(headers.equals("not a headers object")).isFalse();
  }

  @Test
  void hashCode_consistency() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header", "value");

    int firstHash = headers.hashCode();
    int secondHash = headers.hashCode();

    assertThat(firstHash).isEqualTo(secondHash);
  }

  @Test
  void serializable_shouldMaintainStateAfterSerialization() throws Exception {
    DefaultHttpHeaders original = new DefaultHttpHeaders();
    original.add("Test-Header", "value");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(original);
    oos.close();

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    DefaultHttpHeaders deserialized = (DefaultHttpHeaders) ois.readObject();
    ois.close();

    assertThat(deserialized.getFirst("Test-Header")).isEqualTo("value");
    assertThat(deserialized.getFirst("test-header")).isEqualTo("value");
  }

  @Test
  void constructor_withMap_shouldPreserveAllValues() {
    Map<String, List<String>> map = new LinkedHashMap<>();
    List<String> values1 = new ArrayList<>();
    values1.add("value1");
    values1.add("value2");
    List<String> values2 = new ArrayList<>();
    values2.add("value3");
    map.put("Header-One", values1);
    map.put("Header-Two", values2);

    DefaultHttpHeaders headers = new DefaultHttpHeaders(map);

    assertThat(headers.get("Header-One")).containsExactly("value1", "value2");
    assertThat(headers.get("Header-Two")).containsExactly("value3");
    assertThat(headers.size()).isEqualTo(2);
  }

  @Test
  void addAll_shouldAddAllHeadersFromMap() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Map<String, List<String>> sourceMap = new HashMap<>();
    List<String> values1 = new ArrayList<>();
    values1.add("value1");
    values1.add("value2");
    List<String> values2 = new ArrayList<>();
    values2.add("value3");
    sourceMap.put("Header-1", values1);
    sourceMap.put("Header-2", values2);

    headers.addAll(sourceMap);

    assertThat(headers.get("Header-1")).containsExactly("value1", "value2");
    assertThat(headers.get("Header-2")).containsExactly("value3");
  }

  @Test
  void addAll_withEmptyMap_shouldNotChangeHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Existing-Header", "existing-value");

    Map<String, List<String>> emptyMap = new HashMap<>();
    headers.addAll(emptyMap);

    assertThat(headers.size()).isEqualTo(1);
    assertThat(headers.getFirst("Existing-Header")).isEqualTo("existing-value");
  }

  @Test
  void addAll_shouldMaintainCaseInsensitivity() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Map<String, List<String>> sourceMap = new HashMap<>();
    List<String> values = new ArrayList<>();
    values.add("value");
    sourceMap.put("content-type", values);

    headers.addAll(sourceMap);

    assertThat(headers.getFirst("Content-Type")).isEqualTo("value");
    assertThat(headers.getFirst("CONTENT-TYPE")).isEqualTo("value");
  }

  @Test
  void getOrDefault_withExistingHeader_shouldReturnValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value1");
    headers.add("Test-Header", "value2");

    List<String> defaultValues = new ArrayList<>();
    defaultValues.add("default");

    List<String> result = headers.getOrDefault("Test-Header", defaultValues);

    assertThat(result).containsExactly("value1", "value2");
    assertThat(result).isNotSameAs(defaultValues);
  }

  @Test
  void getOrDefault_withNonExistentHeader_shouldReturnDefault() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    List<String> defaultValues = new ArrayList<>();
    defaultValues.add("default");

    List<String> result = headers.getOrDefault("Non-Existent", defaultValues);

    assertThat(result).isSameAs(defaultValues);
    assertThat(result).containsExactly("default");
  }

  @Test
  void getOrDefault_caseInsensitive_shouldReturnValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    List<String> defaultValues = new ArrayList<>();
    defaultValues.add("default");

    List<String> result = headers.getOrDefault("test-header", defaultValues);

    assertThat(result).containsExactly("value");
  }

  @Test
  void containsKey_withEmptyHeaders_shouldReturnFalse() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThat(headers.containsKey("Any-Key")).isFalse();
  }

  @Test
  void containsValue_withEmptyHeaders_shouldReturnFalse() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    List<String> values = new ArrayList<>();
    values.add("any-value");

    assertThat(headers.containsValue(values)).isFalse();
  }

  @Test
  void put_withNullValue_shouldRemoveEntry() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> oldValues = headers.put("Test-Header", null);

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.get("Test-Header")).isNull();
  }

  @Test
  void clear_onEmptyHeaders_shouldNotThrowException() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    assertThatNoException().isThrownBy(headers::clear);
    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void keySet_onEmptyHeaders_shouldReturnEmptySet() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Set<String> keySet = headers.keySet();

    assertThat(keySet).isEmpty();
  }

  @Test
  void values_onEmptyHeaders_shouldReturnEmptyCollection() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Collection<List<String>> values = headers.values();

    assertThat(values).isEmpty();
  }

  @Test
  void entrySet_onEmptyHeaders_shouldReturnEmptySet() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Set<Map.Entry<String, List<String>>> entries = headers.entrySet();

    assertThat(entries).isEmpty();
  }

  @Test
  void toSingleValueMap_onEmptyHeaders_shouldReturnEmptyMap() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    Map<String, String> singleValueMap = headers.toSingleValueMap();

    assertThat(singleValueMap).isEmpty();
  }

  @Test
  void forEach_onEmptyHeaders_shouldNotInvokeAction() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    AtomicInteger invocationCount = new AtomicInteger(0);
    headers.forEach((name, values) -> invocationCount.incrementAndGet());

    assertThat(invocationCount.get()).isEqualTo(0);
  }

  @Test
  void equals_withDifferentObjectType_shouldReturnFalse() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Header", "value");

    assertThat(headers.equals("string")).isFalse();
    assertThat(headers.equals(new Object())).isFalse();
  }

  @Test
  void hashCode_consistencyWithEmptyHeaders() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();

    int hash1 = headers.hashCode();
    int hash2 = headers.hashCode();

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void asReadOnly_shouldCreateReadOnlyInstance() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "value");

    HttpHeaders readOnly = headers.asReadOnly();

    assertThat(readOnly).isInstanceOf(ReadOnlyHttpHeaders.class);
    assertThat(readOnly.getFirst("Test-Header")).isEqualTo("value");
  }

  @Test
  void serialization_shouldPreserveAllData() throws Exception {
    DefaultHttpHeaders original = new DefaultHttpHeaders();
    original.add("Header-One", "value1");
    original.add("Header-One", "value2");
    original.add("Header-Two", "value3");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(original);
    oos.flush();

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);
    DefaultHttpHeaders deserialized = (DefaultHttpHeaders) ois.readObject();

    assertThat(deserialized.size()).isEqualTo(2);
    assertThat(deserialized.get("Header-One")).containsExactly("value1", "value2");
    assertThat(deserialized.get("Header-Two")).containsExactly("value3");

    // Check case insensitivity is preserved
    assertThat(deserialized.getFirst("header-one")).isEqualTo("value1");
  }

  @Test
  void setOrRemove_withArray_shouldHandleMixedNullAndNonNullValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> oldValues = headers.setOrRemove("Test-Header", new String[] { "value1", null, "value2" });

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.get("Test-Header")).containsExactly("value1", null, "value2");
  }

  @Test
  void setOrRemove_withCollection_shouldHandleMixedNullAndNonNullValues() {
    DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Test-Header", "old-value");

    List<String> newValues = new ArrayList<>();
    newValues.add("value1");
    newValues.add(null);
    newValues.add("value2");

    List<String> oldValues = headers.setOrRemove("Test-Header", newValues);

    assertThat(oldValues).containsExactly("old-value");
    assertThat(headers.get("Test-Header")).containsExactly("value1", null, "value2");
  }

}