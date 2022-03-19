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

package cn.taketoday.test.context.support;

import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.core.Conventions;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListener;

/**
 * {@code TestExecutionListener} which provides support for dependency
 * injection and initialization of test instances.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DependencyInjectionTestExecutionListener extends AbstractTestExecutionListener {

  /**
   * Attribute name for a {@link TestContext} attribute which indicates
   * whether or not the dependencies of a test instance should be
   * <em>reinjected</em> in
   * {@link #beforeTestMethod(TestContext) beforeTestMethod()}. Note that
   * dependencies will be injected in
   * {@link #prepareTestInstance(TestContext) prepareTestInstance()} in any
   * case.
   * <p>Clients of a {@link TestContext} (e.g., other
   * {@link TestExecutionListener TestExecutionListeners})
   * may therefore choose to set this attribute to signal that dependencies
   * should be reinjected <em>between</em> execution of individual test
   * methods.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String REINJECT_DEPENDENCIES_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          DependencyInjectionTestExecutionListener.class, "reinjectDependencies");

  private static final Logger logger = LoggerFactory.getLogger(DependencyInjectionTestExecutionListener.class);

  /**
   * Returns {@code 2000}.
   */
  @Override
  public final int getOrder() {
    return 2000;
  }

  /**
   * Performs dependency injection on the
   * {@link TestContext#getTestInstance() test instance} of the supplied
   * {@link TestContext test context} by
   * {@link AutowireCapableBeanFactory#autowireBeanProperties(Object, int, boolean) autowiring}
   * and
   * {@link AutowireCapableBeanFactory#initializeBean(Object, String) initializing}
   * the test instance via its own
   * {@link TestContext#getApplicationContext() application context} (without
   * checking dependencies).
   * <p>The {@link #REINJECT_DEPENDENCIES_ATTRIBUTE} will be subsequently removed
   * from the test context, regardless of its value.
   */
  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("Performing dependency injection for test context [" + testContext + "].");
    }
    injectDependencies(testContext);
  }

  /**
   * If the {@link #REINJECT_DEPENDENCIES_ATTRIBUTE} in the supplied
   * {@link TestContext test context} has a value of {@link Boolean#TRUE},
   * this method will have the same effect as
   * {@link #prepareTestInstance(TestContext) prepareTestInstance()};
   * otherwise, this method will have no effect.
   */
  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    if (Boolean.TRUE.equals(testContext.getAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE))) {
      if (logger.isDebugEnabled()) {
        logger.debug("Reinjecting dependencies for test context [" + testContext + "].");
      }
      injectDependencies(testContext);
    }
  }

  /**
   * Performs dependency injection and bean initialization for the supplied
   * {@link TestContext} as described in
   * {@link #prepareTestInstance(TestContext) prepareTestInstance()}.
   * <p>The {@link #REINJECT_DEPENDENCIES_ATTRIBUTE} will be subsequently removed
   * from the test context, regardless of its value.
   *
   * @param testContext the test context for which dependency injection should
   * be performed (never {@code null})
   * @throws Exception allows any exception to propagate
   * @see #prepareTestInstance(TestContext)
   * @see #beforeTestMethod(TestContext)
   */
  protected void injectDependencies(TestContext testContext) throws Exception {
    Object bean = testContext.getTestInstance();
    Class<?> clazz = testContext.getTestClass();
    AutowireCapableBeanFactory beanFactory = testContext.getApplicationContext().getAutowireCapableBeanFactory();
    beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
    beanFactory.initializeBean(bean, clazz.getName() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX);
    testContext.removeAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE);
  }

}
