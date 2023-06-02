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

package cn.taketoday.test.context.junit4.event;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.event.ApplicationEvents;
import cn.taketoday.test.context.event.RecordApplicationEvents;
import cn.taketoday.test.context.junit4.InfraRunner;
import cn.taketoday.test.context.junit4.event.JUnit4ApplicationEventsIntegrationTests.CustomEvent;

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
            .untilAsserted(() -> assertThat(assertThat(this.applicationEvents.stream(CustomEvent.class))
                    .singleElement()
                    .extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
                    .isEqualTo("sync")));
  }

  @Configuration
  static class Config {
  }
}
