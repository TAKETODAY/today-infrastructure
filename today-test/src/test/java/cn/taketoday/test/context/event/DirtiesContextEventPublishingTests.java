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

package cn.taketoday.test.context.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.testkit.engine.EngineTestKit;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.MethodMode;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.event.annotation.AfterTestClass;
import cn.taketoday.test.context.event.annotation.AfterTestExecution;
import cn.taketoday.test.context.event.annotation.AfterTestMethod;
import cn.taketoday.test.context.event.annotation.BeforeTestClass;
import cn.taketoday.test.context.event.annotation.BeforeTestExecution;
import cn.taketoday.test.context.event.annotation.BeforeTestMethod;
import cn.taketoday.test.context.event.annotation.PrepareTestInstance;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Tests for the {@link EventPublishingTestExecutionListener} which verify
 * behavior for test context events when {@link DirtiesContext @DirtiesContext}
 * is used.
 *
 * @author Sam Brannen
 * @since 5.3.17
 * @see https://github.com/spring-projects/spring-framework/issues/27757
 */
class DirtiesContextEventPublishingTests {

	private static final List<Class<? extends TestContextEvent>> events = new ArrayList<>();


	@BeforeEach
	@AfterEach
	void resetEvents() {
		events.clear();
	}

	@Test
	void classLevelDirtiesContext() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(ClassLevelDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class //
		);
	}

	@Test
	void methodLevelAfterMethodDirtiesContext() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(MethodLevelAfterMethodDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class //
			// AfterTestClassEvent.class -- missing b/c of @DirtiestContext "after method" at the method level
		);
	}

	@Test
	void methodLevelAfterMethodDirtiesContextWithSubsequentTestMethod() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(MethodLevelAfterMethodDirtiesContextWithSubsequentTestMethodTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			// test1()
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			// test2()
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class // b/c @DirtiestContext is not applied for test2()
		);
	}

	@Test
	void methodLevelBeforeMethodDirtiesContext() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(MethodLevelBeforeMethodDirtiesContextTestCase.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));

		assertThat(events).containsExactly(//
			// BeforeTestClassEvent.class -- always missing for 1st test class by default
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class // b/c @DirtiestContext happens "before method" at the method level
		);
	}

	@JUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = class-level")
	@DirtiesContext
	static class ClassLevelDirtiesContextTestCase {

		@Test
		void test() {
		}
	}

	@JUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = method-level-after-method")
	static class MethodLevelAfterMethodDirtiesContextTestCase {

		@Test
		@DirtiesContext
		void test1() {
		}
	}

	@JUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = method-level-after-method-with-subsequent-test-method")
	@TestMethodOrder(DisplayName.class)
	static class MethodLevelAfterMethodDirtiesContextWithSubsequentTestMethodTestCase {

		@Test
		@DirtiesContext
		void test1() {
		}

		@Test
		void test2() {
		}
	}

	@JUnitConfig(Config.class)
	// add unique property to get a unique ApplicationContext
	@TestPropertySource(properties = "DirtiesContextEventPublishingTests.key = method-level-before-method")
	static class MethodLevelBeforeMethodDirtiesContextTestCase {

		@Test
		@DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
		void test() {
		}
	}

	@Configuration
	static class Config {

		@BeforeTestClass
		public void beforeTestClass(BeforeTestClassEvent e) {
			events.add(e.getClass());
		}

		@PrepareTestInstance
		public void prepareTestInstance(PrepareTestInstanceEvent e) {
			events.add(e.getClass());
		}

		@BeforeTestMethod
		public void beforeTestMethod(BeforeTestMethodEvent e) {
			events.add(e.getClass());
		}

		@BeforeTestExecution
		public void beforeTestExecution(BeforeTestExecutionEvent e) {
			events.add(e.getClass());
		}

		@AfterTestExecution
		public void afterTestExecution(AfterTestExecutionEvent e) {
			events.add(e.getClass());
		}

		@AfterTestMethod
		public void afterTestMethod(AfterTestMethodEvent e) {
			events.add(e.getClass());
		}

		@AfterTestClass
		public void afterTestClass(AfterTestClassEvent e) {
			events.add(e.getClass());
		}

	}

}
