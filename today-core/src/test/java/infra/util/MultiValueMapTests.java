/*
 * Copyright 2017 - 2024 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
    assertThat(map).hasSize(initialSize + 1);
    assertThat(map.get("key")).isEmpty();
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