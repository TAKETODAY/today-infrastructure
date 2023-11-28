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

package cn.taketoday.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.framework.env.RandomValuePropertySource;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;

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
            .isThrownBy(() -> new DefaultConfigurationPropertySource(null, mock(PropertyMapper.class)))
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
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource, mapper);
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
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource, mapper);
    ConfigurationProperty configurationProperty = adapter.getConfigurationProperty(name);
    assertThat(configurationProperty.getOrigin().toString()).isEqualTo("\"key\" from property source \"test\"");
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
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource, mapper);
    assertThat(adapter.getConfigurationProperty(name).getOrigin().toString()).isEqualTo("TestOrigin key");
  }

  @Test
  void containsDescendantOfShouldReturnEmpty() {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("foo.bar", "value");
    PropertySource<?> propertySource = new MapPropertySource("test", source);
    DefaultConfigurationPropertySource adapter = new DefaultConfigurationPropertySource(propertySource,
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
    PropertySource<?> propertySource = new PropertySource<Object>("test", new Object()) {

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
    Map<String, Object> source = new LinkedHashMap<String, Object>() {

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
      name = name.toLowerCase();
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
