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

package infra.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/26 17:55
 */
class MultiValueMapTests {

  @ParameterizedMultiValueMapTest
  void add(MultiValueMap<String, String> map) {
    int initialSize = map.size();
    map.add("key", "value1");
    map.add("key", "value2");
    assertThat(map).hasSize(initialSize + 1);
    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @ParameterizedMultiValueMapTest
  void addIfAbsentWhenAbsent(MultiValueMap<String, String> map) {
    map.addIfAbsent("key", "value1");
    assertThat(map.get("key")).containsExactly("value1");
  }

  @ParameterizedMultiValueMapTest
  void addIfAbsentWhenPresent(MultiValueMap<String, String> map) {
    map.add("key", "value1");
    map.addIfAbsent("key", "value2");
    assertThat(map.get("key")).containsExactly("value1");
  }

  @ParameterizedMultiValueMapTest
  void setOrRemove(MultiValueMap<String, String> map) {
    map.setOrRemove("key", "value1");
    map.setOrRemove("key", "value2");
    assertThat(map.get("key")).containsExactly("value2");
  }

  @ParameterizedMultiValueMapTest
  void addAll(MultiValueMap<String, String> map) {
    int initialSize = map.size();
    map.add("key", "value1");
    map.addAll("key", Arrays.asList("value2", "value3"));
    assertThat(map).hasSize(initialSize + 1);
    assertThat(map.get("key")).containsExactly("value1", "value2", "value3");
  }

  @ParameterizedMultiValueMapTest
  void addAllWithEmptyList(MultiValueMap<String, String> map) {
    int initialSize = map.size();
    map.addAll("key", List.of());
    assertThat(map).hasSize(initialSize);
    assertThat(map.get("key")).isNull();
    assertThat(map.getFirst("key")).isNull();
  }

  @ParameterizedMultiValueMapTest
  void getFirst(MultiValueMap<String, String> map) {
    List<String> values = List.of("value1", "value2");
    map.put("key", values);
    assertThat(map.getFirst("key")).isEqualTo("value1");
    assertThat(map.getFirst("other")).isNull();
  }

  @ParameterizedMultiValueMapTest
  void toSingleValueMap(MultiValueMap<String, String> map) {
    int initialSize = map.size();
    List<String> values = List.of("value1", "value2");
    map.put("key", values);
    Map<String, String> singleValueMap = map.toSingleValueMap();
    assertThat(singleValueMap).hasSize(initialSize + 1);
    assertThat(singleValueMap.get("key")).isEqualTo("value1");
  }

  @ParameterizedMultiValueMapTest
  void toSingleValueMapWithEmptyList(MultiValueMap<String, String> map) {
    int initialSize = map.size();
    map.put("key", List.of());
    Map<String, String> singleValueMap = map.toSingleValueMap();
    assertThat(singleValueMap).hasSize(initialSize);
    assertThat(singleValueMap.get("key")).isNull();
  }

  @ParameterizedMultiValueMapTest
  void equalsOnExistingValues(MultiValueMap<String, String> map) {
    map.clear();
    map.setOrRemove("key1", "value1");
    assertThat(map).isEqualTo(map);
  }

  @ParameterizedMultiValueMapTest
  void equalsOnEmpty(MultiValueMap<String, String> map) {
    map.clear();
    map.setOrRemove("key1", "value1");
    MultiValueMap<String, String> map1 = new LinkedMultiValueMap<>();
    map1.setOrRemove("key1", "value1");
    assertThat(map1).isEqualTo(map);
    assertThat(map).isEqualTo(map1);
    Map<String, List<String>> map2 = Map.of("key1", List.of("value1"));
    assertThat(map2).isEqualTo(map);
    assertThat(map).isEqualTo(map2);
  }

  @Test
  void canNotChangeAnUnmodifiableMultiValueMap() {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    MultiValueMap<String, String> unmodifiableMap = map.asReadOnly();
    assertSoftly(softly -> {
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.add("key", "value"));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.addIfAbsent("key", "value"));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.addAll("key", exampleListOfValues()));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.addAll(exampleMultiValueMap()));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.setOrRemove("key", "value"));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.setAll(Map.of("key", exampleListOfValues())));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.put("key", exampleListOfValues()));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.putIfAbsent("key", exampleListOfValues()));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.putAll(exampleMultiValueMap()));
      softly.assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> unmodifiableMap.remove("key1"));
    });
  }

  @Test
  void emptyReturnsSameUnmodifiableInstance() {
    MultiValueMap<String, String> empty1 = MultiValueMap.empty();
    MultiValueMap<String, String> empty2 = MultiValueMap.empty();

    assertThat(empty1).isSameAs(empty2);
    assertThatThrownBy(() -> empty1.add("key", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void forAdaptionCreatesMultiValueMapFromExistingMap() {
    Map<String, List<String>> source = new HashMap<>();
    source.put("key", List.of("value"));

    MultiValueMap<String, String> adapted = MultiValueMap.forAdaption(source);
    assertThat(adapted.get("key")).containsExactly("value");
  }

  @Test
  void forSmartListAdaptionUsesSmartList() {
    MultiValueMap<String, String> map = MultiValueMap.forSmartListAdaption();
    map.add("key", "value");

    assertThat(map.get("key"))
            .isInstanceOf(SmartList.class)
            .containsExactly("value");
  }

  @Test
  void copyOfCreatesIndependentCopy() {
    Map<String, List<String>> source = new HashMap<>();
    source.put("key", new ArrayList<>(List.of("value")));

    MultiValueMap<String, String> copy = MultiValueMap.copyOf(source);
    source.get("key").add("another");

    assertThat(copy.get("key")).containsExactly("value", "another");
  }

  @Test
  void forLinkedHashMapCreatesEmptyMapWithDefaultCapacity() {
    MultiValueMap<String, String> map = MultiValueMap.forLinkedHashMap();
    assertThat(map).isEmpty();
    assertThat(map).isInstanceOf(LinkedMultiValueMap.class);
  }

  @Test
  void forLinkedHashMapWithSizeCreatesEmptyMapWithCapacity() {
    MultiValueMap<String, String> map = MultiValueMap.forLinkedHashMap(10);
    assertThat(map).isEmpty();
    assertThat(map).isInstanceOf(LinkedMultiValueMap.class);
  }

  @Test
  void copyOfWithMappingFunctionCreatesNewMapWithCustomLists() {
    Map<String, List<String>> source = new HashMap<>();
    source.put("key", List.of("value"));

    MultiValueMap<String, String> copy = MultiValueMap.copyOf(
            k -> new LinkedList<>(), source);

    assertThat(copy.get("key"))
            .isInstanceOf(LinkedList.class)
            .containsExactly("value");
  }

  @Test
  void addAllWithMapPreservesOrderOfValues() {
    Map<String, List<String>> source = new LinkedHashMap<>();
    source.put("key", List.of("value1", "value2"));

    MultiValueMap<String, String> map = MultiValueMap.forLinkedHashMap();
    map.addAll(source);

    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void copyToArrayMapConvertsListsToArrays() {
    MultiValueMap<String, String> source = MultiValueMap.forLinkedHashMap();
    source.add("key", "value1");
    source.add("key", "value2");

    Map<String, String[]> arrayMap = new HashMap<>();
    source.copyToArrayMap(arrayMap, String[]::new);

    assertThat(arrayMap.get("key")).containsExactly("value1", "value2");
  }

  @ParameterizedMultiValueMapTest
  void putIfAbsentWhenKeyExists(MultiValueMap<String, String> map) {
    map.put("key", List.of("value1"));
    List<String> previous = map.putIfAbsent("key", List.of("value2"));
    assertThat(previous).containsExactly("value1");
    assertThat(map.get("key")).containsExactly("value1");
  }

  @ParameterizedMultiValueMapTest
  void putIfAbsentWhenKeyDoesNotExist(MultiValueMap<String, String> map) {
    List<String> previous = map.putIfAbsent("key", List.of("value1"));
    assertThat(previous).isNull();
    assertThat(map.get("key")).containsExactly("value1");
  }

  @ParameterizedMultiValueMapTest
  void setOrRemoveWithArrayValuesReplacesExistingList(MultiValueMap<String, String> map) {
    map.add("key", "original");
    map.setOrRemove("key", new String[] { "value1", "value2" });
    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @ParameterizedMultiValueMapTest
  void toArrayMapConvertsToCorrectFormat(MultiValueMap<String, String> map) {
    map.add("key1", "value1");
    map.add("key1", "value2");
    map.add("key2", "value3");

    Map<String, String[]> arrayMap = map.toArrayMap(String[]::new);

    assertThat(arrayMap.get("key1")).containsExactly("value1", "value2");
    assertThat(arrayMap.get("key2")).containsExactly("value3");
  }

  @ParameterizedMultiValueMapTest
  void copyToArrayMapCopiesAllValues(MultiValueMap<String, String> map) {
    map.add("key1", "value1");
    map.add("key1", "value2");

    Map<String, String[]> target = new HashMap<>();
    map.copyToArrayMap(target, String[]::new);

    assertThat(target.get("key1")).containsExactly("value1", "value2");
  }

  @ParameterizedMultiValueMapTest
  void emptyMultiValueMapIsImmutable() {
    MultiValueMap<String, String> empty = MultiValueMap.empty();
    assertThatThrownBy(() -> empty.add("key", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedMultiValueMapTest
  void asWritableReturnsOriginalInstance(MultiValueMap<String, String> map) {
    MultiValueMap<String, String> writable = map.asWritable();
    assertThat(writable).isSameAs(map);
  }

  @ParameterizedMultiValueMapTest
  void addingEntryPairAddsKeyAndValue(MultiValueMap<String, String> map) {
    Entry<String, String> entry = Map.entry("key", "value");
    map.add(entry);
    assertThat(map.get("key")).containsExactly("value");
  }

  @ParameterizedMultiValueMapTest
  void addAllWithEntryPairAddsAllValues(MultiValueMap<String, String> map) {
    Entry<String, List<String>> entry = Map.entry("key", List.of("value1", "value2"));
    map.addAll(entry);
    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @ParameterizedMultiValueMapTest
  void addAllWithNullMapDoesNothing(MultiValueMap<String, String> map) {
    int initialSize = map.size();
    map.addAll((Map<String, List<String>>) null);
    assertThat(map).hasSize(initialSize);
  }

  @ParameterizedMultiValueMapTest
  void nullValueInListIsAllowed(MultiValueMap<String, String> map) {
    map.add("key", null);
    assertThat(map.get("key")).containsExactly((String) null);
  }

  @ParameterizedMultiValueMapTest
  void setOrRemoveWithCollectionPreservesOrder(MultiValueMap<String, String> map) {
    map.setOrRemove("key", Arrays.asList("value1", "value2", "value3"));
    assertThat(map.get("key")).containsExactly("value1", "value2", "value3");
  }

  @ParameterizedMultiValueMapTest
  void addingEntryWithNullKeyThrowsException(MultiValueMap<String, String> map) {
    assertThatThrownBy(() -> map.add(Map.entry(null, "value")))
            .isInstanceOf(NullPointerException.class);
  }

  @ParameterizedMultiValueMapTest
  void toArrayMapWithEmptyListsCreatesEmptyArrays(MultiValueMap<String, String> map) {
    map.add("key1", "");
    map.add("key2", null);

    Map<String, String[]> arrayMap = map.toArrayMap(String[]::new);

    assertThat(arrayMap.get("key1")).containsExactly("");
    assertThat(arrayMap.get("key2")).containsExactly((String) null);
  }

  @ParameterizedMultiValueMapTest
  void removeValueFromListRemovesOnlyThatValue(MultiValueMap<String, String> map) {
    map.addAll("key", Arrays.asList("value1", "value2", "value3"));
    map.get("key").remove("value2");

    assertThat(map.get("key")).containsExactly("value1", "value3");
  }

  @ParameterizedMultiValueMapTest
  void putAllFromEmptyMapDoesNothing(MultiValueMap<String, String> map) {
    int initialSize = map.size();
    map.putAll(new HashMap<>());
    assertThat(map).hasSize(initialSize);
  }

  @ParameterizedMultiValueMapTest
  void asReadOnlyPreventsMutationButAllowsReading(MultiValueMap<String, String> map) {
    map.add("key", "value");
    MultiValueMap<String, String> readOnly = map.asReadOnly();

    assertThat(readOnly.get("key")).containsExactly("value");
    assertThatThrownBy(() -> readOnly.add("key2", "value2"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedMultiValueMapTest
  void addingAllEntriesWithNullValuesIsAllowed(MultiValueMap<String, String> map) {
    Entry<String, List<String>> entry = Map.entry("key", Arrays.asList(null, "value", null));
    map.addAll(entry);
    assertThat(map.get("key")).containsExactly(null, "value", null);
  }

  @ParameterizedMultiValueMapTest
  void addingAllWithEmptyEnumerationDoesNotModifyMap(MultiValueMap<String, String> map) {
    map.add("key", "value");
    map.addAll("key", new Vector<String>().elements());
    assertThat(map.get("key")).containsExactly("value");
  }

  @ParameterizedMultiValueMapTest
  void setOrRemoveWithNullArrayRemovesKey(MultiValueMap<String, String> map) {
    map.add("key", "value");
    map.setOrRemove("key", (String[]) null);
    assertThat(map).doesNotContainKey("key");
  }

  @ParameterizedMultiValueMapTest
  void toArrayMapWithNullValueReturnsEmptyArray(MultiValueMap<String, String> map) {
    map.add("key", null);
    assertThat(map.toArrayMap(String[]::new)).containsEntry("key", new String[] { null });
  }

  @ParameterizedMultiValueMapTest
  void copyToArrayMapWithEmptyMapCreatesNoEntries(MultiValueMap<String, String> map) {
    Map<String, String[]> target = new HashMap<>();
    map.copyToArrayMap(target, String[]::new);
    assertThat(target).hasSize(map.size());
  }

  //

  @ParameterizedMultiValueMapTest
  void setAllWithNullMapDoesNothing(MultiValueMap<String, String> map) {
    map.add("key", "value");
    map.setAll(null);
    assertThat(map.get("key")).containsExactly("value");
  }

  @ParameterizedMultiValueMapTest
  void setAllReplacesExistingValues(MultiValueMap<String, String> map) {
    map.add("key", "original");
    map.setAll(Map.of("key", List.of("new")));
    assertThat(map.get("key")).containsExactly("new");
  }

  private static List<String> exampleListOfValues() {
    return List.of("value1", "value2");
  }

  private static Map<String, String> exampleHashMap() {
    return Map.of("key2", "key2.value1");
  }

  private static MultiValueMap<String, String> exampleMultiValueMap() {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.put("key1", Arrays.asList("key1.value1", "key1.value2"));
    return map;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("mapsUnderTest")
  @interface ParameterizedMultiValueMapTest {
  }

  static Stream<Arguments> mapsUnderTest() {
    return Stream.of(
            arguments(named("new LinkedMultiValueMap<>()", MultiValueMap.forLinkedHashMap())),
            arguments(named("new LinkedMultiValueMap<>(new HashMap<>())", new LinkedMultiValueMap<>(new HashMap<>()))),
            arguments(named("new LinkedMultiValueMap<>(new LinkedHashMap<>())", new LinkedMultiValueMap<>(new LinkedHashMap<>()))),
            arguments(named("new LinkedMultiValueMap<>(Map.of(...))", new LinkedMultiValueMap<>(Map.of("existingkey", List.of("existingvalue1", "existingvalue2"))))),
            arguments(named("MultiValueMap.forAdaption(HashMap)", MultiValueMap.forAdaption(new HashMap<>()))),
            arguments(named("CollectionUtils.forAdaption(LinkedHashMap)", MultiValueMap.forAdaption(new LinkedHashMap<>()))),
            arguments(named("MultiValueMap.forSmartListAdaption()", MultiValueMap.forSmartListAdaption())),
            arguments(named("MultiValueMap.forAdaption(ArrayList)", MultiValueMap.forAdaption(k -> new ArrayList<>()))),
            arguments(named("MultiValueMap.forAdaption(SmartList)", MultiValueMap.forAdaption(k -> new SmartList<>()))),
            arguments(named("MultiValueMap.forAdaption(HashMap, SmartList)", MultiValueMap.forAdaption(new HashMap<>(), k -> new SmartList<>()))),
            arguments(named("MultiValueMap.forAdaption(LinkedHashMap, SmartList)", MultiValueMap.forAdaption(new LinkedHashMap<>(), k -> new SmartList<>()))),
            arguments(named("MultiValueMap.forSmartListAdaption(HashMap)", MultiValueMap.forSmartListAdaption(new HashMap<>())))
    );
  }

}