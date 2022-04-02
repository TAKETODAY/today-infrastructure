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
import cn.taketoday.test.context.transaction.AfterTransaction;
import cn.taketoday.test.context.transaction.BeforeTransaction;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests which verify that '<i>before</i>' and '<i>after</i>'
 * methods of {@link TestExecutionListener TestExecutionListeners} as well as
 * {@code @BeforeTransaction} and {@code @AfterTransaction} methods can fail
 * tests run via the {@link Runner} in a JUnit 4 environment.
 *
 * <p>See: <a href="https://jira.spring.io/browse/SPR-3960" target="_blank">SPR-3960</a>.
 *
 * <p>Indirectly, this class also verifies that all {@code TestExecutionListener}
 * lifecycle callbacks are called.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class FailingBeforeAndAfterMethodsSpringRunnerTests {

  protected final Class<?> clazz;

  @Parameters(name = "{0}")
  public static Object[] testCases() {
    return new Object[] {//
            AlwaysFailingBeforeTestClassTestCase.class.getSimpleName(),//
            AlwaysFailingAfterTestClassTestCase.class.getSimpleName(),//
            AlwaysFailingPrepareTestInstanceTestCase.class.getSimpleName(),//
            AlwaysFailingBeforeTestMethodTestCase.class.getSimpleName(),//
            AlwaysFailingBeforeTestExecutionTestCase.class.getSimpleName(), //
            AlwaysFailingAfterTestExecutionTestCase.class.getSimpleName(), //
            AlwaysFailingAfterTestMethodTestCase.class.getSimpleName(),//
            FailingBeforeTransactionTestCase.class.getSimpleName(),//
            FailingAfterTransactionTestCase.class.getSimpleName() //
    };
  }

  public FailingBeforeAndAfterMethodsSpringRunnerTests(String testClassName) throws Exception {
    this.clazz = ClassUtils.forName(getClass().getName() + "." + testClassName, getClass().getClassLoader());
  }

  protected Class<? extends org.junit.runner.Runner> getRunnerClass() {
    return Runner.class;
  }

  @Test
  public void runTestAndAssertCounters() throws Exception {
    int expectedStartedCount = this.clazz.getSimpleName().startsWith("AlwaysFailingBeforeTestClass") ? 0 : 1;
    int expectedFinishedCount = this.clazz.getSimpleName().startsWith("AlwaysFailingBeforeTestClass") ? 0 : 1;

    JUnitTestingUtils.runTestsAndAssertCounters(getRunnerClass(), this.clazz, expectedStartedCount, 1, expectedFinishedCount, 0, 0);
  }

  // -------------------------------------------------------------------

  protected static class AlwaysFailingBeforeTestClassTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) {
      fail("always failing beforeTestClass()");
    }
  }

  protected static class AlwaysFailingAfterTestClassTestExecutionListener implements TestExecutionListener {

    @Override
    public void afterTestClass(TestContext testContext) {
      fail("always failing afterTestClass()");
    }
  }

  protected static class AlwaysFailingPrepareTestInstanceTestExecutionListener implements TestExecutionListener {

    @Override
    public void prepareTestInstance(TestContext testContext) throws Exception {
      fail("always failing prepareTestInstance()");
    }
  }

  protected static class AlwaysFailingBeforeTestMethodTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
      fail("always failing beforeTestMethod()");
    }
  }

  protected static class AlwaysFailingBeforeTestExecutionTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestExecution(TestContext testContext) {
      fail("always failing beforeTestExecution()");
    }
  }

  protected static class AlwaysFailingAfterTestMethodTestExecutionListener implements TestExecutionListener {

    @Override
    public void afterTestMethod(TestContext testContext) {
      fail("always failing afterTestMethod()");
    }
  }

  protected static class AlwaysFailingAfterTestExecutionTestExecutionListener implements TestExecutionListener {

    @Override
    public void afterTestExecution(TestContext testContext) {
      fail("always failing afterTestExecution()");
    }
  }

  @RunWith(Runner.class)
  public static abstract class BaseTestCase {

    @Test
    public void testNothing() {
    }
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
  public static class AlwaysFailingBeforeTestClassTestCase extends BaseTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
  public static class AlwaysFailingAfterTestClassTestCase extends BaseTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
  public static class AlwaysFailingPrepareTestInstanceTestCase extends BaseTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
  public static class AlwaysFailingBeforeTestMethodTestCase extends BaseTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingBeforeTestExecutionTestExecutionListener.class)
  public static class AlwaysFailingBeforeTestExecutionTestCase extends BaseTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingAfterTestExecutionTestExecutionListener.class)
  public static class AlwaysFailingAfterTestExecutionTestCase extends BaseTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
  public static class AlwaysFailingAfterTestMethodTestCase extends BaseTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @RunWith(Runner.class)
  @ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
  @Transactional
  public static class FailingBeforeTransactionTestCase {

    @Test
    public void testNothing() {
    }

    @BeforeTransaction
    public void beforeTransaction() {
      fail("always failing beforeTransaction()");
    }
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @RunWith(Runner.class)
  @ContextConfiguration("FailingBeforeAndAfterMethodsTests-context.xml")
  @Transactional
  public static class FailingAfterTransactionTestCase {

    @Test
    public void testNothing() {
    }

    @AfterTransaction
    public void afterTransaction() {
      fail("always failing afterTransaction()");
    }
  }

}
