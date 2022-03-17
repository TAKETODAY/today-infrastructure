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

package cn.taketoday.test.context.event;

import java.io.Serializable;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} which provides support for {@link ApplicationEvents}.
 *
 * <p>This listener manages the registration of {@code ApplicationEvents} for the
 * current thread at various points within the test execution lifecycle and makes
 * the current instance of {@code ApplicationEvents} available to tests via an
 * {@link cn.taketoday.beans.factory.annotation.Autowired @Autowired}
 * field in the test class.
 *
 * <p>If the test class is not annotated or meta-annotated with
 * {@link RecordApplicationEvents @RecordApplicationEvents}, this listener
 * effectively does nothing.
 *
 * @author Sam Brannen
 * @see ApplicationEvents
 * @see ApplicationEventsHolder
 * @since 4.0
 */
public class ApplicationEventsTestExecutionListener extends AbstractTestExecutionListener {

  /**
   * Attribute name for a {@link TestContext} attribute which indicates
   * whether the test class for the given test context is annotated with
   * {@link RecordApplicationEvents @RecordApplicationEvents}.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  private static final String RECORD_APPLICATION_EVENTS = Conventions.getQualifiedAttributeName(
          ApplicationEventsTestExecutionListener.class, "recordApplicationEvents");

  private static final Object applicationEventsMonitor = new Object();

  /**
   * Returns {@code 1800}.
   */
  @Override
  public final int getOrder() {
    return 1800;
  }

  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    if (recordApplicationEvents(testContext)) {
      registerListenerAndResolvableDependencyIfNecessary(testContext.getApplicationContext());
      ApplicationEventsHolder.registerApplicationEvents();
    }
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    if (recordApplicationEvents(testContext)) {
      // Register a new ApplicationEvents instance for the current thread
      // in case the test instance is shared -- for example, in TestNG or
      // JUnit Jupiter with @TestInstance(PER_CLASS) semantics.
      ApplicationEventsHolder.registerApplicationEventsIfNecessary();
    }
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    if (recordApplicationEvents(testContext)) {
      ApplicationEventsHolder.unregisterApplicationEvents();
    }
  }

  private boolean recordApplicationEvents(TestContext testContext) {
    return testContext.computeAttribute(RECORD_APPLICATION_EVENTS, name ->
            TestContextAnnotationUtils.hasAnnotation(testContext.getTestClass(), RecordApplicationEvents.class));
  }

  private void registerListenerAndResolvableDependencyIfNecessary(ApplicationContext applicationContext) {
    Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext,
            "The ApplicationContext for the test must be an AbstractApplicationContext");
    AbstractApplicationContext aac = (AbstractApplicationContext) applicationContext;
    // Synchronize to avoid race condition in parallel test execution
    synchronized(applicationEventsMonitor) {
      boolean notAlreadyRegistered = aac.getApplicationListeners().stream()
              .map(Object::getClass)
              .noneMatch(ApplicationEventsApplicationListener.class::equals);
      if (notAlreadyRegistered) {
        // Register a new ApplicationEventsApplicationListener.
        aac.addApplicationListener(new ApplicationEventsApplicationListener());

        // Register ApplicationEvents as a resolvable dependency for @Autowired support in test classes.
        ConfigurableBeanFactory beanFactory = aac.getBeanFactory();
        beanFactory.registerDependency(ApplicationEvents.class, new ApplicationEventsObjectFactory());
      }
    }
  }

  /**
   * Factory that exposes the current {@link ApplicationEvents} object on demand.
   */
  @SuppressWarnings("serial")
  private static class ApplicationEventsObjectFactory implements Supplier<ApplicationEvents>, Serializable {

    @Override
    public ApplicationEvents get() {
      return ApplicationEventsHolder.getRequiredApplicationEvents();
    }

    @Override
    public String toString() {
      return "Current ApplicationEvents";
    }
  }

}
