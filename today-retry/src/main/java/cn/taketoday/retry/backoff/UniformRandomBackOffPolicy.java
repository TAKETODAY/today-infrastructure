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

package cn.taketoday.retry.backoff;

import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;

/**
 * Implementation of {@link BackOffPolicy} that pauses for a random period of time before
 * continuing. A pause is implemented using {@link Sleeper#sleep(long)}.
 *
 * {@link #setMinBackOffPeriod(long)} is thread-safe and it is safe to call
 * {@link #setMaxBackOffPeriod(long)} during execution from multiple threads, however this
 * may cause a single retry operation to have pauses of different intervals.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @author Tomaz Fernandes
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UniformRandomBackOffPolicy extends StatelessBackOffPolicy
        implements SleepingBackOffPolicy<UniformRandomBackOffPolicy> {

  /**
   * Default min back off period - 500ms.
   */
  private static final long DEFAULT_BACK_OFF_MIN_PERIOD = 500L;

  /**
   * Default max back off period - 1500ms.
   */
  private static final long DEFAULT_BACK_OFF_MAX_PERIOD = 1500L;

  private Supplier<Long> minBackOffPeriod = () -> DEFAULT_BACK_OFF_MIN_PERIOD;

  private Supplier<Long> maxBackOffPeriod = () -> DEFAULT_BACK_OFF_MAX_PERIOD;

  private final Random random = new Random(System.currentTimeMillis());

  private Sleeper sleeper = new ThreadWaitSleeper();

  public UniformRandomBackOffPolicy withSleeper(Sleeper sleeper) {
    UniformRandomBackOffPolicy res = new UniformRandomBackOffPolicy();
    res.minBackOffPeriodSupplier(minBackOffPeriod);
    res.maxBackOffPeriodSupplier(maxBackOffPeriod);
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
   * Set the minimum back off period in milliseconds. Cannot be &lt; 1. Default value is
   * 500ms.
   *
   * @param backOffPeriod the backoff period
   */
  public void setMinBackOffPeriod(long backOffPeriod) {
    this.minBackOffPeriod = () -> (backOffPeriod > 0 ? backOffPeriod : 1);
  }

  /**
   * Set a supplier for the minimum back off period in milliseconds. Cannot be &lt; 1.
   * Default supplier supplies 500ms.
   *
   * @param backOffPeriodSupplier the backoff period
   */
  public void minBackOffPeriodSupplier(Supplier<Long> backOffPeriodSupplier) {
    Assert.notNull(backOffPeriodSupplier, "'backOffPeriodSupplier' is required");
    this.minBackOffPeriod = backOffPeriodSupplier;
  }

  /**
   * The minimum backoff period in milliseconds.
   *
   * @return the backoff period
   */
  public long getMinBackOffPeriod() {
    return minBackOffPeriod.get();
  }

  /**
   * Set the maximum back off period in milliseconds. Cannot be &lt; 1. Default value is
   * 1500ms.
   *
   * @param backOffPeriod the back off period
   */
  public void setMaxBackOffPeriod(long backOffPeriod) {
    this.maxBackOffPeriod = () -> (backOffPeriod > 0 ? backOffPeriod : 1);
  }

  /**
   * Set a supplier for the maximum back off period in milliseconds. Cannot be &lt; 1.
   * Default supplier supplies 1500ms.
   *
   * @param backOffPeriodSupplier the back off period
   */
  public void maxBackOffPeriodSupplier(Supplier<Long> backOffPeriodSupplier) {
    Assert.notNull(backOffPeriodSupplier, "'backOffPeriodSupplier' cannot be null");
    this.maxBackOffPeriod = backOffPeriodSupplier;
  }

  /**
   * The maximum backoff period in milliseconds.
   *
   * @return the backoff period
   */
  public long getMaxBackOffPeriod() {
    return maxBackOffPeriod.get();
  }

  /**
   * Pause for the {@link #setMinBackOffPeriod(long)}.
   *
   * @throws BackOffInterruptedException if interrupted during sleep.
   */
  @Override
  protected void doBackOff() throws BackOffInterruptedException {
    try {
      Long min = minBackOffPeriod.get();
      long delta = Objects.equals(maxBackOffPeriod.get(), minBackOffPeriod.get())
                   ? 0
                   : random.nextInt((int) (maxBackOffPeriod.get() - min));
      this.sleeper.sleep(min + delta);
    }
    catch (InterruptedException e) {
      throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
    }
  }

  @Override
  public String toString() {
    return "RandomBackOffPolicy[backOffPeriod=" + minBackOffPeriod + ", " + maxBackOffPeriod + "]";
  }

}
