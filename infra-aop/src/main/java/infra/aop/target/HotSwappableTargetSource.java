/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.target;

import java.io.Serializable;
import java.util.Objects;

import infra.aop.TargetSource;
import infra.lang.Assert;

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
