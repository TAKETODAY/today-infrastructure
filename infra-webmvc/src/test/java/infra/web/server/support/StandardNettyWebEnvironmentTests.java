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

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 22:01
 */
class StandardNettyWebEnvironmentTests {

  @Test
  void shouldCreateStandardNettyWebEnvironment() {
    // when
    StandardNettyWebEnvironment environment = new StandardNettyWebEnvironment();

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(ConfigurableNettyWebEnvironment.class);
    assertThat(environment).isInstanceOf(StandardEnvironment.class);
  }

  @Test
  void shouldCreateStandardNettyWebEnvironmentWithPropertySources() {
    // given
    PropertySources propertySources = mock(PropertySources.class);

    // when
    StandardNettyWebEnvironment environment = new StandardNettyWebEnvironment(propertySources);

    // then
    assertThat(environment).isNotNull();
    assertThat(environment).isInstanceOf(ConfigurableNettyWebEnvironment.class);
    assertThat(environment).isInstanceOf(StandardEnvironment.class);
  }

}