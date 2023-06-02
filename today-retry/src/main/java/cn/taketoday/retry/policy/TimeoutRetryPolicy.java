/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.retry.policy;

import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.context.RetryContextSupport;

/**
 * A {@link RetryPolicy} that allows a retry only if it hasn't timed out. The clock is
 * started on a call to {@link #open(RetryContext)}.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class TimeoutRetryPolicy implements RetryPolicy {

  /**
   * Default value for timeout (milliseconds).
   */
  public static final long DEFAULT_TIMEOUT = 1000;

  private long timeout;

  /**
   * Create a new instance with the timeout set to {@link #DEFAULT_TIMEOUT}.
   */
  public TimeoutRetryPolicy() {
    this(DEFAULT_TIMEOUT);
  }

  /**
   * Create a new instance with a configurable timeout.
   *
   * @param timeout timeout in milliseconds
   */
  public TimeoutRetryPolicy(long timeout) {
    this.timeout = timeout;
  }

  /**
   * Setter for timeout in milliseconds. Default is {@link #DEFAULT_TIMEOUT}.
   *
   * @param timeout how long to wait until a timeout
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  /**
   * The value of the timeout.
   *
   * @return the timeout in milliseconds
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * Only permits a retry if the timeout has not expired. Does not check the exception
   * at all.
   *
   * @see RetryPolicy#canRetry(RetryContext)
   */
  public boolean canRetry(RetryContext context) {
    return ((TimeoutRetryContext) context).isAlive();
  }

  public void close(RetryContext context) {
  }

  public RetryContext open(RetryContext parent) {
    return new TimeoutRetryContext(parent, timeout);
  }

  public void registerThrowable(RetryContext context, Throwable throwable) {
    ((RetryContextSupport) context).registerThrowable(throwable);
    // otherwise no-op - we only time out, otherwise retry everything...
  }

  private static class TimeoutRetryContext extends RetryContextSupport {

    private final long timeout;
    private final long start;

    public TimeoutRetryContext(RetryContext parent, long timeout) {
      super(parent);
      this.start = System.currentTimeMillis();
      this.timeout = timeout;
    }

    public boolean isAlive() {
      return (System.currentTimeMillis() - start) <= timeout;
    }

  }

}
