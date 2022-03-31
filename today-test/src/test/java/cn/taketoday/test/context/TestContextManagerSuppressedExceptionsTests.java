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

package cn.taketoday.test.context;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * JUnit 4 based unit tests for {@link TestContextManager}, which verify proper
 * support for <em>suppressed exceptions</em> thrown by {@link TestExecutionListener
 * TestExecutionListeners}.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see Throwable#getSuppressed()
 */
class TestContextManagerSuppressedExceptionsTests {

	@Test
	void afterTestExecution() throws Exception {
		test("afterTestExecution", FailingAfterTestExecutionTestCase.class,
			(tcm, c, m) -> tcm.afterTestExecution(this, m, null));
	}

	@Test
	void afterTestMethod() throws Exception {
		test("afterTestMethod", FailingAfterTestMethodTestCase.class,
			(tcm, c, m) -> tcm.afterTestMethod(this, m, null));
	}

	@Test
	void afterTestClass() throws Exception {
		test("afterTestClass", FailingAfterTestClassTestCase.class, (tcm, c, m) -> tcm.afterTestClass());
	}

	private void test(String useCase, Class<?> testClass, Callback callback) throws Exception {
		TestContextManager testContextManager = new TestContextManager(testClass);
		assertThat(testContextManager.getTestExecutionListeners().size()).as("Registered TestExecutionListeners").isEqualTo(2);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> {
				Method testMethod = getClass().getMethod("toString");
				callback.invoke(testContextManager, testClass, testMethod);
				fail("should have thrown an AssertionError");
			}).satisfies(ex -> {
				// 'after' callbacks are reversed, so 2 comes before 1.
				assertThat(ex.getMessage()).isEqualTo(useCase + "-2");
				Throwable[] suppressed = ex.getSuppressed();
				assertThat(suppressed).hasSize(1);
				assertThat(suppressed[0].getMessage()).isEqualTo(useCase + "-1");
			});
	}


	// -------------------------------------------------------------------

	@FunctionalInterface
	private interface Callback {

		void invoke(TestContextManager tcm, Class<?> clazz, Method method) throws Exception;
	}

	private static class FailingAfterTestClassListener1 implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("afterTestClass-1");
		}
	}

	private static class FailingAfterTestClassListener2 implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			fail("afterTestClass-2");
		}
	}

	private static class FailingAfterTestMethodListener1 implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("afterTestMethod-1");
		}
	}

	private static class FailingAfterTestMethodListener2 implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			fail("afterTestMethod-2");
		}
	}

	private static class FailingAfterTestExecutionListener1 implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			fail("afterTestExecution-1");
		}
	}

	private static class FailingAfterTestExecutionListener2 implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			fail("afterTestExecution-2");
		}
	}

	@TestExecutionListeners({ FailingAfterTestExecutionListener1.class, FailingAfterTestExecutionListener2.class })
	private static class FailingAfterTestExecutionTestCase {
	}

	@TestExecutionListeners({ FailingAfterTestMethodListener1.class, FailingAfterTestMethodListener2.class })
	private static class FailingAfterTestMethodTestCase {
	}

	@TestExecutionListeners({ FailingAfterTestClassListener1.class, FailingAfterTestClassListener2.class })
	private static class FailingAfterTestClassTestCase {
	}

}
