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

import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import cn.taketoday.lang.Assert;
import cn.taketoday.test.annotation.TestAnnotationUtils;
import cn.taketoday.test.annotation.Timed;

/**
 * {@code FailOnTimeout} is a custom JUnit {@link Statement} which adds
 * support for Framework's {@link Timed @Timed}
 * annotation by throwing an exception if the next statement in the execution
 * chain takes more than the specified number of milliseconds.
 *
 * <p>In contrast to JUnit's
 * {@link org.junit.internal.runners.statements.FailOnTimeout FailOnTimeout},
 * the next {@code statement} will be executed in the same thread as the
 * caller and will therefore not be aborted preemptively.
 *
 * @author Sam Brannen
 * @see #evaluate()
 * @since 4.0
 */
public class FailOnTimeout extends Statement {

  private final Statement next;

  private final long timeout;

  /**
   * Construct a new {@code FailOnTimeout} statement for the supplied
   * {@code testMethod}, retrieving the configured timeout from the
   * {@code @Timed} annotation on the supplied method.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testMethod the current test method
   * @see TestAnnotationUtils#getTimeout(Method)
   */
  public FailOnTimeout(Statement next, Method testMethod) {
    this(next, TestAnnotationUtils.getTimeout(testMethod));
  }

  /**
   * Construct a new {@code FailOnTimeout} statement for the supplied
   * {@code timeout}.
   * <p>If the supplied {@code timeout} is {@code 0}, the execution of the
   * {@code next} statement will not be timed.
   *
   * @param next the next {@code Statement} in the execution chain; never {@code null}
   * @param timeout the configured {@code timeout} for the current test, in milliseconds;
   * never negative
   */
  public FailOnTimeout(Statement next, long timeout) {
    Assert.notNull(next, "next statement must not be null");
    Assert.isTrue(timeout >= 0, "timeout must be non-negative");
    this.next = next;
    this.timeout = timeout;
  }

  /**
   * Evaluate the next {@link Statement statement} in the execution chain
   * (typically an instance of {@link RepeatTest}) and throw a
   * {@link TimeoutException} if the next {@code statement} executes longer
   * than the specified {@code timeout}.
   */
  @Override
  public void evaluate() throws Throwable {
    if (this.timeout == 0) {
      this.next.evaluate();
    }
    else {
      long startTime = System.currentTimeMillis();
      this.next.evaluate();
      long elapsed = System.currentTimeMillis() - startTime;
      if (elapsed > this.timeout) {
        throw new TimeoutException(
                String.format("Test took %s ms; limit was %s ms.", elapsed, this.timeout));
      }
    }
  }

}
