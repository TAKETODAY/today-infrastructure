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

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.lang.Assert;

/**
 * Static utility methods for composing {@link ClassFilter ClassFilters}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author TODAY 2021/2/1 18:19
 * @see MethodMatchers
 * @see Pointcuts
 * @since 3.0
 */
public abstract class ClassFilters {

  /**
   * Match all classes that <i>either</i> (or both) of the given ClassFilters matches.
   *
   * @param cf1 the first ClassFilter
   * @param cf2 the second ClassFilter
   * @return a distinct ClassFilter that matches all classes that either
   * of the given ClassFilter matches
   */
  public static ClassFilter union(ClassFilter cf1, ClassFilter cf2) {
    Assert.notNull(cf1, "First ClassFilter must not be null");
    Assert.notNull(cf2, "Second ClassFilter must not be null");
    return new UnionClassFilter(new ClassFilter[] { cf1, cf2 });
  }

  /**
   * Match all classes that <i>either</i> (or all) of the given ClassFilters matches.
   *
   * @param classFilters the ClassFilters to match
   * @return a distinct ClassFilter that matches all classes that either
   * of the given ClassFilter matches
   */
  public static ClassFilter union(ClassFilter[] classFilters) {
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
  public static ClassFilter intersection(ClassFilter cf1, ClassFilter cf2) {
    Assert.notNull(cf1, "First ClassFilter must not be null");
    Assert.notNull(cf2, "Second ClassFilter must not be null");
    return new IntersectionClassFilter(new ClassFilter[] { cf1, cf2 });
  }

  /**
   * Match all classes that <i>all</i> of the given ClassFilters match.
   *
   * @param classFilters the ClassFilters to match
   * @return a distinct ClassFilter that matches all classes that both
   * of the given ClassFilter match
   */
  public static ClassFilter intersection(ClassFilter[] classFilters) {
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
  public static ClassFilter negate(ClassFilter classFilter) {
    Assert.notNull(classFilter, "ClassFilter must not be null");
    return new NegateClassFilter(classFilter);
  }

  /**
   * ClassFilter implementation for a union of the given ClassFilters.
   */
  static class UnionClassFilter implements ClassFilter, Serializable {
    private static final long serialVersionUID = 1L;
    private final ClassFilter[] filters;

    UnionClassFilter(ClassFilter[] filters) {
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
   * ClassFilter implementation for an intersection of the given ClassFilters.
   */
  static class IntersectionClassFilter implements ClassFilter, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ClassFilter[] filters;

    IntersectionClassFilter(ClassFilter[] filters) {
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
  private static class NegateClassFilter implements ClassFilter, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ClassFilter original;

    NegateClassFilter(ClassFilter original) {
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
