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

package cn.taketoday.framework;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.core.env.AbstractEnvironment;
import cn.taketoday.core.env.ConfigurablePropertyResolver;
import cn.taketoday.core.env.MockPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 22:22
 */
public abstract class AbstractApplicationEnvironmentTests {

  @Test
  void getActiveProfilesDoesNotResolveProperty() {
    StandardEnvironment environment = createEnvironment();
    new MockPropertySource().withProperty("", "");
    environment.getPropertySources().addFirst(
            new MockPropertySource().withProperty(AbstractEnvironment.KEY_ACTIVE_PROFILES, "test"));
    assertThat(environment.getActiveProfiles()).isEmpty();
  }

  @Test
  void getDefaultProfilesDoesNotResolveProperty() {
    StandardEnvironment environment = createEnvironment();
    new MockPropertySource().withProperty("", "");
    environment.getPropertySources().addFirst(
            new MockPropertySource().withProperty(AbstractEnvironment.KEY_DEFAULT_PROFILES, "test"));
    assertThat(environment.getDefaultProfiles()).containsExactly("default");
  }

  @Test
  void propertyResolverIsOptimizedForConfigurationProperties() {
    StandardEnvironment environment = createEnvironment();
    ConfigurablePropertyResolver expected = ConfigurationPropertySources.createPropertyResolver(new PropertySources());
    Object propertyResolver = BeanWrapper.forDirectFieldAccess(environment)
            .getPropertyValue("propertyResolver");
    assertThat(propertyResolver).isInstanceOf(expected.getClass());
  }

  protected abstract StandardEnvironment createEnvironment();

}
