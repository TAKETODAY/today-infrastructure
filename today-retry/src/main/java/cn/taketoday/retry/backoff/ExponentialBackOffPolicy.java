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

package cn.taketoday.retry.backoff;

import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.util.ClassUtils;

/**
 * Implementation of {@link BackOffPolicy} that increases the back off period for each
 * retry attempt in a given set up to a limit.
 *
 * This implementation is thread-safe and suitable for concurrent access. Modifications to
 * the configuration do not affect any retry sets that are already in progress.
 *
 * The {@link #setInitialInterval(long)} property controls the initial delay value for the
 * first retry and the {@link #setMultiplier(double)} property controls by how much the
 * delay is increased for each subsequent attempt. The delay interval is capped at
 * {@link #setMaxInterval(long)}.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ExponentialBackOffPolicy implements SleepingBackOffPolicy<ExponentialBackOffPolicy> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * The default 'initialInterval' value - 100 millisecs. Coupled with the default
   * 'multiplier' value this gives a useful initial spread of pauses for 1-5 retries.
   */
  public static final long DEFAULT_INITIAL_INTERVAL = 100L;

  /**
   * The default maximum backoff time (30 seconds).
   */
  public static final long DEFAULT_MAX_INTERVAL = 30000L;

  /**
   * The default 'multiplier' value - value 2 (100% increase per backoff).
   */
  public static final double DEFAULT_MULTIPLIER = 2;

  /**
   * The initial backoff interval.
   */
  private volatile long initialInterval = DEFAULT_INITIAL_INTERVAL;

  /**
   * The maximum value of the backoff period in milliseconds.
   */
  private volatile long maxInterval = DEFAULT_MAX_INTERVAL;

  /**
   * The value to add to the backoff period for each retry attempt.
   */
  private volatile double multiplier = DEFAULT_MULTIPLIER;

  private Sleeper sleeper = new ThreadWaitSleeper();

  /**
   * The initial backoff interval.
   */
  private Supplier<Long> initialIntervalSupplier;

  /**
   * The maximum value of the backoff period in milliseconds.
   */
  private Supplier<Long> maxIntervalSupplier;

  /**
   * The value to add to the backoff period for each retry attempt.
   */
  private Supplier<Double> multiplierSupplier;

  /**
   * Public setter for the {@link Sleeper} strategy.
   *
   * @param sleeper the sleeper to set defaults to {@link ThreadWaitSleeper}.
   */
  public void setSleeper(Sleeper sleeper) {
    this.sleeper = sleeper;
  }

  @Override
  public ExponentialBackOffPolicy withSleeper(Sleeper sleeper) {
    ExponentialBackOffPolicy res = newInstance();
    cloneValues(res);
    res.setSleeper(sleeper);
    return res;
  }

  protected ExponentialBackOffPolicy newInstance() {
    return new ExponentialBackOffPolicy();
  }

  protected void cloneValues(ExponentialBackOffPolicy target) {
    target.setInitialInterval(getInitialInterval());
    target.setMaxInterval(getMaxInterval());
    target.setMultiplier(getMultiplier());
    target.setSleeper(this.sleeper);
  }

  /**
   * Set the initial sleep interval value. Default is {@code 100} millisecond. Cannot be
   * set to a value less than one.
   *
   * @param initialInterval the initial interval
   */
  public void setInitialInterval(long initialInterval) {
    this.initialInterval = (initialInterval > 1 ? initialInterval : 1);
  }

  /**
   * Set the multiplier value. Default is '<code>2.0</code>'. Hint: do not use values
   * much in excess of 1.0 (or the backoff will get very long very fast).
   *
   * @param multiplier the multiplier
   */
  public void setMultiplier(double multiplier) {
    this.multiplier = Math.max(multiplier, 1.0);
  }

  /**
   * Setter for maximum back off period. Default is 30000 (30 seconds). the value will
   * be reset to 1 if this method is called with a value less than 1. Set this to avoid
   * infinite waits if backing off a large number of times (or if the multiplier is set
   * too high).
   *
   * @param maxInterval in milliseconds.
   */
  public void setMaxInterval(long maxInterval) {
    this.maxInterval = maxInterval > 0 ? maxInterval : 1;
  }

  /**
   * The initial period to sleep on the first backoff.
   *
   * @return the initial interval
   */
  public long getInitialInterval() {
    return this.initialInterval;
  }

  /**
   * The maximum interval to sleep for. Defaults to 30 seconds.
   *
   * @return the maximum interval.
   */
  public long getMaxInterval() {
    return this.maxInterval;
  }

  /**
   * The multiplier to use to generate the next backoff interval from the last.
   *
   * @return the multiplier in use
   */
  public double getMultiplier() {
    return this.multiplier;
  }

  /**
   * Set the initial sleep interval value. Default supplier supplies {@code 100}
   * millisecond.
   *
   * @param initialIntervalSupplier the initial interval
   */
  public void setInitialInterval(Supplier<Long> initialIntervalSupplier) {
    Assert.notNull(initialIntervalSupplier, "'initialIntervalSupplier' cannot be null");
    this.initialIntervalSupplier = initialIntervalSupplier;
  }

  /**
   * Set the multiplier value. Default supplier supplies '<code>2.0</code>'. Hint: do
   * not use values much in excess of 1.0 (or the backoff will get very long very fast).
   *
   * @param multiplierSupplier the multiplier
   */
  public void setMultiplier(Supplier<Double> multiplierSupplier) {
    Assert.notNull(multiplierSupplier, "multiplierSupplier is required");
    this.multiplierSupplier = multiplierSupplier;
  }

  /**
   * Setter for maximum back off period. Default is 30000 (30 seconds). the value will
   * be reset to 1 if this method is called with a value less than 1. Set this to avoid
   * infinite waits if backing off a large number of times (or if the multiplier is set
   * too high).
   *
   * @param maxIntervalSupplier in milliseconds.
   */
  public void setMaxInterval(Supplier<Long> maxIntervalSupplier) {
    Assert.notNull(maxIntervalSupplier, "maxIntervalSupplier is required");
    this.maxIntervalSupplier = maxIntervalSupplier;
  }

  protected Supplier<Long> getInitialIntervalSupplier() {
    return initialIntervalSupplier;
  }

  protected Supplier<Long> getMaxIntervalSupplier() {
    return maxIntervalSupplier;
  }

  protected Supplier<Double> getMultiplierSupplier() {
    return multiplierSupplier;
  }

  /**
   * Returns a new instance of {@link BackOffContext} with the configured properties.
   */
  @Override
  public BackOffContext start(RetryContext context) {
    return new ExponentialBackOffContext(this.initialInterval, this.multiplier, this.maxInterval,
            this.initialIntervalSupplier, this.multiplierSupplier, this.maxIntervalSupplier);
  }

  /**
   * Pause for the current backoff interval.
   */
  @Override
  public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
    ExponentialBackOffContext context = (ExponentialBackOffContext) backOffContext;
    try {
      long sleepTime = context.getSleepAndIncrement();
      if (this.logger.isDebugEnabled()) {
        this.logger.debug("Sleeping for " + sleepTime);
      }
      this.sleeper.sleep(sleepTime);
    }
    catch (InterruptedException e) {
      throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
    }
  }

  static class ExponentialBackOffContext implements BackOffContext {

    private final double multiplier;

    private long interval;

    private final long maxInterval;

    private final Supplier<Long> intervalSupplier;
    private final Supplier<Long> maxIntervalSupplier;
    private final Supplier<Double> multiplierSupplier;

    public ExponentialBackOffContext(long interval, double multiplier, long maxInterval,
            Supplier<Long> intervalSupplier, Supplier<Double> multiplierSupplier,
            Supplier<Long> maxIntervalSupplier) {
      this.interval = interval;
      this.multiplier = multiplier;
      this.maxInterval = maxInterval;
      this.intervalSupplier = intervalSupplier;
      this.multiplierSupplier = multiplierSupplier;
      this.maxIntervalSupplier = maxIntervalSupplier;
    }

    public synchronized long getSleepAndIncrement() {
      long sleep = getInterval();
      long max = getMaxInterval();
      if (sleep > max) {
        sleep = max;
      }
      else {
        this.interval = getNextInterval();
      }
      return sleep;
    }

    protected long getNextInterval() {
      return (long) (this.interval * getMultiplier());
    }

    public double getMultiplier() {
      return this.multiplierSupplier != null ? this.multiplierSupplier.get() : this.multiplier;
    }

    public long getInterval() {
      return this.intervalSupplier != null ? this.intervalSupplier.get() : this.interval;
    }

    public long getMaxInterval() {
      return this.maxIntervalSupplier != null ? this.maxIntervalSupplier.get() : this.maxInterval;
    }

  }

  @Override
  public String toString() {
    return ClassUtils.getShortName(getClass()) + "[initialInterval=" + this.initialInterval + ", multiplier="
            + this.multiplier + ", maxInterval=" + this.maxInterval + "]";
  }

}
