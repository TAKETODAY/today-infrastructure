/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop.target;

import java.io.Serializable;
import java.util.Objects;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.lang.Assert;

/**
 * {@link TargetSource} implementation that
 * caches a local target object, but allows the target to be swapped
 * while the application is running.
 *
 * <p>If configuring an object of this class in a IoC container,
 * use constructor injection.
 *
 * <p>This TargetSource is serializable if the target is at the time
 * of serialization.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:26
 * @since 3.0
 */
public class HotSwappableTargetSource implements TargetSource, Serializable {

  private static final long serialVersionUID = 1L;

  /** The current target object. */
  private Object target;

  /**
   * Create a new HotSwappableTargetSource with the given initial target object.
   *
   * @param initialTarget the initial target object
   */
  public HotSwappableTargetSource(Object initialTarget) {
    Assert.notNull(initialTarget, "Target object is required");
    this.target = initialTarget;
  }

  /**
   * Return the type of the current target object.
   * <p>The returned type should usually be constant across all target objects.
   */
  @Override
  public synchronized Class<?> getTargetClass() {
    return this.target.getClass();
  }

  @Override
  public final boolean isStatic() {
    return false;
  }

  @Override
  public synchronized Object getTarget() {
    return this.target;
  }

  /**
   * Swap the target, returning the old target object.
   *
   * @param newTarget the new target object
   * @return the old target object
   * @throws IllegalArgumentException if the new target is invalid
   */
  public synchronized Object swap(Object newTarget) {
    Assert.notNull(newTarget, "Target object is required");
    Object old = this.target;
    this.target = newTarget;
    return old;
  }

  /**
   * Two HotSwappableTargetSources are equal if the current target
   * objects are equal.
   */
  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof HotSwappableTargetSource &&
            Objects.equals(target, ((HotSwappableTargetSource) other).target)));
  }

  @Override
  public int hashCode() {
    return HotSwappableTargetSource.class.hashCode();
  }

  @Override
  public String toString() {
    return "HotSwappableTargetSource for target: " + this.target;
  }

}
