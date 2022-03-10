/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LinkedCaseInsensitiveMap}.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author TODAY <br>
 * 2020-01-18 13:36
 */
public class LinkedCaseInsensitiveMapTest {

  private final LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();

  @Test
  public void putAndGet() {
    assertThat(map.put("key", "value1")).isNull();
    assertThat(map.put("key", "value2")).isEqualTo("value1");
    assertThat(map.put("key", "value3")).isEqualTo("value2");
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get("key")).isEqualTo("value3");
    assertThat(map.get("KEY")).isEqualTo("value3");
    assertThat(map.get("Key")).isEqualTo("value3");
    assertThat(map.containsKey("key")).isTrue();
    assertThat(map.containsKey("KEY")).isTrue();
    assertThat(map.containsKey("Key")).isTrue();
    assertThat(map.keySet().contains("key")).isTrue();
    assertThat(map.keySet().contains("KEY")).isTrue();
    assertThat(map.keySet().contains("Key")).isTrue();
  }

  @Test
  public void putWithOverlappingKeys() {
    assertThat(map.put("key", "value1")).isNull();
    assertThat(map.put("KEY", "value2")).isEqualTo("value1");
    assertThat(map.put("Key", "value3")).isEqualTo("value2");
    assertThat(map.size()).isEqualTo(1);
    assertThat(map.get("key")).isEqualTo("value3");
    assertThat(map.get("KEY")).isEqualTo("value3");
    assertThat(map.get("Key")).isEqualTo("value3");
    assertThat(map.containsKey("key")).isTrue();
    assertThat(map.containsKey("KEY")).isTrue();
    assertThat(map.containsKey("Key")).isTrue();
    assertThat(map.keySet().contains("key")).isTrue();
    assertThat(map.keySet().contains("KEY")).isTrue();
    assertThat(map.keySet().contains("Key")).isTrue();
  }

  @Test
  public void getOrDefault() {
    assertThat(map.put("key", "value1")).isNull();
    assertThat(map.put("KEY", "value2")).isEqualTo("value1");
    assertThat(map.put("Key", "value3")).isEqualTo("value2");
    assertThat(map.getOrDefault("key", "N")).isEqualTo("value3");
    assertThat(map.getOrDefault("KEY", "N")).isEqualTo("value3");
    assertThat(map.getOrDefault("Key", "N")).isEqualTo("value3");
    assertThat(map.getOrDefault("keeeey", "N")).isEqualTo("N");
    assertThat(map.getOrDefault(new Object(), "N")).isEqualTo("N");
  }

  @Test
  public void getOrDefaultWithNullValue() {
    assertThat(map.put("key", null)).isNull();
    assertThat(map.put("KEY", null)).isNull();
    assertThat(map.put("Key", null)).isNull();
    assertThat(map.getOrDefault("key", "N")).isNull();
    assertThat(map.getOrDefault("KEY", "N")).isNull();
    assertThat(map.getOrDefault("Key", "N")).isNull();
    assertThat(map.getOrDefault("keeeey", "N")).isEqualTo("N");
    assertThat(map.getOrDefault(new Object(), "N")).isEqualTo("N");
  }

  @Test
  public void computeIfAbsentWithExistingValue() {
    assertThat(map.putIfAbsent("key", "value1")).isNull();
    assertThat(map.putIfAbsent("KEY", "value2")).isEqualTo("value1");
    assertThat(map.put("Key", "value3")).isEqualTo("value1");
    assertThat(map.computeIfAbsent("key", key2 -> "value1")).isEqualTo("value3");
    assertThat(map.computeIfAbsent("KEY", key1 -> "value2")).isEqualTo("value3");
    assertThat(map.computeIfAbsent("Key", key -> "value3")).isEqualTo("value3");
  }

  @Test
  public void computeIfAbsentWithComputedValue() {
    assertThat(map.computeIfAbsent("key", key2 -> "value1")).isEqualTo("value1");
    assertThat(map.computeIfAbsent("KEY", key1 -> "value2")).isEqualTo("value1");
    assertThat(map.computeIfAbsent("Key", key -> "value3")).isEqualTo("value1");
  }

  @Test
  public void mapClone() {
    assertThat(map.put("key", "value1")).isNull();
    LinkedCaseInsensitiveMap<String> copy = map.clone();

    assertThat(copy.getLocale()).isEqualTo(map.getLocale());
    assertThat(map.get("key")).isEqualTo("value1");
    assertThat(map.get("KEY")).isEqualTo("value1");
    assertThat(map.get("Key")).isEqualTo("value1");
    assertThat(copy.get("key")).isEqualTo("value1");
    assertThat(copy.get("KEY")).isEqualTo("value1");
    assertThat(copy.get("Key")).isEqualTo("value1");

    copy.put("Key", "value2");
    assertThat(map.size()).isEqualTo(1);
    assertThat(copy.size()).isEqualTo(1);
    assertThat(map.get("key")).isEqualTo("value1");
    assertThat(map.get("KEY")).isEqualTo("value1");
    assertThat(map.get("Key")).isEqualTo("value1");
    assertThat(copy.get("key")).isEqualTo("value2");
    assertThat(copy.get("KEY")).isEqualTo("value2");
    assertThat(copy.get("Key")).isEqualTo("value2");
  }

  @Test
  public void clearFromKeySet() {
    map.put("key", "value");
    map.keySet().clear();
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void removeFromKeySet() {
    map.put("key", "value");
    map.keySet().remove("key");
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void removeFromKeySetViaIterator() {
    map.put("key", "value");
    nextAndRemove(map.keySet().iterator());
    assertThat(map.size()).isEqualTo(0);
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void clearFromValues() {
    map.put("key", "value");
    map.values().clear();
    assertThat(map.size()).isEqualTo(0);
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void removeFromValues() {
    map.put("key", "value");
    map.values().remove("value");
    assertThat(map.size()).isEqualTo(0);
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void removeFromValuesViaIterator() {
    map.put("key", "value");
    nextAndRemove(map.values().iterator());
    assertThat(map.size()).isEqualTo(0);
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void clearFromEntrySet() {
    map.put("key", "value");
    map.entrySet().clear();
    assertThat(map.size()).isEqualTo(0);
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void removeFromEntrySet() {
    map.put("key", "value");
    map.entrySet().remove(map.entrySet().iterator().next());
    assertThat(map.size()).isEqualTo(0);
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  @Test
  public void removeFromEntrySetViaIterator() {
    map.put("key", "value");
    nextAndRemove(map.entrySet().iterator());
    assertThat(map.size()).isEqualTo(0);
    map.computeIfAbsent("key", k -> "newvalue");
    assertThat(map.get("key")).isEqualTo("newvalue");
  }

  private void nextAndRemove(Iterator<?> iterator) {
    iterator.next();
    iterator.remove();
  }

}
