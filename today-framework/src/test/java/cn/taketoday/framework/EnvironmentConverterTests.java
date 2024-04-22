/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.conversion.support.ConfigurableConversionService;
import cn.taketoday.core.env.AbstractEnvironment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.mock.env.MockEnvironment;

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
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            originalEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment.getActiveProfiles()).containsExactly("activeProfile1", "activeProfile2");
  }

  @Test
  void convertedEnvironmentHasSameConversionService() {
    AbstractEnvironment originalEnvironment = new MockEnvironment();
    ConfigurableConversionService conversionService = mock(ConfigurableConversionService.class);
    originalEnvironment.setConversionService(conversionService);
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            originalEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment.getConversionService()).isEqualTo(conversionService);
  }

  @Test
  void envClassSameShouldReturnEnvironmentUnconverted() {
    StandardEnvironment standardEnvironment = new StandardEnvironment();
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            standardEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment).isSameAs(standardEnvironment);
  }

}
