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

package cn.taketoday.test.classpath;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.lang.reflect.Method;
import java.net.URLClassLoader;

import cn.taketoday.util.CollectionUtils;

/**
 * A custom {@link Extension} that runs tests using a modified class path. Entries are
 * excluded from the class path using {@link ClassPathExclusions @ClassPathExclusions} and
 * overridden using {@link ClassPathOverrides @ClassPathOverrides} on the test class. For
 * an unchanged copy of the class path {@link ForkedClassPath @ForkedClassPath} can be
 * used. A class loader is created with the customized class path and is used both to load
 * the test class and as the thread context class loader while the test is being run.
 *
 * @author Christoph Dreis
 */
class ModifiedClassPathExtension implements InvocationInterceptor {

  @Override
  public void interceptBeforeAllMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptBeforeEachMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptAfterEachMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptAfterAllMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    intercept(invocation, extensionContext);
  }

  @Override
  public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {
    interceptMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptTestTemplateMethod(Invocation<Void> invocation,
          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
    interceptMethod(invocation, invocationContext, extensionContext);
  }

  private void interceptMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
          ExtensionContext extensionContext) throws Throwable {
    if (isModifiedClassPathClassLoader(extensionContext)) {
      invocation.proceed();
      return;
    }
    Class<?> testClass = extensionContext.getRequiredTestClass();
    Method testMethod = invocationContext.getExecutable();
    URLClassLoader modifiedClassLoader = ModifiedClassPathClassLoader.get(
            testClass, testMethod, invocationContext.getArguments());
    if (modifiedClassLoader == null) {
      invocation.proceed();
      return;
    }
    invocation.skip();
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(modifiedClassLoader);
    try {
      runTest(extensionContext.getUniqueId());
    }
    finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private void runTest(String testId) throws Throwable {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectUniqueId(testId)).build();
    Launcher launcher = LauncherFactory.create();
    TestPlan testPlan = launcher.discover(request);
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(listener);
    launcher.execute(testPlan);
    TestExecutionSummary summary = listener.getSummary();
    if (CollectionUtils.isNotEmpty(summary.getFailures())) {
      throw summary.getFailures().get(0).getException();
    }
  }

  private void intercept(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
    if (isModifiedClassPathClassLoader(extensionContext)) {
      invocation.proceed();
      return;
    }
    invocation.skip();
  }

  private boolean isModifiedClassPathClassLoader(ExtensionContext extensionContext) {
    Class<?> testClass = extensionContext.getRequiredTestClass();
    ClassLoader classLoader = testClass.getClassLoader();
    return classLoader.getClass().getName().equals(ModifiedClassPathClassLoader.class.getName());
  }

}
