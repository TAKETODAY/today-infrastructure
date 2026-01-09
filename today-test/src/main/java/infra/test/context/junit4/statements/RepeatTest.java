/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.junit4.statements;

import org.junit.runners.model.Statement;

import java.lang.reflect.Method;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.annotation.Repeat;
import infra.test.annotation.TestAnnotationUtils;

/**
 * {@code RepeatTest} is a custom JUnit {@link Statement} which adds support
 * for Framework's {@link Repeat @Repeat}
 * annotation by repeating the test the specified number of times.
 *
 * @author Sam Brannen
 * @see #evaluate()
 * @since 4.0
 */
public class RepeatTest extends Statement {

  protected static final Logger logger = LoggerFactory.getLogger(RepeatTest.class);

  private final Statement next;

  private final Method testMethod;

  private final int repeat;

  /**
   * Construct a new {@code RepeatTest} statement for the supplied
   * {@code testMethod}, retrieving the configured repeat count from the
   * {@code @Repeat} annotation on the supplied method.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testMethod the current test method
   * @see TestAnnotationUtils#getRepeatCount(Method)
   */
  public RepeatTest(Statement next, Method testMethod) {
    this(next, testMethod, TestAnnotationUtils.getRepeatCount(testMethod));
  }

  /**
   * Construct a new {@code RepeatTest} statement for the supplied
   * {@code testMethod} and {@code repeat} count.
   *
   * @param next the next {@code Statement} in the execution chain
   * @param testMethod the current test method
   * @param repeat the configured repeat count for the current test method
   */
  public RepeatTest(Statement next, Method testMethod, int repeat) {
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
