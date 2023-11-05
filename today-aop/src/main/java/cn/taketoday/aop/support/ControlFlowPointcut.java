/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Pointcut and method matcher for use in simple <b>cflow</b>-style pointcut.
 * Note that evaluating such pointcuts is 10-15 times slower than evaluating
 * normal pointcuts, but they are useful in some cases.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 23:01
 */
@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

  /**
   * The class against which to match.
   */
  protected final Class<?> clazz;

  /**
   * An immutable list of distinct method name patterns against which to match.
   */
  protected final List<String> methodNamePatterns;

  private final AtomicInteger evaluationCount = new AtomicInteger();

  /**
   * Construct a new pointcut that matches all control flows below the given class.
   *
   * @param clazz the class
   */
  public ControlFlowPointcut(Class<?> clazz) {
    this(clazz, (String) null);
  }

  /**
   * Construct a new pointcut that matches all calls below a method matching
   * the given method name pattern in the given class.
   * <p>If no method name pattern is given, the pointcut matches all control flows
   * below the given class.
   *
   * @param clazz the class
   * @param methodNamePattern the method name pattern (may be {@code null})
   */
  public ControlFlowPointcut(Class<?> clazz, @Nullable String methodNamePattern) {
    Assert.notNull(clazz, "Class is required");
    this.clazz = clazz;
    this.methodNamePatterns = (methodNamePattern != null ?
                               Collections.singletonList(methodNamePattern) : Collections.emptyList());
  }

  /**
   * Construct a new pointcut that matches all calls below a method matching
   * one of the given method name patterns in the given class.
   * <p>If no method name pattern is given, the pointcut matches all control flows
   * below the given class.
   *
   * @param clazz the class
   * @param methodNamePatterns the method name patterns (potentially empty)
   */
  public ControlFlowPointcut(Class<?> clazz, String... methodNamePatterns) {
    this(clazz, Arrays.asList(methodNamePatterns));
  }

  /**
   * Construct a new pointcut that matches all calls below a method matching
   * one of the given method name patterns in the given class.
   * <p>If no method name pattern is given, the pointcut matches all control flows
   * below the given class.
   *
   * @param clazz the class
   * @param methodNamePatterns the method name patterns (potentially empty)
   */
  public ControlFlowPointcut(Class<?> clazz, List<String> methodNamePatterns) {
    Assert.notNull(clazz, "Class is required");
    Assert.notNull(methodNamePatterns, "List of method name patterns is required");
    Assert.noNullElements(methodNamePatterns, "List of method name patterns must not contain null elements");
    this.clazz = clazz;
    this.methodNamePatterns = methodNamePatterns.stream().distinct().toList();
  }

  /**
   * Subclasses can override this for greater filtering (and performance).
   * <p>The default implementation always returns {@code true}.
   */
  @Override
  public boolean matches(Class<?> clazz) {
    return true;
  }

  /**
   * Subclasses can override this if it's possible to filter out some candidate classes.
   * <p>The default implementation always returns {@code true}.
   */
  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return true;
  }

  @Override
  public boolean isRuntime() {
    return true;
  }

  @Override
  public boolean matches(MethodInvocation invocation) {
    incrementEvaluationCount();

    for (StackTraceElement element : new Throwable().getStackTrace()) {
      if (element.getClassName().equals(this.clazz.getName())) {
        if (this.methodNamePatterns.isEmpty()) {
          return true;
        }
        String methodName = element.getMethodName();
        for (int i = 0; i < this.methodNamePatterns.size(); i++) {
          if (isMatch(methodName, i)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Get the number of times {@link #matches(MethodInvocation)} has been
   * evaluated.
   * <p>Useful for optimization and testing purposes.
   */
  public int getEvaluations() {
    return this.evaluationCount.get();
  }

  /**
   * Increment the {@link #getEvaluations() evaluation count}.
   *
   * @see #matches(MethodInvocation)
   */
  protected final void incrementEvaluationCount() {
    this.evaluationCount.incrementAndGet();
  }

  /**
   * Determine if the given method name matches the method name pattern at the
   * specified index.
   * <p>This method is invoked by {@link #matches(MethodInvocation)}.
   * <p>The default implementation retrieves the method name pattern from
   * {@link #methodNamePatterns} and delegates to {@link #isMatch(String, String)}.
   * <p>Can be overridden in subclasses &mdash; for example, to support
   * regular expressions.
   *
   * @param methodName the method name to check
   * @param patternIndex the index of the method name pattern
   * @return {@code true} if the method name matches the pattern at the specified
   * index
   * @see #methodNamePatterns
   * @see #isMatch(String, String)
   * @see #matches(MethodInvocation)
   */
  protected boolean isMatch(String methodName, int patternIndex) {
    String methodNamePattern = this.methodNamePatterns.get(patternIndex);
    return isMatch(methodName, methodNamePattern);
  }

  /**
   * Determine if the given method name matches the method name pattern.
   * <p>This method is invoked by {@link #isMatch(String, int)}.
   * <p>The default implementation checks for direct equality as well as
   * {@code xxx*}, {@code *xxx}, {@code *xxx*}, and {@code xxx*yyy} matches.
   * <p>Can be overridden in subclasses &mdash; for example, to support a
   * different style of simple pattern matching.
   *
   * @param methodName the method name to check
   * @param methodNamePattern the method name pattern
   * @return {@code true} if the method name matches the pattern
   * @see #isMatch(String, int)
   * @see cn.taketoday.util.StringUtils#simpleMatch(String, String)
   */
  protected boolean isMatch(String methodName, String methodNamePattern) {
    return methodName.equals(methodNamePattern)
            || StringUtils.simpleMatch(methodNamePattern, methodName);
  }

  @Override
  public ClassFilter getClassFilter() {
    return this;
  }

  @Override
  public MethodMatcher getMethodMatcher() {
    return this;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof ControlFlowPointcut that &&
            this.clazz.equals(that.clazz)) && this.methodNamePatterns.equals(that.methodNamePatterns));
  }

  @Override
  public int hashCode() {
    int code = this.clazz.hashCode();
    code = 37 * code + this.methodNamePatterns.hashCode();
    return code;
  }

  @Override
  public String toString() {
    return getClass().getName() + ": class = " + this.clazz.getName() + "; methodNamePatterns = " + this.methodNamePatterns;
  }

}
