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

package infra.app.config.availability;

import org.junit.jupiter.api.Test;

import infra.app.LazyInitializationBeanFactoryPostProcessor;
import infra.app.availability.ApplicationAvailability;
import infra.app.availability.AvailabilityChangeEvent;
import infra.app.availability.ReadinessState;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/27 21:30
 */
class ApplicationAvailabilityAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ApplicationAvailabilityAutoConfiguration.class));

  @Test
  void providerIsPresentWhenNotRegistered() {
    this.contextRunner.run(((context) -> assertThat(context).hasSingleBean(ApplicationAvailability.class)
            .hasBean("applicationAvailability")));
  }

  @Test
  void providerIsNotConfiguredWhenCustomOneIsPresent() {
    this.contextRunner
            .withBean("customApplicationAvailability", ApplicationAvailability.class,
                    () -> mock(ApplicationAvailability.class))
            .run(((context) -> assertThat(context).hasSingleBean(ApplicationAvailability.class)
                    .hasBean("customApplicationAvailability")));
  }

  @Test
  void whenLazyInitializationIsEnabledApplicationAvailabilityBeanShouldStillReceiveAvailabilityChangeEvents() {
    this.contextRunner.withBean(LazyInitializationBeanFactoryPostProcessor.class).run((context) -> {
      AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
      ApplicationAvailability applicationAvailability = context.getBean(ApplicationAvailability.class);
      assertThat(applicationAvailability.getLastChangeEvent(ReadinessState.class)).isNotNull();
    });
  }

}