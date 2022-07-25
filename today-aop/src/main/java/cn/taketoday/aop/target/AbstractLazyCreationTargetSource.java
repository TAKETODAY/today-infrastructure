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

import cn.taketoday.aop.TargetSource;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * {@link TargetSource} implementation that will
 * lazily create a user-managed object.
 *
 * <p>Creation of the lazy target object is controlled by the user by implementing
 * the {@link #createObject()} method. This {@code TargetSource} will invoke
 * this method the first time the proxy is accessed.
 *
 * <p>Useful when you need to pass a reference to some dependency to an object
 * but you don't actually want the dependency to be created until it is first used.
 * A typical scenario for this is a connection to a remote resource.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:15
 * @see #isInitialized()
 * @see #createObject()
 * @since 3.0
 */
public abstract class AbstractLazyCreationTargetSource implements TargetSource {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /** The lazily initialized target object. */
  private Object lazyTarget;

  /**
   * Return whether the lazy target object of this TargetSource
   * has already been fetched.
   */
  public synchronized boolean isInitialized() {
    return (this.lazyTarget != null);
  }

  /**
   * This default implementation returns {@code null} if the
   * target is {@code null} (it is hasn't yet been initialized),
   * or the target class if the target has already been initialized.
   * <p>Subclasses may wish to override this method in order to provide
   * a meaningful value when the target is still {@code null}.
   *
   * @see #isInitialized()
   */
  @Override
  public synchronized Class<?> getTargetClass() {
    return (this.lazyTarget != null ? this.lazyTarget.getClass() : null);
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  /**
   * Returns the lazy-initialized target object,
   * creating it on-the-fly if it doesn't exist already.
   *
   * @see #createObject()
   */
  @Override
  public synchronized Object getTarget() throws Exception {
    if (this.lazyTarget == null) {
      logger.debug("Initializing lazy target object");
      this.lazyTarget = createObject();
    }
    return this.lazyTarget;
  }

  /**
   * Subclasses should implement this method to return the lazy initialized object.
   * Called the first time the proxy is invoked.
   *
   * @return the created object
   */
  protected abstract Object createObject() throws Exception;

}
