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

package cn.taketoday.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.EnumerablePropertySource;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;

import static cn.taketoday.test.context.support.TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support with
 * inlined properties.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(ApplicationExtension.class)
@ContextConfiguration
@TestPropertySource(properties = { "", "foo = bar", "baz quux", "enigma: 42", "x.y.z = a=b=c",
        "server.url = https://example.com", "key.value.1: key=value", "key.value.2 key=value", "key.value.3 key:value" })
class InlinedPropertiesTestPropertySourceTests {

  @Autowired
  private ConfigurableEnvironment env;

  private String property(String key) {
    return env.getProperty(key);
  }

  @Test
  void propertiesAreAvailableInEnvironment() {
    // Simple key/value pairs
    assertThat(property("foo")).isEqualTo("bar");
    assertThat(property("baz")).isEqualTo("quux");
    assertThat(property("enigma")).isEqualTo("42");

    // Values containing key/value delimiters (":", "=", " ")
    assertThat(property("x.y.z")).isEqualTo("a=b=c");
    assertThat(property("server.url")).isEqualTo("https://example.com");
    assertThat(property("key.value.1")).isEqualTo("key=value");
    assertThat(property("key.value.2")).isEqualTo("key=value");
    assertThat(property("key.value.3")).isEqualTo("key:value");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void propertyNameOrderingIsPreservedInEnvironment() {
    final String[] expectedPropertyNames = new String[] { "foo", "baz", "enigma", "x.y.z", "server.url",
            "key.value.1", "key.value.2", "key.value.3" };
    EnumerablePropertySource eps = (EnumerablePropertySource) env.getPropertySources().get(
            INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
    assertThat(eps.getPropertyNames()).isEqualTo(expectedPropertyNames);
  }

  // -------------------------------------------------------------------

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

}
