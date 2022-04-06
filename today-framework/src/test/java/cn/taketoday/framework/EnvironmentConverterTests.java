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

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.context.support.MockEnvironment;
import cn.taketoday.core.conversion.support.ConfigurableConversionService;
import cn.taketoday.core.env.AbstractEnvironment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.web.context.support.StandardServletEnvironment;

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
            getClass().getClassLoader(), originalEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment.getActiveProfiles()).containsExactly("activeProfile1", "activeProfile2");
  }

  @Test
  void convertedEnvironmentHasSameConversionService() {
    AbstractEnvironment originalEnvironment = new MockEnvironment();
    ConfigurableConversionService conversionService = mock(ConfigurableConversionService.class);
    originalEnvironment.setConversionService(conversionService);
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            getClass().getClassLoader(), originalEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment.getConversionService()).isEqualTo(conversionService);
  }

  @Test
  void envClassSameShouldReturnEnvironmentUnconverted() {
    StandardEnvironment standardEnvironment = new StandardEnvironment();
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            getClass().getClassLoader(), standardEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment).isSameAs(standardEnvironment);
  }

  @Test
  void standardServletEnvironmentIsConverted() {
    StandardServletEnvironment standardServletEnvironment = new StandardServletEnvironment();
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            getClass().getClassLoader(), standardServletEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment).isNotSameAs(standardServletEnvironment);
  }

  @Test
  void servletPropertySourcesAreNotCopiedOverIfNotWebEnvironment() {
    StandardServletEnvironment standardServletEnvironment = new StandardServletEnvironment();
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            getClass().getClassLoader(), standardServletEnvironment, StandardEnvironment.class);
    assertThat(convertedEnvironment).isNotSameAs(standardServletEnvironment);
    Set<String> names = new HashSet<>();
    for (PropertySource<?> propertySource : convertedEnvironment.getPropertySources()) {
      names.add(propertySource.getName());
    }
    assertThat(names).doesNotContain(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
            StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
            StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME);
  }

  @Test
  void envClassSameShouldReturnEnvironmentUnconvertedEvenForWeb() {
    StandardServletEnvironment standardServletEnvironment = new StandardServletEnvironment();
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            getClass().getClassLoader(), standardServletEnvironment, StandardServletEnvironment.class);
    assertThat(convertedEnvironment).isSameAs(standardServletEnvironment);
  }

  @Test
  void servletPropertySourcesArePresentWhenTypeToConvertIsWeb() {
    StandardEnvironment standardEnvironment = new StandardEnvironment();
    StandardEnvironment convertedEnvironment = EnvironmentConverter.convertIfNecessary(
            getClass().getClassLoader(), standardEnvironment, StandardServletEnvironment.class);
    assertThat(convertedEnvironment).isNotSameAs(standardEnvironment);
    Set<String> names = new HashSet<>();
    for (PropertySource<?> propertySource : convertedEnvironment.getPropertySources()) {
      names.add(propertySource.getName());
    }
    assertThat(names).contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
            StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME);
  }

}