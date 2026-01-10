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

package infra.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import infra.app.env.OriginTrackedMapPropertySource;
import infra.core.env.EnumerablePropertySource;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.core.env.SystemEnvironmentPropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultIterableConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Fahim Farook
 */
class DefaultIterableConfigurationPropertySourceTests {

  @Test
  void createWhenPropertySourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DefaultIterableConfigurationPropertySource(null, false, mock(PropertyMapper.class)))
            .withMessageContaining("PropertySource is required");
  }

  @Test
  void iteratorShouldAdaptNames() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key1", "value1");
    source.put("key2", "value2");
    source.put("key3", "value3");
    source.put("key4", "value4");
    EnumerablePropertySource<?> propertySource = new MapPropertySource("test", source);
    TestPropertyMapper mapper1 = new TestPropertyMapper();
    mapper1.addFromPropertySource("key1", "my.key1");
    mapper1.addFromPropertySource("key2", "my.key2a");
    mapper1.addFromPropertySource("key4", "my.key4");
    TestPropertyMapper mapper2 = new TestPropertyMapper();
    mapper2.addFromPropertySource("key2", "my.key2b");
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(
            propertySource, false, mapper1, mapper2);
    assertThat(adapter.iterator()).toIterable()
            .extracting(Object::toString)
            .containsExactly("my.key1", "my.key2a", "my.key4");
  }

  @Test
  void getValueShouldUseDirectMapping() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key1", "value1");
    source.put("key2", "value2");
    source.put("key3", "value3");
    EnumerablePropertySource<?> propertySource = new MapPropertySource("test", source);
    TestPropertyMapper mapper = new TestPropertyMapper();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
    mapper.addFromConfigurationProperty(name, "key2");
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(
            propertySource, false, mapper);
    assertThat(adapter.getConfigurationProperty(name).getValue()).isEqualTo("value2");
  }

  @Test
  void getValueShouldUseEnumerableMapping() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key1", "value1");
    source.put("key2", "value2");
    source.put("key3", "value3");
    EnumerablePropertySource<?> propertySource = new MapPropertySource("test", source);
    TestPropertyMapper mapper = new TestPropertyMapper();
    mapper.addFromPropertySource("key1", "my.missing");
    mapper.addFromPropertySource("key2", "my.k-e-y");
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(
            propertySource, false, mapper);
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
    assertThat(adapter.getConfigurationProperty(name).getValue()).isEqualTo("value2");
  }

  @Test
  void getValueOrigin() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key", "value");
    EnumerablePropertySource<?> propertySource = new MapPropertySource("test", source);
    TestPropertyMapper mapper = new TestPropertyMapper();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
    mapper.addFromConfigurationProperty(name, "key");
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(
            propertySource, false, mapper);
    assertThat(adapter.getConfigurationProperty(name).getOrigin())
            .hasToString("\"key\" from property source \"test\"");
  }

  @Test
  void getValueWhenOriginCapableShouldIncludeSourceOrigin() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key", "value");
    EnumerablePropertySource<?> propertySource = new OriginCapablePropertySource<>(
            new MapPropertySource("test", source));
    TestPropertyMapper mapper = new TestPropertyMapper();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
    mapper.addFromConfigurationProperty(name, "key");
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(
            propertySource, false, mapper);
    assertThat(adapter.getConfigurationProperty(name).getOrigin()).hasToString("TestOrigin key");
  }

  @Test
  void containsDescendantOfShouldCheckSourceNames() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("foo.bar", "value");
    source.put("faf", "value");
    EnumerablePropertySource<?> propertySource = new OriginCapablePropertySource<>(
            new MapPropertySource("test", source));
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(
            propertySource, false, DefaultPropertyMapper.INSTANCE);
    assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("faf")))
            .isEqualTo(ConfigurationPropertyState.ABSENT);
    assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("fof")))
            .isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  @Test
  void containsDescendantOfWhenSystemEnvironmentPropertySourceShouldLegacyProperty() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("FOO_BAR_BAZ_BONG", "bing");
    source.put("FOO_ALPHABRAVO_GAMMA", "delta");
    SystemEnvironmentPropertySource propertySource = new SystemEnvironmentPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(
            propertySource, true, SystemEnvironmentPropertyMapper.INSTANCE);
    assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("foo.bar-baz")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("foo.alpha-bravo")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("foo.blah")))
            .isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  @Test
  void simpleMapPropertySourceKeyDataChangeInvalidatesCache() {
    // gh-13344
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");
    EnumerablePropertySource<?> source = new MapPropertySource("test", map);
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(source, false,
            DefaultPropertyMapper.INSTANCE);
    assertThat(adapter.stream()).hasSize(2);
    map.put("key3", "value3");
    assertThat(adapter.stream()).hasSize(3);
  }

  @Test
  void concurrentModificationExceptionInvalidatesCache() {
    // gh-17013
    ConcurrentModificationThrowingMap<String, Object> map = new ConcurrentModificationThrowingMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");
    EnumerablePropertySource<?> source = new MapPropertySource("test", map);
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(source, false,
            DefaultPropertyMapper.INSTANCE);
    assertThat(adapter.stream()).hasSize(2);
    map.setThrowException(true);
    map.put("key3", "value3");
    assertThat(adapter.stream()).hasSize(3);
  }

  @Test
  void originTrackedMapPropertySourceKeyAdditionInvalidatesCache() {
    // gh-13344
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");
    EnumerablePropertySource<?> source = new OriginTrackedMapPropertySource("test", map);
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(source, false,
            DefaultPropertyMapper.INSTANCE);
    assertThat(adapter.stream()).hasSize(2);
    map.put("key3", "value3");
    assertThat(adapter.stream()).hasSize(3);
  }

  @Test
  void readOnlyOriginTrackedMapPropertySourceKeyAdditionDoesNotInvalidateCache() {
    // gh-16717
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");
    EnumerablePropertySource<?> source = new OriginTrackedMapPropertySource("test", map, true);
    DefaultIterableConfigurationPropertySource adapter = new DefaultIterableConfigurationPropertySource(source, false,
            DefaultPropertyMapper.INSTANCE);
    assertThat(adapter.stream()).hasSize(2);
    map.put("key3", "value3");
    assertThat(adapter.stream()).hasSize(2);
  }

  @Test
  void orderOfUnderlyingSourceIsPreserved() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("test.map.alpha", "value1");
    map.put("test.map.bravo", "value2");
    map.put("test.map.charlie", "value3");
    map.put("test.map.delta", "value4");
    EnumerablePropertySource<?> source = new OriginTrackedMapPropertySource("test", map, true);
    DefaultIterableConfigurationPropertySource propertySource = new DefaultIterableConfigurationPropertySource(source,
            false, DefaultPropertyMapper.INSTANCE);
    assertThat(propertySource.stream().map(ConfigurationPropertyName::toString)).containsExactly("test.map.alpha",
            "test.map.bravo", "test.map.charlie", "test.map.delta");
  }

  @Test
  void cacheRefreshRecalculatesDescendants() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("one.two.three", "test");
    EnumerablePropertySource<?> source = new OriginTrackedMapPropertySource("test", map, false);
    var propertySource = new DefaultIterableConfigurationPropertySource(source,
            false, DefaultPropertyMapper.INSTANCE);
    assertThat(propertySource.containsDescendantOf(ConfigurationPropertyName.of("one.two")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
    map.put("new", "value");
    assertThat(propertySource.containsDescendantOf(ConfigurationPropertyName.of("one.two")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
  }

  /**
   * Test {@link PropertySource} that's also an {@link OriginLookup}.
   *
   * @param <T> the source type
   */
  static class OriginCapablePropertySource<T> extends EnumerablePropertySource<T> implements OriginLookup<String> {

    private final EnumerablePropertySource<T> propertySource;

    OriginCapablePropertySource(EnumerablePropertySource<T> propertySource) {
      super(propertySource.getName(), propertySource.getSource());
      this.propertySource = propertySource;
    }

    @Override
    public Object getProperty(String name) {
      return this.propertySource.getProperty(name);
    }

    @Override
    public String[] getPropertyNames() {
      return this.propertySource.getPropertyNames();
    }

    @Override
    public Origin getOrigin(String name) {
      return new Origin() {

        @Override
        public String toString() {
          return "TestOrigin " + name;
        }

      };
    }

  }

  static class ConcurrentModificationThrowingMap<K, V> extends LinkedHashMap<K, V> {

    private boolean throwException;

    void setThrowException(boolean throwException) {
      this.throwException = throwException;
    }

    @Override
    public Set<K> keySet() {
      return new ConcurrentModificationThrowingMap.KeySet(super.keySet());
    }

    private class KeySet extends LinkedHashSet<K> {

      KeySet(Set<K> keySet) {
        super(keySet);
      }

      @Override
      public Iterator<K> iterator() {
        if (ConcurrentModificationThrowingMap.this.throwException) {
          ConcurrentModificationThrowingMap.this.throwException = false;
          throw new ConcurrentModificationException();
        }
        return super.iterator();
      }

    }

  }

}
