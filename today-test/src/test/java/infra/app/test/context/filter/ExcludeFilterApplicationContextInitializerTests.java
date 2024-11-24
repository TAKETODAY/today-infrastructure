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

package infra.app.test.context.filter;

import org.junit.jupiter.api.Test;

import infra.app.Application;
import infra.app.ApplicationType;
import infra.app.test.context.TestConfiguration;
import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.context.annotation.ComponentScan;
import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.FilterType;
import infra.context.annotation.config.TypeExcludeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/27 20:25
 */
class ExcludeFilterApplicationContextInitializerTests {

  @Test
  void testConfigurationIsExcluded() {
    Application application = new Application(TestApplication.class);
    application.setApplicationType(ApplicationType.NORMAL);
    AssertableApplicationContext applicationContext = AssertableApplicationContext.get(application::run);
    assertThat(applicationContext).hasSingleBean(TestApplication.class);
    assertThat(applicationContext).doesNotHaveBean(ExcludedTestConfiguration.class);
  }

  @ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class) })
  static class TestApplication {

  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ExcludedTestConfiguration {

  }

}