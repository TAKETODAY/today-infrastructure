/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.env;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Spliterators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/24 19:08
 */
class MapPropertyResolverTests {

  @Test
  void nullMap() {
    doTestNullMap(new MapPropertyResolver(null));
    doTestNullMap(new PropertiesPropertyResolver(null));
  }

  @Test
  void nonnullMap() {
    Map<String, String> map = Map.of("k1", "v1", "k2", "v2");
    Properties keyValues = new Properties();
    keyValues.putAll(map);
    doTestNonnullMap(new MapPropertyResolver(map));
    doTestNonnullMap(new PropertiesPropertyResolver(keyValues));
  }

  @Test
  void getProperty() {
    Map<String, String> map = Map.of("true", "true", "false", "false");
    Properties keyValues = new Properties();
    keyValues.putAll(map);

    getProperty(new MapPropertyResolver(keyValues));
    getProperty(new PropertiesPropertyResolver(keyValues));
  }

  void doTestNullMap(MapPropertyResolver resolver) {
    assertThat(resolver.getProperty("")).isNull();
    resolver.forEach(s -> fail("no elements"));
    assertThat(resolver.iterator()).isSameAs(Collections.emptyIterator());
    assertThat(resolver.spliterator()).isEqualTo(Spliterators.emptySpliterator());
  }

  void getProperty(PropertyResolver resolver) {
    assertThat(resolver.getFlag("")).isFalse();
    assertThat(resolver.getFlag("false")).isFalse();
    assertThat(resolver.getFlag("true")).isTrue();

    assertThat(resolver.getFlag("", true)).isTrue();
    assertThat(resolver.getFlag("false", true)).isFalse();
    assertThat(resolver.getFlag("true", false)).isTrue();

    assertThat(resolver.getProperty("true", boolean.class)).isTrue();
    assertThat(resolver.getProperty("false", boolean.class)).isFalse();

    assertThat(resolver.getProperty("true", Boolean.class, Boolean.FALSE)).isTrue();
    assertThat(resolver.getProperty("false", Boolean.class, Boolean.TRUE)).isFalse();
    assertThat(resolver.getProperty("", Boolean.class, Boolean.FALSE)).isFalse();
    assertThat(resolver.getProperty("", boolean.class, false)).isFalse();

    assertThat(resolver.getRequiredProperty("true", boolean.class)).isTrue();
    assertThat(resolver.getRequiredProperty("false", boolean.class)).isFalse();

    assertThatThrownBy(() -> resolver.getRequiredProperty("v1")).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> resolver.getRequiredProperty("v1", String.class)).isInstanceOf(IllegalStateException.class);

  }

  void doTestNonnullMap(MapPropertyResolver resolver) {
    assertThat(resolver.getProperty("")).isNull();
    resolver.forEach(s -> assertThat(s).startsWith("k"));
    assertThat(resolver.getProperty("k1")).isEqualTo("v1");
    assertThat(resolver.getProperty("k2")).isEqualTo("v2");
    assertThat(resolver.spliterator()).isNotEqualTo(Spliterators.emptySpliterator());
  }

}