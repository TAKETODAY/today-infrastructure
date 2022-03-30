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

package cn.taketoday.test.context.junit.jupiter.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.event.ApplicationEvents;
import cn.taketoday.test.context.event.RecordApplicationEvents;
import cn.taketoday.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests that verify parallel execution support for {@link ApplicationEvents}
 * in conjunction with JUnit Jupiter.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
class ParallelApplicationEventsIntegrationTests {

	private static final Set<String> payloads = ConcurrentHashMap.newKeySet();


	@ParameterizedTest
	@ValueSource(classes = {TestInstancePerMethodTestCase.class, TestInstancePerClassTestCase.class})
	void executeTestsInParallel(Class<?> testClass) {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(testClass))//
				.configurationParameter("junit.jupiter.execution.parallel.enabled", "true")//
				.configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "10")//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(10).succeeded(10).failed(0));

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


	@SpringJUnitConfig
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
