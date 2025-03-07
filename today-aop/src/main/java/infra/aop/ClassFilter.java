/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import infra.lang.Assert;

/**
 * Filter that restricts matching of a pointcut or introduction to
 * a given set of target classes.
 *
 * <p>Can be used as part of a {@link Pointcut} or for the entire
 * targeting of an {@link IntroductionAdvisor}.
 *
 * <p><strong>WARNING</strong>: Concrete implementations of this interface must
 * provide proper implementations of {@link Object#equals(Object)},
 * {@link Object#hashCode()}, and {@link Object#toString()} in order to allow the
 * filter to be used in caching scenarios &mdash; for example, in proxies generated
 * by CGLIB. As of 4.0, the {@code toString()} implementation
 * must generate a unique string representation that aligns with the logic used
 * to implement {@code equals()}. See concrete implementations of this interface
 * within the framework for examples.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:12
 * @see Pointcut
 * @see MethodMatcher
 * @since 3.0
 */
@FunctionalInterface
public interface ClassFilter {

  /**
   * Should the pointcut apply to the given interface or target class?
   *
   * @param clazz the candidate target class
   * @return whether the advice should apply to the given target class
   */
  boolean matches(Class<?> clazz);

  /**
   * Canonical instance of a ClassFilter that matches all classes.
   */
  ClassFilter TRUE = TrueClassFilter.INSTANCE;

  //---------------------------------------------------------------------
  // Static factory methods
  //---------------------------------------------------------------------

  /**
   * Match all classes that <i>either</i> (or both) of the given ClassFilters matches.
   *
   * @param cf1 the first ClassFilter
   * @param cf2 the second ClassFilter
   * @return a distinct ClassFilter that matches all classes that either
   * of the given ClassFilter matches
   */
  static ClassFilter union(ClassFilter cf1, ClassFilter cf2) {
    Assert.notNull(cf1, "First ClassFilter is required");
    Assert.notNull(cf2, "Second ClassFilter is required");
    return new UnionClassFilter(new ClassFilter[] { cf1, cf2 });
  }

  /**
   * Match all classes that <i>either</i> (or all) of the given ClassFilters matches.
   *
   * @param classFilters the ClassFilters to match
   * @return a distinct ClassFilter that matches all classes that either
   * of the given ClassFilter matches
   */
  static ClassFilter union(ClassFilter[] classFilters) {
    Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
    return new UnionClassFilter(classFilters);
  }

  /**
   * Match all classes that <i>both</i> of the given ClassFilters match.
   *
   * @param cf1 the first ClassFilter
   * @param cf2 the second ClassFilter
   * @return a distinct ClassFilter that matches all classes that both
   * of the given ClassFilter match
   */
  static ClassFilter intersection(ClassFilter cf1, ClassFilter cf2) {
    Assert.notNull(cf1, "First ClassFilter is required");
    Assert.notNull(cf2, "Second ClassFilter is required");
    return new IntersectionClassFilter(new ClassFilter[] { cf1, cf2 });
  }

  /**
   * Match all classes that <i>all</i> of the given ClassFilters match.
   *
   * @param classFilters the ClassFilters to match
   * @return a distinct ClassFilter that matches all classes that both
   * of the given ClassFilter match
   */
  static ClassFilter intersection(ClassFilter[] classFilters) {
    Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
    return new IntersectionClassFilter(classFilters);
  }

  /**
   * Return a class filter that represents the logical negation of the specified
   * filter instance.
   *
   * @param classFilter the {@link ClassFilter} to negate
   * @return a filter that represents the logical negation of the specified filter
   * @since 4.0
   */
  static ClassFilter negate(ClassFilter classFilter) {
    Assert.notNull(classFilter, "ClassFilter is required");
    return new NegateClassFilter(classFilter);
  }

  /**
   * ClassFilter implementation for a union of the given ClassFilter.
   */
  final class UnionClassFilter implements ClassFilter, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ClassFilter[] filters;

    private UnionClassFilter(ClassFilter[] filters) {
      this.filters = filters;
    }

    @Override
    public boolean matches(Class<?> clazz) {
      for (ClassFilter filter : this.filters) {
        if (filter.matches(clazz)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean equals(Object other) {
      return (this == other ||
              (other instanceof UnionClassFilter && Arrays.equals(this.filters, ((UnionClassFilter) other).filters)));
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(this.filters);
    }

    @Override
    public String toString() {
      return getClass().getName() + ": " + Arrays.toString(this.filters);
    }

  }

  /**
   * ClassFilter implementation for an intersection of the given ClassFilter.
   */
  final class IntersectionClassFilter implements ClassFilter, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ClassFilter[] filters;

    private IntersectionClassFilter(ClassFilter[] filters) {
      this.filters = filters;
    }

    @Override
    public boolean matches(Class<?> clazz) {
      for (ClassFilter filter : this.filters) {
        if (!filter.matches(clazz)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean equals(Object other) {
      return (this == other || (other instanceof IntersectionClassFilter &&
              Arrays.equals(this.filters, ((IntersectionClassFilter) other).filters)));
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(this.filters);
    }

    @Override
    public String toString() {
      return getClass().getName() + ": " + Arrays.toString(this.filters);
    }

  }

  /**
   * ClassFilter implementation for a logical negation of the given ClassFilter.
   */
  final class NegateClassFilter implements ClassFilter, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ClassFilter original;

    private NegateClassFilter(ClassFilter original) {
      this.original = original;
    }

    @Override
    public boolean matches(Class<?> clazz) {
      return !this.original.matches(clazz);
    }

    @Override
    public boolean equals(Object other) {
      return (this == other || (other instanceof NegateClassFilter that
              && this.original.equals(that.original)));
    }

    @Override
    public int hashCode() {
      return Objects.hash(getClass(), this.original);
    }

    @Override
    public String toString() {
      return "Negate " + this.original;
    }

  }

}
