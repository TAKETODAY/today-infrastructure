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

package infra.origin;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link PropertySourceOrigin}.
 *
 * @author Phillip Webb
 */
class PropertySourceOriginTests {

  @Test
  void createWhenPropertySourceIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new PropertySourceOrigin(null, "name"))
            .withMessageContaining("'propertySource' is required");
  }

  @Test
  void createWhenPropertyNameIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new PropertySourceOrigin(mock(PropertySource.class), null))
            .withMessageContaining("'propertyName' must not be empty");
  }

  @Test
  void createWhenPropertyNameIsEmptyShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new PropertySourceOrigin(mock(PropertySource.class), ""))
            .withMessageContaining("'propertyName' must not be empty");
  }

  @Test
  void getPropertySourceShouldReturnPropertySource() {
    MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
    PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
    assertThat(origin.getPropertySource()).isEqualTo(propertySource);
  }

  @Test
  void getPropertyNameShouldReturnPropertyName() {
    MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
    PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
    assertThat(origin.getPropertyName()).isEqualTo("foo");
  }

  @Test
  void toStringShouldShowDetails() {
    MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
    PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
    assertThat(origin).hasToString("\"foo\" from property source \"test\"");
  }

  @Test
  @SuppressWarnings("unchecked")
  void getWhenPropertySourceSupportsOriginLookupShouldReturnOrigin() {
    Origin origin = mock(Origin.class);
    PropertySource<?> propertySource = mock(PropertySource.class,
            withSettings().extraInterfaces(OriginLookup.class));
    OriginLookup<String> originCapablePropertySource = (OriginLookup<String>) propertySource;
    given(originCapablePropertySource.getOrigin("foo")).willReturn(origin);
    Origin actual = PropertySourceOrigin.get(propertySource, "foo");
    assertThat(actual).hasToString(origin.toString());
    assertThat(((PropertySourceOrigin) actual).getOrigin()).isSameAs(origin);
  }

  @Test
  void getWhenPropertySourceSupportsOriginLookupButNoOriginShouldWrap() {
    PropertySource<?> propertySource = mock(PropertySource.class,
            withSettings().extraInterfaces(OriginLookup.class));
    assertThat(PropertySourceOrigin.get(propertySource, "foo")).isInstanceOf(PropertySourceOrigin.class);
  }

  @Test
  void getWhenPropertySourceIsNotOriginAwareShouldWrap() {
    MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
    PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
    assertThat(origin.getPropertySource()).isEqualTo(propertySource);
    assertThat(origin.getPropertyName()).isEqualTo("foo");
  }

}
