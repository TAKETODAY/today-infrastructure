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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import infra.app.env.RandomValuePropertySource;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DefaultConfigurationPropertySourceTests {

  @Test
  void createWhenPropertySourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DefaultConfigurationPropertySource(null, false, mock(PropertyMapper.class)))
            .withMessageContaining("PropertySource is required");
  }

  @Test
  void getValueShouldUseDirectMapping() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key1", "value1");
    source.put("key2", "value2");
    source.put("key3", "value3");
    PropertySource<?> propertySource = new MapPropertySource("test", source);
    TestPropertyMapper mapper = new TestPropertyMapper();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
    mapper.addFromConfigurationProperty(name, "key2");
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource, false, mapper);
    assertThat(adapter.getConfigurationProperty(name).getValue()).isEqualTo("value2");
  }

  @Test
  void getValueOriginAndPropertySource() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key", "value");
    PropertySource<?> propertySource = new MapPropertySource("test", source);
    TestPropertyMapper mapper = new TestPropertyMapper();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
    mapper.addFromConfigurationProperty(name, "key");
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource, false,
            mapper);
    ConfigurationProperty configurationProperty = adapter.getConfigurationProperty(name);
    assertThat(configurationProperty.getOrigin()).hasToString("\"key\" from property source \"test\"");
    assertThat(configurationProperty.getSource()).isEqualTo(adapter);
  }

  @Test
  void getValueWhenOriginCapableShouldIncludeSourceOrigin() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("key", "value");
    PropertySource<?> propertySource = new OriginCapablePropertySource<>(new MapPropertySource("test", source));
    TestPropertyMapper mapper = new TestPropertyMapper();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.key");
    mapper.addFromConfigurationProperty(name, "key");
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource, false,
            mapper);
    assertThat(adapter.getConfigurationProperty(name).getOrigin()).hasToString("TestOrigin key");
  }

  @Test
  void containsDescendantOfShouldReturnEmpty() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("foo.bar", "value");
    PropertySource<?> propertySource = new MapPropertySource("test", source);
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource, false,
            DefaultPropertyMapper.INSTANCE);
    assertThat(adapter.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.UNKNOWN);
  }

  @Test
  void fromWhenPropertySourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> DefaultConfigurationPropertySource.from(null))
            .withMessageContaining("Source is required");
  }

  @Test
  void fromWhenNonEnumerableShouldReturnNonIterable() {
    PropertySource<?> propertySource = new PropertySource<>("test", new Object()) {

      @Override
      public Object getProperty(String name) {
        return null;
      }

    };
    assertThat(DefaultConfigurationPropertySource.from(propertySource))
            .isNotInstanceOf(IterableConfigurationPropertySource.class);

  }

  @Test
  void fromWhenEnumerableButRestrictedShouldReturnNonIterable() {
    Map<String, Object> source = new LinkedHashMap<>() {

      @Override
      public int size() {
        throw new UnsupportedOperationException("Same as security restricted");
      }

    };
    PropertySource<?> propertySource = new MapPropertySource("test", source);
    assertThat(DefaultConfigurationPropertySource.from(propertySource))
            .isNotInstanceOf(IterableConfigurationPropertySource.class);
  }

  @Test
  void getWhenEnumerableShouldBeIterable() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("fooBar", "Spring ${barBaz} ${bar-baz}");
    source.put("barBaz", "Boot");
    PropertySource<?> propertySource = new MapPropertySource("test", source);
    assertThat(DefaultConfigurationPropertySource.from(propertySource))
            .isInstanceOf(IterableConfigurationPropertySource.class);
  }

  @Test
  void containsDescendantOfWhenRandomSourceAndRandomPropertyReturnsPresent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomValuePropertySource());
    ConfigurationPropertyName name = ConfigurationPropertyName.of("random");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
    assertThat(source.getConfigurationProperty(name)).isNull();
  }

  @Test
  void containsDescendantOfWhenRandomSourceAndRandomPrefixedPropertyReturnsPresent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomValuePropertySource());
    ConfigurationPropertyName name = ConfigurationPropertyName.of("random.int");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(source.getConfigurationProperty(name)).isNotNull();
  }

  @Test
  void containsDescendantOfWhenRandomSourceWithDifferentNameAndRandomPrefixedPropertyReturnsPresent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomValuePropertySource("different"));
    ConfigurationPropertyName name = ConfigurationPropertyName.of("random.int");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(source.getConfigurationProperty(name)).isNotNull();
  }

  @Test
  void containsDescendantOfWhenRandomSourceAndNonRandomPropertyReturnsAbsent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomValuePropertySource());
    ConfigurationPropertyName name = ConfigurationPropertyName.of("abandon.int");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
    assertThat(source.getConfigurationProperty(name)).isNull();
  }

  @Test
  void containsDescendantOfWhenWrappedRandomSourceAndRandomPropertyReturnsPresent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomWrapperPropertySource());
    ConfigurationPropertyName name = ConfigurationPropertyName.of("cachedrandom");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
    assertThat(source.getConfigurationProperty(name)).isNull();
  }

  @Test
  void containsDescendantOfWhenWrappedRandomSourceAndRandomPrefixedPropertyReturnsPresent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomWrapperPropertySource());
    ConfigurationPropertyName name = ConfigurationPropertyName.of("cachedrandom.something.int");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
    assertThat(source.getConfigurationProperty(name)).isNull();
  }

  @Test
  void containsDescendantOfWhenWrappedRandomSourceWithMatchingNameAndRandomPrefixedPropertyReturnsPresent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomWrapperPropertySource("cachedrandom"));
    ConfigurationPropertyName name = ConfigurationPropertyName.of("cachedrandom.something.int");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(source.getConfigurationProperty(name)).isNotNull();
  }

  @Test
  void containsDescendantOfWhenWrappedRandomSourceAndRandomDashPrefixedPropertyReturnsPresent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomWrapperPropertySource());
    ConfigurationPropertyName name = ConfigurationPropertyName.of("cached-random.something.int");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
    assertThat(source.getConfigurationProperty(name)).isNull();
  }

  @Test
  void containsDescendantOfWhenWrappedRandomSourceAndNonRandomPropertyReturnsAbsent() {
    DefaultConfigurationPropertySource source = DefaultConfigurationPropertySource
            .from(new RandomWrapperPropertySource());
    ConfigurationPropertyName name = ConfigurationPropertyName.of("abandon.something.int");
    assertThat(source.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
    assertThat(source.getConfigurationProperty(name)).isNull();
  }

  static class RandomWrapperPropertySource extends PropertySource<RandomValuePropertySource> {

    private final String prefix;

    RandomWrapperPropertySource() {
      this("cachedRandom");
    }

    RandomWrapperPropertySource(String name) {
      super(name, new RandomValuePropertySource());
      this.prefix = name + ".";
    }

    @Override
    public Object getProperty(String name) {
      name = name.toLowerCase(Locale.ROOT);
      if (!name.startsWith(this.prefix)) {
        return null;
      }
      return getSource().getProperty("random." + name.substring(this.prefix.length()));
    }

  }

  /**
   * Test {@link PropertySource} that's also an {@link OriginLookup}.
   *
   * @param <T> The source type
   */
  static class OriginCapablePropertySource<T> extends PropertySource<T> implements OriginLookup<String> {

    private final PropertySource<T> propertySource;

    OriginCapablePropertySource(PropertySource<T> propertySource) {
      super(propertySource.getName(), propertySource.getSource());
      this.propertySource = propertySource;
    }

    @Override
    public Object getProperty(String name) {
      return this.propertySource.getProperty(name);
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

}
