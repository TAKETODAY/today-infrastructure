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
 * Abstract {@link TargetSource} implementation that
 * wraps a refreshable target object. Subclasses can determine whether a
 * refresh is required, and need to provide fresh target objects.
 *
 * <p>Implements the {@link Refreshable} interface in order to allow for
 * explicit control over the refresh status.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:20
 * @see #requiresRefresh()
 * @see #freshTarget()
 * @since 3.0
 */
public abstract class AbstractRefreshableTargetSource implements TargetSource, Refreshable {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected Object targetObject;

  private long refreshCheckDelay = -1;

  private long lastRefreshCheck = -1;

  private long lastRefreshTime = -1;

  private long refreshCount = 0;

  /**
   * Set the delay between refresh checks, in milliseconds.
   * Default is -1, indicating no refresh checks at all.
   * <p>Note that an actual refresh will only happen when
   * {@link #requiresRefresh()} returns {@code true}.
   */
  public void setRefreshCheckDelay(long refreshCheckDelay) {
    this.refreshCheckDelay = refreshCheckDelay;
  }

  @Override
  public synchronized Class<?> getTargetClass() {
    if (this.targetObject == null) {
      refresh();
    }
    return this.targetObject.getClass();
  }

  /**
   * Not static.
   */
  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public final synchronized Object getTarget() {
    if ((refreshCheckDelayElapsed() && requiresRefresh()) || this.targetObject == null) {
      refresh();
    }
    return this.targetObject;
  }

  @Override
  public final synchronized void refresh() {
    logger.debug("Attempting to refresh target");

    this.targetObject = freshTarget();
    this.refreshCount++;
    this.lastRefreshTime = System.currentTimeMillis();

    logger.debug("Target refreshed successfully");
  }

  @Override
  public synchronized long getRefreshCount() {
    return this.refreshCount;
  }

  @Override
  public synchronized long getLastRefreshTime() {
    return this.lastRefreshTime;
  }

  private boolean refreshCheckDelayElapsed() {
    if (this.refreshCheckDelay < 0) {
      return false;
    }
    final long currentTimeMillis = System.currentTimeMillis();
    if (this.lastRefreshCheck < 0 || currentTimeMillis - this.lastRefreshCheck > this.refreshCheckDelay) {
      // Going to perform a refresh check - update the timestamp.
      this.lastRefreshCheck = currentTimeMillis;
      logger.debug("Refresh check delay elapsed - checking whether refresh is required");
      return true;
    }

    return false;
  }

  /**
   * Determine whether a refresh is required.
   * Invoked for each refresh check, after the refresh check delay has elapsed.
   * <p>The default implementation always returns {@code true}, triggering
   * a refresh every time the delay has elapsed. To be overridden by subclasses
   * with an appropriate check of the underlying target resource.
   *
   * @return whether a refresh is required
   */
  protected boolean requiresRefresh() {
    return true;
  }

  /**
   * Obtain a fresh target object.
   * <p>Only invoked if a refresh check has found that a refresh is required
   * (that is, {@link #requiresRefresh()} has returned {@code true}).
   *
   * @return the fresh target object
   */
  protected abstract Object freshTarget();

}
