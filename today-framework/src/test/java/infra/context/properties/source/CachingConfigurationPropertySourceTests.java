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

import infra.context.properties.source.CachingConfigurationPropertySource;
import infra.context.properties.source.ConfigurationPropertyCaching;
import infra.context.properties.source.ConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link CachingConfigurationPropertySource}.
 *
 * @author Phillip Webb
 */
class CachingConfigurationPropertySourceTests {

  @Test
  void findWhenNullSourceReturnsNull() {
    ConfigurationPropertySource source = null;
    assertThat(CachingConfigurationPropertySource.find(source)).isNull();
  }

  @Test
  void findWhenNotCachingConfigurationPropertySourceReturnsNull() {
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class);
    assertThat(CachingConfigurationPropertySource.find(source)).isNull();
  }

  @Test
  void findWhenCachingConfigurationPropertySourceReturnsCaching() {
    ConfigurationPropertySource source = mock(ConfigurationPropertySource.class,
            withSettings().extraInterfaces(CachingConfigurationPropertySource.class));
    ConfigurationPropertyCaching caching = mock(ConfigurationPropertyCaching.class);
    given(((CachingConfigurationPropertySource) source).getCaching()).willReturn(caching);
    assertThat(CachingConfigurationPropertySource.find(source)).isEqualTo(caching);
  }

}
