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

package cn.taketoday.test.context.junit4.statements;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.test.context.TestContextManager;

/**
 * {@code RunAfterTestExecutionCallbacks} is a custom JUnit {@link Statement}
 * which allows the <em>Spring TestContext Framework</em> to be plugged into the
 * JUnit 4 execution chain by calling {@link TestContextManager#afterTestExecution
 * afterTestExecution()} on the supplied {@link TestContextManager}.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @see #evaluate()
 * @see RunBeforeTestExecutionCallbacks
 * @since 4.0
 */
public class RunAfterTestExecutionCallbacks extends Statement {

  private final Statement next;

  private final Object testInstance;

  private final Method testMethod;

  private final TestContextManager testContextManager;

  /**
   * Construct a new {@code RunAfterTestExecutionCallbacks} statement.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testInstance the current test instance (never {@code null})
   * @param testMethod the test method which has just been executed on the
   * test instance
   * @param testContextManager the TestContextManager upon which to call
   * {@code afterTestExecution()}
   */
  public RunAfterTestExecutionCallbacks(Statement next, Object testInstance, Method testMethod,
          TestContextManager testContextManager) {

    this.next = next;
    this.testInstance = testInstance;
    this.testMethod = testMethod;
    this.testContextManager = testContextManager;
  }

  /**
   * Evaluate the next {@link Statement} in the execution chain (typically an
   * instance of {@link RunBeforeTestExecutionCallbacks}), catching any exceptions
   * thrown, and then invoke {@link TestContextManager#afterTestExecution} supplying
   * the first caught exception (if any).
   * <p>If the invocation of {@code afterTestExecution()} throws an exception, that
   * exception will also be tracked. Multiple exceptions will be combined into a
   * {@link MultipleFailureException}.
   */
  @Override
  public void evaluate() throws Throwable {
    Throwable testException = null;
    List<Throwable> errors = new ArrayList<>();
    try {
      this.next.evaluate();
    }
    catch (Throwable ex) {
      testException = ex;
      errors.add(ex);
    }

    try {
      this.testContextManager.afterTestExecution(this.testInstance, this.testMethod, testException);
    }
    catch (Throwable ex) {
      errors.add(ex);
    }

    MultipleFailureException.assertEmpty(errors);
  }

}
