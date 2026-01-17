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