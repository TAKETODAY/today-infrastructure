/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.MapConfigurationPropertySource;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigDataActivationContext}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataActivationContextTests {

  @Test
  void getCloudPlatformWhenCloudPropertyNotPresentDeducesCloudPlatform() {
    MockEnvironment environment = new MockEnvironment();
    Binder binder = Binder.get(environment);
    ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
    assertThat(context.cloudPlatform).isNull();
  }

  @Test
  void getCloudPlatformWhenCloudPropertyInEnvironmentDeducesCloudPlatform() {
    MockEnvironment environment = createKubernetesEnvironment();
    Binder binder = Binder.get(environment);
    ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
    assertThat(context.cloudPlatform).isEqualTo(CloudPlatform.KUBERNETES);
  }

  @Test
  void getCloudPlatformWhenCloudPropertyHasBeenContributedDuringInitialLoadDeducesCloudPlatform() {
    Environment environment = createKubernetesEnvironment();
    Binder binder = new Binder(
            new MapConfigurationPropertySource(Collections.singletonMap("app.main.cloud-platform", "HEROKU")));
    ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
    assertThat(context.cloudPlatform).isEqualTo(CloudPlatform.HEROKU);
  }

  @Test
  void getProfilesWhenWithoutProfilesReturnsNull() {
    MockEnvironment environment = new MockEnvironment();
    Binder binder = Binder.get(environment);
    ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
    assertThat(context.profiles).isNull();
  }

  @Test
  void getProfilesWhenWithProfilesReturnsProfiles() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("a", "b", "c");
    Binder binder = Binder.get(environment);
    ConfigDataActivationContext context = new ConfigDataActivationContext(environment, binder);
    Profiles profiles = new Profiles(environment, binder, null);
    context = context.withProfiles(profiles);
    assertThat(context.profiles).isEqualTo(profiles);
  }

  private MockEnvironment createKubernetesEnvironment() {
    MockEnvironment environment = new MockEnvironment();
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("KUBERNETES_SERVICE_HOST", "host");
    map.put("KUBERNETES_SERVICE_PORT", "port");
    PropertySource<?> propertySource = new MapPropertySource(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, map);
    environment.getPropertySources().addLast(propertySource);
    return environment;
  }

}
