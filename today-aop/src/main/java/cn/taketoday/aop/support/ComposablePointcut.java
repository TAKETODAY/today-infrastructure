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

package cn.taketoday.aop.support;

import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.lang.Assert;

/**
 * Convenient class for building up pointcuts.
 *
 * <p>All methods return {@code ComposablePointcut}, so we can use concise idioms
 * like in the following example.
 *
 * <pre class="code">Pointcut pc = new ComposablePointcut()
 *                      .union(classFilter)
 *                      .intersection(methodMatcher)
 *                      .intersection(pointcut);</pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Pointcut
 * @since 3.0 2021/2/1 18:18
 */
public class ComposablePointcut implements Pointcut, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private ClassFilter classFilter;

  private MethodMatcher methodMatcher;

  /**
   * Create a default ComposablePointcut, with {@code ClassFilter.TRUE}
   * and {@code MethodMatcher.TRUE}.
   */
  public ComposablePointcut() {
    this.classFilter = ClassFilter.TRUE;
    this.methodMatcher = MethodMatcher.TRUE;
  }

  /**
   * Create a ComposablePointcut based on the given Pointcut.
   *
   * @param pointcut the original Pointcut
   */
  public ComposablePointcut(Pointcut pointcut) {
    Assert.notNull(pointcut, "Pointcut is required");
    this.classFilter = pointcut.getClassFilter();
    this.methodMatcher = pointcut.getMethodMatcher();
  }

  /**
   * Create a ComposablePointcut for the given ClassFilter,
   * with {@code MethodMatcher.TRUE}.
   *
   * @param classFilter the ClassFilter to use
   */
  public ComposablePointcut(ClassFilter classFilter) {
    Assert.notNull(classFilter, "ClassFilter is required");
    this.classFilter = classFilter;
    this.methodMatcher = MethodMatcher.TRUE;
  }

  /**
   * Create a ComposablePointcut for the given MethodMatcher,
   * with {@code ClassFilter.TRUE}.
   *
   * @param methodMatcher the MethodMatcher to use
   */
  public ComposablePointcut(MethodMatcher methodMatcher) {
    Assert.notNull(methodMatcher, "MethodMatcher is required");
    this.classFilter = ClassFilter.TRUE;
    this.methodMatcher = methodMatcher;
  }

  /**
   * Create a ComposablePointcut for the given ClassFilter and MethodMatcher.
   *
   * @param classFilter the ClassFilter to use
   * @param methodMatcher the MethodMatcher to use
   */
  public ComposablePointcut(ClassFilter classFilter, MethodMatcher methodMatcher) {
    Assert.notNull(classFilter, "ClassFilter is required");
    Assert.notNull(methodMatcher, "MethodMatcher is required");
    this.classFilter = classFilter;
    this.methodMatcher = methodMatcher;
  }

  /**
   * Apply a union with the given ClassFilter.
   *
   * @param other the ClassFilter to apply a union with
   * @return this composable pointcut (for call chaining)
   */
  public ComposablePointcut union(ClassFilter other) {
    this.classFilter = ClassFilter.union(this.classFilter, other);
    return this;
  }

  /**
   * Apply an intersection with the given ClassFilter.
   *
   * @param other the ClassFilter to apply an intersection with
   * @return this composable pointcut (for call chaining)
   */
  public ComposablePointcut intersection(ClassFilter other) {
    this.classFilter = ClassFilter.intersection(this.classFilter, other);
    return this;
  }

  /**
   * Apply a union with the given MethodMatcher.
   *
   * @param other the MethodMatcher to apply a union with
   * @return this composable pointcut (for call chaining)
   */
  public ComposablePointcut union(MethodMatcher other) {
    this.methodMatcher = MethodMatcher.union(this.methodMatcher, other);
    return this;
  }

  /**
   * Apply an intersection with the given MethodMatcher.
   *
   * @param other the MethodMatcher to apply an intersection with
   * @return this composable pointcut (for call chaining)
   */
  public ComposablePointcut intersection(MethodMatcher other) {
    this.methodMatcher = MethodMatcher.intersection(this.methodMatcher, other);
    return this;
  }

  /**
   * Apply a union with the given Pointcut.
   * <p>Note that for a Pointcut union, methods will only match if their
   * original ClassFilter (from the originating Pointcut) matches as well.
   * MethodMatchers and ClassFilters from different Pointcuts will never
   * get interleaved with each other.
   *
   * @param other the Pointcut to apply a union with
   * @return this composable pointcut (for call chaining)
   */
  public ComposablePointcut union(Pointcut other) {
    this.methodMatcher = MethodMatcher.union(
            this.methodMatcher, this.classFilter, other.getMethodMatcher(), other.getClassFilter());
    this.classFilter = ClassFilter.union(this.classFilter, other.getClassFilter());
    return this;
  }

  /**
   * Apply an intersection with the given Pointcut.
   *
   * @param other the Pointcut to apply an intersection with
   * @return this composable pointcut (for call chaining)
   */
  public ComposablePointcut intersection(Pointcut other) {
    this.classFilter = ClassFilter.intersection(this.classFilter, other.getClassFilter());
    this.methodMatcher = MethodMatcher.intersection(this.methodMatcher, other.getMethodMatcher());
    return this;
  }

  @Override
  public ClassFilter getClassFilter() {
    return this.classFilter;
  }

  @Override
  public MethodMatcher getMethodMatcher() {
    return this.methodMatcher;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ComposablePointcut otherPointcut)) {
      return false;
    }
    return classFilter.equals(otherPointcut.classFilter)
            && methodMatcher.equals(otherPointcut.methodMatcher);
  }

  @Override
  public int hashCode() {
    return this.classFilter.hashCode() * 37 + this.methodMatcher.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.classFilter + ", " + this.methodMatcher;
  }

}
