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
import org.mockito.Answers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrefixedConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 */
class PrefixedConfigurationPropertySourceTests {

  @Test
  void getConfigurationPropertyShouldConsiderPrefix() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("my.foo.bar", "bing");
    source.put("my.foo.baz", "biff");
    ConfigurationPropertySource prefixed = source.nonIterable().withPrefix("my");
    assertThat(getName(prefixed, "foo.bar").toString()).isEqualTo("foo.bar");
    assertThat(getValue(prefixed, "foo.bar")).isEqualTo("bing");
    assertThat(getName(prefixed, "foo.baz").toString()).isEqualTo("foo.baz");
    assertThat(getValue(prefixed, "foo.baz")).isEqualTo("biff");
  }

  @Test
  void containsDescendantOfWhenSourceReturnsUnknownShouldReturnUnknown() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.UNKNOWN);
    ConfigurationPropertySource prefixed = source.withPrefix("my");
    assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.UNKNOWN);
  }

  @Test
  void containsDescendantOfWhenSourceReturnsPresentShouldReturnPresent() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.PRESENT);
    given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
            .willReturn(ConfigurationPropertyState.UNKNOWN);
    ConfigurationPropertySource prefixed = source.withPrefix("my");
    assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.PRESENT);
  }

  @Test
  void containsDescendantOfWhenSourceReturnsAbsentShouldReturnAbsent() {
    ConfigurationPropertyName name = ConfigurationPropertyName.of("my.foo");
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class, Answers.CALLS_REAL_METHODS);
    given(source.containsDescendantOf(name)).willReturn(ConfigurationPropertyState.ABSENT);
    given(source.containsDescendantOf(ConfigurationPropertyName.of("bar")))
            .willReturn(ConfigurationPropertyState.ABSENT);
    ConfigurationPropertySource prefixed = source.withPrefix("my");
    assertThat(prefixed.containsDescendantOf(ConfigurationPropertyName.of("foo")))
            .isEqualTo(ConfigurationPropertyState.ABSENT);
  }

  @Test
  void withPrefixWhenPrefixIsNullReturnsOriginalSource() {
    ConfigurationPropertySource source = new MockConfigurationPropertySource().nonIterable();
    ConfigurationPropertySource prefixed = source.withPrefix(null);
    assertThat(prefixed).isSameAs(source);
  }

  @Test
  void withPrefixWhenPrefixIsEmptyReturnsOriginalSource() {
    ConfigurationPropertySource source = new MockConfigurationPropertySource().nonIterable();
    ConfigurationPropertySource prefixed = source.withPrefix("");
    assertThat(prefixed).isSameAs(source);
  }

  private ConfigurationPropertyName getName(ConfigurationPropertySource source, String name) {
    ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
    return (property != null) ? property.getName() : null;
  }

  private Object getValue(ConfigurationPropertySource source, String name) {
    ConfigurationProperty property = source.getConfigurationProperty(ConfigurationPropertyName.of(name));
    return (property != null) ? property.getValue() : null;
  }

}
