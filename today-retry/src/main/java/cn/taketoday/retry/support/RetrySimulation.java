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

package cn.taketoday.retry.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The results of a simulation.
 */
public class RetrySimulation {

  private final List<SleepSequence> sleepSequences = new ArrayList<SleepSequence>();

  private final List<Long> sleepHistogram = new ArrayList<Long>();

  public RetrySimulation() {
  }

  /**
   * Add a sequence of sleeps to the simulation.
   *
   * @param sleeps the times to be created as a {@link SleepSequence}
   */
  public void addSequence(List<Long> sleeps) {
    sleepHistogram.addAll(sleeps);
    sleepSequences.add(new SleepSequence(sleeps));
  }

  /**
   * @return Returns a list of all the unique sleep values which were executed within
   * all simulations.
   */
  public List<Double> getPercentiles() {
    List<Double> res = new ArrayList<Double>();
    for (double percentile : new double[] { 10, 20, 30, 40, 50, 60, 70, 80, 90 }) {
      res.add(getPercentile(percentile / 100));
    }
    return res;
  }

  public double getPercentile(double p) {
    Collections.sort(sleepHistogram);
    int size = sleepHistogram.size();
    double pos = p * (size - 1);
    int i0 = (int) pos;
    int i1 = i0 + 1;
    double weight = pos - i0;
    return sleepHistogram.get(i0) * (1 - weight) + sleepHistogram.get(i1) * weight;

  }

  /**
   * @return the longest total time slept by a retry sequence.
   */
  public SleepSequence getLongestTotalSleepSequence() {
    SleepSequence longest = null;
    for (SleepSequence sequence : sleepSequences) {
      if (longest == null || sequence.getTotalSleep() > longest.getTotalSleep()) {
        longest = sequence;
      }
    }
    return longest;
  }

  public static class SleepSequence {

    private final List<Long> sleeps;

    private final long longestSleep;

    private final long totalSleep;

    public SleepSequence(List<Long> sleeps) {
      this.sleeps = sleeps;
      this.longestSleep = Collections.max(sleeps);
      long totalSleep = 0;
      for (Long sleep : sleeps) {
        totalSleep += sleep;
      }
      this.totalSleep = totalSleep;
    }

    public List<Long> getSleeps() {
      return sleeps;
    }

    /**
     * @return the longest individual sleep within this sequence
     */
    public long getLongestSleep() {
      return longestSleep;
    }

    public long getTotalSleep() {
      return totalSleep;
    }

    public String toString() {
      return "totalSleep=" + totalSleep + ": " + sleeps.toString();
    }

  }

}
