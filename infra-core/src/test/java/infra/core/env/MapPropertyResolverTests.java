/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.core.env;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Spliterator;
import java.util.Spliterators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

  @Test
  void constructorWithNullMap() {
    MapPropertyResolver resolver = new MapPropertyResolver(null);
    assertThat(resolver).isNotNull();
    assertThat(resolver.containsProperty("any")).isFalse();
  }

  @Test
  void containsPropertyWithNullMapReturnsFalse() {
    MapPropertyResolver resolver = new MapPropertyResolver(null);
    assertThat(resolver.containsProperty("key")).isFalse();
  }

  @Test
  void containsPropertyWithEmptyMapReturnsFalse() {
    MapPropertyResolver resolver = new MapPropertyResolver(Collections.emptyMap());
    assertThat(resolver.containsProperty("key")).isFalse();
  }

  @Test
  void containsPropertyWithExistingKeyReturnsTrue() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.containsProperty("key")).isTrue();
  }

  @Test
  void containsPropertyWithNonExistingKeyReturnsFalse() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.containsProperty("nonexistent")).isFalse();
  }

  @Test
  void getPropertyReturnsNullWhenMapIsNull() {
    MapPropertyResolver resolver = new MapPropertyResolver(null);
    assertThat(resolver.getProperty("key")).isNull();
  }

  @Test
  void getPropertyReturnsNullWhenKeyNotPresent() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.getProperty("nonexistent")).isNull();
  }

  @Test
  void getPropertyReturnsValueWhenKeyPresent() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.getProperty("key")).isEqualTo("value");
  }

  @Test
  void getPropertyWithTargetTypeConvertsValue() {
    Map<String, String> map = Map.of("number", "42");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.getProperty("number", Integer.class)).isEqualTo(42);
  }

  @Test
  void getPropertyWithDefaultValueReturnsDefaultWhenKeyNotPresent() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.getProperty("nonexistent", "default")).isEqualTo("default");
  }

  @Test
  void getPropertyWithTargetTypeAndDefaultValueReturnsDefaultWhenKeyNotPresent() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.getProperty("nonexistent", Integer.class, 42)).isEqualTo(42);
  }

  @Test
  void iteratorReturnsEmptyIteratorWhenMapIsNull() {
    MapPropertyResolver resolver = new MapPropertyResolver(null);
    assertThat(resolver.iterator().hasNext()).isFalse();
  }

  @Test
  void iteratorReturnsKeysWhenMapIsPresent() {
    Map<String, String> map = Map.of("key1", "value1", "key2", "value2");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    Iterator<String> iterator = resolver.iterator();

    assertThat(iterator.hasNext()).isTrue();
    String key1 = iterator.next();
    assertThat(key1).isIn("key1", "key2");

    assertThat(iterator.hasNext()).isTrue();
    String key2 = iterator.next();
    assertThat(key2).isIn("key1", "key2");
    assertThat(key2).isNotEqualTo(key1);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void forEachExecutesActionForAllKeys() {
    Map<String, String> map = Map.of("key1", "value1", "key2", "value2");
    MapPropertyResolver resolver = new MapPropertyResolver(map);

    java.util.List<String> collectedKeys = new java.util.ArrayList<>();
    resolver.forEach(collectedKeys::add);

    assertThat(collectedKeys).containsExactlyInAnyOrder("key1", "key2");
  }

  @Test
  void forEachDoesNotExecuteWhenMapIsNull() {
    MapPropertyResolver resolver = new MapPropertyResolver(null);
    assertThatCode(() -> resolver.forEach(s -> {
      throw new RuntimeException("Should not be called");
    })).doesNotThrowAnyException();
  }

  @Test
  void spliteratorReturnsEmptySpliteratorWhenMapIsNull() {
    MapPropertyResolver resolver = new MapPropertyResolver(null);
    Spliterator<String> spliterator = resolver.spliterator();
    assertThat(spliterator.estimateSize()).isEqualTo(0);
  }

  @Test
  void spliteratorReturnsKeysWhenMapIsPresent() {
    Map<String, String> map = Map.of("key1", "value1", "key2", "value2");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    Spliterator<String> spliterator = resolver.spliterator();

    assertThat(spliterator.estimateSize()).isEqualTo(2);
    assertThat(spliterator.hasCharacteristics(Spliterator.SIZED)).isTrue();
  }

  @Test
  void getPropertyWithNestedPlaceholdersAndResolveFlag() {
    Map<String, String> map = Map.of("prop1", "value1", "prop2", "${prop1}");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    String value = resolver.getProperty("prop2", String.class, true);
    assertThat(value).isEqualTo("value1");
  }

  @Test
  void getPropertyWithNestedPlaceholdersAndDoNotResolveFlag() {
    Map<String, String> map = Map.of("prop1", "value1", "prop2", "${prop1}");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    String value = resolver.getProperty("prop2", String.class, false);
    assertThat(value).isEqualTo("${prop1}");
  }

  @Test
  void getRequiredPropertyThrowsExceptionWhenKeyNotPresent() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);

    assertThatThrownBy(() -> resolver.getRequiredProperty("nonexistent"))
            .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void getRequiredPropertyReturnsValueWhenKeyPresent() {
    Map<String, String> map = Map.of("key", "value");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.getRequiredProperty("key")).isEqualTo("value");
  }

  @Test
  void getRequiredPropertyWithTargetTypeConvertsValue() {
    Map<String, String> map = Map.of("number", "42");
    MapPropertyResolver resolver = new MapPropertyResolver(map);
    assertThat(resolver.getRequiredProperty("number", Integer.class)).isEqualTo(42);
  }

}