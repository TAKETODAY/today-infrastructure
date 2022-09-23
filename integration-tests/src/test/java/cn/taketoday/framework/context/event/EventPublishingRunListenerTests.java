/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.context.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EventPublishingStartupListener}
 *
 * @author Brian Clozel
 */
class EventPublishingRunListenerTests {

  @Test
  void shouldPublishLifecycleEvents() {
    DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
    StaticApplicationContext context = new StaticApplicationContext();
    TestApplicationListener applicationListener = new TestApplicationListener();
    Application application = mock(Application.class);
    given(application.getListeners()).willReturn(Collections.singleton(applicationListener));
    EventPublishingStartupListener publishingListener = new EventPublishingStartupListener(application, null);
    applicationListener.assertReceivedNoEvents();
    publishingListener.starting(bootstrapContext, null, null);
    applicationListener.assertReceivedEvent(ApplicationStartingEvent.class);
    publishingListener.environmentPrepared(bootstrapContext, null);
    applicationListener.assertReceivedEvent(ApplicationEnvironmentPreparedEvent.class);
    publishingListener.contextPrepared(context);
    applicationListener.assertReceivedEvent(ApplicationContextInitializedEvent.class);
    publishingListener.contextLoaded(context);
    applicationListener.assertReceivedEvent(ApplicationPreparedEvent.class);
    context.refresh();
    publishingListener.started(context, null);
    applicationListener.assertReceivedEvent(ApplicationStartedEvent.class, AvailabilityChangeEvent.class);
    publishingListener.ready(context, null);
    applicationListener.assertReceivedEvent(ApplicationReadyEvent.class, AvailabilityChangeEvent.class);
  }

  @Test
  void initialEventListenerCanAddAdditionalListenersToApplication() {
    Application application = new Application();
    DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
    ConfigurableEnvironment environment = new StandardEnvironment();
    TestApplicationListener lateAddedApplicationListener = new TestApplicationListener();
    ApplicationListener<ApplicationStartingEvent> listener = (event) -> event.getApplication()
            .addListeners(lateAddedApplicationListener);
    application.addListeners(listener);
    EventPublishingStartupListener runListener = new EventPublishingStartupListener(application, null);
    runListener.starting(bootstrapContext, null, null);
    runListener.environmentPrepared(bootstrapContext, environment);
    lateAddedApplicationListener.assertReceivedEvent(ApplicationEnvironmentPreparedEvent.class);
  }

  static class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

    private final Deque<ApplicationEvent> events = new ArrayDeque<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
      this.events.add(event);
    }

    void assertReceivedNoEvents() {
      assertThat(this.events).isEmpty();
    }

    void assertReceivedEvent(Class<?>... eventClasses) {
      List<ApplicationEvent> receivedEvents = new ArrayList<>();
      while (!this.events.isEmpty()) {
        receivedEvents.add(this.events.pollFirst());
      }
      assertThat(receivedEvents).extracting("class").contains(eventClasses);
    }

  }

}
