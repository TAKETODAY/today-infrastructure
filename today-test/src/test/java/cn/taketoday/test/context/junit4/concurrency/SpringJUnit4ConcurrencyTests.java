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

package cn.taketoday.test.context.junit4.concurrency;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import cn.taketoday.core.testfixture.TestGroup;
import cn.taketoday.test.context.TestContextConcurrencyTests;
import cn.taketoday.test.context.junit4.InheritedConfigSpringJUnit4ClassRunnerAppCtxTests;
import cn.taketoday.test.context.junit4.JUnitTestingUtils;
import cn.taketoday.test.context.junit4.MethodLevelTransactionalSpringRunnerTests;
import cn.taketoday.test.context.junit4.TimedTransactionalSpringRunnerTests;
import cn.taketoday.test.context.junit4.rules.BasicAnnotationConfigWacSpringRuleTests;
import cn.taketoday.test.context.junit4.SpringJUnit47ClassRunnerRuleTests;
import cn.taketoday.test.context.junit4.SpringJUnit4ClassRunnerAppCtxTests;
import cn.taketoday.test.context.junit4.Runner;
import cn.taketoday.test.context.junit4.rules.BaseAppCtxRuleTests;
import cn.taketoday.test.context.junit4.rules.ApplicationClassRule;
import cn.taketoday.test.context.junit4.rules.ApplicationMethodRule;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.w.test.context.TestContextConcurrencyTests;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.junit.Assume.assumeTrue;
import static cn.taketoday.core.annotation.AnnotatedElementUtils.hasAnnotation;
import static cn.taketoday.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * Concurrency tests for the {@link Runner}, {@link ApplicationClassRule}, and
 * {@link ApplicationMethodRule} that use JUnit 4's experimental {@link ParallelComputer}
 * to execute tests in parallel.
 *
 * <p>The tests executed by this test class come from a hand-picked collection of test
 * classes within the test suite that is intended to cover most categories of tests
 * that are currently supported by the TestContext Framework on JUnit 4.
 *
 * <p>The chosen test classes intentionally do <em>not</em> include any classes that
 * fall under the following categories.
 *
 * <ul>
 * <li>tests that make use of Spring's {@code @DirtiesContext} support
 * <li>tests that make use of JUnit 4's {@code @FixMethodOrder} support
 * <li>tests that commit changes to the state of a shared in-memory database
 * </ul>
 *
 * <p><strong>NOTE</strong>: these tests only run if the {@link TestGroup#LONG_RUNNING
 * LONG_RUNNING} test group is enabled.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see TestContextConcurrencyTests
 */
public class SpringJUnit4ConcurrencyTests {

	private final Class<?>[] testClasses = new Class<?>[] {
		// Basics
			SpringJUnit4ClassRunnerAppCtxTests.class,
			InheritedConfigSpringJUnit4ClassRunnerAppCtxTests.class,
			SpringJUnit47ClassRunnerRuleTests.class,
			BaseAppCtxRuleTests.class,
		// Transactional
			MethodLevelTransactionalSpringRunnerTests.class,
			TimedTransactionalSpringRunnerTests.class,
		// Web and Scopes
			BasicAnnotationConfigWacSpringRuleTests.class,
		// Spring MVC Test
	};


	@BeforeClass
	public static void abortIfLongRunningTestGroupIsNotEnabled() {
		assumeTrue("TestGroup " + TestGroup.LONG_RUNNING + " is not active.", TestGroup.LONG_RUNNING.isActive());
	}

	@Test
	public void runAllTestsConcurrently() throws Exception {
		final int FAILED = 0;
		final int ABORTED = 0;
		final int IGNORED = countAnnotatedMethods(Ignore.class);
		final int TESTS = countAnnotatedMethods(Test.class) - IGNORED;

		JUnitTestingUtils.runTestsAndAssertCounters(new ParallelComputer(true, true), TESTS, FAILED, TESTS, IGNORED, ABORTED,
				this.testClasses);
	}

	private int countAnnotatedMethods(Class<? extends Annotation> annotationType) {
		return (int) Arrays.stream(this.testClasses)
				.map(ReflectionUtils::getUniqueDeclaredMethods)
				.flatMap(Arrays::stream)
				.filter(method -> hasAnnotation(method, annotationType))
				.count();
	}

}
