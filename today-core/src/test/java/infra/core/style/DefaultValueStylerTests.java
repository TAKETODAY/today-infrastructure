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

package infra.core.style;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:57
 */
class DefaultValueStylerTests {

  @Test
  void styleNullValue() {
    DefaultValueStyler styler = new DefaultValueStyler();
    String result = styler.style(null);
    assertThat(result).isEqualTo("[null]");
  }

  @Test
  void styleStringValue() {
    DefaultValueStyler styler = new DefaultValueStyler();
    String result = styler.style("test");
    assertThat(result).isEqualTo("'test'");
  }

  @Test
  void styleClassValue() {
    DefaultValueStyler styler = new DefaultValueStyler();
    String result = styler.style(String.class);
    assertThat(result).isEqualTo("String");
  }

  @Test
  void styleMethodValue() throws Exception {
    DefaultValueStyler styler = new DefaultValueStyler();
    Method method = String.class.getMethod("toString");
    String result = styler.style(method);
    assertThat(result).isEqualTo("toString@String");
  }

  @Test
  void styleEmptyMap() {
    DefaultValueStyler styler = new DefaultValueStyler();
    Map<String, String> map = Map.of();
    String result = styler.style(map);
    assertThat(result).isEqualTo("map[[empty]]");
  }

  @Test
  void styleMapWithEntries() {
    DefaultValueStyler styler = new DefaultValueStyler();
    Map<String, String> map = Map.of("key1", "value1", "key2", "value2");
    String result = styler.style(map);
    assertThat(result).contains("map[");
    assertThat(result).contains("'key1' -> 'value1'");
    assertThat(result).contains("'key2' -> 'value2'");
  }

  @Test
  void styleMapEntry() {
    DefaultValueStyler styler = new DefaultValueStyler();
    Map.Entry<String, String> entry = Map.entry("key", "value");
    String result = styler.style(entry);
    assertThat(result).isEqualTo("'key' -> 'value'");
  }

  @Test
  void styleEmptyList() {
    DefaultValueStyler styler = new DefaultValueStyler();
    List<String> list = List.of();
    String result = styler.style(list);
    assertThat(result).isEqualTo("list[[empty]]");
  }

  @Test
  void styleListWithElements() {
    DefaultValueStyler styler = new DefaultValueStyler();
    List<String> list = List.of("a", "b", "c");
    String result = styler.style(list);
    assertThat(result).isEqualTo("list['a', 'b', 'c']");
  }

  @Test
  void styleEmptySet() {
    DefaultValueStyler styler = new DefaultValueStyler();
    Set<String> set = Set.of();
    String result = styler.style(set);
    assertThat(result).isEqualTo("set[[empty]]");
  }

  @Test
  void styleSetWithElements() {
    DefaultValueStyler styler = new DefaultValueStyler();
    Set<String> set = Set.of("a", "b", "c");
    String result = styler.style(set);
    assertThat(result).isEqualTo("set['a', 'b', 'c']");
  }

  @Test
  void styleEmptyArray() {
    DefaultValueStyler styler = new DefaultValueStyler();
    String[] array = new String[0];
    String result = styler.style(array);
    assertThat(result).isEqualTo("array<String>[[empty]]");
  }

  @Test
  void styleArrayWithElements() {
    DefaultValueStyler styler = new DefaultValueStyler();
    String[] array = { "a", "b", "c" };
    String result = styler.style(array);
    assertThat(result).isEqualTo("array<String>['a', 'b', 'c']");
  }

  @Test
  void styleGenericObject() {
    DefaultValueStyler styler = new DefaultValueStyler();
    Object obj = new Object() {
      @Override
      public String toString() {
        return "customObject";
      }
    };
    String result = styler.style(obj);
    assertThat(result).isEqualTo("customObject");
  }

  @Test
  void styleInteger() {
    DefaultValueStyler styler = new DefaultValueStyler();
    String result = styler.style(42);
    assertThat(result).isEqualTo("42");
  }

  @Test
  void styleBoolean() {
    DefaultValueStyler styler = new DefaultValueStyler();
    String result = styler.style(true);
    assertThat(result).isEqualTo("true");
  }

  

}