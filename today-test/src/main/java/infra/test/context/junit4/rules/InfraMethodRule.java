/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.junit4.rules;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.annotation.IfProfileValue;
import infra.test.annotation.ProfileValueSourceConfiguration;
import infra.test.annotation.Repeat;
import infra.test.annotation.Timed;
import infra.test.context.TestContextBootstrapper;
import infra.test.context.TestContextManager;
import infra.test.context.TestExecutionListener;
import infra.test.context.junit4.JUnit4ClassRunner;
import infra.test.context.junit4.statements.FailOnTimeout;
import infra.test.context.junit4.statements.ProfileValueChecker;
import infra.test.context.junit4.statements.RepeatTest;
import infra.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import infra.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import infra.test.context.junit4.statements.RunPrepareTestInstanceCallbacks;

/**
 * {@code ApplicationMethodRule} is a custom JUnit 4 {@link MethodRule} that
 * supports instance-level and method-level features of the
 * <em>TestContext Framework</em> in standard JUnit tests by means
 * of the {@link TestContextManager} and associated support classes and
 * annotations.
 *
 * <p>In contrast to the {@link JUnit4ClassRunner
 * JUnit4ClassRunner}, Framework's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code JUnit4ClassRunner},
 * however, a {@code ApplicationMethodRule} must be combined with a {@link InfraClassRule},
 * since {@code ApplicationMethodRule} only supports the instance-level and method-level
 * features of the {@code JUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code> public class ExampleInfraIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final ApplicationClassRule applicationClassRule = new ApplicationClassRule();
 *
 *    &#064;Rule
 *    public final ApplicationMethodRule applicationMethodRule = new ApplicationMethodRule();
 *
 *    // ...
 * }</code></pre>
 *
 * <p>The following list constitutes all annotations currently supported directly
 * or indirectly by {@code ApplicationMethodRule}. <em>(Note that additional annotations
 * may be supported by various
 * {@link TestExecutionListener TestExecutionListener} or
 * {@link TestContextBootstrapper TestContextBootstrapper}
 * implementations.)</em>
 *
 * <ul>
 * <li>{@link Timed @Timed}</li>
 * <li>{@link Repeat @Repeat}</li>
 * <li>{@link ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> this class requires JUnit 4.12 or higher.
 *
 * <p><strong>WARNING:</strong> Due to the shortcomings of JUnit rules, the
 * {@code ApplicationMethodRule}
 * {@linkplain TestExecutionListener#prepareTestInstance
 * prepares the test instance} before {@code @Before} lifecycle methods instead of
 * immediately after instantiation of the test class. In addition, the
 * {@code ApplicationMethodRule} does <strong>not</strong> support the
 * {@code beforeTestExecution()} and {@code afterTestExecution()} callbacks of the
 * {@link TestExecutionListener TestExecutionListener}
 * API.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @see #apply(Statement, FrameworkMethod, Object)
 * @see InfraClassRule
 * @see TestContextManager
 * @see JUnit4ClassRunner
 * @since 4.0
 */
public class InfraMethodRule implements MethodRule {

  private static final Logger logger = LoggerFactory.getLogger(InfraMethodRule.class);

  /**
   * Apply <em>instance-level</em> and <em>method-level</em> features of
   * the <em>TestContext Framework</em> to the supplied {@code base}
   * statement.
   * <p>Specifically, this method invokes the
   * {@link TestContextManager#prepareTestInstance prepareTestInstance()},
   * {@link TestContextManager#beforeTestMethod beforeTestMethod()}, and
   * {@link TestContextManager#afterTestMethod afterTestMethod()} methods
   * on the {@code TestContextManager}, potentially with Infra timeouts
   * and repetitions.
   * <p>In addition, this method checks whether the test is enabled in
   * the current execution environment. This prevents methods with a
   * non-matching {@code @IfProfileValue} annotation from running altogether,
   * even skipping the execution of {@code prepareTestInstance()} methods
   * in {@code TestExecutionListeners}.
   *
   * @param base the base {@code Statement} that this rule should be applied to
   * @param frameworkMethod the method which is about to be invoked on the test instance
   * @param testInstance the current test instance
   * @return a statement that wraps the supplied {@code base} with instance-level
   * and method-level features of the TestContext Framework
   * @see #withBeforeTestMethodCallbacks
   * @see #withAfterTestMethodCallbacks
   * @see #withPotentialRepeat
   * @see #withPotentialTimeout
   * @see #withTestInstancePreparation
   * @see #withProfileValueCheck
   */
  @Override
  public Statement apply(Statement base, FrameworkMethod frameworkMethod, Object testInstance) {
    Method testMethod = frameworkMethod.getMethod();
    if (logger.isDebugEnabled()) {
      logger.debug("Applying ApplicationMethodRule to test method [{}]", testMethod);
    }
    Class<?> testClass = testInstance.getClass();
    TestContextManager testContextManager = InfraClassRule.getTestContextManager(testClass);

    Statement statement = base;
    statement = withBeforeTestMethodCallbacks(statement, testMethod, testInstance, testContextManager);
    statement = withAfterTestMethodCallbacks(statement, testMethod, testInstance, testContextManager);
    statement = withTestInstancePreparation(statement, testInstance, testContextManager);
    statement = withPotentialRepeat(statement, testMethod, testInstance);
    statement = withPotentialTimeout(statement, testMethod, testInstance);
    statement = withProfileValueCheck(statement, testMethod, testInstance);
    return statement;
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code RunBeforeTestMethodCallbacks} statement.
   *
   * @see RunBeforeTestMethodCallbacks
   */
  private Statement withBeforeTestMethodCallbacks(Statement next, Method testMethod,
          Object testInstance, TestContextManager testContextManager) {

    return new RunBeforeTestMethodCallbacks(
            next, testInstance, testMethod, testContextManager);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code RunAfterTestMethodCallbacks} statement.
   *
   * @see RunAfterTestMethodCallbacks
   */
  private Statement withAfterTestMethodCallbacks(Statement next, Method testMethod,
          Object testInstance, TestContextManager testContextManager) {

    return new RunAfterTestMethodCallbacks(
            next, testInstance, testMethod, testContextManager);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code RunPrepareTestInstanceCallbacks} statement.
   *
   * @see RunPrepareTestInstanceCallbacks
   */
  private Statement withTestInstancePreparation(
          Statement next, Object testInstance, TestContextManager testContextManager) {

    return new RunPrepareTestInstanceCallbacks(next, testInstance, testContextManager);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code RepeatTest} statement.
   * <p>Supports Framework's {@link Repeat @Repeat}
   * annotation.
   *
   * @see RepeatTest
   */
  private Statement withPotentialRepeat(Statement next, Method testMethod, Object testInstance) {
    return new RepeatTest(next, testMethod);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code FailOnTimeout} statement.
   * <p>Supports Framework's {@link Timed @Timed}
   * annotation.
   *
   * @see FailOnTimeout
   */
  private Statement withPotentialTimeout(Statement next, Method testMethod, Object testInstance) {
    return new FailOnTimeout(next, testMethod);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code ProfileValueChecker} statement.
   *
   * @see ProfileValueChecker
   */
  private Statement withProfileValueCheck(Statement next, Method testMethod, Object testInstance) {
    return new ProfileValueChecker(next, testInstance.getClass(), testMethod);
  }

}
