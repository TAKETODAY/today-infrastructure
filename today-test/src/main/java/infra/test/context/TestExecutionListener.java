/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context;

import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.lang.TodayStrategies;
import infra.test.context.event.ApplicationEventsTestExecutionListener;
import infra.test.context.event.EventPublishingTestExecutionListener;
import infra.test.context.jdbc.SqlScriptsTestExecutionListener;
import infra.test.context.junit4.rules.InfraMethodRule;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;
import infra.test.context.transaction.TransactionalTestExecutionListener;
import infra.test.context.web.MockTestExecutionListener;

/**
 * {@code TestExecutionListener} defines a <em>listener</em> API for reacting to
 * test execution events published by the {@link TestContextManager} with which
 * the listener is registered.
 *
 * <p>Note that not all testing frameworks support all lifecycle callbacks defined
 * in this API. For example, {@link #beforeTestExecution} and
 * {@link #afterTestExecution} are not supported in conjunction with JUnit 4 when
 * using the {@link InfraMethodRule
 * ApplicationMethodRule}.
 *
 * <p>This interface provides empty {@code default} implementations for all methods.
 * Concrete implementations can therefore choose to override only those methods
 * suitable for the task at hand.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor,
 * so that listeners can be instantiated transparently by tools and configuration
 * mechanisms.
 *
 * <p>Implementations may optionally declare the position in which they should
 * be ordered among the chain of default listeners via the
 * {@link Ordered Ordered} interface or
 * {@link Order @Order} annotation. See
 * {@link TestContextBootstrapper#getTestExecutionListeners()} for details.
 *
 *
 * <h3>Registering TestExecutionListener Implementations</h3>
 *
 * <p>A {@code TestExecutionListener} can be registered explicitly for a test class,
 * its subclasses, and its nested classes by using the
 * {@link TestExecutionListeners @TestExecutionListeners} annotation. Explicit
 * registration is suitable for custom listeners that are used in limited testing
 * scenarios. However, it can become cumbersome if a custom listener needs to be
 * used across an entire test suite. This issue is addressed through support for
 * automatic discovery of <em>default</em> {@code TestExecutionListener}
 * implementations through the
 * {@link TodayStrategies TodayStrategies}
 * mechanism. Specifically, default {@code TestExecutionListener} implementations
 * can be registered under the {@code infra.test.context.TestExecutionListener}
 * key in a {@link TodayStrategies#STRATEGIES_LOCATION} properties file.
 *
 * <p>Infra provides the following implementations. Each of these implements
 * {@code Ordered} and is registered automatically by default.
 *
 * <ul>
 * <li>{@link MockTestExecutionListener
 * ServletTestExecutionListener}</li>
 * <li>{@link DirtiesContextBeforeModesTestExecutionListener
 * DirtiesContextBeforeModesTestExecutionListener}</li>
 * <li>{@link ApplicationEventsTestExecutionListener
 * ApplicationEventsTestExecutionListener}</li>
 * <li>{@link DependencyInjectionTestExecutionListener
 * DependencyInjectionTestExecutionListener}</li>
 * <li>{@link DirtiesContextTestExecutionListener
 * DirtiesContextTestExecutionListener}</li>
 * <li>{@link TransactionalTestExecutionListener
 * TransactionalTestExecutionListener}</li>
 * <li>{@link SqlScriptsTestExecutionListener
 * SqlScriptsTestExecutionListener}</li>
 * <li>{@link EventPublishingTestExecutionListener
 * EventPublishingTestExecutionListener}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see TestExecutionListeners @TestExecutionListeners
 * @see TestContextManager
 * @see AbstractTestExecutionListener
 * @since 4.0
 */
public interface TestExecutionListener {

  /**
   * Pre-processes a test class <em>before</em> execution of all tests within
   * the class.
   * <p>This method should be called immediately before framework-specific
   * <em>before class</em> lifecycle callbacks.
   * <p>The default implementation is <em>empty</em>. Can be overridden by
   * concrete classes as necessary.
   *
   * @param testContext the test context for the test; never {@code null}
   * @throws Exception allows any exception to propagate
   */
  default void beforeTestClass(TestContext testContext) throws Exception {

  }

