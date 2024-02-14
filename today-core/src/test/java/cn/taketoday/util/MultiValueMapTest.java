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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author TODAY <br>
 * 2020-01-30 20:03
 */
public class MultiValueMapTest {

  private final MappingMultiValueMap<String, String> map = new MappingMultiValueMap<>();

  @Test
  public void add() {
    map.add("key", "value1");
    map.add("key", "value2");
    assertThat(map).hasSize(1);
    assertThat(map.get("key")).containsExactly("value1", "value2");
  }

  @Test
  public void addIfAbsentWhenAbsent() {
    map.addIfAbsent("key", "value1");
    assertThat(map.get("key")).containsExactly("value1");
  }

  @Test
  public void addIfAbsentWhenPresent() {
    map.add("key", "value1");
    map.addIfAbsent("key", "value2");
    assertThat(map.get("key")).containsExactly("value1");
  }

  @Test
  public void set() {
    map.set("key", "value1");
    map.set("key", "value2");
    assertThat(map.get("key")).containsExactly("value2");
  }

  @Test
  public void addAll() {
    map.add("key", "value1");
    map.addAll("key", Arrays.asList("value2", "value3"));
    assertThat(map).hasSize(1);
    assertThat(map.get("key")).containsExactly("value1", "value2", "value3");
  }

  @Test
  public void addAllWithEmptyList() {
    map.addAll("key", Collections.emptyList());
    assertThat(map).hasSize(1);
    assertThat(map.get("key")).isEmpty();
    assertThat(map.getFirst("key")).isNull();
  }

  @Test
  public void getFirst() {
    List<String> values = new ArrayList<>(2);
    values.add("value1");
    values.add("value2");
    map.put("key", values);
    assertThat(map.getFirst("key")).isEqualTo("value1");
    assertThat(map.getFirst("other")).isNull();
  }

  @Test
  public void getFirstWithEmptyList() {
    map.put("key", Collections.emptyList());
    assertThat(map.getFirst("key")).isNull();
    assertThat(map.getFirst("other")).isNull();
  }

  @Test
  public void toSingleValueMap() {
    List<String> values = new ArrayList<>(2);
    values.add("value1");
    values.add("value2");
    map.put("key", values);
    Map<String, String> singleValueMap = map.toSingleValueMap();
    assertThat(singleValueMap).hasSize(1);
    assertThat(singleValueMap.get("key")).isEqualTo("value1");
  }

  @Test
  public void toSingleValueMapWithEmptyList() {
    map.put("key", Collections.emptyList());
    Map<String, String> singleValueMap = map.toSingleValueMap();
    assertThat(singleValueMap).isEmpty();
    assertThat(singleValueMap.get("key")).isNull();
  }

  @Test
  public void equals() {
    map.set("key1", "value1");
    assertThat(map).isEqualTo(map);
    MultiValueMap<String, String> o1 = new MappingMultiValueMap<>();
    o1.set("key1", "value1");
    assertThat(o1).isEqualTo(map);
    assertThat(map).isEqualTo(o1);
    Map<String, List<String>> o2 = new HashMap<>();
    o2.put("key1", Collections.singletonList("value1"));
    assertThat(o2).isEqualTo(map);
    assertThat(map).isEqualTo(o2);
  }

  @Test
  public void toArrayMap() {
    map.set("key1", "value1");

    final Map<String, String[]> arrayMap = map.toArrayMap(String[]::new);
    System.out.println(arrayMap);

    assertThat(arrayMap).hasSize(1).containsKey("key1");

    String[] strings = arrayMap.get("key1");

    assertThat(strings)
            .hasSize(1)
            .isEqualTo(new String[] { "value1" });
    assertThat(strings[0])
            .isEqualTo("value1");
    Map<String, String[]> linkedArrayMap = new LinkedHashMap<>();

    map.copyToArrayMap(linkedArrayMap, String[]::new);

    strings = linkedArrayMap.get("key1");
    assertThat(strings)
            .hasSize(1)
            .isEqualTo(new String[] { "value1" });
    assertThat(strings[0])
            .isEqualTo("value1");
  }

}
