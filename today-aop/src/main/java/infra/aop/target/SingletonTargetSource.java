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

package infra.aop.target;

import java.io.Serializable;

import infra.aop.TargetSource;
import infra.aop.framework.AdvisedSupport;
import infra.lang.Assert;

/**
 * Implementation of the {@link infra.aop.TargetSource} interface
 * that holds a given object. This is the default implementation of the TargetSource
 * interface, as used by the AOP framework. There is usually no need to
 * create objects of this class in application code.
 *
 * <p>This class is serializable. However, the actual serializability of a
 * SingletonTargetSource will depend on whether the target is serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:18
 * @see AdvisedSupport#setTarget(Object)
 */
public class SingletonTargetSource implements TargetSource, Serializable {

  private static final long serialVersionUID = 1L;

  /** Target cached and invoked using reflection. */
  private final Object target;

  /**
   * Create a new SingletonTargetSource for the given target.
   *
   * @param target the target object
   */
  public SingletonTargetSource(Object target) {
    Assert.notNull(target, "Target object is required");
    this.target = target;
  }

  @Override
  public Class<?> getTargetClass() {
    return this.target.getClass();
  }

  @Override
  public Object getTarget() {
    return this.target;
  }

  @Override
  public void releaseTarget(Object target) { }

  @Override
  public boolean isStatic() {
    return true;
  }

  /**
   * Two invoker interceptors are equal if they have the same target or if the
   * targets or the targets are equal.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SingletonTargetSource)) {
      return false;
    }
    return this.target.equals(((SingletonTargetSource) other).target);
  }

  /**
   * SingletonTargetSource uses the hash code of the target object.
   */
  @Override
  public int hashCode() {
    return this.target.hashCode();
  }

  @Override
  public String toString() {
    return "SingletonTargetSource for target object [" + target + ']';
  }

}
