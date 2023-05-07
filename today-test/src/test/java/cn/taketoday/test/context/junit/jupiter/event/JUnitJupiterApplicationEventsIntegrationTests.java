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

package cn.taketoday.test.context.junit.jupiter.event;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;

import java.util.stream.Stream;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.event.ApplicationEvents;
import cn.taketoday.test.context.event.RecordApplicationEvents;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@RecordApplicationEvents
class JUnitJupiterApplicationEventsIntegrationTests {

  @Autowired
  ApplicationContext context;

  @Autowired
  ApplicationEvents applicationEvents;

  @Nested
  @TestInstance(PER_METHOD)
  class TestInstancePerMethodTests {

    @BeforeEach
    void beforeEach() {
      assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent");
      context.publishEvent(new CustomEvent("beforeEach"));
      assertCustomEvents(applicationEvents, "beforeEach");
      assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent");
    }

    @Test
    void test1(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void test2(@Autowired ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
      String testName = testInfo.getTestMethod().get().getName();

      assertEventTypes(events, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
              "BeforeTestExecutionEvent");
      context.publishEvent(new CustomEvent(testName));
      context.publishEvent("payload1");
      context.publishEvent("payload2");
      assertCustomEvents(events, "beforeEach", testName);
      assertPayloads(events.stream(String.class), "payload1", "payload2");
    }

    @AfterEach
    void afterEach(@Autowired ApplicationEvents events, TestInfo testInfo) {
      String testName = testInfo.getTestMethod().get().getName();

      assertEventTypes(events, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
              "BeforeTestExecutionEvent", "CustomEvent", "PayloadApplicationEvent", "PayloadApplicationEvent",
              "AfterTestExecutionEvent");
      context.publishEvent(new CustomEvent("afterEach"));
      assertCustomEvents(events, "beforeEach", testName, "afterEach");
      assertEventTypes(events, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
              "BeforeTestExecutionEvent", "CustomEvent", "PayloadApplicationEvent", "PayloadApplicationEvent",
              "AfterTestExecutionEvent", "CustomEvent");
    }
  }

  @Nested
  @TestInstance(PER_METHOD)
  class TestInstancePerMethodWithClearedEventsTests {

    @BeforeEach
    void beforeEach() {
      assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent");
      context.publishEvent(new CustomEvent("beforeEach"));
      assertCustomEvents(applicationEvents, "beforeEach");
      assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent");
      applicationEvents.clear();
      assertThat(applicationEvents.stream()).isEmpty();
    }

    @Test
    void test1(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void test2(@Autowired ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
      String testName = testInfo.getTestMethod().get().getName();

      assertEventTypes(events, "BeforeTestExecutionEvent");
      context.publishEvent(new CustomEvent(testName));
      assertCustomEvents(events, testName);
      assertEventTypes(events, "BeforeTestExecutionEvent", "CustomEvent");
    }

    @AfterEach
    void afterEach(@Autowired ApplicationEvents events, TestInfo testInfo) {
      events.clear();
      context.publishEvent(new CustomEvent("afterEach"));
      assertCustomEvents(events, "afterEach");
      assertEventTypes(events, "CustomEvent");
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  class TestInstancePerClassTests {

    private boolean testAlreadyExecuted = false;

    @BeforeEach
    void beforeEach(TestInfo testInfo) {
      if (!testAlreadyExecuted) {
        assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
                "BeforeTestMethodEvent");
      }
      else {
        assertEventTypes(applicationEvents, "BeforeTestMethodEvent");
      }

      context.publishEvent(new CustomEvent("beforeEach"));
      assertCustomEvents(applicationEvents, "beforeEach");

      if (!testAlreadyExecuted) {
        assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
                "BeforeTestMethodEvent", "CustomEvent");
      }
      else {
        assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent");
      }
    }

    @Test
    void test1(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void test2(@Autowired ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
      String testName = testInfo.getTestMethod().get().getName();

      if (!testAlreadyExecuted) {
        assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
                "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent");
      }
      else {
        assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent");
      }

      context.publishEvent(new CustomEvent(testName));
      assertCustomEvents(events, "beforeEach", testName);

      if (!testAlreadyExecuted) {
        assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
                "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent", "CustomEvent");
      }
      else {
        assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
                "CustomEvent");
      }
    }

    @AfterEach
    void afterEach(@Autowired ApplicationEvents events, TestInfo testInfo) {
      String testName = testInfo.getTestMethod().get().getName();

      if (!testAlreadyExecuted) {
        assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
                "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent", "CustomEvent",
                "AfterTestExecutionEvent");
      }
      else {
        assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
                "CustomEvent", "AfterTestExecutionEvent");
      }

      context.publishEvent(new CustomEvent("afterEach"));
      assertCustomEvents(events, "beforeEach", testName, "afterEach");

      if (!testAlreadyExecuted) {
        assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestClassEvent",
                "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent", "CustomEvent",
                "AfterTestExecutionEvent", "CustomEvent");
        testAlreadyExecuted = true;
      }
      else {
        assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
                "CustomEvent", "AfterTestExecutionEvent", "CustomEvent");
      }
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  class AsyncEventTests {

    @Autowired
    ApplicationEvents applicationEvents;

    @Test
    void asyncPublication() throws InterruptedException {
      Thread t = new Thread(() -> context.publishEvent(new CustomEvent("async")));
      t.start();
      t.join();

      assertThat(this.applicationEvents.stream(CustomEvent.class))
              .singleElement()
              .extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
              .isEqualTo("async");
    }

    @Test
    void asyncConsumption() {
      context.publishEvent(new CustomEvent("sync"));

      Awaitility.await().atMost(Durations.ONE_SECOND)
              .untilAsserted(() -> assertThat(assertThat(this.applicationEvents.stream(CustomEvent.class))
                      .singleElement()
                      .extracting(CustomEvent::getMessage, InstanceOfAssertFactories.STRING)
                      .isEqualTo("sync")));
    }

  }

  private static void assertEventTypes(ApplicationEvents applicationEvents, String... types) {
    assertThat(applicationEvents.stream().map(event -> event.getClass().getSimpleName()))
            .containsExactly(types);
  }

  private static void assertPayloads(Stream<String> events, String... values) {
    assertThat(events).extracting(Object::toString).containsExactly(values);
  }

  private static void assertCustomEvents(ApplicationEvents events, String... messages) {
    assertThat(events.stream(CustomEvent.class)).extracting(CustomEvent::getMessage).containsExactly(messages);
  }

  @Configuration
  static class Config {
  }

  @SuppressWarnings("serial")
  static class CustomEvent extends ApplicationEvent {

    private final String message;

    CustomEvent(String message) {
      super(message);
      this.message = message;
    }

    String getMessage() {
      return message;
    }
  }

}