  /**
   * Prepares the {@linkplain Object test instance} of the supplied
   * {@linkplain TestContext test context} &mdash; for example, to inject
   * dependencies.
   * <p>This method should be called immediately after instantiation of the test
   * class or as soon after instantiation as possible (as is the case with the
   * {@link InfraMethodRule
   * ApplicationMethodRule}). In any case, this method must be called prior to any
   * framework-specific lifecycle callbacks.
   * <p>The default implementation is <em>empty</em>. Can be overridden by
   * concrete classes as necessary.
   *
   * @param testContext the test context for the test; never {@code null}
   * @throws Exception allows any exception to propagate
   */
  default void prepareTestInstance(TestContext testContext) throws Exception {

  }

  /**
   * Pre-processes a test <em>before</em> execution of <em>before</em>
   * lifecycle callbacks of the underlying test framework &mdash; for example,
   * by setting up test fixtures.
   * <p>This method <strong>must</strong> be called immediately prior to
   * framework-specific <em>before</em> lifecycle callbacks. For historical
   * reasons, this method is named {@code beforeTestMethod}. Since the
   * introduction of {@link #beforeTestExecution}, a more suitable name for
   * this method might be something like {@code beforeTestSetUp} or
   * {@code beforeEach}; however, it is unfortunately impossible to rename
   * this method due to backward compatibility concerns.
   * <p>The default implementation is <em>empty</em>. Can be overridden by
   * concrete classes as necessary.
   *
   * @param testContext the test context in which the test method will be
   * executed; never {@code null}
   * @throws Exception allows any exception to propagate
   * @see #afterTestMethod
   * @see #beforeTestExecution
   * @see #afterTestExecution
   */
  default void beforeTestMethod(TestContext testContext) throws Exception {

  }

  /**
   * Pre-processes a test <em>immediately before</em> execution of the
   * {@linkplain java.lang.reflect.Method test method} in the supplied
   * {@linkplain TestContext test context} &mdash; for example, for timing
   * or logging purposes.
   * <p>This method <strong>must</strong> be called after framework-specific
   * <em>before</em> lifecycle callbacks.
   * <p>The default implementation is <em>empty</em>. Can be overridden by
   * concrete classes as necessary.
   *
   * @param testContext the test context in which the test method will be
   * executed; never {@code null}
   * @throws Exception allows any exception to propagate
   * @see #beforeTestMethod
   * @see #afterTestMethod
   * @see #afterTestExecution
   */
  default void beforeTestExecution(TestContext testContext) throws Exception {

  }

  /**
   * Post-processes a test <em>immediately after</em> execution of the
   * {@linkplain java.lang.reflect.Method test method} in the supplied
   * {@linkplain TestContext test context} &mdash; for example, for timing
   * or logging purposes.
   * <p>This method <strong>must</strong> be called before framework-specific
   * <em>after</em> lifecycle callbacks.
   * <p>The default implementation is <em>empty</em>. Can be overridden by
   * concrete classes as necessary.
   *
   * @param testContext the test context in which the test method will be
   * executed; never {@code null}
   * @throws Exception allows any exception to propagate
   * @see #beforeTestMethod
   * @see #afterTestMethod
   * @see #beforeTestExecution
   */
  default void afterTestExecution(TestContext testContext) throws Exception {

  }

  /**
   * Post-processes a test <em>after</em> execution of <em>after</em>
   * lifecycle callbacks of the underlying test framework &mdash; for example,
   * by tearing down test fixtures.
   * <p>This method <strong>must</strong> be called immediately after
   * framework-specific <em>after</em> lifecycle callbacks. For historical
   * reasons, this method is named {@code afterTestMethod}. Since the
   * introduction of {@link #afterTestExecution}, a more suitable name for
   * this method might be something like {@code afterTestTearDown} or
   * {@code afterEach}; however, it is unfortunately impossible to rename
   * this method due to backward compatibility concerns.
   * <p>The default implementation is <em>empty</em>. Can be overridden by
   * concrete classes as necessary.
   *
   * @param testContext the test context in which the test method was
   * executed; never {@code null}
   * @throws Exception allows any exception to propagate
   * @see #beforeTestMethod
   * @see #beforeTestExecution
   * @see #afterTestExecution
   */
  default void afterTestMethod(TestContext testContext) throws Exception {

  }

  /**
   * Post-processes a test class <em>after</em> execution of all tests within
   * the class.
   * <p>This method should be called immediately after framework-specific
   * <em>after class</em> lifecycle callbacks.
   * <p>The default implementation is <em>empty</em>. Can be overridden by
   * concrete classes as necessary.
   *
   * @param testContext the test context for the test; never {@code null}
   * @throws Exception allows any exception to propagate
   */
  default void afterTestClass(TestContext testContext) throws Exception {

  }

}
