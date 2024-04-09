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

package cn.taketoday.util.backoff;

/**
 * Implementation of {@link BackOff} that increases the back off period for each
 * retry attempt. When the interval has reached the {@linkplain #setMaxInterval(long)
 * max interval}, it is no longer increased. Stops retrying once the
 * {@linkplain #setMaxElapsedTime(long) max elapsed time} has been reached.
 *
 * <p>Example: The default interval is {@value #DEFAULT_INITIAL_INTERVAL} ms;
 * the default multiplier is {@value #DEFAULT_MULTIPLIER}; and the default max
 * interval is {@value #DEFAULT_MAX_INTERVAL}. For 10 attempts the sequence will be
 * as follows:
 *
 * <pre>
 * request#     back off
 *
 *  1              2000
 *  2              3000
 *  3              4500
 *  4              6750
 *  5             10125
 *  6             15187
 *  7             22780
 *  8             30000
 *  9             30000
 * 10             30000
 * </pre>
 *
 * <p>Note that the default max elapsed time is {@link Long#MAX_VALUE}, and the
 * default maximum number of attempts is {@link Integer#MAX_VALUE}. Use
 * {@link #setMaxElapsedTime(long)} to limit the length of time that an instance
 * should accumulate before returning {@link BackOffExecution#STOP}. Alternatively,
 * use {@link #setMaxAttempts(int)} to limit the number of attempts. The execution
 * stops when either of those two limits is reached.
 *
 * @author Stephane Nicoll
 * @author Gary Russell
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ExponentialBackOff implements BackOff {

  /**
   * The default initial interval.
   */
  public static final long DEFAULT_INITIAL_INTERVAL = 2000L;

  /**
   * The default multiplier (increases the interval by 50%).
   */
  public static final double DEFAULT_MULTIPLIER = 1.5;

  /**
   * The default maximum back off time.
   */
  public static final long DEFAULT_MAX_INTERVAL = 30000L;

  /**
   * The default maximum elapsed time.
   */
  public static final long DEFAULT_MAX_ELAPSED_TIME = Long.MAX_VALUE;

  /**
   * The default maximum attempts.
   */
  public static final int DEFAULT_MAX_ATTEMPTS = Integer.MAX_VALUE;

  private long initialInterval = DEFAULT_INITIAL_INTERVAL;

  private double multiplier = DEFAULT_MULTIPLIER;

  private long maxInterval = DEFAULT_MAX_INTERVAL;

  private long maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME;

  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

  /**
   * Create an instance with the default settings.
   *
   * @see #DEFAULT_INITIAL_INTERVAL
   * @see #DEFAULT_MULTIPLIER
   * @see #DEFAULT_MAX_INTERVAL
   * @see #DEFAULT_MAX_ELAPSED_TIME
   * @see #DEFAULT_MAX_ATTEMPTS
   */
  public ExponentialBackOff() { }

  /**
   * Create an instance with the supplied settings.
   *
   * @param initialInterval the initial interval in milliseconds
   * @param multiplier the multiplier (should be greater than or equal to 1)
   */
  public ExponentialBackOff(long initialInterval, double multiplier) {
    checkMultiplier(multiplier);
    this.initialInterval = initialInterval;
    this.multiplier = multiplier;
  }

  /**
   * The initial interval in milliseconds.
   */
  public void setInitialInterval(long initialInterval) {
    this.initialInterval = initialInterval;
  }

  /**
   * Return the initial interval in milliseconds.
   */
  public long getInitialInterval() {
    return this.initialInterval;
  }

  /**
   * The value to multiply the current interval by for each retry attempt.
   */
  public void setMultiplier(double multiplier) {
    checkMultiplier(multiplier);
    this.multiplier = multiplier;
  }

  /**
   * Return the value to multiply the current interval by for each retry attempt.
   */
  public double getMultiplier() {
    return this.multiplier;
  }

  /**
   * The maximum back off time.
   */
  public void setMaxInterval(long maxInterval) {
    this.maxInterval = maxInterval;
  }

  /**
   * Return the maximum back off time.
   */
  public long getMaxInterval() {
    return this.maxInterval;
  }

  /**
   * The maximum elapsed time in milliseconds after which a call to
   * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
   *
   * @param maxElapsedTime the maximum elapsed time
   * @see #setMaxAttempts(int)
   */
  public void setMaxElapsedTime(long maxElapsedTime) {
    this.maxElapsedTime = maxElapsedTime;
  }

  /**
   * Return the maximum elapsed time in milliseconds after which a call to
   * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
   *
   * @return the maximum elapsed time
   * @see #getMaxAttempts()
   */
  public long getMaxElapsedTime() {
    return this.maxElapsedTime;
  }

  /**
   * The maximum number of attempts after which a call to
   * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
   *
   * @param maxAttempts the maximum number of attempts
   * @see #setMaxElapsedTime(long)
   */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * Return the maximum number of attempts after which a call to
   * {@link BackOffExecution#nextBackOff()} returns {@link BackOffExecution#STOP}.
   *
   * @return the maximum number of attempts
   * @see #getMaxElapsedTime()
   */
  public int getMaxAttempts() {
    return this.maxAttempts;
  }

  @Override
  public BackOffExecution start() {
    return new ExponentialBackOffExecution();
  }

  private void checkMultiplier(double multiplier) {
    if (multiplier < 1) {
      throw new IllegalArgumentException("Invalid multiplier '%s'. Should be greater than or equal to 1. A multiplier of 1 is equivalent to a fixed interval."
              .formatted(multiplier));
    }
  }

  private class ExponentialBackOffExecution implements BackOffExecution {

    private long currentInterval = -1;

    private long currentElapsedTime = 0;

    private int attempts;

    @Override
    public long nextBackOff() {
      if (this.currentElapsedTime >= getMaxElapsedTime()
              || this.attempts >= getMaxAttempts()) {
        return STOP;
      }

      long nextInterval = computeNextInterval();
      this.currentElapsedTime += nextInterval;
      this.attempts++;
      return nextInterval;
    }

    private long computeNextInterval() {
      long maxInterval = getMaxInterval();
      if (this.currentInterval >= maxInterval) {
        return maxInterval;
      }
      else if (this.currentInterval < 0) {
        long initialInterval = getInitialInterval();
        this.currentInterval = Math.min(initialInterval, maxInterval);
      }
      else {
        this.currentInterval = multiplyInterval(maxInterval);
      }
      return this.currentInterval;
    }

    private long multiplyInterval(long maxInterval) {
      long i = (long) (this.currentInterval * getMultiplier());
      return Math.min(i, maxInterval);
    }

    @Override
    public String toString() {
      return "ExponentialBackOff{currentInterval=%s, multiplier=%s}"
              .formatted(this.currentInterval < 0 ? "n/a" : this.currentInterval + "ms", getMultiplier());
    }
  }

}
