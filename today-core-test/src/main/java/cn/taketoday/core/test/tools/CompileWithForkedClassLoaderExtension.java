/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.tools;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.commons.util.ReflectionUtils.getFullyQualifiedMethodName;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

/**
 * JUnit Jupiter {@link InvocationInterceptor} to support
 * {@link CompileWithForkedClassLoader @CompileWithForkedClassLoader}.
 *
 * @author Christoph Dreis
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
class CompileWithForkedClassLoaderExtension implements InvocationInterceptor {

  @Override
  public void interceptBeforeAllMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {

    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptBeforeEachMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {

    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptAfterEachMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {

    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptAfterAllMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {

    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptTestMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {

    intercept(invocation, extensionContext,
            () -> runTestWithModifiedClassPath(invocationContext, extensionContext));
  }

  private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext)
          throws Throwable {

    intercept(invocation, extensionContext, Action.NONE);
  }

  private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext,
          Action action) throws Throwable {

    if (isUsingForkedClassPathLoader(extensionContext)) {
      invocation.proceed();
      return;
    }
    invocation.skip();
    action.run();
  }

  private boolean isUsingForkedClassPathLoader(ExtensionContext extensionContext) {
    Class<?> testClass = extensionContext.getRequiredTestClass();
    ClassLoader classLoader = testClass.getClassLoader();
    return classLoader.getClass().getName()
            .equals(CompileWithForkedClassLoaderClassLoader.class.getName());
  }

  private void runTestWithModifiedClassPath(
          ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {

    Class<?> testClass = extensionContext.getRequiredTestClass();
    Method testMethod = invocationContext.getExecutable();
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader forkedClassPathClassLoader = new CompileWithForkedClassLoaderClassLoader(
            testClass.getClassLoader());
    Thread.currentThread().setContextClassLoader(forkedClassPathClassLoader);
    try {
      runTest(testClass, testMethod);
    }
    finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private void runTest(Class<?> testClass, Method testMethod) throws Throwable {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectMethod(getFullyQualifiedMethodName(testClass, testMethod)))
            .filters(includeEngines("junit-jupiter"))
            .build();
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    Launcher launcher = LauncherFactory.create();
    launcher.execute(request, listener);
    TestExecutionSummary summary = listener.getSummary();
    if (summary.getTotalFailureCount() > 0) {
      throw summary.getFailures().get(0).getException();
    }
  }

  @FunctionalInterface
  interface Action {

    Action NONE = () -> { };

    void run() throws Throwable;

  }

}
