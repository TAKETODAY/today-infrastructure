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

package cn.taketoday.test.context.testng.event;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.event.ApplicationEvents;
import cn.taketoday.test.context.event.RecordApplicationEvents;
import cn.taketoday.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/**
 * Integration tests for {@link ApplicationEvents} in conjunction with TestNG.
 *
 * @author Sam Brannen
 * @since 5.3.3
 */
@RecordApplicationEvents
class TestNGApplicationEventsIntegrationTests extends AbstractTestNGSpringContextTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	ApplicationEvents applicationEvents;

	private boolean testAlreadyExecuted = false;


	@BeforeMethod
	void beforeEach() {
		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent");
		}

		context.publishEvent(new CustomEvent("beforeEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach");

		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent");
		}
	}

	@Test
	public void test1() {
		assertTestExpectations("test1");
	}

	@Test
	public void test2() {
		assertTestExpectations("test2");
	}

	private void assertTestExpectations(String testName) {
		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent");
		}

		context.publishEvent(new CustomEvent(testName));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", testName);

		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
					"CustomEvent");
		}
	}

	@AfterMethod
	void afterEach(Method testMethod) {
		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent", "AfterTestExecutionEvent");
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
					"CustomEvent", "AfterTestExecutionEvent");
		}

		context.publishEvent(new CustomEvent("afterEach"));
		assertThat(applicationEvents.stream(CustomEvent.class)).extracting(CustomEvent::getMessage)//
				.containsExactly("beforeEach", testMethod.getName(), "afterEach");

		if (!testAlreadyExecuted) {
			assertEventTypes(applicationEvents, "PrepareTestInstanceEvent", "BeforeTestMethodEvent", "CustomEvent",
					"BeforeTestExecutionEvent", "CustomEvent", "AfterTestExecutionEvent", "CustomEvent");
			testAlreadyExecuted = true;
		}
		else {
			assertEventTypes(applicationEvents, "BeforeTestMethodEvent", "CustomEvent", "BeforeTestExecutionEvent",
					"CustomEvent", "AfterTestExecutionEvent", "CustomEvent");
		}
	}


	private static void assertEventTypes(ApplicationEvents applicationEvents, String... types) {
		assertThat(applicationEvents.stream().map(event -> event.getClass().getSimpleName()))
			.containsExactly(types);
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
