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

package cn.taketoday.retry.policy;

import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.context.RetryContextSupport;
import cn.taketoday.retry.support.RetryTemplate;

/**
 * Simple retry policy that is aware only about attempt count and retries a fixed number
 * of times. The number of attempts includes the initial try.
 * <p>
 * It is not recommended to use it directly, because usually exception classification is
 * strongly recommended (to not retry on OutOfMemoryError, for example).
 * <p>
 * For daily usage see {@link RetryTemplate#builder()}
 * <p>
 * Volatility of maxAttempts allows concurrent modification and does not require safe
 * publication of new instance after construction.
 */
@SuppressWarnings("serial")
public class MaxAttemptsRetryPolicy implements RetryPolicy {

  /**
   * The default limit to the number of attempts for a new policy.
   */
  public final static int DEFAULT_MAX_ATTEMPTS = 3;

  private volatile int maxAttempts;

  /**
   * Create a {@link MaxAttemptsRetryPolicy} with the default number of retry attempts
   * (3), retrying all throwables.
   */
  public MaxAttemptsRetryPolicy() {
    this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
  }

  /**
   * Create a {@link MaxAttemptsRetryPolicy} with the specified number of retry
   * attempts, retrying all throwables.
   *
   * @param maxAttempts the maximum number of attempts
   */
  public MaxAttemptsRetryPolicy(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * Set the number of attempts before retries are exhausted. Includes the initial
   * attempt before the retries begin so, generally, will be {@code >= 1}. For example
   * setting this property to 3 means 3 attempts total (initial + 2 retries).
   *
   * @param maxAttempts the maximum number of attempts including the initial attempt.
   */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * The maximum number of attempts before failure.
   *
   * @return the maximum number of attempts
   */
  public int getMaxAttempts() {
    return this.maxAttempts;
  }

  /**
   * Test for retryable operation based on the status.
   *
   * @return true if the last exception was retryable and the number of attempts so far
   * is less than the limit.
   * @see RetryPolicy#canRetry(RetryContext)
   */
  @Override
  public boolean canRetry(RetryContext context) {
    return context.getRetryCount() < this.maxAttempts;
  }

  @Override
  public void close(RetryContext status) {
  }

  /**
   * Update the status with another attempted retry and the latest exception.
   *
   * @see RetryPolicy#registerThrowable(RetryContext, Throwable)
   */
  @Override
  public void registerThrowable(RetryContext context, Throwable throwable) {
    ((RetryContextSupport) context).registerThrowable(throwable);
  }

  /**
   * Get a status object that can be used to track the current operation according to
   * this policy. Has to be aware of the latest exception and the number of attempts.
   *
   * @see RetryPolicy#open(RetryContext)
   */
  @Override
  public RetryContext open(RetryContext parent) {
    return new RetryContextSupport(parent);
  }

}
