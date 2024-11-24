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

package infra.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.EnumerablePropertySource;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.context.support.TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support with
 * inlined properties.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration
@TestPropertySource(properties = { "", "foo = bar", "baz quux", "enigma: 42", "x.y.z = a=b=c",
        "server.url = https://example.com", "key.value.1: key=value", "key.value.2 key=value", "key.value.3 key:value" })
class InlinedPropertiesTestPropertySourceTests {

  @Autowired
  ConfigurableEnvironment env;

  @Test
  void propertiesAreAvailableInEnvironment() {
    // Simple key/value pairs
    assertEnvironmentProperty("foo", "bar");
    assertEnvironmentProperty("baz", "quux");
    assertEnvironmentProperty("enigma", "42");

    // Values containing key/value delimiters (":", "=", " ")
    assertEnvironmentProperty("x.y.z", "a=b=c");
    assertEnvironmentProperty("server.url", "https://example.com");
    assertEnvironmentProperty("key.value.1", "key=value");
    assertEnvironmentProperty("key.value.2", "key=value");
    assertEnvironmentProperty("key.value.3", "key:value");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void propertyNameOrderingIsPreservedInEnvironment() {
    EnumerablePropertySource eps = (EnumerablePropertySource) env.getPropertySources().get(
            INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(eps.getPropertyNames()).containsExactly("foo", "baz", "enigma", "x.y.z", "server.url",
            "key.value.1", "key.value.2", "key.value.3");
  }

  private void assertEnvironmentProperty(String name, Object value) {
    assertThat(this.env.getProperty(name)).as("environment property '%s'", name).isEqualTo(value);
  }

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
