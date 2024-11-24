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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.UnboundElementsSourceFilter;
import infra.core.env.StandardEnvironment;
import infra.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UnboundElementsSourceFilter}.
 *
 * @author Madhura Bhave
 */
class UnboundElementsSourceFilterTests {

  private UnboundElementsSourceFilter filter;

  private ConfigurationPropertySource source;

  @BeforeEach
  void setUp() {
    this.filter = new UnboundElementsSourceFilter();
    this.source = mock(ConfigurationPropertySource.class);
  }

  @Test
  void filterWhenSourceIsSystemPropertiesPropertySourceShouldReturnFalse() {
    MockPropertySource propertySource = new MockPropertySource(
            StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    given(this.source.getUnderlyingSource()).willReturn(propertySource);
    assertThat(this.filter.apply(this.source)).isFalse();
  }

  @Test
  void filterWhenSourceIsSystemEnvironmentPropertySourceShouldReturnFalse() {
    MockPropertySource propertySource = new MockPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    given(this.source.getUnderlyingSource()).willReturn(propertySource);
    assertThat(this.filter.apply(this.source)).isFalse();
  }

  @Test
  void filterWhenSourceIsNotSystemShouldReturnTrue() {
    MockPropertySource propertySource = new MockPropertySource("test");
    given(this.source.getUnderlyingSource()).willReturn(propertySource);
    assertThat(this.filter.apply(this.source)).isTrue();
  }

}
