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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationPropertyCaching}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertyCachingTests {

  private StandardEnvironment environment;

  private MapPropertySource propertySource;

  @BeforeEach
  void setup() {
    this.environment = new StandardEnvironment();
    this.propertySource = new MapPropertySource("test", Collections.singletonMap("spring", "boot"));
    this.environment.getPropertySources().addLast(this.propertySource);
  }

  @Test
  void getFromEnvironmentReturnsCaching() {
    ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(this.environment);
    assertThat(caching).isInstanceOf(ConfigurationPropertySourcesCaching.class);
  }

  @Test
  void getFromEnvironmentForUnderlyingSourceReturnsCaching() {
    ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(this.environment, this.propertySource);
    assertThat(caching).isInstanceOf(SoftReferenceConfigurationPropertyCache.class);
  }

  @Test
  void getFromSourcesWhenSourcesIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ConfigurationPropertyCaching.get((Iterable<ConfigurationPropertySource>) null))
            .withMessage("Sources must not be null");
  }

  @Test
  void getFromSourcesReturnsCachingComposite() {
    List<ConfigurationPropertySource> sources = new ArrayList<>();
    sources.add(DefaultConfigurationPropertySource.from(this.propertySource));
    ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(sources);
    assertThat(caching).isInstanceOf(ConfigurationPropertySourcesCaching.class);
  }

  @Test
  void getFromSourcesForUnderlyingSourceReturnsCaching() {
    List<ConfigurationPropertySource> sources = new ArrayList<>();
    sources.add(DefaultConfigurationPropertySource.from(this.propertySource));
    ConfigurationPropertyCaching caching = ConfigurationPropertyCaching.get(sources, this.propertySource);
    assertThat(caching).isInstanceOf(SoftReferenceConfigurationPropertyCache.class);
  }

  @Test
  void getFromSourcesForUnderlyingSourceWhenCantFindThrowsException() {
    List<ConfigurationPropertySource> sources = new ArrayList<>();
    sources.add(DefaultConfigurationPropertySource.from(this.propertySource));
    MapPropertySource anotherPropertySource = new MapPropertySource("test2", Collections.emptyMap());
    assertThatIllegalStateException()
            .isThrownBy(() -> ConfigurationPropertyCaching.get(sources, anotherPropertySource))
            .withMessage("Unable to find cache from configuration property sources");
  }

}
