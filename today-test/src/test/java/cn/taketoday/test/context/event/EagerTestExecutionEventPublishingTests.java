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
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.TestExecutionListeners.MergeMode;
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
 * Tests for the {@link EventPublishingTestExecutionListener} which verify that
 * a {@link BeforeTestClassEvent} can be eagerly published; whereas, such an
 * event is not published by default for the first run of a test class for a
 * specific {@code ApplicationContext}.
 *
 * @author Sam Brannen
 * @since 5.3.17
 * @see https://github.com/spring-projects/spring-framework/issues/27757
 */
class EagerTestExecutionEventPublishingTests {

	private static final List<Class<? extends TestContextEvent>> events = new ArrayList<>();


	@BeforeEach
	@AfterEach
	void resetEvents() {
		events.clear();
	}

	@Test
	void beforeTestClassEventIsNotPublishedByDefaultForFirstTestClass() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(LazyTestCase1.class), selectClass(LazyTestCase2.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

		assertThat(events).containsExactly(//
			// 1st test class
			// BeforeTestClassEvent.class -- missing for 1st test class
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class, //
			// 2nd test class
			BeforeTestClassEvent.class, //
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class//
		);
	}

	@Test
	void beforeTestClassEventIsPublishedForAllTestClassesIfCustomListenerEagerlyLoadsContext() {
		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(EagerTestCase1.class), selectClass(EagerTestCase2.class))//
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

		assertThat(events).containsExactly(//
			// 1st test class
			BeforeTestClassEvent.class, //
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class, //
			// 2nd test class
			BeforeTestClassEvent.class, //
			PrepareTestInstanceEvent.class, //
			BeforeTestMethodEvent.class, //
			BeforeTestExecutionEvent.class, //
			AfterTestExecutionEvent.class, //
			AfterTestMethodEvent.class, //
			AfterTestClassEvent.class//
		);
	}


	@JUnitConfig(Config.class)
	static class LazyTestCase1 {

		@Test
		void test() {
		}
	}

	static class LazyTestCase2 extends LazyTestCase1 {
	}

	@TestExecutionListeners(listeners = EagerLoadingTestExecutionListener.class, mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
	static class EagerTestCase1 extends LazyTestCase1 {
	}

	static class EagerTestCase2 extends EagerTestCase1 {
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

	@Order(0)
	static class EagerLoadingTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			testContext.getApplicationContext();
		}
	}

}
