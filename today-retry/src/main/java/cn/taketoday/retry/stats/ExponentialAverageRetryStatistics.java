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

package cn.taketoday.retry.stats;

/**
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class ExponentialAverageRetryStatistics extends DefaultRetryStatistics {

  private long window = 15000;

  private ExponentialAverage started;

  private ExponentialAverage error;

  private ExponentialAverage complete;

  private ExponentialAverage recovery;

  private ExponentialAverage abort;

  public ExponentialAverageRetryStatistics(String name) {
    super(name);
    init();
  }

  private void init() {
    started = new ExponentialAverage(window);
    error = new ExponentialAverage(window);
    complete = new ExponentialAverage(window);
    abort = new ExponentialAverage(window);
    recovery = new ExponentialAverage(window);
  }

  /**
   * Window in milliseconds for exponential decay factor in rolling average.
   *
   * @param window the window to set
   */
  public void setWindow(long window) {
    this.window = window;
    init();
  }

  public int getRollingStartedCount() {
    return (int) Math.round(started.getValue());
  }

  public int getRollingErrorCount() {
    return (int) Math.round(error.getValue());
  }

  public int getRollingAbortCount() {
    return (int) Math.round(abort.getValue());
  }

  public int getRollingRecoveryCount() {
    return (int) Math.round(recovery.getValue());
  }

  public int getRollingCompleteCount() {
    return (int) Math.round(complete.getValue());
  }

  public double getRollingErrorRate() {
    if (Math.round(started.getValue()) == 0) {
      return 0.;
    }
    return (abort.getValue() + recovery.getValue()) / started.getValue();
  }

  @Override
  public void incrementStartedCount() {
    super.incrementStartedCount();
    started.increment();
  }

  @Override
  public void incrementCompleteCount() {
    super.incrementCompleteCount();
    complete.increment();
  }

  @Override
  public void incrementRecoveryCount() {
    super.incrementRecoveryCount();
    recovery.increment();
  }

  @Override
  public void incrementErrorCount() {
    super.incrementErrorCount();
    error.increment();
  }

  @Override
  public void incrementAbortCount() {
    super.incrementAbortCount();
    abort.increment();
  }

  private static class ExponentialAverage {

    private final double alpha;

    private volatile long lastTime = System.currentTimeMillis();

    private volatile double value = 0;

    public ExponentialAverage(long window) {
      alpha = 1. / window;
    }

    public synchronized void increment() {
      long time = System.currentTimeMillis();
      value = value * Math.exp(-alpha * (time - lastTime)) + 1;
      lastTime = time;
    }

    public double getValue() {
      long time = System.currentTimeMillis();
      return value * Math.exp(-alpha * (time - lastTime));
    }

  }

}
