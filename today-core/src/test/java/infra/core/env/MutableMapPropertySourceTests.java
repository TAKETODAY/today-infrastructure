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

package infra.core.env;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/21 16:49
 */
class MutableMapPropertySourceTests {

  @Test
  void setProperty() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    source.setProperty("name", "value");
    assertThat(source.containsProperty("name")).isTrue();
    assertThat(source.getProperty("name")).isEqualTo("value");
  }

  @Test
  void withProperty() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    source.withProperty("name", "value")
            .withProperty("name1", "value1");
    assertThat(source.containsProperty("name")).isTrue();
    assertThat(source.getProperty("name")).isEqualTo("value");

    assertThat(source.containsProperty("name1")).isTrue();
    assertThat(source.getProperty("name1")).isEqualTo("value1");
  }

  @Test
  void constructorWithDefaultNameCreatesInstance() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    assertThat(source.getName()).isEqualTo(MutableMapPropertySource.MUTABLE_MAP_PROPERTY_SOURCE_NAME);
    assertThat(source.getProperty("nonexistent")).isNull();
  }

  @Test
  void constructorWithNameCreatesInstance() {
    String name = "test-source";
    MutableMapPropertySource source = new MutableMapPropertySource(name);
    assertThat(source.getName()).isEqualTo(name);
    assertThat(source.getProperty("nonexistent")).isNull();
  }

  @Test
  void constructorWithNameAndMapCreatesInstance() {
    String name = "test-source";
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key", "value");
    MutableMapPropertySource source = new MutableMapPropertySource(name, map);
    assertThat(source.getName()).isEqualTo(name);
    assertThat(source.getProperty("key")).isEqualTo("value");
  }

  @Test
  void constructorWithMapCreatesInstance() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key", "value");
    MutableMapPropertySource source = new MutableMapPropertySource(map);
    assertThat(source.getName()).isEqualTo(MutableMapPropertySource.MUTABLE_MAP_PROPERTY_SOURCE_NAME);
    assertThat(source.getProperty("key")).isEqualTo("value");
  }

  @Test
  void setPropertyAddsPropertyToSource() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    source.setProperty("testKey", "testValue");
    assertThat(source.containsProperty("testKey")).isTrue();
    assertThat(source.getProperty("testKey")).isEqualTo("testValue");
  }

  @Test
  void setPropertyOverwritesExistingProperty() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    source.setProperty("key", "originalValue");
    source.setProperty("key", "newValue");
    assertThat(source.getProperty("key")).isEqualTo("newValue");
  }

  @Test
  void withPropertyReturnsSameInstance() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    MutableMapPropertySource returned = source.withProperty("key", "value");
    assertThat(returned).isSameAs(source);
  }

  @Test
  void withPropertyAddsPropertyToSource() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    source.withProperty("testKey", "testValue");
    assertThat(source.containsProperty("testKey")).isTrue();
    assertThat(source.getProperty("testKey")).isEqualTo("testValue");
  }

  @Test
  void withPropertySupportsMethodChaining() {
    MutableMapPropertySource source = new MutableMapPropertySource()
            .withProperty("key1", "value1")
            .withProperty("key2", "value2");

    assertThat(source.containsProperty("key1")).isTrue();
    assertThat(source.getProperty("key1")).isEqualTo("value1");
    assertThat(source.containsProperty("key2")).isTrue();
    assertThat(source.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  void containsPropertyReturnsFalseForNonExistentProperty() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    assertThat(source.containsProperty("nonexistent")).isFalse();
  }

  @Test
  void getPropertyReturnsNullForNonExistentProperty() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    assertThat(source.getProperty("nonexistent")).isNull();
  }

  @Test
  void propertySourceMaintainsInsertionOrder() {
    MutableMapPropertySource source = new MutableMapPropertySource();
    source.setProperty("first", "1");
    source.setProperty("second", "2");
    source.setProperty("third", "3");

    // LinkedHashMap maintains insertion order
    Object[] keys = source.getSource().keySet().toArray();
    assertThat(keys[0]).isEqualTo("first");
    assertThat(keys[1]).isEqualTo("second");
    assertThat(keys[2]).isEqualTo("third");
  }

}