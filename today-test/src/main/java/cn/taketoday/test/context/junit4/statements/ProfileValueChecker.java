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

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.annotation.IfProfileValue;
import cn.taketoday.test.annotation.ProfileValueUtils;

/**
 * {@code ProfileValueChecker} is a custom JUnit {@link Statement} that checks
 * whether a test class or test method is enabled in the current environment
 * via Spring's {@link IfProfileValue @IfProfileValue} annotation.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @see #evaluate()
 * @see IfProfileValue
 * @see ProfileValueUtils
 * @since 4.0
 */
public class ProfileValueChecker extends Statement {

  private final Statement next;

  private final Class<?> testClass;

  @Nullable
  private final Method testMethod;

  /**
   * Construct a new {@code ProfileValueChecker} statement.
   *
   * @param next the next {@code Statement} in the execution chain;
   * never {@code null}
   * @param testClass the test class to check; never {@code null}
   * @param testMethod the test method to check; may be {@code null} if
   * this {@code ProfileValueChecker} is being applied at the class level
   */
  public ProfileValueChecker(Statement next, Class<?> testClass, @Nullable Method testMethod) {
    Assert.notNull(next, "The next statement must not be null");
    Assert.notNull(testClass, "The test class must not be null");
    this.next = next;
    this.testClass = testClass;
    this.testMethod = testMethod;
  }

  /**
   * Determine if the test specified by arguments to the
   * {@linkplain #ProfileValueChecker constructor} is <em>enabled</em> in
   * the current environment, as configured via the {@link IfProfileValue
   * &#064;IfProfileValue} annotation.
   * <p>If the test is not annotated with {@code @IfProfileValue} it is
   * considered enabled.
   * <p>If a test is not enabled, this method will abort further evaluation
   * of the execution chain with a failed assumption; otherwise, this method
   * will simply evaluate the next {@link Statement} in the execution chain.
   *
   * @throws AssumptionViolatedException if the test is disabled
   * @throws Throwable if evaluation of the next statement fails
   * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
   * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Method, Class)
   */
  @Override
  public void evaluate() throws Throwable {
    if (this.testMethod == null) {
      if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testClass)) {
        Annotation ann = AnnotatedElementUtils.findMergedAnnotation(this.testClass, IfProfileValue.class);
        throw new AssumptionViolatedException(String.format(
                "Profile configured via [%s] is not enabled in this environment for test class [%s].",
                ann, this.testClass.getName()));
      }
    }
    else {
      if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testMethod, this.testClass)) {
        throw new AssumptionViolatedException(String.format(
                "Profile configured via @IfProfileValue is not enabled in this environment for test method [%s].",
                this.testMethod));
      }
    }

    this.next.evaluate();
  }

}
