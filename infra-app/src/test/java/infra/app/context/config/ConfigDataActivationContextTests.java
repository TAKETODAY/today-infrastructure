/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.context.config;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import infra.app.cloud.CloudPlatform;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.MapConfigurationPropertySource;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.mock.env.MockEnvironment;

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
