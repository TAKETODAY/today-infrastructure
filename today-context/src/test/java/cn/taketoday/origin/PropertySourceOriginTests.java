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

package cn.taketoday.origin;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;

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
            .withMessageContaining("PropertySource must not be null");
  }

  @Test
  void createWhenPropertyNameIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new PropertySourceOrigin(mock(PropertySource.class), null))
            .withMessageContaining("PropertyName must not be empty");
  }

  @Test
  void createWhenPropertyNameIsEmptyShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new PropertySourceOrigin(mock(PropertySource.class), ""))
            .withMessageContaining("PropertyName must not be empty");
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
    assertThat(origin.toString()).isEqualTo("\"foo\" from property source \"test\"");
  }

  @Test
  @SuppressWarnings("unchecked")
  void getWhenPropertySourceSupportsOriginLookupShouldReturnOrigin() {
    Origin origin = mock(Origin.class);
    PropertySource<?> propertySource = mock(PropertySource.class,
            withSettings().extraInterfaces(OriginLookup.class));
    OriginLookup<String> originCapablePropertySource = (OriginLookup<String>) propertySource;
    given(originCapablePropertySource.getOrigin("foo")).willReturn(origin);
    assertThat(PropertySourceOrigin.get(propertySource, "foo")).isSameAs(origin);
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
