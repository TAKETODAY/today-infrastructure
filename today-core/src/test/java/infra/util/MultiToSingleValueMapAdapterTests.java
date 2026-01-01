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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/1 11:51
 */
class MultiToSingleValueMapAdapterTests {

  private LinkedMultiValueMap<String, String> delegate;

  private Map<String, String> adapter;

  @BeforeEach
  void setUp() {
    this.delegate = new LinkedMultiValueMap<>();
    this.delegate.add("foo", "bar");
    this.delegate.add("foo", "baz");
    this.delegate.add("qux", "quux");

    this.adapter = new MultiToSingleValueMapAdapter<>(this.delegate);
  }

  @Test
  void size() {
    assertThat(this.adapter.size()).isEqualTo(this.delegate.size()).isEqualTo(2);
  }

  @Test
  void isEmpty() {
    assertThat(this.adapter.isEmpty()).isFalse();

    this.adapter = new MultiToSingleValueMapAdapter<>(new LinkedMultiValueMap<>());
    assertThat(this.adapter.isEmpty()).isTrue();
  }

  @Test
  void containsKey() {
    assertThat(this.adapter.containsKey("foo")).isTrue();
    assertThat(this.adapter.containsKey("qux")).isTrue();
    assertThat(this.adapter.containsKey("corge")).isFalse();
  }

  @Test
  void containsValue() {
    assertThat(this.adapter.containsValue("bar")).isTrue();
    assertThat(this.adapter.containsValue("quux")).isTrue();
    assertThat(this.adapter.containsValue("corge")).isFalse();
  }

  @Test
  void get() {
    assertThat(this.adapter.get("foo")).isEqualTo("bar");
    assertThat(this.adapter.get("qux")).isEqualTo("quux");
    assertThat(this.adapter.get("corge")).isNull();
  }

  @Test
  void put() {
    String result = this.adapter.put("foo", "bar");
    assertThat(result).isEqualTo("bar");
    assertThat(this.delegate.get("foo")).containsExactly("bar");
  }

  @Test
  void remove() {
    this.adapter.remove("foo");
    assertThat(this.adapter.containsKey("foo")).isFalse();
    assertThat(this.delegate.containsKey("foo")).isFalse();
  }

  @Test
  void putAll() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    map.put("foo", "bar");
    map.put("qux", null);
    this.adapter.putAll(map);
    assertThat(this.adapter.get("foo")).isEqualTo("bar");
    assertThat(this.adapter.get("qux")).isNull();

    assertThat(this.delegate.get("foo")).isEqualTo(List.of("bar"));
    assertThat(this.adapter.get("qux")).isNull();
  }

  @Test
  void clear() {
    this.adapter.clear();
    assertThat(this.adapter).isEmpty();
    assertThat(this.delegate).isEmpty();
  }

  @Test
  void keySet() {
    assertThat(this.adapter.keySet()).containsExactly("foo", "qux");
  }

  @Test
  void values() {
    assertThat(this.adapter.values()).containsExactly("bar", "quux");
  }

  @Test
  void entrySet() {
    assertThat(this.adapter.entrySet()).containsExactly(entry("foo", "bar"), entry("qux", "quux"));
  }
}