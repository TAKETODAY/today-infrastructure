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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.env.SystemEnvironmentPropertySource;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesTests {

  @Test
  void attachShouldAddAdapterAtBeginning() {
    ConfigurableEnvironment environment = new StandardEnvironment();
    PropertySources sources = environment.getPropertySources();
    sources.addLast(new SystemEnvironmentPropertySource("system", Collections.singletonMap("SERVER_PORT", "1234")));
    sources.addLast(new MapPropertySource("config", Collections.singletonMap("server.port", "4568")));
    int size = sources.size();
    ConfigurationPropertySources.attach(environment);
    assertThat(sources.size()).isEqualTo(size + 1);
    PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
    assertThat(resolver.getProperty("server.port")).isEqualTo("1234");
  }

  @Test
  void attachShouldReattachInMergedSetup() {
    ConfigurableEnvironment parent = new StandardEnvironment();
    ConfigurationPropertySources.attach(parent);
    ConfigurableEnvironment child = new StandardEnvironment();
    child.merge(parent);
    child.getPropertySources()
            .addLast(new MapPropertySource("config", Collections.singletonMap("my.example_property", "1234")));
    ConfigurationPropertySources.attach(child);
    assertThat(child.getProperty("my.example-property")).isEqualTo("1234");
  }

  @Test
  void attachWhenAlreadyAttachedWithSameSourcesShouldReturnExistingInstance() {
    ConfigurableEnvironment environment = new StandardEnvironment();
    PropertySources sources = environment.getPropertySources();
    sources.addLast(new SystemEnvironmentPropertySource("system", Collections.singletonMap("SERVER_PORT", "1234")));
    sources.addLast(new MapPropertySource("config", Collections.singletonMap("server.port", "4568")));
    ConfigurationPropertySources.attach(environment);
    Iterable<ConfigurationPropertySource> first = ConfigurationPropertySources.get(environment);
    ConfigurationPropertySources.attach(environment);
    Iterable<ConfigurationPropertySource> second = ConfigurationPropertySources.get(environment);
    assertThat(first).isSameAs(second);
  }

  @Test
  void getWhenNotAttachedShouldReturnAdapted() {
    ConfigurableEnvironment environment = new StandardEnvironment();
    assertThat(ConfigurationPropertySources.get(environment)).isNotEmpty();
  }

  @Test
  void getWhenAttachedShouldReturnAttached() {
    ConfigurableEnvironment environment = new StandardEnvironment();
    PropertySources sources = environment.getPropertySources();
    sources.addFirst(new MapPropertySource("test", Collections.singletonMap("a", "b")));
    int expectedSize = sources.size();
    ConfigurationPropertySources.attach(environment);
    assertThat(ConfigurationPropertySources.get(environment)).hasSize(expectedSize);
  }

  @Test
  void environmentPropertyExpansionShouldWorkWhenAttached() {
    ConfigurableEnvironment environment = new StandardEnvironment();
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("fooBar", "Spring ${barBaz} ${bar-baz}");
    source.put("barBaz", "Boot");
    PropertySource<?> propertySource = new MapPropertySource("test", source);
    environment.getPropertySources().addFirst(propertySource);
    ConfigurationPropertySources.attach(environment);
    assertThat(environment.getProperty("foo-bar")).isEqualTo("Spring Boot Boot");
  }

  @Test
  void fromPropertySourceShouldReturnDefaultConfigurationPropertySource() {
    PropertySource<?> source = new MapPropertySource("foo", Collections.singletonMap("foo", "bar"));
    ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySources.from(source).iterator()
            .next();
    assertThat(configurationPropertySource).isInstanceOf(DefaultConfigurationPropertySource.class);
  }

  @Test
  void fromPropertySourceShouldFlattenPropertySources() {
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
    Iterable<ConfigurationPropertySource> configurationSources = ConfigurationPropertySources.from(sources);
    assertThat(configurationSources.iterator()).toIterable().hasSize(5);
  }

  @Test
    // gh-20625
  void environmentPropertyAccessWhenImmutableShouldBePerformant() {
    long baseline = testPropertySourcePerformance(false);
    long immutable = testPropertySourcePerformance(true);
    assertThat(immutable).isLessThan(baseline / 2);
  }

  @Test
    // gh-20625
  void environmentPropertyAccessWhenMutableWithCacheShouldBePerformant() {
    StandardEnvironment environment = createPerformanceTestEnvironment(false);
    long uncached = testPropertySourcePerformance(environment);
    ConfigurationPropertyCaching.get(environment).enable();
    long cached = testPropertySourcePerformance(environment);
    assertThat(cached).isLessThan(uncached / 2);
  }

  @Test // gh-20625
  @Disabled("for manual testing")
  void environmentPropertyAccessWhenMutableShouldBeTolerable() {
    assertThat(testPropertySourcePerformance(false)).isLessThan(5000);
  }

  @Test
    // gh-21416
  void descendantOfPropertyAccessWhenMutableWithCacheShouldBePerformant() {
    Function<StandardEnvironment, Long> descendantOfPerformance = (environment) -> {
      Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
      ConfigurationPropertyName missing = ConfigurationPropertyName.of("missing");
      long start = System.nanoTime();
      for (int i = 0; i < 1000; i++) {
        for (ConfigurationPropertySource source : sources) {
          assertThat(source.containsDescendantOf(missing)).isEqualTo(ConfigurationPropertyState.ABSENT);
        }
      }
      return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    };
    StandardEnvironment environment = createPerformanceTestEnvironment(false);
    long baseline = descendantOfPerformance.apply(environment);
    ConfigurationPropertyCaching.get(environment).enable();
    long cached = descendantOfPerformance.apply(environment);
    assertThat(cached).isLessThan(baseline / 2);
  }

  private long testPropertySourcePerformance(boolean immutable) {
    StandardEnvironment environment = createPerformanceTestEnvironment(immutable);
    return testPropertySourcePerformance(environment);
  }

  private StandardEnvironment createPerformanceTestEnvironment(boolean immutable) {
    StandardEnvironment environment = new StandardEnvironment();
    PropertySources propertySources = environment.getPropertySources();
    for (int i = 0; i < 100; i++) {
      propertySources.addLast(new TestPropertySource(i, immutable));
    }
    ConfigurationPropertySources.attach(environment);
    return environment;
  }

  private long testPropertySourcePerformance(StandardEnvironment environment) {
    long start = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      environment.getProperty("missing" + i);
    }
    long total = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    assertThat(environment.getProperty("test-10-property-80")).isEqualTo("test-10-property-80-value");
    return total;
  }

  static class TestPropertySource extends MapPropertySource implements OriginLookup<String> {

    private final boolean immutable;

    TestPropertySource(int index, boolean immutable) {
      super("test-" + index, createProperties(index));
      this.immutable = immutable;
    }

    private static Map<String, Object> createProperties(int index) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (int i = 0; i < 1000; i++) {
        String name = "test-" + index + "-property-" + i;
        String value = name + "-value";
        map.put(name, value);
      }
      return map;
    }

    @Override
    public Origin getOrigin(String key) {
      return null;
    }

    @Override
    public boolean isImmutable() {
      return this.immutable;
    }

  }

}
