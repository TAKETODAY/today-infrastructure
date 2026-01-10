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

package infra.app;

import org.junit.jupiter.api.Test;

import infra.beans.BeanWrapper;
import infra.context.properties.source.ConfigurationPropertySources;
import infra.core.env.AbstractEnvironment;
import infra.core.env.ConfigurablePropertyResolver;
import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;
import infra.mock.env.MockPropertySource;

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
