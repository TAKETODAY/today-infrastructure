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

import infra.core.conversion.support.ConfigurableConversionService;
import infra.core.env.AbstractEnvironment;
import infra.core.env.StandardEnvironment;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/6 16:23
 */
class EnvironmentConverterTests {

  @Test
  void convertedEnvironmentHasSameActiveProfiles() {
    AbstractEnvironment originalEnvironment = new MockEnvironment();
    originalEnvironment.setActiveProfiles("activeProfile1", "activeProfile2");
    var convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            originalEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment.getActiveProfiles()).containsExactly("activeProfile1", "activeProfile2");
  }

  @Test
  void convertedEnvironmentHasSameConversionService() {
    AbstractEnvironment originalEnvironment = new MockEnvironment();
    ConfigurableConversionService conversionService = mock(ConfigurableConversionService.class);
    originalEnvironment.setConversionService(conversionService);
    var convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            originalEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment.getConversionService()).isEqualTo(conversionService);
  }

  @Test
  void envClassSameShouldReturnEnvironmentUnconverted() {
    StandardEnvironment standardEnvironment = new StandardEnvironment();
    var convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            standardEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment).isSameAs(standardEnvironment);
  }

}
