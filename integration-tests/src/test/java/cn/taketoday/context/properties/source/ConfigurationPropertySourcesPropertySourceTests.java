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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertySourcesPropertySource}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertySourcesPropertySourceTests {

  private List<ConfigurationPropertySource> configurationSources = new ArrayList<>();

  private ConfigurationPropertySourcesPropertySource propertySource = new ConfigurationPropertySourcesPropertySource(
          "test", this.configurationSources);

  @Test
  void getPropertyShouldReturnValue() {
    this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
    assertThat(this.propertySource.getProperty("foo.bar")).isEqualTo("baz");
  }

  @Test
  void getPropertyWhenNameIsNotValidShouldReturnNull() {
    this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
    assertThat(this.propertySource.getProperty("FOO.B-A-R")).isNull();
    assertThat(this.propertySource.getProperty("FOO.B A R")).isNull();
    assertThat(this.propertySource.getProperty(".foo.bar")).isNull();
  }

  @Test
  void getPropertyWhenMultipleShouldReturnFirst() {
    this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
    this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "bill"));
    assertThat(this.propertySource.getProperty("foo.bar")).isEqualTo("baz");
  }

  @Test
  void getPropertyWhenNoneShouldReturnFirst() {
    this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz"));
    assertThat(this.propertySource.getProperty("foo.foo")).isNull();
  }

  @Test
  void getPropertyOriginShouldReturnOrigin() {
    this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz", "line1"));
    assertThat(this.propertySource.getOrigin("foo.bar").toString()).isEqualTo("line1");
  }

  @Test
  void getPropertyOriginWhenMissingShouldReturnNull() {
    this.configurationSources.add(new MockConfigurationPropertySource("foo.bar", "baz", "line1"));
    assertThat(this.propertySource.getOrigin("foo.foo")).isNull();
  }

  @Test
  void getNameShouldReturnName() {
    assertThat(this.propertySource.getName()).isEqualTo("test");
  }

  @Test
  void getSourceShouldReturnSource() {
    assertThat(this.propertySource.getSource()).isSameAs(this.configurationSources);
  }

}
