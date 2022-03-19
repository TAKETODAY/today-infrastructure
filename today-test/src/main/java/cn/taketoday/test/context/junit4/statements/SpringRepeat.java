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

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.annotation.Repeat;
import cn.taketoday.test.annotation.TestAnnotationUtils;

/**
 * {@code SpringRepeat} is a custom JUnit {@link Statement} which adds support
 * for Framework's {@link Repeat @Repeat}
 * annotation by repeating the test the specified number of times.
 *
 * @author Sam Brannen
 * @see #evaluate()
 * @since 3.0
 */
public class SpringRepeat extends Statement {

  protected static final Logger logger = LoggerFactory.getLogger(SpringRepeat.class);

  private final Statement next;

  private final Method testMethod;

  private final int repeat;

  /**
   * Construct a new {@code SpringRepeat} statement for the supplied
   * {@code testMethod}, retrieving the configured repeat count from the
   * {@code @Repeat} annotation on the supplied method.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testMethod the current test method
   * @see TestAnnotationUtils#getRepeatCount(Method)
   */
  public SpringRepeat(Statement next, Method testMethod) {
    this(next, testMethod, TestAnnotationUtils.getRepeatCount(testMethod));
  }

  /**
   * Construct a new {@code SpringRepeat} statement for the supplied
   * {@code testMethod} and {@code repeat} count.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testMethod the current test method
   * @param repeat the configured repeat count for the current test method
   */
  public SpringRepeat(Statement next, Method testMethod, int repeat) {
    this.next = next;
    this.testMethod = testMethod;
    this.repeat = Math.max(1, repeat);
  }

  /**
   * Evaluate the next {@link Statement statement} in the execution chain
   * repeatedly, using the specified repeat count.
   */
  @Override
  public void evaluate() throws Throwable {
    for (int i = 0; i < this.repeat; i++) {
      if (this.repeat > 1 && logger.isInfoEnabled()) {
        logger.info(String.format("Repetition %d of test %s#%s()", (i + 1),
                this.testMethod.getDeclaringClass().getSimpleName(), this.testMethod.getName()));
      }
      this.next.evaluate();
    }
  }

}
