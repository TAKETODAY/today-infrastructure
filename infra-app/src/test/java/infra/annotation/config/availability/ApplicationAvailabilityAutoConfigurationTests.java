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

package infra.annotation.config.availability;

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