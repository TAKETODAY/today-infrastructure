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

import org.junit.runners.model.Statement;

import cn.taketoday.test.context.TestContextManager;

/**
 * {@code RunBeforeTestClassCallbacks} is a custom JUnit {@link Statement} which allows
 * the <em>Spring TestContext Framework</em> to be plugged into the JUnit execution chain
 * by calling {@link TestContextManager#beforeTestClass() beforeTestClass()} on the
 * supplied {@link TestContextManager}.
 *
 * @author Sam Brannen
 * @see #evaluate()
 * @see RunAfterTestMethodCallbacks
 * @since 3.0
 */
public class RunBeforeTestClassCallbacks extends Statement {

  private final Statement next;

  private final TestContextManager testContextManager;

  /**
   * Construct a new {@code RunBeforeTestClassCallbacks} statement.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testContextManager the TestContextManager upon which to call
   * {@code beforeTestClass()}
   */
  public RunBeforeTestClassCallbacks(Statement next, TestContextManager testContextManager) {
    this.next = next;
    this.testContextManager = testContextManager;
  }

  /**
   * Invoke {@link TestContextManager#beforeTestClass()} and then evaluate
   * the next {@link Statement} in the execution chain (typically an instance
   * of {@link org.junit.internal.runners.statements.RunBefores RunBefores}).
   */
  @Override
  public void evaluate() throws Throwable {
    this.testContextManager.beforeTestClass();
    this.next.evaluate();
  }

}
