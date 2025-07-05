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
import java.util.Collections;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A simple instance filter that checks if a given instance match based on
 * a collection of includes and excludes element.
 *
 * <p>Subclasses may want to override {@link #match(Object, Object)} to provide
 * a custom matching algorithm.
 *
 * @param <T> the instance type
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 20:40
 */
public class InstanceFilter<T> {

  private final Collection<? extends T> includes;

  private final Collection<? extends T> excludes;

  private final boolean matchIfEmpty;

  /**
   * Create a new {@code InstanceFilter} based on include and exclude collections,
   * with the {@code matchIfEmpty} flag set to {@code true}.
   * <p>See {@link #InstanceFilter(Collection, Collection, boolean)} for details.
   *
   * @param includes the collection of includes
   * @param excludes the collection of excludes
   * @since 5.0
   */
  public InstanceFilter(@Nullable Collection<? extends T> includes,
          @Nullable Collection<? extends T> excludes) {

    this(includes, excludes, true);
  }

  /**
   * Create a new {@code InstanceFilter} based on include and exclude collections.
   * <p>A particular element will match if it <em>matches</em> one of the elements
   * in the {@code includes} list and does not match one of the elements in the
   * {@code excludes} list.
   * <p>Subclasses may redefine what matching means. By default, an element
   * {@linkplain #match(Object, Object) matches} another if the two elements are
   * {@linkplain Object#equals(Object) equal}.
   * <p>If both collections are empty, {@code matchIfEmpty} defines if an element
   * matches or not.
   *
   * @param includes the collection of includes
   * @param excludes the collection of excludes
   * @param matchIfEmpty the matching result if the includes and the excludes
   * collections are both {@code null} or empty
   */
  public InstanceFilter(@Nullable Collection<? extends T> includes,
          @Nullable Collection<? extends T> excludes, boolean matchIfEmpty) {

    this.includes = (includes != null ? includes : Collections.emptyList());
    this.excludes = (excludes != null ? excludes : Collections.emptyList());
    this.matchIfEmpty = matchIfEmpty;
  }

  /**
   * Determine if the specified {@code instance} matches this filter.
   */
  public boolean match(T instance) {
    Assert.notNull(instance, "Instance to match is required");

    boolean includesSet = !this.includes.isEmpty();
    boolean excludesSet = !this.excludes.isEmpty();
    if (!includesSet && !excludesSet) {
      return this.matchIfEmpty;
    }

    boolean matchIncludes = match(instance, this.includes);
    boolean matchExcludes = match(instance, this.excludes);
    if (!includesSet) {
      return !matchExcludes;
    }
    if (!excludesSet) {
      return matchIncludes;
    }
    return matchIncludes && !matchExcludes;
  }

  /**
   * Determine if the specified {@code instance} matches the specified
   * {@code candidate}.
   * <p>By default, the two instances match if they are
   * {@linkplain Object#equals(Object) equal}.
   * <p>Can be overridden by subclasses.
   *
   * @param instance the instance to check
   * @param candidate a candidate defined by this filter
   * @return {@code true} if the instance matches the candidate
   */
  protected boolean match(T instance, T candidate) {
    return instance.equals(candidate);
  }

  /**
   * Determine if the specified {@code instance} matches one of the candidates.
   *
   * @param instance the instance to check
   * @param candidates the collection of candidates
   * @return {@code true} if the instance matches; {@code false} if the
   * candidates collection is empty or there is no match
   */
  protected boolean match(T instance, Collection<? extends T> candidates) {
    for (T candidate : candidates) {
      if (match(instance, candidate)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "%s: includes=%s, excludes=%s, matchIfEmpty=%s".formatted(getClass().getSimpleName(), this.includes, this.excludes, this.matchIfEmpty);
  }

}

