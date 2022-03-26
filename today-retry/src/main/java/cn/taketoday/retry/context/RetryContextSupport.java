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

package cn.taketoday.retry.context;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;

/**
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class RetryContextSupport extends AttributeAccessorSupport implements RetryContext {

  private final RetryContext parent;

  private volatile boolean terminate = false;

  private volatile int count;

  private volatile Throwable lastException;

  public RetryContextSupport(RetryContext parent) {
    super();
    this.parent = parent;
  }

  public RetryContext getParent() {
    return this.parent;
  }

  public boolean isExhaustedOnly() {
    return terminate;
  }

  public void setExhaustedOnly() {
    terminate = true;
  }

  public int getRetryCount() {
    return count;
  }

  public Throwable getLastThrowable() {
    return lastException;
  }

  /**
   * Set the exception for the public interface {@link RetryContext}, and also increment
   * the retry count if the throwable is non-null.
   *
   * All {@link RetryPolicy} implementations should use this method when they register
   * the throwable. It should only be called once per retry attempt because it
   * increments a counter.
   *
   * Use of this method is not enforced by the framework - it is a service provider
   * contract for authors of policies.
   *
   * @param throwable the exception that caused the current retry attempt to fail.
   */
  public void registerThrowable(Throwable throwable) {
    this.lastException = throwable;
    if (throwable != null)
      count++;
  }

  @Override
  public String toString() {
    return String.format("[RetryContext: count=%d, lastException=%s, exhausted=%b]", count, lastException,
            terminate);
  }

}
