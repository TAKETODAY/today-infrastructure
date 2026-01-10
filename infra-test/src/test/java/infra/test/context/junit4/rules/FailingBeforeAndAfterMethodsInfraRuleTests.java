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

package infra.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized.Parameters;

import infra.test.context.ContextConfiguration;
import infra.test.context.TestExecutionListeners;
import infra.test.context.junit4.FailingBeforeAndAfterMethodsInfraRunnerTests;
import infra.test.context.transaction.AfterTransaction;
import infra.test.context.transaction.BeforeTransaction;
import infra.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.fail;

/**
 * This class is an extension of {@link FailingBeforeAndAfterMethodsInfraRunnerTests}
 * that has been modified to use {@link InfraClassRule} and
 * {@link InfraMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class FailingBeforeAndAfterMethodsInfraRuleTests extends FailingBeforeAndAfterMethodsInfraRunnerTests {

  @Parameters(name = "{0}")
  public static Object[] testData() {
    return new Object[] {//
            AlwaysFailingBeforeTestClassInfraRuleTestCase.class.getSimpleName(),//
            AlwaysFailingAfterTestClassInfraRuleTestCase.class.getSimpleName(),//
            AlwaysFailingPrepareTestInstanceInfraRuleTestCase.class.getSimpleName(),//
            AlwaysFailingBeforeTestMethodInfraRuleTestCase.class.getSimpleName(),//
            AlwaysFailingAfterTestMethodInfraRuleTestCase.class.getSimpleName(),//
            FailingBeforeTransactionInfraRuleTestCase.class.getSimpleName(),//
            FailingAfterTransactionInfraRuleTestCase.class.getSimpleName() //
    };
  }

  public FailingBeforeAndAfterMethodsInfraRuleTests(String testClassName) throws Exception {
    super(testClassName);
  }

  @Override
  protected Class<? extends Runner> getRunnerClass() {
    return JUnit4.class;
  }

  // All tests are in superclass.

  @RunWith(JUnit4.class)
  public static abstract class BaseInfraRuleTestCase {

    @ClassRule
    public static final InfraClassRule applicationClassRule = new InfraClassRule();

    @Rule
    public final InfraMethodRule infraMethodRule = new InfraMethodRule();

    @Test
    public void testNothing() {
    }
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingBeforeTestClassTestExecutionListener.class)
  public static class AlwaysFailingBeforeTestClassInfraRuleTestCase extends BaseInfraRuleTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingAfterTestClassTestExecutionListener.class)
  public static class AlwaysFailingAfterTestClassInfraRuleTestCase extends BaseInfraRuleTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingPrepareTestInstanceTestExecutionListener.class)
  public static class AlwaysFailingPrepareTestInstanceInfraRuleTestCase extends BaseInfraRuleTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingBeforeTestMethodTestExecutionListener.class)
  public static class AlwaysFailingBeforeTestMethodInfraRuleTestCase extends BaseInfraRuleTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @TestExecutionListeners(AlwaysFailingAfterTestMethodTestExecutionListener.class)
  public static class AlwaysFailingAfterTestMethodInfraRuleTestCase extends BaseInfraRuleTestCase {
  }

  @Ignore("TestCase classes are run manually by the enclosing test class")
  @RunWith(JUnit4.class)
  @ContextConfiguration("../FailingBeforeAndAfterMethodsTests-context.xml")
  @Transactional
  public static class FailingBeforeTransactionInfraRuleTestCase {

    @ClassRule
    public static final InfraClassRule applicationClassRule = new InfraClassRule();

    @Rule
    public final InfraMethodRule infraMethodRule = new InfraMethodRule();

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
  public static class FailingAfterTransactionInfraRuleTestCase {

    @ClassRule
    public static final InfraClassRule applicationClassRule = new InfraClassRule();

    @Rule
    public final InfraMethodRule infraMethodRule = new InfraMethodRule();

    @Test
    public void testNothing() {
    }

    @AfterTransaction
    public void afterTransaction() {
      fail("always failing afterTransaction()");
    }
  }

}
