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

package cn.taketoday.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Support class for throttling concurrent access to a specific resource.
 *
 * <p>Designed for use as a base class, with the subclass invoking
 * the {@link #beforeAccess()} and {@link #afterAccess()} methods at
 * appropriate points of its workflow. Note that {@code afterAccess}
 * should usually be called in a finally block!
 *
 * <p>The default concurrency limit of this support class is -1
 * ("unbounded concurrency"). Subclasses may override this default;
 * check the javadoc of the concrete class that you're using.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/9/11 15:45
 * @see #setConcurrencyLimit
 * @see #beforeAccess()
 * @see #afterAccess()
 * @see cn.taketoday.aop.interceptor.ConcurrencyThrottleInterceptor
 * @see java.io.Serializable
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class ConcurrencyThrottleSupport implements Serializable {

  /**
   * Permit any number of concurrent invocations: that is, don't throttle concurrency.
   */
  public static final int UNBOUNDED_CONCURRENCY = -1;

  /**
   * Switch concurrency 'off': that is, don't allow any concurrent invocations.
   */
  public static final int NO_CONCURRENCY = 0;

  /** Transient to optimize serialization. */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private transient Object monitor = new Object();

  private int concurrencyLimit = UNBOUNDED_CONCURRENCY;

  private int concurrencyCount = 0;

  /**
   * Set the maximum number of concurrent access attempts allowed.
   * -1 indicates unbounded concurrency.
   * <p>In principle, this limit can be changed at runtime,
   * although it is generally designed as a config time setting.
   * <p>NOTE: Do not switch between -1 and any concrete limit at runtime,
   * as this will lead to inconsistent concurrency counts: A limit
   * of -1 effectively turns off concurrency counting completely.
   */
  public void setConcurrencyLimit(int concurrencyLimit) {
    this.concurrencyLimit = concurrencyLimit;
  }

  /**
   * Return the maximum number of concurrent access attempts allowed.
   */
  public int getConcurrencyLimit() {
    return this.concurrencyLimit;
  }

  /**
   * Return whether this throttle is currently active.
   *
   * @return {@code true} if the concurrency limit for this instance is active
   * @see #getConcurrencyLimit()
   */
  public boolean isThrottleActive() {
    return (this.concurrencyLimit >= 0);
  }

  /**
   * To be invoked before the main execution logic of concrete subclasses.
   * <p>This implementation applies the concurrency throttle.
   *
   * @see #afterAccess()
   */
  protected void beforeAccess() {
    if (this.concurrencyLimit == NO_CONCURRENCY) {
      throw new IllegalStateException(
              "Currently no invocations allowed - concurrency limit set to NO_CONCURRENCY");
    }
    if (this.concurrencyLimit > 0) {
      boolean debug = logger.isDebugEnabled();
      synchronized(this.monitor) {
        boolean interrupted = false;
        while (this.concurrencyCount >= this.concurrencyLimit) {
          if (interrupted) {
            throw new IllegalStateException(
                    "Thread was interrupted while waiting for invocation access, " +
                            "but concurrency limit still does not allow for entering");
          }
          if (debug) {
            logger.debug("Concurrency count {} has reached limit {} - blocking",
                         this.concurrencyCount, this.concurrencyLimit);
          }
          try {
            this.monitor.wait();
          }
          catch (InterruptedException ex) {
            // Re-interrupt current thread, to allow other threads to react.
            Thread.currentThread().interrupt();
            interrupted = true;
          }
        }
        if (debug) {
          logger.debug("Entering throttle at concurrency count {}", this.concurrencyCount);
        }
        this.concurrencyCount++;
      }
    }
  }

  /**
   * To be invoked after the main execution logic of concrete subclasses.
   *
   * @see #beforeAccess()
   */
  protected void afterAccess() {
    if (this.concurrencyLimit >= 0) {
      synchronized(this.monitor) {
        this.concurrencyCount--;
        if (logger.isDebugEnabled()) {
          logger.debug("Returning from throttle at concurrency count {}", this.concurrencyCount);
        }
        this.monitor.notify();
      }
    }
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization, just initialize state after deserialization.
    ois.defaultReadObject();

    // Initialize transient fields.
    this.logger = LoggerFactory.getLogger(getClass());
    this.monitor = new Object();
  }

}
