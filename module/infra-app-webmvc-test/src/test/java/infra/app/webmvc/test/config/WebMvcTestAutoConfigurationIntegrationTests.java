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

package infra.app.webmvc.test.config;

import org.junit.jupiter.api.Test;

import infra.app.config.task.TaskExecutionAutoConfiguration;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.core.task.AsyncTaskExecutor;
import infra.web.async.WebAsyncManagerFactory;

import static infra.app.config.AutoConfigurationImportedCondition.importedAutoConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the auto-configuration imported by {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 * @author Levi Puot Paul
 * @author Madhura Bhave
 */
@WebMvcTest
class WebMvcTestAutoConfigurationIntegrationTests {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void taskExecutionAutoConfigurationWasImported() {
    assertThat(this.applicationContext).has(importedAutoConfiguration(TaskExecutionAutoConfiguration.class));
  }

  @Test
  void asyncTaskExecutorWithApplicationTaskExecutor() {
    assertThat(this.applicationContext.getBeansOfType(AsyncTaskExecutor.class)).hasSize(1);
    assertThat(this.applicationContext.getBean(WebAsyncManagerFactory.class)).extracting("taskExecutor")
            .isSameAs(this.applicationContext.getBean("applicationTaskExecutor"));
  }

}
