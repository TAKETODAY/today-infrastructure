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

package infra.context.properties.source;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import infra.app.env.OriginTrackedMapPropertySource;
import infra.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FilteredIterableConfigurationPropertiesSource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class FilteredIterableConfigurationPropertiesSourceTests extends FilteredConfigurationPropertiesSourceTests {

  @Test
  void iteratorFiltersNames() {
    MockConfigurationPropertySource source = (MockConfigurationPropertySource) createTestSource();
    IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
    assertThat(filtered.iterator()).toIterable()
            .extracting(ConfigurationPropertyName::toString)
            .containsExactly("a", "b", "c");
  }

  @Test
  void containsDescendantOfUsesContents() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar.baz", "1");
    source.put("foo.bar[0]", "1");
    source.put("faf.bar[0]", "1");
    IterableConfigurationPropertySource filtered = source.filter(this::noBrackets);
    assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
            .isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  @Test
  void iteratorWhenSpringPropertySourceFiltersNames() {
    IterableConfigurationPropertySource testSource = (IterableConfigurationPropertySource) createTestSource();
    Map<String, Object> map = new LinkedHashMap<>();
    for (ConfigurationPropertyName name : testSource) {
      map.put(name.toString(), testSource.getConfigurationProperty(name).getValue());
    }
    PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
    ConfigurationPropertySource source = ConfigurationPropertySource.from(propertySource);
    IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
            .filter(this::noBrackets);
    assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
    assertThat(filtered.iterator()).toIterable()
            .extracting(ConfigurationPropertyName::toString)
            .containsExactly("a", "b", "c");
  }

  @Test
  void iteratorWhenSpringPropertySourceAndAnotherFilterFiltersNames() {
    IterableConfigurationPropertySource testSource = (IterableConfigurationPropertySource) createTestSource();
    Map<String, Object> map = new LinkedHashMap<>();
    for (ConfigurationPropertyName name : testSource) {
      map.put(name.toString(), testSource.getConfigurationProperty(name).getValue());
    }
    PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
    ConfigurationPropertySource source = ConfigurationPropertySource.from(propertySource);
    IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
            .filter(this::noBrackets);
    IterableConfigurationPropertySource secondFiltered = filtered.filter((name) -> !name.toString().contains("c"));
    assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
    assertThat(secondFiltered.iterator()).toIterable()
            .extracting(ConfigurationPropertyName::toString)
            .containsExactly("a", "b");
  }

  @Test
  void containsDescendantOfWhenSpringPropertySourceUsesContents() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("foo.bar.baz", "1");
    map.put("foo.bar[0]", "1");
    map.put("faf.bar[0]", "1");
    PropertySource<?> propertySource = new OriginTrackedMapPropertySource("test", map, true);
    ConfigurationPropertySource source = ConfigurationPropertySource.from(propertySource);
    IterableConfigurationPropertySource filtered = (IterableConfigurationPropertySource) source
            .filter(this::noBrackets);
    assertThat(Extractors.byName("filteredNames").apply(filtered)).isNotNull();
    assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
    assertThat(filtered.containsDescendantOf(ConfigurationPropertyName.of("faf")))
            .isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  @Override
  protected ConfigurationPropertySource convertSource(MockConfigurationPropertySource source) {
    return source;
  }

  private boolean noBrackets(ConfigurationPropertyName name) {
    return !name.toString().contains("[");
  }

}
