/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MapConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class MapConfigurationPropertySourceTests {

  @Test
  void createWhenMapIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new MapConfigurationPropertySource(null))
            .withMessageContaining("Map must not be null");
  }

  @Test
  void createWhenMapHasEntriesShouldAdaptMap() {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put("foo.BAR", "spring");
    map.put(ConfigurationPropertyName.of("foo.baz"), "boot");
    MapConfigurationPropertySource source = new MapConfigurationPropertySource(map);
    assertThat(getValue(source, "foo.bar")).isEqualTo("spring");
    assertThat(getValue(source, "foo.baz")).isEqualTo("boot");
  }

  @Test
  void putAllWhenMapIsNullShouldThrowException() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    assertThatIllegalArgumentException().isThrownBy(() -> source.putAll(null))
            .withMessageContaining("Map must not be null");
  }

  @Test
  void putAllShouldPutEntries() {
    Map<Object, Object> map = new LinkedHashMap<>();
    map.put("foo.BAR", "spring");
    map.put("foo.baz", "boot");
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.putAll(map);
    assertThat(getValue(source, "foo.bar")).isEqualTo("spring");
    assertThat(getValue(source, "foo.baz")).isEqualTo("boot");
  }

  @Test
  void putShouldPutEntry() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("foo.bar", "baz");
    assertThat(getValue(source, "foo.bar")).isEqualTo("baz");
  }

  @Test
  void getConfigurationPropertyShouldGetFromMemory() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("foo.bar", "baz");
    assertThat(getValue(source, "foo.bar")).isEqualTo("baz");
    source.put("foo.bar", "big");
    assertThat(getValue(source, "foo.bar")).isEqualTo("big");
  }

  @Test
  void iteratorShouldGetFromMemory() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("foo.BAR", "spring");
    source.put("foo.baz", "boot");
    assertThat(source.iterator()).toIterable().containsExactly(ConfigurationPropertyName.of("foo.bar"),
            ConfigurationPropertyName.of("foo.baz"));
  }

  @Test
  void streamShouldGetFromMemory() {
    MapConfigurationPropertySource source = new MapConfigurationPropertySource();
    source.put("foo.BAR", "spring");
    source.put("foo.baz", "boot");
    assertThat(source.stream()).containsExactly(ConfigurationPropertyName.of("foo.bar"),
            ConfigurationPropertyName.of("foo.baz"));

  }

  private Object getValue(ConfigurationPropertySource source, String name) {
    ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
    return (property != null) ? property.getValue() : null;
  }

}
