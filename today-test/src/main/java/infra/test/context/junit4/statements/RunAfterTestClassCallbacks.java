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

package infra.test.context.junit4.statements;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

import infra.test.context.TestContextManager;

/**
 * {@code RunAfterTestClassCallbacks} is a custom JUnit {@link Statement} which allows
 * the <em>TestContext Framework</em> to be plugged into the JUnit execution chain
 * by calling {@link TestContextManager#afterTestClass afterTestClass()} on the supplied
 * {@link TestContextManager}.
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @see #evaluate()
 * @see RunBeforeTestClassCallbacks
 * @since 4.0
 */
public class RunAfterTestClassCallbacks extends Statement {

  private final Statement next;

  private final TestContextManager testContextManager;

  /**
   * Construct a new {@code RunAfterTestClassCallbacks} statement.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testContextManager the TestContextManager upon which to call
   * {@code afterTestClass()}
   */
  public RunAfterTestClassCallbacks(Statement next, TestContextManager testContextManager) {
    this.next = next;
    this.testContextManager = testContextManager;
  }

  /**
   * Evaluate the next {@link Statement} in the execution chain (typically an instance of
   * {@link org.junit.internal.runners.statements.RunAfters RunAfters}), catching any
   * exceptions thrown, and then invoke {@link TestContextManager#afterTestClass()}.
   * <p>If the invocation of {@code afterTestClass()} throws an exception, it will also
   * be tracked. Multiple exceptions will be combined into a {@link MultipleFailureException}.
   */
  @Override
  public void evaluate() throws Throwable {
    List<Throwable> errors = new ArrayList<>();
    try {
      this.next.evaluate();
    }
    catch (Throwable ex) {
      errors.add(ex);
    }

    try {
      this.testContextManager.afterTestClass();
    }
    catch (Throwable ex) {
      errors.add(ex);
    }

    MultipleFailureException.assertEmpty(errors);
  }

}
