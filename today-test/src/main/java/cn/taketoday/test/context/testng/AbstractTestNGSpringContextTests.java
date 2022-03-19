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

package cn.taketoday.test.context.testng;

import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.event.ApplicationEventsTestExecutionListener;
import cn.taketoday.test.context.event.EventPublishingTestExecutionListener;
import cn.taketoday.test.context.junit4.AbstractJUnit4SpringContextTests;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;
import cn.taketoday.test.context.web.ServletTestExecutionListener;

/**
 * Abstract base test class which integrates the <em>Spring TestContext Framework</em>
 * with explicit {@link ApplicationContext} testing support in a <strong>TestNG</strong>
 * environment.
 *
 * <p>Concrete subclasses should typically declare a class-level
 * {@link ContextConfiguration @ContextConfiguration} annotation to
 * configure the {@linkplain ApplicationContext application context} {@linkplain
 * ContextConfiguration#locations() resource locations} or {@linkplain
 * ContextConfiguration#classes() component classes}. <em>If your test does not
 * need to load an application context, you may choose to omit the
 * {@link ContextConfiguration @ContextConfiguration} declaration and to configure
 * the appropriate {@link TestExecutionListener
 * TestExecutionListeners} manually.</em> Concrete subclasses must also have
 * constructors which either implicitly or explicitly delegate to {@code super();}.
 *
 * <p>The following {@link TestExecutionListener
 * TestExecutionListeners} are configured by default:
 *
 * <ul>
 * <li>{@link ServletTestExecutionListener}
 * <li>{@link DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link ApplicationEventsTestExecutionListener}
 * <li>{@link DependencyInjectionTestExecutionListener}
 * <li>{@link DirtiesContextTestExecutionListener}
 * <li>{@link EventPublishingTestExecutionListener}
 * </ul>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see ContextConfiguration
 * @see TestContext
 * @see TestContextManager
 * @see TestExecutionListeners
 * @see ServletTestExecutionListener
 * @see DirtiesContextBeforeModesTestExecutionListener
 * @see ApplicationEventsTestExecutionListener
 * @see DependencyInjectionTestExecutionListener
 * @see DirtiesContextTestExecutionListener
 * @see EventPublishingTestExecutionListener
 * @see AbstractTransactionalTestNGSpringContextTests
 * @see AbstractJUnit4SpringContextTests
 * @since 4.0
 */
@TestExecutionListeners({ ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
        ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class, EventPublishingTestExecutionListener.class })
public abstract class AbstractTestNGSpringContextTests implements IHookable, ApplicationContextAware {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * The {@link ApplicationContext} that was injected into this test instance
   * via {@link #setApplicationContext(ApplicationContext)}.
   */
  @Nullable
  protected ApplicationContext applicationContext;

  private final TestContextManager testContextManager;

  @Nullable
  private Throwable testException;

  /**
   * Construct a new {@code AbstractTestNGSpringContextTests} instance and initialize
   * the internal {@link TestContextManager} for the current test class.
   */
  public AbstractTestNGSpringContextTests() {
    this.testContextManager = new TestContextManager(getClass());
  }

  /**
   * Set the {@link ApplicationContext} to be used by this test instance,
   * provided via {@link ApplicationContextAware} semantics.
   *
   * @param applicationContext the ApplicationContext that this test runs in
   */
  @Override
  public final void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Delegates to the configured {@link TestContextManager} to call
   * {@linkplain TestContextManager#beforeTestClass() 'before test class'} callbacks.
   *
   * @throws Exception if a registered TestExecutionListener throws an exception
   */
  @BeforeClass(alwaysRun = true)
  protected void springTestContextBeforeTestClass() throws Exception {
    this.testContextManager.beforeTestClass();
  }

  /**
   * Delegates to the configured {@link TestContextManager} to
   * {@linkplain TestContextManager#prepareTestInstance(Object) prepare} this test
   * instance prior to execution of any individual tests, for example for
   * injecting dependencies, etc.
   *
   * @throws Exception if a registered TestExecutionListener throws an exception
   */
  @BeforeClass(alwaysRun = true, dependsOnMethods = "springTestContextBeforeTestClass")
  protected void springTestContextPrepareTestInstance() throws Exception {
    this.testContextManager.prepareTestInstance(this);
  }

  /**
   * Delegates to the configured {@link TestContextManager} to
   * {@linkplain TestContextManager#beforeTestMethod(Object, Method) pre-process}
   * the test method before the actual test is executed.
   *
   * @param testMethod the test method which is about to be executed
   * @throws Exception allows all exceptions to propagate
   */
  @BeforeMethod(alwaysRun = true)
  protected void springTestContextBeforeTestMethod(Method testMethod) throws Exception {
    this.testContextManager.beforeTestMethod(this, testMethod);
  }

  /**
   * Delegates to the {@linkplain IHookCallBack#runTestMethod(ITestResult) test
   * method} in the supplied {@code callback} to execute the actual test
   * and then tracks the exception thrown during test execution, if any.
   *
   * @see org.testng.IHookable#run(IHookCallBack, ITestResult)
   */
  @Override
  public void run(IHookCallBack callBack, ITestResult testResult) {
    Method testMethod = testResult.getMethod().getConstructorOrMethod().getMethod();
    boolean beforeCallbacksExecuted = false;

    try {
      this.testContextManager.beforeTestExecution(this, testMethod);
      beforeCallbacksExecuted = true;
    }
    catch (Throwable ex) {
      this.testException = ex;
    }

    if (beforeCallbacksExecuted) {
      callBack.runTestMethod(testResult);
      this.testException = getTestResultException(testResult);
    }

    try {
      this.testContextManager.afterTestExecution(this, testMethod, this.testException);
    }
    catch (Throwable ex) {
      if (this.testException == null) {
        this.testException = ex;
      }
    }

    if (this.testException != null) {
      throwAsUncheckedException(this.testException);
    }
  }

  /**
   * Delegates to the configured {@link TestContextManager} to
   * {@linkplain TestContextManager#afterTestMethod(Object, Method, Throwable)
   * post-process} the test method after the actual test has executed.
   *
   * @param testMethod the test method which has just been executed on the
   * test instance
   * @throws Exception allows all exceptions to propagate
   */
  @AfterMethod(alwaysRun = true)
  protected void springTestContextAfterTestMethod(Method testMethod) throws Exception {
    try {
      this.testContextManager.afterTestMethod(this, testMethod, this.testException);
    }
    finally {
      this.testException = null;
    }
  }

  /**
   * Delegates to the configured {@link TestContextManager} to call
   * {@linkplain TestContextManager#afterTestClass() 'after test class'} callbacks.
   *
   * @throws Exception if a registered TestExecutionListener throws an exception
   */
  @AfterClass(alwaysRun = true)
  protected void springTestContextAfterTestClass() throws Exception {
    this.testContextManager.afterTestClass();
  }

  private Throwable getTestResultException(ITestResult testResult) {
    Throwable testResultException = testResult.getThrowable();
    if (testResultException instanceof InvocationTargetException) {
      testResultException = testResultException.getCause();
    }
    return testResultException;
  }

  private RuntimeException throwAsUncheckedException(Throwable t) {
    throwAs(t);
    // Appeasing the compiler: the following line will never be executed.
    throw new IllegalStateException(t);
  }

  @SuppressWarnings("unchecked")
  private <T extends Throwable> void throwAs(Throwable t) throws T {
    throw (T) t;
  }

}
