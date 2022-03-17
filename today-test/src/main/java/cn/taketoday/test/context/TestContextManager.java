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

package cn.taketoday.test.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.junit4.rules.SpringMethodRule;
import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@code TestContextManager} is the main entry point into the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Specifically, a {@code TestContextManager} is responsible for managing a
 * single {@link TestContext} and signaling events to each registered
 * {@link TestExecutionListener} at the following test execution points.
 *
 * <ul>
 * <li>{@link #beforeTestClass() before test class execution}: prior to any
 * <em>before class callbacks</em> of a particular testing framework (e.g.,
 * JUnit 4's {@link org.junit.BeforeClass @BeforeClass})</li>
 * <li>{@link #prepareTestInstance test instance preparation}:
 * immediately following instantiation of the test class</li>
 * <li>{@link #beforeTestMethod before test setup}:
 * prior to any <em>before method callbacks</em> of a particular testing framework
 * (e.g., JUnit 4's {@link org.junit.Before @Before})</li>
 * <li>{@link #beforeTestExecution before test execution}:
 * immediately before execution of the {@linkplain Method
 * test method} but after test setup</li>
 * <li>{@link #afterTestExecution after test execution}:
 * immediately after execution of the {@linkplain Method
 * test method} but before test tear down</li>
 * <li>{@link #afterTestMethod(Object, Method, Throwable) after test tear down}:
 * after any <em>after method callbacks</em> of a particular testing
 * framework (e.g., JUnit 4's {@link org.junit.After @After})</li>
 * <li>{@link #afterTestClass() after test class execution}: after any
 * <em>after class callbacks</em> of a particular testing framework (e.g., JUnit 4's
 * {@link org.junit.AfterClass @AfterClass})</li>
 * </ul>
 *
 * <p>Support for loading and accessing
 * {@linkplain cn.taketoday.context.ApplicationContext application contexts},
 * dependency injection of test instances,
 * {@linkplain cn.taketoday.transaction.annotation.Transactional transactional}
 * execution of test methods, etc. is provided by
 * {@link SmartContextLoader ContextLoaders} and {@code TestExecutionListeners},
 * which are configured via {@link ContextConfiguration @ContextConfiguration} and
 * {@link TestExecutionListeners @TestExecutionListeners}, respectively.
 *
 * <p>Bootstrapping of the {@code TestContext}, the default {@code ContextLoader},
 * default {@code TestExecutionListeners}, and their collaborators is performed
 * by a {@link TestContextBootstrapper}, which is configured via
 * {@link BootstrapWith @BootstrapWith}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 * @see TestContext
 * @see TestExecutionListener
 * @see TestExecutionListeners
 * @see ContextConfiguration
 * @see ContextHierarchy
 * @since 2.5
 */
public class TestContextManager {

  private static final Log logger = LogFactory.getLog(TestContextManager.class);

  private final TestContext testContext;

  private final ThreadLocal<TestContext> testContextHolder = ThreadLocal.withInitial(
          // Implemented as an anonymous inner class instead of a lambda expression due to a bug
          // in Eclipse IDE: "The blank final field testContext may not have been initialized"
          new Supplier<TestContext>() {
            @Override
            public TestContext get() {
              return copyTestContext(TestContextManager.this.testContext);
            }
          });

  private final List<TestExecutionListener> testExecutionListeners = new ArrayList<>();

  /**
   * Construct a new {@code TestContextManager} for the supplied {@linkplain Class test class}.
   * <p>Delegates to {@link #TestContextManager(TestContextBootstrapper)} with
   * the {@link TestContextBootstrapper} configured for the test class. If the
   * {@link BootstrapWith @BootstrapWith} annotation is present on the test
   * class, either directly or as a meta-annotation, then its
   * {@link BootstrapWith#value value} will be used as the bootstrapper type;
   * otherwise, the {@link DefaultTestContextBootstrapper
   * DefaultTestContextBootstrapper} will be used.
   *
   * @param testClass the test class to be managed
   * @see #TestContextManager(TestContextBootstrapper)
   */
  public TestContextManager(Class<?> testClass) {
    this(BootstrapUtils.resolveTestContextBootstrapper(BootstrapUtils.createBootstrapContext(testClass)));
  }

  /**
   * Construct a new {@code TestContextManager} using the supplied {@link TestContextBootstrapper}
   * and {@linkplain #registerTestExecutionListeners register} the necessary
   * {@link TestExecutionListener TestExecutionListeners}.
   * <p>Delegates to the supplied {@code TestContextBootstrapper} for building
   * the {@code TestContext} and retrieving the {@code TestExecutionListeners}.
   *
   * @param testContextBootstrapper the bootstrapper to use
   * @see TestContextBootstrapper#buildTestContext
   * @see TestContextBootstrapper#getTestExecutionListeners
   * @see #registerTestExecutionListeners
   */
  public TestContextManager(TestContextBootstrapper testContextBootstrapper) {
    this.testContext = testContextBootstrapper.buildTestContext();
    registerTestExecutionListeners(testContextBootstrapper.getTestExecutionListeners());
  }

  /**
   * Get the {@link TestContext} managed by this {@code TestContextManager}.
   */
  public final TestContext getTestContext() {
    return this.testContextHolder.get();
  }

  /**
   * Register the supplied list of {@link TestExecutionListener TestExecutionListeners}
   * by appending them to the list of listeners used by this {@code TestContextManager}.
   *
   * @see #registerTestExecutionListeners(TestExecutionListener...)
   */
  public void registerTestExecutionListeners(List<TestExecutionListener> testExecutionListeners) {
    registerTestExecutionListeners(testExecutionListeners.toArray(new TestExecutionListener[0]));
  }

  /**
   * Register the supplied array of {@link TestExecutionListener TestExecutionListeners}
   * by appending them to the list of listeners used by this {@code TestContextManager}.
   */
  public void registerTestExecutionListeners(TestExecutionListener... testExecutionListeners) {
    for (TestExecutionListener listener : testExecutionListeners) {
      if (logger.isTraceEnabled()) {
        logger.trace("Registering TestExecutionListener: " + listener);
      }
      this.testExecutionListeners.add(listener);
    }
  }

  /**
   * Get the current {@link TestExecutionListener TestExecutionListeners}
   * registered for this {@code TestContextManager}.
   * <p>Allows for modifications, e.g. adding a listener to the beginning of the list.
   * However, make sure to keep the list stable while actually executing tests.
   */
  public final List<TestExecutionListener> getTestExecutionListeners() {
    return this.testExecutionListeners;
  }

  /**
   * Get a copy of the {@link TestExecutionListener TestExecutionListeners}
   * registered for this {@code TestContextManager} in reverse order.
   */
  private List<TestExecutionListener> getReversedTestExecutionListeners() {
    List<TestExecutionListener> listenersReversed = new ArrayList<>(getTestExecutionListeners());
    Collections.reverse(listenersReversed);
    return listenersReversed;
  }

  /**
   * Hook for pre-processing a test class <em>before</em> execution of any
   * tests within the class. Should be called prior to any framework-specific
   * <em>before class methods</em> (e.g., methods annotated with JUnit 4's
   * {@link org.junit.BeforeClass @BeforeClass}).
   * <p>An attempt will be made to give each registered
   * {@link TestExecutionListener} a chance to pre-process the test class
   * execution. If a listener throws an exception, however, the remaining
   * registered listeners will <strong>not</strong> be called.
   *
   * @throws Exception if a registered TestExecutionListener throws an
   * exception
   * @see #getTestExecutionListeners()
   * @since 3.0
   */
  public void beforeTestClass() throws Exception {
    Class<?> testClass = getTestContext().getTestClass();
    if (logger.isTraceEnabled()) {
      logger.trace("beforeTestClass(): class [" + testClass.getName() + "]");
    }
    getTestContext().updateState(null, null, null);

    for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
      try {
        testExecutionListener.beforeTestClass(getTestContext());
      }
      catch (Throwable ex) {
        logException(ex, "beforeTestClass", testExecutionListener, testClass);
        ReflectionUtils.rethrowException(ex);
      }
    }
  }

  /**
   * Hook for preparing a test instance prior to execution of any individual
   * test methods &mdash; for example, to inject dependencies.
   * <p>This method should be called immediately after instantiation of the test
   * class or as soon after instantiation as possible (as is the case with the
   * {@link SpringMethodRule
   * SpringMethodRule}). In any case, this method must be called prior to any
   * framework-specific lifecycle callbacks.
   * <p>The managed {@link TestContext} will be updated with the supplied
   * {@code testInstance}.
   * <p>An attempt will be made to give each registered
   * {@link TestExecutionListener} a chance to prepare the test instance. If a
   * listener throws an exception, however, the remaining registered listeners
   * will <strong>not</strong> be called.
   *
   * @param testInstance the test instance to prepare (never {@code null})
   * @throws Exception if a registered TestExecutionListener throws an exception
   * @see #getTestExecutionListeners()
   */
  public void prepareTestInstance(Object testInstance) throws Exception {
    if (logger.isTraceEnabled()) {
      logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
    }
    getTestContext().updateState(testInstance, null, null);

    for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
      try {
        testExecutionListener.prepareTestInstance(getTestContext());
      }
      catch (Throwable ex) {
        if (logger.isErrorEnabled()) {
          logger.error("Caught exception while allowing TestExecutionListener [" + testExecutionListener +
                  "] to prepare test instance [" + testInstance + "]", ex);
        }
        ReflectionUtils.rethrowException(ex);
      }
    }
  }

  /**
   * Hook for pre-processing a test <em>before</em> execution of <em>before</em>
   * lifecycle callbacks of the underlying test framework &mdash; for example,
   * setting up test fixtures, starting a transaction, etc.
   * <p>This method <strong>must</strong> be called immediately prior to
   * framework-specific <em>before</em> lifecycle callbacks (e.g., methods
   * annotated with JUnit 4's {@link org.junit.Before @Before}). For historical
   * reasons, this method is named {@code beforeTestMethod}. Since the
   * introduction of {@link #beforeTestExecution}, a more suitable name for
   * this method might be something like {@code beforeTestSetUp} or
   * {@code beforeEach}; however, it is unfortunately impossible to rename
   * this method due to backward compatibility concerns.
   * <p>The managed {@link TestContext} will be updated with the supplied
   * {@code testInstance} and {@code testMethod}.
   * <p>An attempt will be made to give each registered
   * {@link TestExecutionListener} a chance to perform its pre-processing.
   * If a listener throws an exception, however, the remaining registered
   * listeners will <strong>not</strong> be called.
   *
   * @param testInstance the current test instance (never {@code null})
   * @param testMethod the test method which is about to be executed on the
   * test instance
   * @throws Exception if a registered TestExecutionListener throws an exception
   * @see #afterTestMethod
   * @see #beforeTestExecution
   * @see #afterTestExecution
   * @see #getTestExecutionListeners()
   */
  public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
    String callbackName = "beforeTestMethod";
    prepareForBeforeCallback(callbackName, testInstance, testMethod);

    for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
      try {
        testExecutionListener.beforeTestMethod(getTestContext());
      }
      catch (Throwable ex) {
        handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
      }
    }
  }

  /**
   * Hook for pre-processing a test <em>immediately before</em> execution of
   * the {@linkplain Method test method} in the supplied
   * {@linkplain TestContext test context} &mdash; for example, for timing
   * or logging purposes.
   * <p>This method <strong>must</strong> be called after framework-specific
   * <em>before</em> lifecycle callbacks (e.g., methods annotated with JUnit 4's
   * {@link org.junit.Before @Before}).
   * <p>The managed {@link TestContext} will be updated with the supplied
   * {@code testInstance} and {@code testMethod}.
   * <p>An attempt will be made to give each registered
   * {@link TestExecutionListener} a chance to perform its pre-processing.
   * If a listener throws an exception, however, the remaining registered
   * listeners will <strong>not</strong> be called.
   *
   * @param testInstance the current test instance (never {@code null})
   * @param testMethod the test method which is about to be executed on the
   * test instance
   * @throws Exception if a registered TestExecutionListener throws an exception
   * @see #beforeTestMethod
   * @see #afterTestMethod
   * @see #beforeTestExecution
   * @see #afterTestExecution
   * @see #getTestExecutionListeners()
   * @since 4.0
   */
  public void beforeTestExecution(Object testInstance, Method testMethod) throws Exception {
    String callbackName = "beforeTestExecution";
    prepareForBeforeCallback(callbackName, testInstance, testMethod);

    for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
      try {
        testExecutionListener.beforeTestExecution(getTestContext());
      }
      catch (Throwable ex) {
        handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
      }
    }
  }

  /**
   * Hook for post-processing a test <em>immediately after</em> execution of
   * the {@linkplain Method test method} in the supplied
   * {@linkplain TestContext test context} &mdash; for example, for timing
   * or logging purposes.
   * <p>This method <strong>must</strong> be called before framework-specific
   * <em>after</em> lifecycle callbacks (e.g., methods annotated with JUnit 4's
   * {@link org.junit.After @After}).
   * <p>The managed {@link TestContext} will be updated with the supplied
   * {@code testInstance}, {@code testMethod}, and {@code exception}.
   * <p>Each registered {@link TestExecutionListener} will be given a chance
   * to perform its post-processing. If a listener throws an exception, the
   * remaining registered listeners will still be called. After all listeners
   * have executed, the first caught exception will be rethrown with any
   * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
   * the first exception.
   * <p>Note that registered listeners will be executed in the opposite
   * order in which they were registered.
   *
   * @param testInstance the current test instance (never {@code null})
   * @param testMethod the test method which has just been executed on the
   * test instance
   * @param exception the exception that was thrown during execution of the
   * test method or by a TestExecutionListener, or {@code null} if none
   * was thrown
   * @throws Exception if a registered TestExecutionListener throws an exception
   * @see #beforeTestMethod
   * @see #afterTestMethod
   * @see #beforeTestExecution
   * @see #getTestExecutionListeners()
   * @see Throwable#addSuppressed(Throwable)
   * @since 4.0
   */
  public void afterTestExecution(Object testInstance, Method testMethod, @Nullable Throwable exception)
          throws Exception {

    String callbackName = "afterTestExecution";
    prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
    Throwable afterTestExecutionException = null;

    // Traverse the TestExecutionListeners in reverse order to ensure proper
    // "wrapper"-style execution of listeners.
    for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
      try {
        testExecutionListener.afterTestExecution(getTestContext());
      }
      catch (Throwable ex) {
        logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
        if (afterTestExecutionException == null) {
          afterTestExecutionException = ex;
        }
        else {
          afterTestExecutionException.addSuppressed(ex);
        }
      }
    }

    if (afterTestExecutionException != null) {
      ReflectionUtils.rethrowException(afterTestExecutionException);
    }
  }

  /**
   * Hook for post-processing a test <em>after</em> execution of <em>after</em>
   * lifecycle callbacks of the underlying test framework &mdash; for example,
   * tearing down test fixtures, ending a transaction, etc.
   * <p>This method <strong>must</strong> be called immediately after
   * framework-specific <em>after</em> lifecycle callbacks (e.g., methods
   * annotated with JUnit 4's {@link org.junit.After @After}). For historical
   * reasons, this method is named {@code afterTestMethod}. Since the
   * introduction of {@link #afterTestExecution}, a more suitable name for
   * this method might be something like {@code afterTestTearDown} or
   * {@code afterEach}; however, it is unfortunately impossible to rename
   * this method due to backward compatibility concerns.
   * <p>The managed {@link TestContext} will be updated with the supplied
   * {@code testInstance}, {@code testMethod}, and {@code exception}.
   * <p>Each registered {@link TestExecutionListener} will be given a chance
   * to perform its post-processing. If a listener throws an exception, the
   * remaining registered listeners will still be called. After all listeners
   * have executed, the first caught exception will be rethrown with any
   * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
   * the first exception.
   * <p>Note that registered listeners will be executed in the opposite
   *
   * @param testInstance the current test instance (never {@code null})
   * @param testMethod the test method which has just been executed on the
   * test instance
   * @param exception the exception that was thrown during execution of the test
   * method or by a TestExecutionListener, or {@code null} if none was thrown
   * @throws Exception if a registered TestExecutionListener throws an exception
   * @see #beforeTestMethod
   * @see #beforeTestExecution
   * @see #afterTestExecution
   * @see #getTestExecutionListeners()
   * @see Throwable#addSuppressed(Throwable)
   */
  public void afterTestMethod(Object testInstance, Method testMethod, @Nullable Throwable exception)
          throws Exception {

    String callbackName = "afterTestMethod";
    prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
    Throwable afterTestMethodException = null;

    // Traverse the TestExecutionListeners in reverse order to ensure proper
    // "wrapper"-style execution of listeners.
    for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
      try {
        testExecutionListener.afterTestMethod(getTestContext());
      }
      catch (Throwable ex) {
        logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
        if (afterTestMethodException == null) {
          afterTestMethodException = ex;
        }
        else {
          afterTestMethodException.addSuppressed(ex);
        }
      }
    }

    if (afterTestMethodException != null) {
      ReflectionUtils.rethrowException(afterTestMethodException);
    }
  }

  /**
   * Hook for post-processing a test class <em>after</em> execution of all
   * tests within the class. Should be called after any framework-specific
   * <em>after class methods</em> (e.g., methods annotated with JUnit 4's
   * {@link org.junit.AfterClass @AfterClass}).
   * <p>Each registered {@link TestExecutionListener} will be given a chance
   * to perform its post-processing. If a listener throws an exception, the
   * remaining registered listeners will still be called. After all listeners
   * have executed, the first caught exception will be rethrown with any
   * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
   * the first exception.
   * <p>Note that registered listeners will be executed in the opposite
   *
   * @throws Exception if a registered TestExecutionListener throws an exception
   * @see #getTestExecutionListeners()
   * @see Throwable#addSuppressed(Throwable)
   * @since 3.0
   */
  public void afterTestClass() throws Exception {
    Class<?> testClass = getTestContext().getTestClass();
    if (logger.isTraceEnabled()) {
      logger.trace("afterTestClass(): class [" + testClass.getName() + "]");
    }
    getTestContext().updateState(null, null, null);

    Throwable afterTestClassException = null;
    // Traverse the TestExecutionListeners in reverse order to ensure proper
    // "wrapper"-style execution of listeners.
    for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
      try {
        testExecutionListener.afterTestClass(getTestContext());
      }
      catch (Throwable ex) {
        logException(ex, "afterTestClass", testExecutionListener, testClass);
        if (afterTestClassException == null) {
          afterTestClassException = ex;
        }
        else {
          afterTestClassException.addSuppressed(ex);
        }
      }
    }

    this.testContextHolder.remove();

    if (afterTestClassException != null) {
      ReflectionUtils.rethrowException(afterTestClassException);
    }
  }

  private void prepareForBeforeCallback(String callbackName, Object testInstance, Method testMethod) {
    if (logger.isTraceEnabled()) {
      logger.trace(String.format("%s(): instance [%s], method [%s]", callbackName, testInstance, testMethod));
    }
    getTestContext().updateState(testInstance, testMethod, null);
  }

  private void prepareForAfterCallback(String callbackName, Object testInstance, Method testMethod,
          @Nullable Throwable exception) {

    if (logger.isTraceEnabled()) {
      logger.trace(String.format("%s(): instance [%s], method [%s], exception [%s]",
              callbackName, testInstance, testMethod, exception));
    }
    getTestContext().updateState(testInstance, testMethod, exception);
  }

  private void handleBeforeException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
          Object testInstance, Method testMethod) throws Exception {

    logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
    ReflectionUtils.rethrowException(ex);
  }

  private void logException(
          Throwable ex, String callbackName, TestExecutionListener testExecutionListener, Class<?> testClass) {

    if (logger.isWarnEnabled()) {
      logger.warn(String.format("Caught exception while invoking '%s' callback on " +
                      "TestExecutionListener [%s] for test class [%s]", callbackName, testExecutionListener,
              testClass), ex);
    }
  }

  private void logException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
          Object testInstance, Method testMethod) {

    if (logger.isWarnEnabled()) {
      logger.warn(String.format("Caught exception while invoking '%s' callback on " +
                      "TestExecutionListener [%s] for test method [%s] and test instance [%s]",
              callbackName, testExecutionListener, testMethod, testInstance), ex);
    }
  }

  /**
   * Attempt to create a copy of the supplied {@code TestContext} using its
   * <em>copy constructor</em>.
   */
  private static TestContext copyTestContext(TestContext testContext) {
    Constructor<? extends TestContext> constructor =
            ReflectionUtils.getConstructorIfAvailable(testContext.getClass(), testContext.getClass());

    if (constructor != null) {
      try {
        ReflectionUtils.makeAccessible(constructor);
        return constructor.newInstance(testContext);
      }
      catch (Exception ex) {
        if (logger.isInfoEnabled()) {
          logger.info(String.format("Failed to invoke copy constructor for [%s]; " +
                          "concurrent test execution is therefore likely not supported.",
                  testContext), ex);
        }
      }
    }

    // Fallback to original instance
    return testContext;
  }

}
