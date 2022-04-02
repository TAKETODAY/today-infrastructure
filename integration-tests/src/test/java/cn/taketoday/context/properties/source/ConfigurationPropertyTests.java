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

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationProperty}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertyTests {

  private static final ConfigurationPropertyName NAME = ConfigurationPropertyName.of("foo");

  private ConfigurationPropertySource source = ConfigurationPropertySource.from(mock(PropertySource.class));

  @Test
  void createWhenNameIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurationProperty(null, "bar", null))
            .withMessageContaining("Name must not be null");
  }

  @Test
  void createWhenValueIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurationProperty(NAME, null, null))
            .withMessageContaining("Value must not be null");
  }

  @Test
  void getNameShouldReturnName() {
    ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", null);
    assertThat((Object) property.getName()).isEqualTo(NAME);
  }

  @Test
  void getValueShouldReturnValue() {
    ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", null);
    assertThat(property.getValue()).isEqualTo("foo");
  }

  @Test
  void getPropertyOriginShouldReturnValuePropertyOrigin() {
    Origin origin = mock(Origin.class);
    OriginProvider property = ConfigurationProperty.of(this.source, NAME, "foo", origin);
    assertThat(property.getOrigin()).isEqualTo(origin);
  }

  @Test
  void getPropertySourceShouldReturnPropertySource() {
    Origin origin = mock(Origin.class);
    ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", origin);
    assertThat(property.getSource()).isEqualTo(this.source);
  }

  @Test
  void equalsAndHashCode() {
    ConfigurationProperty property1 = new ConfigurationProperty(ConfigurationPropertyName.of("foo"), "bar", null);
    ConfigurationProperty property2 = new ConfigurationProperty(ConfigurationPropertyName.of("foo"), "bar", null);
    ConfigurationProperty property3 = new ConfigurationProperty(ConfigurationPropertyName.of("foo"), "baz", null);
    ConfigurationProperty property4 = new ConfigurationProperty(ConfigurationPropertyName.of("baz"), "bar", null);
    assertThat(property1.hashCode()).isEqualTo(property2.hashCode());
    assertThat(property1).isEqualTo(property2).isNotEqualTo(property3).isNotEqualTo(property4);
  }

  @Test
  void toStringShouldReturnValue() {
    ConfigurationProperty property = ConfigurationProperty.of(this.source, NAME, "foo", null);
    assertThat(property.toString()).contains("name").contains("value");
  }

}
