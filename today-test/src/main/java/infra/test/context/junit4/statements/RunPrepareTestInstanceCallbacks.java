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

import org.junit.runners.model.Statement;

import infra.test.context.TestContextManager;

/**
 * {@code RunPrepareTestInstanceCallbacks} is a custom JUnit {@link Statement} which
 * allows the <em>TestContext Framework</em> to be plugged into the JUnit
 * execution chain by calling {@link TestContextManager#prepareTestInstance(Object)
 * prepareTestInstance()} on the supplied {@link TestContextManager}.
 *
 * @author Sam Brannen
 * @see #evaluate()
 * @since 4.0
 */
public class RunPrepareTestInstanceCallbacks extends Statement {

  private final Statement next;

  private final Object testInstance;

  private final TestContextManager testContextManager;

  /**
   * Construct a new {@code RunPrepareTestInstanceCallbacks} statement.
   *
   * @param next the next {@code Statement} in the execution chain; never {@code null}
   * @param testInstance the current test instance; never {@code null}
   * @param testContextManager the {@code TestContextManager} upon which to call
   * {@code prepareTestInstance()}; never {@code null}
   */
  public RunPrepareTestInstanceCallbacks(Statement next, Object testInstance, TestContextManager testContextManager) {
    this.next = next;
    this.testInstance = testInstance;
    this.testContextManager = testContextManager;
  }

  /**
   * Invoke {@link TestContextManager#prepareTestInstance(Object)} and
   * then evaluate the next {@link Statement} in the execution chain
   * (typically an instance of {@link RunAfterTestMethodCallbacks}).
   */
  @Override
  public void evaluate() throws Throwable {
    this.testContextManager.prepareTestInstance(this.testInstance);
    this.next.evaluate();
  }

}
