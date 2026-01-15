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

package infra.app.config.context;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.support.AbstractApplicationContext;
import infra.context.support.DefaultLifecycleProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 22:06
 */
class LifecycleAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(infra.app.config.context.LifecycleAutoConfiguration.class));

  @Test
  void lifecycleProcessorIsConfiguredWithDefaultTimeout() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
      Object processor = context.getBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
      assertThat(processor).extracting("timeoutPerShutdownPhase").isEqualTo(30000L);
    });
  }

  @Test
  void lifecycleProcessorIsConfiguredWithCustomTimeout() {
    this.contextRunner.withPropertyValues("infra.lifecycle.timeout-per-shutdown-phase=15s").run((context) -> {
      assertThat(context).hasBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
      Object processor = context.getBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
      assertThat(processor).extracting("timeoutPerShutdownPhase").isEqualTo(15000L);
    });
  }

  @Test
  void lifecycleProcessorIsConfiguredWithCustomTimeoutInAChildContext() {
    new ApplicationContextRunner().run((parent) -> {
      this.contextRunner.withParent(parent).withPropertyValues("infra.lifecycle.timeout-per-shutdown-phase=15s")
              .run((child) -> {
                assertThat(child).hasBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
                Object processor = child.getBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
                assertThat(processor).extracting("timeoutPerShutdownPhase").isEqualTo(15000L);
              });
    });
  }

  @Test
  void whenUserDefinesALifecycleProcessorBeanThenTheAutoConfigurationBacksOff() {
    this.contextRunner.withUserConfiguration(LifecycleProcessorConfiguration.class).run((context) -> {
      assertThat(context).hasBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
      Object processor = context.getBean(AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME);
      assertThat(processor).extracting("timeoutPerShutdownPhase").isEqualTo(5000L);
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class LifecycleProcessorConfiguration {

    @Bean(name = AbstractApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME)
    DefaultLifecycleProcessor customLifecycleProcessor() {
      DefaultLifecycleProcessor processor = new DefaultLifecycleProcessor();
      processor.setTimeoutPerShutdownPhase(5000);
      return processor;
    }

  }

}