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


package cn.taketoday.util.backoff;

/**
 * A simple {@link BackOff} implementation that provides a fixed interval
 * between two attempts and a maximum number of retries.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FixedBackOff implements BackOff {

  /**
   * The default recovery interval: 5000 ms = 5 seconds.
   */
  public static final long DEFAULT_INTERVAL = 5000;

  /**
   * Constant value indicating an unlimited number of attempts.
   */
  public static final long UNLIMITED_ATTEMPTS = Long.MAX_VALUE;

  private long interval = DEFAULT_INTERVAL;

  private long maxAttempts = UNLIMITED_ATTEMPTS;

  /**
   * Create an instance with an interval of {@value #DEFAULT_INTERVAL}
   * ms and an unlimited number of attempts.
   */
  public FixedBackOff() { }

  /**
   * Create an instance.
   *
   * @param interval the interval between two attempts
   * @param maxAttempts the maximum number of attempts
   */
  public FixedBackOff(long interval, long maxAttempts) {
    this.interval = interval;
    this.maxAttempts = maxAttempts;
  }

  /**
   * Set the interval between two attempts in milliseconds.
   */
  public void setInterval(long interval) {
    this.interval = interval;
  }

  /**
   * Return the interval between two attempts in milliseconds.
   */
  public long getInterval() {
    return this.interval;
  }

  /**
   * Set the maximum number of attempts in milliseconds.
   */
  public void setMaxAttempts(long maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * Return the maximum number of attempts in milliseconds.
   */
  public long getMaxAttempts() {
    return this.maxAttempts;
  }

  @Override
  public BackOffExecution start() {
    return new FixedBackOffExecution();
  }

  private class FixedBackOffExecution implements BackOffExecution {

    private long currentAttempts = 0;

    @Override
    public long nextBackOff() {
      this.currentAttempts++;
      if (this.currentAttempts <= getMaxAttempts()) {
        return getInterval();
      }
      else {
        return STOP;
      }
    }

    @Override
    public String toString() {
      String attemptValue = (FixedBackOff.this.maxAttempts == Long.MAX_VALUE ?
              "unlimited" : String.valueOf(FixedBackOff.this.maxAttempts));
      return "FixedBackOff{interval=%d, currentAttempts=%d, maxAttempts=%s}".formatted(FixedBackOff.this.interval, this.currentAttempts, attemptValue);
    }
  }

}
