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

package infra.retry.context;

import java.io.Serializable;

import infra.core.AttributeAccessorSupport;
import infra.retry.RetryContext;
import infra.retry.RetryPolicy;

/**
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RetryContextSupport extends AttributeAccessorSupport implements RetryContext, Serializable {

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
