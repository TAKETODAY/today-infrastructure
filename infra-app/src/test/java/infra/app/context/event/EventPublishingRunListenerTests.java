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

package infra.app.context.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import infra.app.Application;
import infra.app.DefaultBootstrapContext;
import infra.app.availability.AvailabilityChangeEvent;
import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.context.support.StaticApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.StandardEnvironment;

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
