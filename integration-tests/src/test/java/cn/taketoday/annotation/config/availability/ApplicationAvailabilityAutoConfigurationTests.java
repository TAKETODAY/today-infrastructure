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

package cn.taketoday.annotation.config.availability;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.LazyInitializationBeanFactoryPostProcessor;
import cn.taketoday.framework.availability.ApplicationAvailability;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.framework.availability.ReadinessState;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

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