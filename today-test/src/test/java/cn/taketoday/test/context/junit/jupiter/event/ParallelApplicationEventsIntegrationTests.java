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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.PayloadApplicationEvent;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.event.ApplicationEvents;
import cn.taketoday.test.context.event.ApplicationEventsHolder;
import cn.taketoday.test.context.event.RecordApplicationEvents;
import cn.taketoday.test.context.event.TestContextEvent;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests that verify parallel execution support for {@link ApplicationEvents}
 * in conjunction with JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class ParallelApplicationEventsIntegrationTests {

  private static final Set<String> payloads = ConcurrentHashMap.newKeySet();

  @Test
  void rejectTestsInParallelWithInstancePerClassAndRecordApplicationEvents() {
    Class<?> testClass = TestInstancePerClassTestCase.class;

    final EngineExecutionResults results = EngineTestKit.engine("junit-jupiter")//
            .selectors(selectClass(testClass))//
            .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
            .configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
            .execute();

    //extract the messages from failed TextExecutionResults
    assertThat(results.containerEvents().failed()//
            .stream().map(e -> e.getRequiredPayload(TestExecutionResult.class)//
                    .getThrowable().get().getMessage()))//
            .singleElement(InstanceOfAssertFactories.STRING)
            .isEqualToIgnoringNewLines("""
                    Test classes or inner classes that @RecordApplicationEvents\s
                    must not be run in parallel with the @TestInstance(Lifecycle.PER_CLASS) configuration.\s
                    Use either @Execution(SAME_THREAD), @TestInstance(PER_METHOD) or disable parallel\s
                    execution altogether. Note that when recording events in parallel, one might see events\s
                    published by other tests as the application context can be common.
                    """);
  }

  @Test
  void executeTestsInParallelInstancePerMethod() {
    Class<?> testClass = TestInstancePerMethodTestCase.class;
    Events testEvents = EngineTestKit.engine("junit-jupiter")//
            .selectors(selectClass(testClass))//
            .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
            .configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
            .execute()//
            .testEvents();
    //list failed events in case of test errors to get a sense of which tests failed
    Events failedTests = testEvents.failed();
    if (failedTests.count() > 0) {
      failedTests.debug();
    }
    testEvents.assertStatistics(stats -> stats.started(13).succeeded(13).failed(0));

    Set<String> testNames = payloads.stream()//
            .map(payload -> payload.substring(0, payload.indexOf("-")))//
            .collect(Collectors.toSet());

    assertThat(payloads).hasSize(10);
    assertThat(testNames).hasSize(10);

    // The following assertion is currently commented out, since it fails
    // regularly on the CI server due to only 1 thread being used for
    // parallel test execution on the CI server.
		/*
		Set<String> threadNames = payloads.stream()//
				.map(payload -> payload.substring(payload.indexOf("-")))//
				.collect(Collectors.toSet());
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		// Skip the following assertion entirely if too few processors are available
		// to the current JVM.
		if (availableProcessors >= 6) {
			// There are probably 10 different thread names on a developer's machine,
			// but we really just want to assert that at least two different threads
			// were used, since the CI server often has fewer threads available.
			assertThat(threadNames)
				.as("number of threads used with " + availableProcessors + " available processors")
				.hasSizeGreaterThanOrEqualTo(2);
		}
		*/
  }

  @AfterEach
  void resetPayloads() {
    payloads.clear();
  }

  @JUnitConfig
  @RecordApplicationEvents
  @Execution(ExecutionMode.CONCURRENT)
  @TestInstance(Lifecycle.PER_METHOD)
  static class TestInstancePerMethodTestCase {

    @Autowired
    ApplicationContext context;

    @Autowired
    ApplicationEvents events;

    @Test
    void test1(TestInfo testInfo) {
      assertTestExpectations(this.events, testInfo);
    }

    @Test
    void test2(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void test3(TestInfo testInfo) {
      assertTestExpectations(this.events, testInfo);
    }

    @Test
    void test4(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void test5(TestInfo testInfo) {
      assertTestExpectations(this.events, testInfo);
    }

    @Test
    void test6(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void test7(TestInfo testInfo) {
      assertTestExpectations(this.events, testInfo);
    }

    @Test
    void test8(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void test9(TestInfo testInfo) {
      assertTestExpectations(this.events, testInfo);
    }

    @Test
    void test10(ApplicationEvents events, TestInfo testInfo) {
      assertTestExpectations(events, testInfo);
    }

    @Test
    void compareToApplicationEventsHolder(ApplicationEvents applicationEvents) {
      ApplicationEvents fromThreadHolder = ApplicationEventsHolder.getRequiredApplicationEvents();
      assertThat(fromThreadHolder.stream())
              .hasSameElementsAs(this.events.stream().toList())
              .hasSameElementsAs(applicationEvents.stream().toList());
    }

    @Test
    void asyncPublication(ApplicationEvents events) throws InterruptedException {
      final ExecutorService executorService = Executors.newSingleThreadExecutor();
      executorService.execute(() -> this.context.publishEvent("asyncPublication"));
      executorService.shutdown();
      executorService.awaitTermination(10, TimeUnit.SECONDS);

      assertThat(events.stream().filter(e -> !(e instanceof TestContextEvent))
              .map(e -> (e instanceof PayloadApplicationEvent<?> pae ? pae.getPayload().toString() : e.toString())))
              .containsExactly("asyncPublication");
    }

    @Test
    void asyncConsumption() {
      this.context.publishEvent("asyncConsumption");

      Awaitility.await().atMost(Durations.ONE_SECOND).untilAsserted(() ->//
              assertThat(ApplicationEventsHolder//
                      .getRequiredApplicationEvents()//
                      .stream()//
                      .filter(e -> !(e instanceof TestContextEvent))//
                      .map(e -> (e instanceof PayloadApplicationEvent<?> pae ? pae.getPayload().toString() : e.toString()))//
              ).containsExactly("asyncConsumption"));
    }

    private void assertTestExpectations(ApplicationEvents events, TestInfo testInfo) {
      String testName = testInfo.getTestMethod().get().getName();
      String threadName = Thread.currentThread().getName();
      String localPayload = testName + "-" + threadName;
      context.publishEvent(localPayload);
      assertPayloads(events.stream(String.class), localPayload);
    }

    private static void assertPayloads(Stream<String> events, String... values) {
      assertThat(events.peek(payloads::add)).extracting(Object::toString).containsExactly(values);
    }

    @Configuration
    static class Config {
    }

  }

  @TestInstance(Lifecycle.PER_CLASS)
  static class TestInstancePerClassTestCase extends TestInstancePerMethodTestCase {
  }

}
