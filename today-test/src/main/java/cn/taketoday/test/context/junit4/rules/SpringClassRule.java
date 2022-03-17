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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.lang.Assert;
import cn.taketoday.test.context.junit4.statements.ProfileValueChecker;
import cn.taketoday.test.context.junit4.statements.RunAfterTestClassCallbacks;
import cn.taketoday.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import cn.taketoday.test.context.TestContextBootstrapper;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.annotation.IfProfileValue;
import cn.taketoday.test.annotation.ProfileValueSourceConfiguration;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * {@code SpringClassRule} is a custom JUnit {@link TestRule} that supports
 * <em>class-level</em> features of the <em>Spring TestContext Framework</em>
 * in standard JUnit tests by means of the {@link TestContextManager} and
 * associated support classes and annotations.
 *
 * <p>In contrast to the {@link SpringJUnit4ClassRunner
 * SpringJUnit4ClassRunner}, Framework's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code SpringJUnit4ClassRunner},
 * however, a {@code SpringClassRule} must be combined with a {@link SpringMethodRule},
 * since {@code SpringClassRule} only supports the class-level features of the
 * {@code SpringJUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code> public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringClassRule springClassRule = new SpringClassRule();
 *
 *    &#064;Rule
 *    public final SpringMethodRule springMethodRule = new SpringMethodRule();
 *
 *    // ...
 * }</code></pre>
 *
 * <p>The following list constitutes all annotations currently supported directly
 * or indirectly by {@code SpringClassRule}. <em>(Note that additional annotations
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
 * <p><strong>NOTE:</strong> As of Spring Framework 4.3, this class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @see #apply(Statement, Description)
 * @see SpringMethodRule
 * @see TestContextManager
 * @see SpringJUnit4ClassRunner
 * @since 4.0
 */
public class SpringClassRule implements TestRule {

  private static final Log logger = LogFactory.getLog(SpringClassRule.class);

  /**
   * Cache of {@code TestContextManagers} keyed by test class.
   */
  private static final Map<Class<?>, TestContextManager> testContextManagerCache = new ConcurrentHashMap<>(64);

  /**
   * Apply <em>class-level</em> features of the <em>Spring TestContext
   * Framework</em> to the supplied {@code base} statement.
   * <p>Specifically, this method retrieves the {@link TestContextManager}
   * used by this rule and its associated {@link SpringMethodRule} and
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
   * features of the Spring TestContext Framework
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
      logger.debug("Applying SpringClassRule to test class [" + testClass.getName() + "]");
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
    Assert.notNull(testClass, "Test Class must not be null");
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
