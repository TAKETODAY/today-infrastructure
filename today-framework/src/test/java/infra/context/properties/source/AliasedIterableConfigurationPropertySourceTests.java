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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AliasedConfigurationPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AliasedIterableConfigurationPropertySourceTests extends AliasedConfigurationPropertySourceTests {

  @Test
  void streamShouldIncludeAliases() {
    MockConfigurationPropertySource source = new MockConfigurationPropertySource();
    source.put("foo.bar", "bing");
    source.put("foo.baz", "biff");
    IterableConfigurationPropertySource aliased = source
            .withAliases(new ConfigurationPropertyNameAliases("foo.bar", "foo.bar1"));
    assertThat(aliased.stream()).containsExactly(ConfigurationPropertyName.of("foo.bar"),
            ConfigurationPropertyName.of("foo.bar1"), ConfigurationPropertyName.of("foo.baz"));
  }

}
