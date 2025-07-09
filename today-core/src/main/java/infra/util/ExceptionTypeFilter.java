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

package infra.util;

import java.util.Collection;

import infra.lang.Nullable;

/**
 * An {@link InstanceFilter} implementation that handles exception types. A type
 * will match against a given candidate if it is assignable to that candidate.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 20:40
 */
public class ExceptionTypeFilter extends InstanceFilter<Class<? extends Throwable>> {

  /**
   * Create a new {@code ExceptionTypeFilter} based on include and exclude
   * collections, with the {@code matchIfEmpty} flag set to {@code true}.
   * <p>See {@link #ExceptionTypeFilter(Collection, Collection, boolean)} for
   * details.
   *
   * @param includes the collection of includes
   * @param excludes the collection of excludes
   * @since 5.0
   */
  public ExceptionTypeFilter(@Nullable Collection<Class<? extends Throwable>> includes,
          @Nullable Collection<Class<? extends Throwable>> excludes) {

    super(includes, excludes);
  }

  /**
   * Create a new {@code ExceptionTypeFilter} based on include and exclude
   * collections.
   * <p>See {@link InstanceFilter#InstanceFilter(Collection, Collection, boolean)
   * InstanceFilter} for details.
   *
   * @param includes the collection of includes
   * @param excludes the collection of excludes
   * @param matchIfEmpty the matching result if the includes and the excludes
   * collections are both {@code null} or empty
   */
  public ExceptionTypeFilter(@Nullable Collection<? extends Class<? extends Throwable>> includes,
          @Nullable Collection<? extends Class<? extends Throwable>> excludes, boolean matchIfEmpty) {

    super(includes, excludes, matchIfEmpty);
  }

  /**
   * Determine if the specified {@code instance} matches the specified
   * {@code candidate}.
   * <p>By default, the two instances match if the {@code candidate} type is
   * {@linkplain Class#isAssignableFrom(Class) is assignable from} the
   * {@code instance} type.
   * <p>Can be overridden by subclasses.
   *
   * @param instance the instance to check
   * @param candidate a candidate defined by this filter
   * @return {@code true} if the instance matches the candidate
   */
  @Override
  protected boolean match(Class<? extends Throwable> instance, Class<? extends Throwable> candidate) {
    return candidate.isAssignableFrom(instance);
  }

  /**
   * Determine if the type of the supplied {@code exception} matches this filter.
   *
   * @see InstanceFilter#match(Object)
   * @since 5.0
   */
  public boolean match(Throwable exception) {
    return match(exception.getClass());
  }

}
