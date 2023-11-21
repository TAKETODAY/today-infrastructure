/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.annotation.IfProfileValue;
import cn.taketoday.test.annotation.ProfileValueSourceConfiguration;
import cn.taketoday.test.context.TestContextBootstrapper;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;
import cn.taketoday.test.context.junit4.statements.ProfileValueChecker;
import cn.taketoday.test.context.junit4.statements.RunAfterTestClassCallbacks;
import cn.taketoday.test.context.junit4.statements.RunBeforeTestClassCallbacks;

/**
 * {@code ApplicationClassRule} is a custom JUnit {@link TestRule} that supports
 * <em>class-level</em> features of the <em>TestContext Framework</em>
 * in standard JUnit tests by means of the {@link TestContextManager} and
 * associated support classes and annotations.
 *
 * <p>In contrast to the {@link JUnit4ClassRunner
 * JUnit4ClassRunner}, Framework's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code JUnit4ClassRunner},
 * however, a {@code ApplicationClassRule} must be combined with a {@link InfraMethodRule},
 * since {@code ApplicationClassRule} only supports the class-level features of the
 * {@code JUnit4ClassRunner}.
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
 * or indirectly by {@code ApplicationClassRule}. <em>(Note that additional annotations
 * may be supported by various
 * {@link TestExecutionListener TestExecutionListener} or
 * {@link TestContextBootstrapper TestContextBootstrapper}
 * implementations.)</em>
 *
 * <ul>
 * <li>{@link ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong>  this class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @see #apply(Statement, Description)
 * @see InfraMethodRule
 * @see TestContextManager
 * @see JUnit4ClassRunner
 * @since 4.0
 */
public class InfraClassRule implements TestRule {

  private static final Logger logger = LoggerFactory.getLogger(InfraClassRule.class);

  /**
   * Cache of {@code TestContextManagers} keyed by test class.
   */
  private static final Map<Class<?>, TestContextManager> testContextManagerCache = new ConcurrentHashMap<>(64);

  /**
   * Apply <em>class-level</em> features of the <em>Infra TestContext
   * Framework</em> to the supplied {@code base} statement.
   * <p>Specifically, this method retrieves the {@link TestContextManager}
   * used by this rule and its associated {@link InfraMethodRule} and
   * invokes the {@link TestContextManager#beforeTestClass() beforeTestClass()}
   * and {@link TestContextManager#afterTestClass() afterTestClass()} methods
   * on the {@code TestContextManager}.
   * <p>In addition, this method checks whether the test is enabled in
   * the current execution environment. This prevents classes with a
   * non-matching {@code @IfProfileValue} annotation from running altogether,
   * even skipping the execution of {@code beforeTestClass()} methods
   * in {@code TestExecutionListeners}.
   *
   * @param base the base {@code Statement} that this rule should be applied to
   * @param description a {@code Description} of the current test execution
   * @return a statement that wraps the supplied {@code base} with class-level
   * features of the TestContext Framework
   * @see #getTestContextManager
   * @see #withBeforeTestClassCallbacks
   * @see #withAfterTestClassCallbacks
   * @see #withProfileValueCheck
   * @see #withTestContextManagerCacheEviction
   */
  @Override
  public Statement apply(Statement base, Description description) {
    Class<?> testClass = description.getTestClass();
    if (logger.isDebugEnabled()) {
      logger.debug("Applying ApplicationClassRule to test class [" + testClass.getName() + "]");
    }
    TestContextManager testContextManager = getTestContextManager(testClass);

    Statement statement = base;
    statement = withBeforeTestClassCallbacks(statement, testContextManager);
    statement = withAfterTestClassCallbacks(statement, testContextManager);
    statement = withProfileValueCheck(statement, testClass);
    statement = withTestContextManagerCacheEviction(statement, testClass);
    return statement;
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code RunBeforeTestClassCallbacks} statement.
   *
   * @see RunBeforeTestClassCallbacks
   */
  private Statement withBeforeTestClassCallbacks(Statement next, TestContextManager testContextManager) {
    return new RunBeforeTestClassCallbacks(next, testContextManager);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code RunAfterTestClassCallbacks} statement.
   *
   * @see RunAfterTestClassCallbacks
   */
  private Statement withAfterTestClassCallbacks(Statement next, TestContextManager testContextManager) {
    return new RunAfterTestClassCallbacks(next, testContextManager);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code ProfileValueChecker} statement.
   *
   * @see ProfileValueChecker
   */
  private Statement withProfileValueCheck(Statement next, Class<?> testClass) {
    return new ProfileValueChecker(next, testClass, null);
  }

  /**
   * Wrap the supplied {@link Statement} with a {@code TestContextManagerCacheEvictor} statement.
   *
   * @see TestContextManagerCacheEvictor
   */
  private Statement withTestContextManagerCacheEviction(Statement next, Class<?> testClass) {
    return new TestContextManagerCacheEvictor(next, testClass);
  }

  /**
   * Get the {@link TestContextManager} associated with the supplied test class.
   *
   * @param testClass the test class to be managed; never {@code null}
   */
  static TestContextManager getTestContextManager(Class<?> testClass) {
    Assert.notNull(testClass, "Test Class is required");
    return testContextManagerCache.computeIfAbsent(testClass, TestContextManager::new);
  }

  private static class TestContextManagerCacheEvictor extends Statement {

    private final Statement next;

    private final Class<?> testClass;

    TestContextManagerCacheEvictor(Statement next, Class<?> testClass) {
      this.next = next;
      this.testClass = testClass;
    }

    @Override
    public void evaluate() throws Throwable {
      try {
        this.next.evaluate();
      }
      finally {
        testContextManagerCache.remove(this.testClass);
      }
    }
  }

}
