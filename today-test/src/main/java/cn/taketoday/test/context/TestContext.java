/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Function;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;

/**
 * {@code TestContext} encapsulates the context in which a test is executed,
 * agnostic of the actual testing framework in use.
 *
 * <p>concrete implementations are highly encouraged
 * to implement a <em>copy constructor</em> in order to allow the immutable state
 * and attributes of a {@code TestContext} to be used as a template for additional
 * contexts created for parallel test execution. The copy constructor must accept a
 * single argument of the type of the concrete implementation. Any implementation
 * that does not provide a copy constructor will likely fail in an environment
 * that executes tests concurrently.
 *
 * @author Sam Brannen
 * @see TestContextManager
 * @see TestExecutionListener
 * @since 4.0
 */
// Suppression required due to bug in javac in Java 8: presence of default method in a Serializable interface
@SuppressWarnings("serial")
public interface TestContext extends AttributeAccessor, Serializable {

  /**
   * Determine if the {@linkplain ApplicationContext application context} for
   * this test context is known to be available.
   * <p>If this method returns {@code true}, a subsequent invocation of
   * {@link #getApplicationContext()} should succeed.
   * <p>The default implementation of this method always returns {@code false}.
   * Custom {@code TestContext} implementations are therefore highly encouraged
   * to override this method with a more meaningful implementation. Note that
   * the standard {@code TestContext} implementation in Infra overrides this
   * method appropriately.
   *
   * @return {@code true} if the application context has already been loaded
   * @see #getApplicationContext()
   */
  default boolean hasApplicationContext() {
    return false;
  }

  /**
   * Get the {@linkplain ApplicationContext application context} for this
   * test context, possibly cached.
   * <p>Implementations of this method are responsible for loading the
   * application context if the corresponding context has not already been
   * loaded, potentially caching the context as well.
   *
   * @return the application context (never {@code null})
   * @throws IllegalStateException if an error occurs while retrieving the
   * application context
   * @see #hasApplicationContext()
   */
  ApplicationContext getApplicationContext();

  /**
   * Publish the {@link ApplicationEvent} created by the given {@code eventFactory}
   * to the {@linkplain ApplicationContext application context} for this
   * test context.
   * <p>The {@code ApplicationEvent} will only be published if the application
   * context for this test context {@linkplain #hasApplicationContext() is available}.
   *
   * @param eventFactory factory for lazy creation of the {@code ApplicationEvent}
   * @see #hasApplicationContext()
   * @see #getApplicationContext()
   */
  default void publishEvent(Function<TestContext, ? extends ApplicationEvent> eventFactory) {
    if (hasApplicationContext()) {
      getApplicationContext().publishEvent(eventFactory.apply(this));
    }
  }

  /**
   * Get the {@linkplain Class test class} for this test context.
   *
   * @return the test class (never {@code null})
   */
  Class<?> getTestClass();

  /**
   * Get the current {@linkplain Object test instance} for this test context.
   * <p>Note: this is a mutable property.
   *
   * @return the current test instance (never {@code null})
   * @see #updateState(Object, Method, Throwable)
   */
  Object getTestInstance();

  /**
   * Get the current {@linkplain Method test method} for this test context.
   * <p>Note: this is a mutable property.
   *
   * @return the current test method (never {@code null})
   * @see #updateState(Object, Method, Throwable)
   */
  Method getTestMethod();

  /**
   * Get the {@linkplain Throwable exception} that was thrown during execution
   * of the {@linkplain #getTestMethod() test method}.
   * <p>Note: this is a mutable property.
   *
   * @return the exception that was thrown, or {@code null} if no exception was thrown
   * @see #updateState(Object, Method, Throwable)
   */
  @Nullable
  Throwable getTestException();

  /**
   * Call this method to signal that the {@linkplain ApplicationContext application
   * context} associated with this test context is <em>dirty</em> and should be
   * removed from the context cache.
   * <p>Do this if a test has modified the context &mdash; for example, by
   * modifying the state of a singleton bean, modifying the state of an embedded
   * database, etc.
   *
   * @param hierarchyMode the context cache clearing mode to be applied if the
   * context is part of a hierarchy (may be {@code null})
   */
  void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode);

  /**
   * Update this test context to reflect the state of the currently executing test.
   * <p><strong>WARNING</strong>: This method should only be invoked by the
   * {@link TestContextManager}.
   * <p>Caution: concurrent invocations of this method might not be thread-safe,
   * depending on the underlying implementation.
   *
   * @param testInstance the current test instance (may be {@code null})
   * @param testMethod the current test method (may be {@code null})
   * @param testException the exception that was thrown in the test method,
   * or {@code null} if no exception was thrown
   */
  void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException);

}
