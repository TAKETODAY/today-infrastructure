/*
 * Copyright 2017 - 2023 the original author or authors.
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

/**
 * Implementation of {@link BackOffPolicy} that pauses for a fixed period of time before
 * continuing. A pause is implemented using {@link Sleeper#sleep(long)}.
 *
 * {@link #setBackOffPeriod(long)} is thread-safe and it is safe to call
 * {@link #setBackOffPeriod} during execution from multiple threads, however this may
 * cause a single retry operation to have pauses of different intervals.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @author Artem Bilan
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FixedBackOffPolicy extends StatelessBackOffPolicy
        implements SleepingBackOffPolicy<FixedBackOffPolicy> {

  /**
   * Default back off period - 1000ms.
   */
  private static final long DEFAULT_BACK_OFF_PERIOD = 1000L;

  /**
   * The back off period in milliseconds. Defaults to 1000ms.
   */
  private Supplier<Long> backOffPeriod = () -> DEFAULT_BACK_OFF_PERIOD;

  private Sleeper sleeper = new ThreadWaitSleeper();

  public FixedBackOffPolicy withSleeper(Sleeper sleeper) {
    FixedBackOffPolicy res = new FixedBackOffPolicy();
    res.backOffPeriodSupplier(backOffPeriod);
    res.setSleeper(sleeper);
    return res;
  }

  /**
   * Public setter for the {@link Sleeper} strategy.
   *
   * @param sleeper the sleeper to set defaults to {@link ThreadWaitSleeper}.
   */
  public void setSleeper(Sleeper sleeper) {
    this.sleeper = sleeper;
  }

  /**
   * Set the back off period in milliseconds. Cannot be &lt; 1. Default value is 1000ms.
   *
   * @param backOffPeriod the back off period
   */
  public void setBackOffPeriod(long backOffPeriod) {
    this.backOffPeriod = () -> (backOffPeriod > 0 ? backOffPeriod : 1);
  }

  /**
   * Set a supplier for the back off period in milliseconds. Cannot be &lt; 1. Default
   * supplier supplies 1000ms.
   *
   * @param backOffPeriodSupplier the back off period
   */
  public void backOffPeriodSupplier(Supplier<Long> backOffPeriodSupplier) {
    Assert.notNull(backOffPeriodSupplier, "'backOffPeriodSupplier' is required");
    this.backOffPeriod = backOffPeriodSupplier;
  }

  /**
   * The backoff period in milliseconds.
   *
   * @return the backoff period
   */
  public long getBackOffPeriod() {
    return this.backOffPeriod.get();
  }

  /**
   * Pause for the {@link #setBackOffPeriod(long)}.
   *
   * @throws BackOffInterruptedException if interrupted during sleep.
   */
  @Override
  protected void doBackOff() throws BackOffInterruptedException {
    try {
      sleeper.sleep(this.backOffPeriod.get());
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
    }
  }

  @Override
  public String toString() {
    return "FixedBackOffPolicy[backOffPeriod=" + this.backOffPeriod.get() + "]";
  }

}
