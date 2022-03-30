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

package cn.taketoday.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized.Parameters;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.junit4.FailingBeforeAndAfterMethodsSpringRunnerTests;
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.fail;

/**
 * This class is an extension of {@link FailingBeforeAndAfterMethodsSpringRunnerTests}
 * that has been modified to use {@link ApplicationClassRule} and
 * {@link ApplicationMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
public class FailingBeforeAndAfterMethodsSpringRuleTests extends FailingBeforeAndAfterMethodsSpringRunnerTests {

	@Parameters(name = "{0}")
	public static Object[] testData() {
		return new Object[] {//
			AlwaysFailingBeforeTestClassSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingAfterTestClassSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingPrepareTestInstanceSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingBeforeTestMethodSpringRuleTestCase.class.getSimpleName(),//
			AlwaysFailingAfterTestMethodSpringRuleTestCase.class.getSimpleName(),//
			FailingBeforeTransactionSpringRuleTestCase.class.getSimpleName(),//
			FailingAfterTransactionSpringRuleTestCase.class.getSimpleName() //
		};
	}

	public FailingBeforeAndAfterMethodsSpringRuleTests(String testClassName) throws Exception {
		super(testClassName);
	}

	@Override
	protected Class<? extends Runner> getRunnerClass() {
		return JUnit4.class;
	}

	// All tests are in superclass.

	@RunWith(JUnit4.class)
	public static abstract class BaseSpringRuleTestCase {

		@ClassRule
		public static final ApplicationClassRule applicationClassRule = new ApplicationClassRule();

		@Rule
		public final ApplicationMethodRule applicationMethodRule = new ApplicationMethodRule();


		@Test
		public void testNothing() {
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestClassSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
	public static class AlwaysFailingAfterTestClassSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
	public static class AlwaysFailingPrepareTestInstanceSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
	public static class AlwaysFailingBeforeTestMethodSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
	public static class AlwaysFailingAfterTestMethodSpringRuleTestCase extends BaseSpringRuleTestCase {
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@RunWith(JUnit4.class)
	@ContextConfiguration("../FailingBeforeAndAfterMethodsTests-context.xml")
	@Transactional
	public static class FailingBeforeTransactionSpringRuleTestCase {

		@ClassRule
		public static final ApplicationClassRule applicationClassRule = new ApplicationClassRule();

		@Rule
		public final ApplicationMethodRule applicationMethodRule = new ApplicationMethodRule();


		@Test
		public void testNothing() {
		}

		@BeforeTransaction
		public void beforeTransaction() {
			fail("always failing beforeTransaction()");
		}
	}

	@Ignore("TestCase classes are run manually by the enclosing test class")
	@RunWith(JUnit4.class)
	@ContextConfiguration("../FailingBeforeAndAfterMethodsTests-context.xml")
	@Transactional
	public static class FailingAfterTransactionSpringRuleTestCase {

		@ClassRule
		public static final ApplicationClassRule applicationClassRule = new ApplicationClassRule();

		@Rule
		public final ApplicationMethodRule applicationMethodRule = new ApplicationMethodRule();


		@Test
		public void testNothing() {
		}

		@AfterTransaction
		public void afterTransaction() {
			fail("always failing afterTransaction()");
		}
	}

}
