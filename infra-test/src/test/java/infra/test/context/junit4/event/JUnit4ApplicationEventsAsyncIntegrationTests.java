/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.junit4.event;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.test.context.event.ApplicationEvents;
import infra.test.context.event.RecordApplicationEvents;
import infra.test.context.junit4.InfraRunner;
import infra.test.context.junit4.event.JUnit4ApplicationEventsIntegrationTests.CustomEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationEvents} that record async events
 * or assert the events from a separate thread, in conjunction with JUnit 4.
 *
 * @author Simon Baslé
 */
@RunWith(InfraRunner.class)
@RecordApplicationEvents
public class JUnit4ApplicationEventsAsyncIntegrationTests {

  @Rule
  public final TestName testName = new TestName();

  @Autowired
  ApplicationContext context;

  @Autowired
  ApplicationEvents applicationEvents;

  @Test
  public void asyncPublication() throws InterruptedException {
    Thread t = new Thread(() -> context.publishEvent(new CustomEvent("async")));
    t.start();
    t.join();

    assertThat(this.applicationEvents.stream(CustomEvent.class))
            .singleElement()
            .extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
            .isEqualTo("async");
  }

  @Test
  public void asyncConsumption() {
    context.publishEvent(new CustomEvent("sync"));

    Awaitility.await().atMost(Durations.ONE_SECOND)
            .untilAsserted(() -> assertThat(this.applicationEvents.stream(CustomEvent.class))
                    .singleElement()
                    .extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
                    .isEqualTo("sync"));
  }

  @Configuration
  static class Config {
  }
}
