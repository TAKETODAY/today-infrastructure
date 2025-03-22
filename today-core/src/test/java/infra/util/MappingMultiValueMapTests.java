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

package infra.util;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 20:29
 */
class MappingMultiValueMapTests {

  @Test
  void defaultConstructorCreatesEmptyMap() {
    var map = new MappingMultiValueMap<String, String>();
    assertThat(map).isEmpty();
  }

  @Test
  void addStoresValueInList() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value1");
    map.add("key", "value2");

    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void addAllWithCollectionStoresAllValues() {
    var map = new MappingMultiValueMap<String, String>();
    map.addAll("key", List.of("value1", "value2"));

    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void addAllWithNullCollectionDoesNothing() {
    var map = new MappingMultiValueMap<String, String>();
    map.addAll("key", (Collection<String>) null);

    assertThat(map).isEmpty();
  }

  @Test
  void addAllWithEnumerationStoresAllValues() {
    var map = new MappingMultiValueMap<String, String>();
    var values = Collections.enumeration(List.of("value1", "value2"));
    map.addAll("key", values);

    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void addAllWithNullEnumerationDoesNothing() {
    var map = new MappingMultiValueMap<String, String>();
    map.addAll("key", (Enumeration<String>) null);

    assertThat(map).isEmpty();
  }

  @Test
  void setOrRemoveWithValueStoresValue() {
    var map = new MappingMultiValueMap<String, String>();
    map.setOrRemove("key", "value");

    assertThat(map.get("key")).containsExactly("value");
  }

  @Test
  void setOrRemoveWithNullValueRemovesKey() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value");
    map.setOrRemove("key", (String) null);

    assertThat(map).isEmpty();
  }

  @Test
  void setOrRemoveWithArrayStoresValues() {
    var map = new MappingMultiValueMap<String, String>();
    map.setOrRemove("key", new String[] { "value1", "value2" });

    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void setOrRemoveWithNullArrayRemovesKey() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value");
    map.setOrRemove("key", (String[]) null);

    assertThat(map).isEmpty();
  }

  @Test
  void setOrRemoveWithCollectionStoresValues() {
    var map = new MappingMultiValueMap<String, String>();
    map.setOrRemove("key", List.of("value1", "value2"));

    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void setOrRemoveWithNullCollectionRemovesKey() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value");
    map.setOrRemove("key", (Collection<String>) null);

    assertThat(map).isEmpty();
  }

  @Test
  void customMappingFunctionIsUsed() {
    var map = new MappingMultiValueMap<String, String>(key -> new LinkedList<>());
    map.add("key", "value");

    assertThat(map.get("key"))
            .isInstanceOf(LinkedList.class)
            .containsExactly("value");
  }

  @Test
  void constructorWithNullMappingFunctionThrowsException() {
    assertThatThrownBy(() -> new MappingMultiValueMap<>(new HashMap<>(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mappingFunction is required");
  }

  @Test
  void trimToSizeReducesInternalCapacity() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value");
    map.trimToSize();

    assertThat(map.get("key")).containsExactly("value");
  }

  @Test
  void addingValuesWithSameKeyAppendsToList() {
    var map = new MappingMultiValueMap<String, Integer>();
    map.add("scores", 10);
    map.add("scores", 20);
    map.add("scores", 30);

    assertThat(map.get("scores")).containsExactly(10, 20, 30);
  }

  @Test
  void addingEmptyCollectionDoesNotCreateEmptyList() {
    var map = new MappingMultiValueMap<String, String>();
    map.addAll("key", Collections.emptyList());
    assertThat(map.containsKey("key")).isFalse();
  }

  @Test
  void gettingValueForNonExistentKeyReturnsNull() {
    var map = new MappingMultiValueMap<String, String>();
    assertThat(map.get("nonexistent")).isNull();
  }

  @Test
  void removeValueRemovesEntireList() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value1");
    map.add("key", "value2");

    map.remove("key");
    assertThat(map).isEmpty();
  }

  @Test
  void clearRemovesAllValues() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key1", "value1");
    map.add("key2", "value2");

    map.clear();
    assertThat(map).isEmpty();
  }

  @Test
  void iteratingOverValuesReturnsAllLists() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key1", "value1");
    map.add("key2", "value2");
    map.add("key2", "value3");

    Collection<List<String>> values = map.values();
    assertThat(values).hasSize(2);
    assertThat(values).contains(
            List.of("value1"),
            List.of("value2", "value3")
    );
  }

  @Test
  void equalsComparesContentNotInstances() {
    var map1 = new MappingMultiValueMap<String, String>();
    var map2 = new MappingMultiValueMap<String, String>();

    map1.add("key", "value");
    map2.add("key", "value");

    assertThat(map1).isEqualTo(map2);
  }

  @Test
  void hashCodeGeneratesSameValueForEqualMaps() {
    var map1 = new MappingMultiValueMap<String, String>();
    var map2 = new MappingMultiValueMap<String, String>();

    map1.add("key", "value");
    map2.add("key", "value");

    assertThat(map1.hashCode()).isEqualTo(map2.hashCode());
  }

  @Test
  void putAllWithMultiValueMapPreservesLists() {
    var source = new MappingMultiValueMap<String, String>();
    source.add("key", "value1");
    source.add("key", "value2");

    var target = new MappingMultiValueMap<String, String>();
    target.putAll(source);

    assertThat(target.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void putOperationReplacesExistingList() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value1");
    map.add("key", "value2");

    map.put("key", List.of("newValue"));

    assertThat(map.get("key")).containsExactly("newValue");
  }

  @Test
  void nullKeyIsNotAllowed() {
    var map = new MappingMultiValueMap<String, String>(
            new ConcurrentHashMap<>()
    );

    assertThatThrownBy(() -> map.add(null, "value"))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void getFirstReturnsNullForMissingKey() {
    var map = new MappingMultiValueMap<String, String>();
    assertThat(map.getFirst("missing")).isNull();
  }

  @Test
  void getFirstReturnsFirstValueForExistingKey() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "first");
    map.add("key", "second");

    assertThat(map.getFirst("key")).isEqualTo("first");
  }

  @Test
  void keySetIterationPreservesOrder() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key1", "value1");
    map.add("key2", "value2");
    map.add("key3", "value3");

    assertThat(map.keySet())
            .containsExactly("key1", "key2", "key3");
  }

  @Test
  void entrySetReflectsMapContent() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key1", "value1");
    map.add("key2", "value2a");
    map.add("key2", "value2b");

    var entries = map.entrySet();
    assertThat(entries).hasSize(2);

    for (var entry : entries) {
      if (entry.getKey().equals("key1")) {
        assertThat(entry.getValue()).containsExactly("value1");
      }
      else if (entry.getKey().equals("key2")) {
        assertThat(entry.getValue()).containsExactly("value2a", "value2b");
      }
    }
  }

  @Test
  void sizeReflectsTotalNumberOfKeys() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key1", "value1");
    map.add("key1", "value2");
    map.add("key2", "value3");

    assertThat(map).hasSize(2);
  }

  @Test
  void containsValueChecksAllLists() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key1", "value1");
    map.add("key2", "value2");
    map.add("key2", "value3");

    assertThat(map.containsValue(List.of("value1"))).isTrue();
    assertThat(map.containsValue(List.of("value2", "value3"))).isTrue();
    assertThat(map.containsValue(List.of("missing"))).isFalse();
  }

  @Test
  void toStringGeneratesReadableRepresentation() {
    var map = new MappingMultiValueMap<String, String>();
    map.add("key", "value");

    assertThat(map.toString()).contains("key=[value]");
  }

}