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

package cn.taketoday.util;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
   * Create a new instance based on includes/excludes collections.
   * <p>A particular element will match if it "matches" the one of the element in the
   * includes list and  does not match one of the element in the excludes list.
   * <p>Subclasses may redefine what matching means. By default, an element match with
   * another if it is equals according to {@link Object#equals(Object)}
   * <p>If both collections are empty, {@code matchIfEmpty} defines if
   * an element matches or not.
   *
   * @param includes the collection of includes
   * @param excludes the collection of excludes
   * @param matchIfEmpty the matching result if both the includes and the excludes
   * collections are empty
   */
  public InstanceFilter(@Nullable Collection<? extends T> includes,
          @Nullable Collection<? extends T> excludes, boolean matchIfEmpty) {

    this.includes = (includes != null ? includes : Collections.emptyList());
    this.excludes = (excludes != null ? excludes : Collections.emptyList());
    this.matchIfEmpty = matchIfEmpty;
  }

  /**
   * Determine if the specified {code instance} matches this filter.
   */
  public boolean match(T instance) {
    Assert.notNull(instance, "Instance to match must not be null");

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
   * Determine if the specified {@code instance} is equal to the
   * specified {@code candidate}.
   *
   * @param instance the instance to handle
   * @param candidate a candidate defined by this filter
   * @return {@code true} if the instance matches the candidate
   */
  protected boolean match(T instance, T candidate) {
    return instance.equals(candidate);
  }

  /**
   * Determine if the specified {@code instance} matches one of the candidates.
   * <p>If the candidates collection is {@code null}, returns {@code false}.
   *
   * @param instance the instance to check
   * @param candidates a list of candidates
   * @return {@code true} if the instance match or the candidates collection is null
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
    StringBuilder sb = new StringBuilder(getClass().getSimpleName());
    sb.append(": includes=").append(this.includes);
    sb.append(", excludes=").append(this.excludes);
    sb.append(", matchIfEmpty=").append(this.matchIfEmpty);
    return sb.toString();
  }

}

