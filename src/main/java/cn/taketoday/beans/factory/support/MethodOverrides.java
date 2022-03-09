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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import cn.taketoday.lang.Nullable;

/**
 * Set of method overrides, determining which, if any, methods on a
 * managed object the IoC container will override at runtime.
 *
 * <p>The currently supported {@link MethodOverride} variants are
 * {@link LookupOverride} and {@link ReplaceOverride}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodOverride
 * @since 4.0 2022/3/7 12:42
 */
public class MethodOverrides {

  private final CopyOnWriteArraySet<MethodOverride> overrides = new CopyOnWriteArraySet<>();

  /**
   * Create new MethodOverrides.
   */
  public MethodOverrides() { }

  /**
   * Deep copy constructor.
   */
  public MethodOverrides(MethodOverrides other) {
    addOverrides(other);
  }

  /**
   * Copy all given method overrides into this object.
   */
  public void addOverrides(@Nullable MethodOverrides other) {
    if (other != null) {
      this.overrides.addAll(other.overrides);
    }
  }

  /**
   * Add the given method override.
   */
  public void addOverride(MethodOverride override) {
    this.overrides.add(override);
  }

  /**
   * Return all method overrides contained by this object.
   *
   * @return a Set of MethodOverride objects
   * @see MethodOverride
   */
  public Set<MethodOverride> getOverrides() {
    return this.overrides;
  }

  /**
   * Return whether the set of method overrides is empty.
   */
  public boolean isEmpty() {
    return this.overrides.isEmpty();
  }

  /**
   * Return the override for the given method, if any.
   *
   * @param method method to check for overrides for
   * @return the method override, or {@code null} if none
   */
  @Nullable
  public MethodOverride getOverride(Method method) {
    MethodOverride match = null;
    for (MethodOverride candidate : this.overrides) {
      if (candidate.matches(method)) {
        match = candidate;
      }
    }
    return match;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MethodOverrides that)) {
      return false;
    }
    return this.overrides.equals(that.overrides);
  }

  @Override
  public int hashCode() {
    return this.overrides.hashCode();
  }

}
