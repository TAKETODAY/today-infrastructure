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

import infra.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertySource}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourceTests {

  @Test
  void fromCreatesConfigurationPropertySourcesPropertySource() {
    MockPropertySource source = new MockPropertySource();
    source.setProperty("spring", "boot");
    ConfigurationPropertySource adapted = ConfigurationPropertySource.from(source);
    assertThat(adapted.getConfigurationProperty(ConfigurationPropertyName.of("spring")).getValue())
            .isEqualTo("boot");
  }

  @Test
  void fromWhenSourceIsAlreadyConfigurationPropertySourcesPropertySourceReturnsNull() {
    ConfigurationPropertySourcesPropertySource source = mock(ConfigurationPropertySourcesPropertySource.class);
    assertThat(ConfigurationPropertySource.from(source)).isNull();
  }

}
