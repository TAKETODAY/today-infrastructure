/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server;

import org.junit.jupiter.api.Test;

import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;
import infra.web.context.ConfigurableWebEnvironment;
import infra.web.context.StandardWebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:01
 */
class StandardWebEnvironmentTests {

  @Test
  void shouldCreateStandardWebEnvironment() {
    // when
    StandardWebEnvironment environment = new StandardWebEnvironment();

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(ConfigurableWebEnvironment.class);
    assertThat(environment).isInstanceOf(StandardEnvironment.class);
  }

  @Test
  void shouldCreateStandardWebEnvironmentWithPropertySources() {
    // given
    PropertySources propertySources = mock(PropertySources.class);

    // when
    StandardWebEnvironment environment = new StandardWebEnvironment(propertySources);

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(ConfigurableWebEnvironment.class);
    assertThat(environment).isInstanceOf(StandardEnvironment.class);
  }

}