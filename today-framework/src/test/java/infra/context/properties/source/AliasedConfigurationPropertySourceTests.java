/*
 * Copyright 2017 - 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.Collections;

import infra.context.properties.source.AliasedConfigurationPropertySource;
import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertyNameAliases;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.ConfigurationPropertyState;
import infra.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AliasedConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedConfigurationPropertySourceTests {

  @Test
  void getConfigurationPropertyShouldConsiderAliases() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "bing");
    source.put("foo.baz", "biff");
    ConfigurationPropertySource aliased = source.nonIterable()
            .withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
    assertThat(getValue(aliased, "foo.bar")).isEqualTo("bing");
    assertThat(getValue(aliased, "foo.bar1")).isEqualTo("bing");
  }

  @Test
  void getConfigurationPropertyWhenNotAliasesShouldReturnValue() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "bing");
    source.put("foo.baz", "biff");
    ConfigurationPropertySource aliased = source.nonIterable()
            .withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
    assertThat(getValue(aliased, "foo.baz")).isEqualTo("biff");
  }

  @Test
  void containsDescendantOfWhenSourceReturnsUnknownShouldReturnUnknown() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.UNKNOWN);
    ConfigurationPropertySource aliased = source
            .withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
    assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.UNKNOWN);
  }

  @Test
  void containsDescendantOfWhenSourceReturnsPresentShouldReturnPresent() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.PRESENT);
    given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
            .willReturn(ConfigurationPropertyState.UNKNOWN);
    ConfigurationPropertySource aliased = source
            .withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
    assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
  }

  @Test
  void containsDescendantOfWhenAllAreAbsentShouldReturnAbsent() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
    given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
            .willReturn(ConfigurationPropertyState.ABSENT);
    ConfigurationPropertySource aliased = source.withAliases(new ConfigurationPropertyNameAliases("foo", "bar"));
    assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  @Test
  void containsDescendantOfWhenAnyIsPresentShouldReturnPresent() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
    given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
            .willReturn(ConfigurationPropertyState.PRESENT);
    ConfigurationPropertySource aliased = source.withAliases(new ConfigurationPropertyNameAliases("foo", "bar"));
    assertThat(aliased.containsDescendantOf(name)).isEqualTo(ConfigurationPropertyState.PRESENT);
  }

  @Test
  void containsDescendantOfWhenPresentInAliasShouldReturnPresent() {
    ConfigurationPropertySource source = new MapConfigurationPropertySource(
            Collections.singletonMap("foo.bar", "foobar"));
    ConfigurationPropertySource aliased = source
            .withAliases(new ConfigurationPropertyNameAliases("foo.bar", "baz.foo"));
    assertThat(aliased.containsDescendantOf(ConfigurationPropertyName.of("baz")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
  }

  private Object getValue(ConfigurationPropertySource source, String name) {
    ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
    return (property != null) ? property.getValue() : null;
  }

}
