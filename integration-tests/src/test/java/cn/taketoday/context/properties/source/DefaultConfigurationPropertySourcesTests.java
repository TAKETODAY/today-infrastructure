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

import java.util.Collections;
import java.util.Iterator;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.env.SystemEnvironmentPropertySource;
import cn.taketoday.framework.env.RandomValuePropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DefaultConfigurationPropertySourcesTests {

  @Test
  void createWhenPropertySourcesIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultConfigurationPropertySources(null))
            .withMessageContaining("Sources is required");
  }

  @Test
  void shouldAdaptPropertySource() {
    PropertySources sources = new PropertySources();
    sources.addFirst(new MapPropertySource("test", Collections.singletonMap("a", "b")));
    Iterator<ConfigurationPropertySource> iterator = new DefaultConfigurationPropertySources(sources).iterator();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
    assertThat(iterator.next().getConfigurationProperty(name).getValue()).isEqualTo("b");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldAdaptSystemEnvironmentPropertySource() {
    PropertySources sources = new PropertySources();
    sources.addLast(new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            Collections.singletonMap("SERVER_PORT", "1234")));
    Iterator<ConfigurationPropertySource> iterator = new DefaultConfigurationPropertySources(sources).iterator();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
    assertThat(iterator.next().getConfigurationProperty(name).getValue()).isEqualTo("1234");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldExtendedAdaptSystemEnvironmentPropertySource() {
    PropertySources sources = new PropertySources();
    sources.addLast(new SystemEnvironmentPropertySource(
            "test-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            Collections.singletonMap("SERVER_PORT", "1234")));
    Iterator<ConfigurationPropertySource> iterator = new DefaultConfigurationPropertySources(sources).iterator();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
    assertThat(iterator.next().getConfigurationProperty(name).getValue()).isEqualTo("1234");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldNotAdaptSystemEnvironmentPropertyOverrideSource() {
    PropertySources sources = new PropertySources();
    sources.addLast(
            new SystemEnvironmentPropertySource("override", Collections.singletonMap("server.port", "1234")));
    Iterator<ConfigurationPropertySource> iterator = new DefaultConfigurationPropertySources(sources).iterator();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
    assertThat(iterator.next().getConfigurationProperty(name).getValue()).isEqualTo("1234");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldAdaptSystemEnvironmentPropertySourceWithUnderscoreValue() {
    PropertySources sources = new PropertySources();
    sources.addLast(new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            Collections.singletonMap("_", "1234")));
    Iterator<ConfigurationPropertySource> iterator = new DefaultConfigurationPropertySources(sources).iterator();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("bar");
    assertThat(iterator.next().getConfigurationProperty(name)).isNull();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldAdaptMultiplePropertySources() {
    PropertySources sources = new PropertySources();
    sources.addLast(new SystemEnvironmentPropertySource("system", Collections.singletonMap("SERVER_PORT", "1234")));
    sources.addLast(new MapPropertySource("test1", Collections.singletonMap("server.po-rt", "4567")));
    sources.addLast(new MapPropertySource("test2", Collections.singletonMap("a", "b")));
    Iterator<ConfigurationPropertySource> iterator = new DefaultConfigurationPropertySources(sources).iterator();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("server.port");
    assertThat(iterator.next().getConfigurationProperty(name).getValue()).isEqualTo("1234");
    assertThat(iterator.next().getConfigurationProperty(name).getValue()).isEqualTo("4567");
    assertThat(iterator.next().getConfigurationProperty(ConfigurationPropertyName.of("a")).getValue())
            .isEqualTo("b");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldFlattenEnvironment() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("foo", Collections.singletonMap("foo", "bar")));
    environment.getPropertySources().addFirst(new MapPropertySource("far", Collections.singletonMap("far", "far")));
    PropertySources sources = new PropertySources();
    sources.addFirst(new PropertySource<Environment>("env", environment) {

      @Override
      public String getProperty(String key) {
        return this.source.getProperty(key);
      }

    });
    sources.addLast(new MapPropertySource("baz", Collections.singletonMap("baz", "barf")));
    DefaultConfigurationPropertySources configurationSources = new DefaultConfigurationPropertySources(sources);
    assertThat(configurationSources.iterator()).toIterable().hasSize(5);
  }

  @Test
  void shouldTrackChanges() {
    PropertySources sources = new PropertySources();
    DefaultConfigurationPropertySources configurationSources = new DefaultConfigurationPropertySources(sources);
    assertThat(configurationSources.iterator()).toIterable().hasSize(0);
    MapPropertySource source1 = new MapPropertySource("test1", Collections.singletonMap("a", "b"));
    sources.addLast(source1);
    assertThat(configurationSources.iterator()).toIterable().hasSize(1);
    MapPropertySource source2 = new MapPropertySource("test2", Collections.singletonMap("b", "c"));
    sources.addLast(source2);
    assertThat(configurationSources.iterator()).toIterable().hasSize(2);
  }

  @Test
  void shouldTrackWhenSourceHasIdenticalName() {
    PropertySources sources = new PropertySources();
    DefaultConfigurationPropertySources configurationSources = new DefaultConfigurationPropertySources(sources);
    ConfigurationPropertyName name = ConfigurationPropertyName.of("a");
    MapPropertySource source1 = new MapPropertySource("test", Collections.singletonMap("a", "s1"));
    sources.addLast(source1);
    assertThat(configurationSources.iterator().next().getConfigurationProperty(name).getValue()).isEqualTo("s1");
    MapPropertySource source2 = new MapPropertySource("test", Collections.singletonMap("a", "s2"));
    sources.remove("test");
    sources.addLast(source2);
    assertThat(configurationSources.iterator().next().getConfigurationProperty(name).getValue()).isEqualTo("s2");
  }

  @Test
    // gh-21659
  void shouldAdaptRandomPropertySource() {
    PropertySources sources = new PropertySources();
    sources.addFirst(new RandomValuePropertySource());
    Iterator<ConfigurationPropertySource> iterator = new DefaultConfigurationPropertySources(sources).iterator();
    ConfigurationPropertyName name = ConfigurationPropertyName.of("random.int");
    assertThat(iterator.next().getConfigurationProperty(name).getValue()).isNotNull();
    assertThat(iterator.hasNext()).isFalse();
  }

}
