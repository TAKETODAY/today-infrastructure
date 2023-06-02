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

package cn.taketoday.framework.env;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationArguments;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.context.event.ApplicationEnvironmentPreparedEvent;
import cn.taketoday.framework.context.event.ApplicationFailedEvent;
import cn.taketoday.framework.context.event.ApplicationPreparedEvent;
import cn.taketoday.framework.context.event.ApplicationStartingEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link EnvironmentPostProcessorApplicationListener}.
 *
 * @author Phillip Webb
 */
class EnvironmentPostProcessorApplicationListenerTests {

  private final DefaultBootstrapContext bootstrapContext = spy(new DefaultBootstrapContext());

  private final EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener();

  @Test
  void createUsesFactories() {
    EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener();
    assertThat(listener.getPostProcessors(null, this.bootstrapContext)).hasSizeGreaterThan(1);
  }

  @Test
  void supportsEventTypeWhenApplicationEnvironmentPreparedEventReturnsTrue() {
    assertThat(this.listener.supportsEventType(ApplicationEnvironmentPreparedEvent.class)).isTrue();
  }

  @Test
  void supportsEventTypeWhenOtherEventReturnsFalse() {
    assertThat(this.listener.supportsEventType(ApplicationStartingEvent.class)).isFalse();
  }

  @Test
  void onApplicationEventWhenApplicationPreparedEventSwitchesLogs() {
    Application application = mock(Application.class);
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    ApplicationPreparedEvent event = new ApplicationPreparedEvent(application,
            new ApplicationArguments(), context);
    this.listener.onApplicationEvent(event);
  }

  @Test
  void onApplicationEventWhenApplicationFailedEventSwitchesLogs() {
    Application application = mock(Application.class);
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    ApplicationFailedEvent event = new ApplicationFailedEvent(application,
            new ApplicationArguments(), context, new RuntimeException());
    this.listener.onApplicationEvent(event);
  }

}
