package infra.http;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/14 20:57
 */
class HeadersMultiValueMapTests {

  @Test
  void getFirst() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Content-Type", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.getFirst("Content-Type")).isEqualTo("application/json");
    assertThat(adapter.getFirst("Non-Existent")).isNull();
  }

  @Test
  void add() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    adapter.add("Accept", "application/json");
    adapter.add("Accept", "text/html");

    assertThat(headers.get("Accept")).containsExactly("application/json", "text/html");
  }

  @Test
  void setOrRemoveWithSingleValue() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Cache-Control", "no-cache");
    headers.add("Cache-Control", "no-store");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.setOrRemove("Cache-Control", "max-age=3600");

    assertThat(oldValue).containsExactly("no-cache", "no-store");
    assertThat(headers.get("Cache-Control")).containsExactly("max-age=3600");
  }

  @Test
  void setOrRemoveWithArrayValue() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept", "text/html");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.setOrRemove("Accept", new String[] { "application/json", "text/plain" });

    assertThat(oldValue).containsExactly("text/html");
    assertThat(headers.get("Accept")).containsExactly("application/json", "text/plain");
  }

  @Test
  void setOrRemoveWithCollectionValue() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept-Language", "en-US");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> values = List.of("zh-CN", "en-US");
    List<String> oldValue = adapter.setOrRemove("Accept-Language", values);

    assertThat(oldValue).containsExactly("en-US");
    assertThat(headers.get("Accept-Language")).containsExactly("zh-CN", "en-US");
  }

  @Test
  void setOrRemoveNullValue() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Custom-Header", "value1");
    headers.add("X-Custom-Header", "value2");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.setOrRemove("X-Custom-Header", (String) null);

    assertThat(oldValue).containsExactly("value1", "value2");
    assertThat(headers.contains("X-Custom-Header")).isFalse();
  }

  @Test
  void size() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");
    headers.add("Header3", "value3");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.size()).isEqualTo(3);
  }

  @Test
  void isEmpty() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.isEmpty()).isTrue();

    headers.add("Content-Type", "application/json");
    assertThat(adapter.isEmpty()).isFalse();
  }

  @Test
  void containsKeyWithStringKey() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.containsKey("Content-Type")).isTrue();
    assertThat(adapter.containsKey("Non-Existent")).isFalse();
  }

  @Test
  void containsKeyWithNonStringKey() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.containsKey(123)).isFalse();
    assertThat(adapter.containsKey(null)).isFalse();
  }

  @Test
  void containsValue() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.containsValue("application/json")).isTrue();
    assertThat(adapter.containsValue("text/html")).isTrue();
    assertThat(adapter.containsValue("non-existent")).isFalse();
  }

  @Test
  void get() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Content-Type", "charset=utf-8");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.get("Content-Type")).containsExactly("application/json", "charset=utf-8");
    assertThat(adapter.get("Non-Existent")).isNull();
  }

  @Test
  void getWithNonStringKey() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.get(123)).isNull();
    assertThat(adapter.get(null)).isNull();
  }

  @Test
  void put() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept", "text/html");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.put("Accept", List.of("application/json", "text/plain"));

    assertThat(oldValue).containsExactly("text/html");
    assertThat(headers.get("Accept")).containsExactly("application/json", "text/plain");
  }

  @Test
  void remove() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Custom-Header", "value1");
    headers.add("X-Custom-Header", "value2");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> removed = adapter.remove("X-Custom-Header");

    assertThat(removed).containsExactly("value1", "value2");
    assertThat(headers.contains("X-Custom-Header")).isFalse();
  }

  @Test
  void removeWithNonStringKey() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.remove(123)).isNull();
  }

  @Test
  void putAll() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    Map<String, List<String>> map = new LinkedHashMap<>();
    map.put("Accept", List.of("application/json"));
    map.put("Content-Type", List.of("text/html"));

    adapter.putAll(map);

    assertThat(headers.get("Accept")).containsExactly("application/json");
    assertThat(headers.get("Content-Type")).containsExactly("text/html");
  }

  @Test
  void clear() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    adapter.clear();

    assertThat(adapter.isEmpty()).isTrue();
    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void keySet() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");
    headers.add("Accept-Language", "en-US");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Set<String> keySet = adapter.keySet();

    assertThat(keySet).containsExactlyInAnyOrder("Content-Type", "Accept", "Accept-Language");
  }

  @Test
  void values() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");
    headers.add("Accept", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Collection<List<String>> values = adapter.values();

    assertThat(values).hasSize(2);
    assertThat(values).contains(List.of("application/json"), List.of("text/html", "text/plain"));
  }

  @Test
  void entrySet() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Set<Entry<String, List<String>>> entrySet = adapter.entrySet();

    assertThat(entrySet).hasSize(2);

    Map<String, List<String>> map = entrySet.stream()
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    assertThat(map.get("Content-Type")).containsExactly("application/json");
    assertThat(map.get("Accept")).containsExactly("text/html");
  }

  @Test
  void integrationWithHttpHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    adapter.add("Authorization", "Bearer token123");
    adapter.add("X-Request-ID", "req-001");
    adapter.add("X-Request-ID", "req-002");

    assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer token123");
    assertThat(headers.get("X-Request-ID")).containsExactly("req-001", "req-002");
    assertThat(adapter.getFirst("Authorization")).isEqualTo("Bearer token123");
    assertThat(adapter.size()).isEqualTo(2);
  }

  @Test
  void caseSensitiveHeaderNames() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    adapter.add("Content-Type", "application/json");
    adapter.add("content-type", "text/html");

    assertThat(headers.get("Content-Type")).contains("application/json");
    assertThat(headers.get("content-type")).contains("text/html");
  }

  // ... existing code ...

  @Test
  void addAllWithMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    Map<String, List<String>> map = new LinkedHashMap<>();
    map.put("Accept", List.of("application/json", "text/html"));
    map.put("Content-Type", List.of("text/plain"));

    adapter.addAll(map);

    assertThat(headers.get("Accept")).containsExactly("application/json", "text/html");
    assertThat(headers.get("Content-Type")).containsExactly("text/plain");
  }

  @Test
  void addAllWithEmptyMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Existing-Header", "value1");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    adapter.addAll(Collections.emptyMap());

    assertThat(adapter.size()).isEqualTo(1);
    assertThat(headers.get("Existing-Header")).containsExactly("value1");
  }

  @Test
  void addIfAbsent() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    adapter.addIfAbsent("Content-Type", "text/html");
    adapter.addIfAbsent("Accept", "application/xml");

    assertThat(headers.get("Content-Type")).containsExactly("application/json");
    assertThat(headers.get("Accept")).containsExactly("application/xml");
  }

  @Test
  void asSingleValueMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");
    headers.add("Accept", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Map<String, String> singleValueMap = adapter.asSingleValueMap();

    assertThat(singleValueMap.get("Content-Type")).isEqualTo("application/json");
    assertThat(singleValueMap.get("Accept")).isEqualTo("text/html");

    headers.add("Cache-Control", "no-cache");
    assertThat(singleValueMap.get("Cache-Control")).isEqualTo("no-cache");
  }

  @Test
  void toSingleValueMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");
    headers.add("Accept", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Map<String, String> singleValueMap = adapter.toSingleValueMap();

    assertThat(singleValueMap).hasSize(2);
    assertThat(singleValueMap.get("Content-Type")).isEqualTo("application/json");
    assertThat(singleValueMap.get("Accept")).isEqualTo("text/html");
  }

  @Test
  void toArrayMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");
    headers.add("Accept", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Map<String, String[]> arrayMap = adapter.toArrayMap(String[]::new);

    assertThat(arrayMap).hasSize(2);
    assertThat(arrayMap.get("Content-Type")).containsExactly("application/json");
    assertThat(arrayMap.get("Accept")).containsExactly("text/html", "text/plain");
  }

  @Test
  void copyToArrayMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");
    headers.add("Accept", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Map<String, String[]> targetMap = new LinkedHashMap<>();
    adapter.copyToArrayMap(targetMap, String[]::new);

    assertThat(targetMap).hasSize(2);
    assertThat(targetMap.get("Content-Type")).containsExactly("application/json");
    assertThat(targetMap.get("Accept")).containsExactly("text/html", "text/plain");
  }

  @Test
  void forEach() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    List<String> visitedKeys = new ArrayList<>();
    adapter.forEach((key, values) -> visitedKeys.add(key));

    assertThat(visitedKeys).containsExactlyInAnyOrder("Content-Type", "Accept");
  }

  @Test
  void equalsAndHashCode() {
    HttpHeaders headers1 = HttpHeaders.forWritable();
    headers1.add("Content-Type", "application/json");
    headers1.add("Accept", "text/html");

    HttpHeaders headers2 = HttpHeaders.forWritable();
    headers2.add("Content-Type", "application/json");
    headers2.add("Accept", "text/html");

    HeadersMultiValueMap adapter1 = new HeadersMultiValueMap(headers1);
    HeadersMultiValueMap adapter2 = new HeadersMultiValueMap(headers2);

    assertThat(adapter1).isEqualTo(adapter2);
    assertThat(adapter1.hashCode()).isEqualTo(adapter2.hashCode());
  }

  @Test
  void equalsWithSameHeadersReference() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter1 = new HeadersMultiValueMap(headers);
    HeadersMultiValueMap adapter2 = new HeadersMultiValueMap(headers);

    assertThat(adapter1).isEqualTo(adapter2);
  }

  @Test
  void notEqualsWithDifferentHeaders() {
    HttpHeaders headers1 = HttpHeaders.forWritable();
    headers1.add("Content-Type", "application/json");

    HttpHeaders headers2 = HttpHeaders.forWritable();
    headers2.add("Content-Type", "text/html");

    HeadersMultiValueMap adapter1 = new HeadersMultiValueMap(headers1);
    HeadersMultiValueMap adapter2 = new HeadersMultiValueMap(headers2);

    assertThat(adapter1).isNotEqualTo(adapter2);
  }

  @Test
  void toStringMethod() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");
    headers.add("Accept", "text/html");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.toString()).isEqualTo(headers.toString());
  }

  @Test
  void setOrRemoveWithEmptyCollection() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.setOrRemove("Content-Type", Collections.emptyList());

    assertThat(oldValue).containsExactly("application/json");
    assertThat(headers.contains("Content-Type")).isTrue();
    assertThat(headers.get("Content-Type")).isEqualTo(List.of());
  }

  @Test
  void setOrRemoveWithNullArray() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.setOrRemove("Content-Type", (String[]) null);

    assertThat(oldValue).containsExactly("application/json");
    assertThat(headers.contains("Content-Type")).isFalse();
  }

  @Test
  void containsValueWithMultipleValues() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept", "text/html");
    headers.add("Accept", "application/json");
    headers.add("Accept", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.containsValue("text/html")).isTrue();
    assertThat(adapter.containsValue("application/json")).isTrue();
    assertThat(adapter.containsValue("text/plain")).isTrue();
    assertThat(adapter.containsValue("image/png")).isFalse();
  }

  @Test
  void putWithNullValue() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.put("Content-Type", null);

    assertThat(oldValue).containsExactly("application/json");
    assertThat(headers.contains("Content-Type")).isFalse();
  }

  @Test
  void putWithEmptyList() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.put("Content-Type", Collections.emptyList());

    assertThat(oldValue).containsExactly("application/json");
    assertThat(headers.contains("Content-Type")).isTrue();
    assertThat(headers.hasValues("Content-Type", List.of())).isTrue();
  }

  @Test
  void removeNonExistentHeader() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.remove("Non-Existent")).isNull();
  }

  @Test
  void valuesReturnsCorrectSize() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");
    headers.add("Header3", "value3");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Collection<List<String>> values = adapter.values();

    assertThat(values).hasSize(3);
  }

  @Test
  void emptyHeadersOperations() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.keySet()).isEmpty();
    assertThat(adapter.values()).isEmpty();
    assertThat(adapter.entrySet()).isEmpty();
    assertThat(adapter.isEmpty()).isTrue();
    assertThat(adapter.size()).isZero();
  }

  @Test
  void addAllEntriesFromAnotherMap() {
    HttpHeaders headers1 = HttpHeaders.forWritable();
    headers1.add("Accept", "application/json");

    HeadersMultiValueMap adapter1 = new HeadersMultiValueMap(headers1);

    HttpHeaders headers2 = HttpHeaders.forWritable();
    headers2.add("Content-Type", "text/html");

    HeadersMultiValueMap adapter2 = new HeadersMultiValueMap(headers2);

    adapter1.addAll(adapter2);

    assertThat(adapter1.get("Accept")).containsExactly("application/json");
    assertThat(adapter1.get("Content-Type")).containsExactly("text/html");
  }

  @Test
  void setOrRemoveWithEmptyArray() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> oldValue = adapter.setOrRemove("Content-Type", new String[0]);

    assertThat(oldValue).containsExactly("application/json");
    assertThat(headers.contains("Content-Type")).isTrue();
    assertThat(headers.get("Content-Type")).isEqualTo(List.of());
  }

  @Test
  void setOrRemoveWithCollectionContainingNullElements() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Custom", "value1");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> valuesWithNulls = new ArrayList<>();
    valuesWithNulls.add("value1");
    valuesWithNulls.add(null);
    valuesWithNulls.add("value2");

    List<String> oldValue = adapter.setOrRemove("X-Custom", valuesWithNulls);

    assertThat(oldValue).containsExactly("value1");
    assertThat(headers.get("X-Custom")).containsExactly("value1", null, "value2");
  }

  @Test
  void addWithNullValue() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    adapter.add("X-Custom", null);

    assertThat(headers.get("X-Custom")).isNull();
  }

  @Test
  void addAllWithNullMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Existing", "value1");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    adapter.addAll((Map<String, List<String>>) null);

    assertThat(adapter.size()).isEqualTo(1);
    assertThat(headers.get("Existing")).containsExactly("value1");
  }

  @Test
  void putIfAbsentWithExistingKey() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> result = adapter.putIfAbsent("Content-Type", List.of("text/html"));

    assertThat(result).containsExactly("application/json");
    assertThat(headers.get("Content-Type")).containsExactly("application/json");
  }

  @Test
  void putIfAbsentWithNonExistentKey() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    List<String> result = adapter.putIfAbsent("Accept", List.of("application/json"));

    assertThat(result).isNull();
    assertThat(headers.get("Accept")).containsExactly("application/json");
  }

  @Test
  void getFirstWithMultipleValues() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Accept", "text/html");
    headers.add("Accept", "application/json");
    headers.add("Accept", "text/plain");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.getFirst("Accept")).isEqualTo("text/html");
  }

  @Test
  void containsKeyWithVariousObjectTypes() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.containsKey("Content-Type")).isTrue();
    assertThat(adapter.containsKey(new StringBuilder("Content-Type"))).isFalse();
    assertThat(adapter.containsKey(123L)).isFalse();
    assertThat(adapter.containsKey(new Object())).isFalse();
  }

  @Test
  void removeWithVariousObjectTypes() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.remove(new StringBuilder("Content-Type"))).isNull();
    assertThat(adapter.remove(123)).isNull();
    assertThat(adapter.remove(null)).isNull();
    assertThat(headers.contains("Content-Type")).isTrue();
  }

  @Test
  void putAllWithNullValuesInMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    Map<String, List<String>> map = new LinkedHashMap<>();
    map.put("Accept", null);
    map.put("Content-Type", List.of("application/json"));

    assertThatThrownBy(() -> adapter.putAll(map))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void setOrRemovePreservesHeaderOrder() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");
    headers.add("Header3", "value3");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    adapter.setOrRemove("Header2", List.of("new-value"));

    List<String> keyOrder = new ArrayList<>(adapter.keySet());
    assertThat(keyOrder).containsExactly("Header1", "Header2", "Header3");
  }

  @Test
  void addAllPreservesInsertionOrder() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    Map<String, List<String>> mapToAdd = new LinkedHashMap<>();
    mapToAdd.put("Header2", List.of("value2"));
    mapToAdd.put("Header3", List.of("value3"));

    adapter.addAll(mapToAdd);

    List<String> keyOrder = new ArrayList<>(adapter.keySet());
    assertThat(keyOrder).containsExactly("Header1", "Header2", "Header3");
  }

  @Test
  void forEachWithNullAction() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThatThrownBy(() -> adapter.forEach(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void entrySetIteratorConsistency() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");
    headers.add("Header2", "value3");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    Set<Entry<String, List<String>>> entrySet = adapter.entrySet();
    int entryCount = 0;
    for (Entry<String, List<String>> entry : entrySet) {
      assertThat(entry.getKey()).isIn("Header1", "Header2");
      assertThat(entry.getValue()).isNotNull();
      entryCount++;
    }
    assertThat(entryCount).isEqualTo(2);
  }

  @Test
  void valuesContainsAllHeaderValues() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");
    headers.add("Header2", "value3");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Collection<List<String>> values = adapter.values();

    assertThat(values).hasSize(2);
    assertThat(values).contains(List.of("value1"), List.of("value2", "value3"));
  }

  @Test
  void setOrRemoveWithSingletonCollection() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    List<String> singletonList = Collections.singletonList("text/html");
    List<String> oldValue = adapter.setOrRemove("Content-Type", singletonList);

    assertThat(oldValue).containsExactly("application/json");
    assertThat(headers.get("Content-Type")).containsExactly("text/html");
  }

  @Test
  void addWithEmptyStringValues() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    adapter.add("X-Empty", "");
    adapter.add("X-Empty", "");

    assertThat(headers.get("X-Empty")).containsExactly("", "");
  }

  @Test
  void containsValueWithEmptyString() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Empty", "");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    assertThat(adapter.containsValue("")).isTrue();
    assertThat(adapter.containsValue(" ")).isFalse();
  }

  @Test
  void toSingleValueMapWithEmptyHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);

    Map<String, String> singleValueMap = adapter.toSingleValueMap();

    assertThat(singleValueMap).isEmpty();
  }

  @Test
  void toArrayMapWithCustomGenerator() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Map<String, String[]> arrayMap = adapter.toArrayMap(size -> new String[size]);

    assertThat(arrayMap).hasSize(2);
    assertThat(arrayMap.get("Header1")).hasSize(1);
    assertThat(arrayMap.get("Header2")).hasSize(1);
  }

  @Test
  void copyToArrayMapWithExistingMap() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    Map<String, String[]> existingMap = new LinkedHashMap<>();
    existingMap.put("Existing", new String[] { "existing-value" });

    adapter.copyToArrayMap(existingMap, String[]::new);

    assertThat(existingMap).hasSize(2);
    assertThat(existingMap.get("Header1")).containsExactly("value1");
    assertThat(existingMap.get("Existing")).containsExactly("existing-value");
  }

  @Test
  void equalsWithDifferentHeaderValues() {
    HttpHeaders headers1 = HttpHeaders.forWritable();
    headers1.add("Content-Type", "application/json");

    HttpHeaders headers2 = HttpHeaders.forWritable();
    headers2.add("Content-Type", "text/html");

    HeadersMultiValueMap adapter1 = new HeadersMultiValueMap(headers1);
    HeadersMultiValueMap adapter2 = new HeadersMultiValueMap(headers2);

    assertThat(adapter1).isNotEqualTo(adapter2);
  }

  @Test
  void hashCodeConsistencyWithEquals() {
    HttpHeaders headers1 = HttpHeaders.forWritable();
    headers1.add("Content-Type", "application/json");

    HttpHeaders headers2 = HttpHeaders.forWritable();
    headers2.add("Content-Type", "application/json");

    HeadersMultiValueMap adapter1 = new HeadersMultiValueMap(headers1);
    HeadersMultiValueMap adapter2 = new HeadersMultiValueMap(headers2);

    assertThat(adapter1).isEqualTo(adapter2);
    assertThat(adapter1.hashCode()).isEqualTo(adapter2.hashCode());
  }

  @Test
  void toStringIncludesAllHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");
    headers.add("Header2", "value3");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    String toString = adapter.toString();

    assertThat(toString).contains("Header1");
    assertThat(toString).contains("Header2");
    assertThat(toString).contains("value1");
    assertThat(toString).contains("value2");
    assertThat(toString).contains("value3");
  }

  @Test
  void mutableThroughAdapterAffectsOriginalHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Existing", "value1");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    adapter.put("New-Header", List.of("new-value"));
    adapter.add("Existing", "value2");

    assertThat(headers.get("Existing")).containsExactly("value1", "value2");
    assertThat(headers.get("New-Header")).containsExactly("new-value");
  }

  @Test
  void clearRemovesAllHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("Header1", "value1");
    headers.add("Header2", "value2");

    HeadersMultiValueMap adapter = new HeadersMultiValueMap(headers);
    adapter.clear();

    assertThat(adapter.isEmpty()).isTrue();
    assertThat(headers.isEmpty()).isTrue();
    assertThat(headers.size()).isZero();
  }

}