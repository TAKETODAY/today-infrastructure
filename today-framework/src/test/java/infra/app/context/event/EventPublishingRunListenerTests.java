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

package infra.app.context.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import infra.app.context.event.ApplicationContextInitializedEvent;
import infra.app.context.event.ApplicationEnvironmentPreparedEvent;
import infra.app.context.event.ApplicationPreparedEvent;
import infra.app.context.event.ApplicationReadyEvent;
import infra.app.context.event.ApplicationStartedEvent;
import infra.app.context.event.ApplicationStartingEvent;
import infra.app.context.event.EventPublishingStartupListener;
import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.context.support.StaticApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.StandardEnvironment;
import infra.app.Application;
import infra.app.DefaultBootstrapContext;
import infra.app.availability.AvailabilityChangeEvent;

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
      assertThat(receivedEvents).extracting("class").contains((Object[]) eventClasses);
    }

  }

}
