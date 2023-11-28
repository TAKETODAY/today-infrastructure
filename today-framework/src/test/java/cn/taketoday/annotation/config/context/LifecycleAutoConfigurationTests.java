/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.context.support.DefaultLifecycleProcessor;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 22:06
 */
class LifecycleAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(LifecycleAutoConfiguration.class));

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