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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/26 11:34
 */
class LinkedMultiValueMapTests {

  private final LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();

  @Test
  void add() {
    map.add("key", "value1");
    map.add("key", "value2");
    assertThat(map).hasSize(1);
    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  void addIfAbsentWhenAbsent() {
    map.addIfAbsent("key", "value1");
    assertThat(map.get("key")).containsExactly("value1");
  }

  @Test
  void addIfAbsentWhenPresent() {
    map.add("key", "value1");
    map.addIfAbsent("key", "value2");
    assertThat(map.get("key")).containsExactly("value1");
  }

  @Test
  void setOrRemove() {
    map.setOrRemove("key", "value1");
    map.setOrRemove("key", "value2");
    assertThat(map.get("key")).containsExactly("value2");
  }

  @Test
  void addAll() {
    map.add("key", "value1");
    map.addAll("key", Arrays.asList("value2", "value3"));
    assertThat(map).hasSize(1);
    assertThat(map.get("key")).containsExactly("value1", "value2", "value3");
  }

  @Test
  void addAllWithEmptyList() {
    map.addAll("key", Collections.emptyList());
    assertThat(map).hasSize(1);
    assertThat(map.get("key")).isEmpty();
    assertThat(map.getFirst("key")).isNull();
  }

  @Test
  void getFirst() {
    List<String> values = new ArrayList<>(2);
    values.add("value1");
    values.add("value2");
    map.put("key", values);
    assertThat(map.getFirst("key")).isEqualTo("value1");
    assertThat(map.getFirst("other")).isNull();
  }

  @Test
  void getFirstWithEmptyList() {
    map.put("key", Collections.emptyList());
    assertThat(map.getFirst("key")).isNull();
    assertThat(map.getFirst("other")).isNull();
  }

  @Test
  void toSingleValueMap() {
    List<String> values = new ArrayList<>(2);
    values.add("value1");
    values.add("value2");
    map.put("key", values);
    Map<String, String> singleValueMap = map.toSingleValueMap();
    assertThat(singleValueMap).hasSize(1);
    assertThat(singleValueMap.get("key")).isEqualTo("value1");
  }

  @Test
  void toSingleValueMapWithEmptyList() {
    map.put("key", Collections.emptyList());
    Map<String, String> singleValueMap = map.toSingleValueMap();
    assertThat(singleValueMap).isEmpty();
    assertThat(singleValueMap.get("key")).isNull();
  }

  @Test
  void equals() {
    map.setOrRemove("key1", "value1");
    assertThat(map).isEqualTo(map);
    MultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
    o1.setOrRemove("key1", "value1");
    assertThat(o1).isEqualTo(map);
    assertThat(map).isEqualTo(o1);
    Map<String, List<String>> o2 = new HashMap<>();
    o2.put("key1", Collections.singletonList("value1"));
    assertThat(o2).isEqualTo(map);
    assertThat(map).isEqualTo(o2);
  }

  @Test
  void deepCopy() {
    map.setOrRemove("key1", "value1");
    assertThat(map).isEqualTo(map);
    LinkedMultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
    o1.setOrRemove("key1", "value1");

    LinkedMultiValueMap<String, String> deepedCopy = o1.deepCopy();
    assertThat(deepedCopy).isEqualTo(o1);
  }

}