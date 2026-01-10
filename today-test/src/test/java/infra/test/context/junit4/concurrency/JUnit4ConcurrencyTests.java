/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.context.junit4.concurrency;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import infra.core.testfixture.TestGroup;
import infra.test.context.junit4.InfraJUnit47ClassRunnerRuleTests;
import infra.test.context.junit4.InfraRunner;
import infra.test.context.junit4.InheritedConfigJUnit4ClassRunnerAppCtxTests;
import infra.test.context.junit4.JUnit4ClassRunnerAppCtxTests;
import infra.test.context.junit4.JUnitTestingUtils;
import infra.test.context.junit4.MethodLevelTransactionalInfraRunnerTests;
import infra.test.context.junit4.TimedTransactionalRunnerTests;
import infra.test.context.junit4.rules.BaseAppCtxRuleTests;
import infra.test.context.junit4.rules.BasicAnnotationConfigWacRuleTests;
import infra.test.context.junit4.rules.InfraClassRule;
import infra.test.context.junit4.rules.InfraMethodRule;
import infra.util.ReflectionUtils;

import static infra.core.annotation.AnnotatedElementUtils.hasAnnotation;
import static org.junit.Assume.assumeTrue;

/**
 * Concurrency tests for the {@link InfraRunner}, {@link InfraClassRule}, and
 * {@link InfraMethodRule} that use JUnit 4's experimental {@link ParallelComputer}
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
 * <li>tests that make use of Infra {@code @DirtiesContext} support
 * <li>tests that make use of JUnit 4's {@code @FixMethodOrder} support
 * <li>tests that commit changes to the state of a shared in-memory database
 * </ul>
 *
 * <p><strong>NOTE</strong>: these tests only run if the {@link TestGroup#LONG_RUNNING
 * LONG_RUNNING} test group is enabled.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class JUnit4ConcurrencyTests {

  private final Class<?>[] testClasses = new Class<?>[] {
          // Basics
          JUnit4ClassRunnerAppCtxTests.class,
          InheritedConfigJUnit4ClassRunnerAppCtxTests.class,
          InfraJUnit47ClassRunnerRuleTests.class,
          BaseAppCtxRuleTests.class,
          // Transactional
          MethodLevelTransactionalInfraRunnerTests.class,
          TimedTransactionalRunnerTests.class,
          // Web and Scopes
          BasicAnnotationConfigWacRuleTests.class,
          // Web MVC Test
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
