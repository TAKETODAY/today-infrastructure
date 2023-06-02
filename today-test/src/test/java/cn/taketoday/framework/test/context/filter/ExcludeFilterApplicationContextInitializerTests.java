/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.context.filter;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.ComponentScan.Filter;
import cn.taketoday.context.annotation.FilterType;
import cn.taketoday.context.annotation.config.TypeExcludeFilter;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.test.context.TestConfiguration;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;

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