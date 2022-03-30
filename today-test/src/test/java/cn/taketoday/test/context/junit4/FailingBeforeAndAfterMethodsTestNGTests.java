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

package cn.taketoday.test.context.junit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.testng.AbstractTestNGSpringContextTests;
import cn.taketoday.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import cn.taketoday.test.context.testng.TrackingTestNGTestListener;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.util.ClassUtils;
import org.testng.TestNG;

/**
 * Integration tests which verify that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@code @BeforeTransaction} and {@code @AfterTransaction} methods can fail
 * tests in a TestNG environment.
 *
 * <p>See: <a href="https://jira.spring.io/browse/SPR-3960" target="_blank">SPR-3960</a>.
 *
 * <p>Indirectly, this class also verifies that all {@code TestExecutionListener}
 * lifecycle callbacks are called.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(Parameterized.class)
public class FailingBeforeAndAfterMethodsTestNGTests {

	protected final Class<?> clazz;

	protected final int expectedTestStartCount;

	protected final int expectedTestSuccessCount;

	protected final int expectedFailureCount;

	protected final int expectedFailedConfigurationsCount;


	@Parameters(name = "{0}")
	public static Object[][] testData() {
		return new Object[][] {
			{ AlwaysFailingBeforeTestClassTestCase.class.getSimpleName(), 1, 0, 0, 1 },
			{ AlwaysFailingAfterTestClassTestCase.class.getSimpleName(), 1, 1, 0, 1 },
			{ AlwaysFailingPrepareTestInstanceTestCase.class.getSimpleName(), 1, 0, 0, 1 },
			{ AlwaysFailingBeforeTestMethodTestCase.class.getSimpleName(), 1, 0, 0, 1 },
			{ AlwaysFailingBeforeTestExecutionTestCase.class.getSimpleName(), 1, 0, 1, 0 },
			{ AlwaysFailingAfterTestExecutionTestCase.class.getSimpleName(), 1, 0, 1, 0 },
			{ AlwaysFailingAfterTestMethodTestCase.class.getSimpleName(), 1, 1, 0, 1 },
			{ FailingBeforeTransactionTestCase.class.getSimpleName(), 1, 0, 0, 1 },
			{ FailingAfterTransactionTestCase.class.getSimpleName(), 1, 1, 0, 1 }
		};
	}


	public FailingBeforeAndAfterMethodsTestNGTests(String testClassName, int expectedTestStartCount,
			int expectedTestSuccessCount, int expectedFailureCount, int expectedFailedConfigurationsCount) throws Exception {

		this.clazz = ClassUtils.forName(getClass().getName() + "." + testClassName, getClass().getClassLoader());
		this.expectedTestStartCount = expectedTestStartCount;
		this.expectedTestSuccessCount = expectedTestSuccessCount;
		this.expectedFailureCount = expectedFailureCount;
		this.expectedFailedConfigurationsCount = expectedFailedConfigurationsCount;
	}


	@Test
	@Ignore("Fails against TestNG 6.11")
	public void runTestAndAssertCounters() throws Exception {
		TrackingTestNGTestListener listener = new TrackingTestNGTestListener();
		TestNG testNG = new TestNG();
		testNG.addListener(listener);
		testNG.setTestClasses(new Class<?>[] {this.clazz});
		testNG.setVerbose(0);
		testNG.run();

		String name = this.clazz.getSimpleName();

		assertThat(listener.testStartCount).as("tests started for [" + name + "] ==> ").isEqualTo(this.expectedTestStartCount);
		assertThat(listener.testSuccessCount).as("successful tests for [" + name + "] ==> ").isEqualTo(this.expectedTestSuccessCount);
		assertThat(listener.testFailureCount).as("failed tests for [" + name + "] ==> ").isEqualTo(this.expectedFailureCount);
		assertThat(listener.failedConfigurationsCount).as("failed configurations for [" + name + "] ==> ").isEqualTo(this.expectedFailedConfigurationsCount);
	}


	static class AlwaysFailingBeforeTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestClass(TestContext testContext) {
			org.testng.Assert.fail("always failing beforeTestClass()");
		}
	}

	static class AlwaysFailingAfterTestClassTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestClass(TestContext testContext) {
			org.testng.Assert.fail("always failing afterTestClass()");
		}
	}

	static class AlwaysFailingPrepareTestInstanceTestExecutionListener implements TestExecutionListener {

		@Override
		public void prepareTestInstance(TestContext testContext) throws Exception {
			org.testng.Assert.fail("always failing prepareTestInstance()");
		}
	}

	static class AlwaysFailingBeforeTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestMethod(TestContext testContext) {
			org.testng.Assert.fail("always failing beforeTestMethod()");
		}
	}

	static class AlwaysFailingBeforeTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void beforeTestExecution(TestContext testContext) {
			org.testng.Assert.fail("always failing beforeTestExecution()");
		}
	}

	static class AlwaysFailingAfterTestExecutionTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestExecution(TestContext testContext) {
			org.testng.Assert.fail("always failing afterTestExecution()");
		}
	}

	static class AlwaysFailingAfterTestMethodTestExecutionListener implements TestExecutionListener {

		@Override
		public void afterTestMethod(TestContext testContext) {
			org.testng.Assert.fail("always failing afterTestMethod()");
		}
	}


	@TestExecutionListeners(inheritListeners = false)
	public static abstract class BaseTestCase extends AbstractTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
	public static class AlwaysFailingAfterTestClassTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
	public static class AlwaysFailingPrepareTestInstanceTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestMethodTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingBeforeTestExecutionTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestExecutionTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestExecutionTestExecutionListener.class)
	public static class AlwaysFailingAfterTestExecutionTestCase extends BaseTestCase {
	}

	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	public static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
	}

	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingBeforeTransactionTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			org.testng.Assert.fail("always failing beforeTransaction()");
		}
	}

	@ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
	public static class FailingAfterTransactionTestCase extends AbstractTransactionalTestNGSpringContextTests {

		@org.testng.annotations.Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			org.testng.Assert.fail("always failing afterTransaction()");
		}
	}

}
