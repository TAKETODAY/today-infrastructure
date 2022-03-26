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

import org.junit.Test;

import java.util.List;

import cn.taketoday.retry.policy.SimpleRetryPolicy;
import cn.taketoday.retry.support.RetrySimulation;
import cn.taketoday.retry.support.RetrySimulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 * @author Jon Travis
 * @author Chase Diem
 */
public class ExponentialRandomBackOffPolicyTests {

  static final int NUM_TRIALS = 10000;
  static final int MAX_RETRIES = 6;

  private ExponentialBackOffPolicy makeBackoffPolicy() {
    ExponentialBackOffPolicy policy = new ExponentialRandomBackOffPolicy();
    policy.setInitialInterval(50);
    policy.setMultiplier(2.0);
    policy.setMaxInterval(3000);
    return policy;
  }

  private SimpleRetryPolicy makeRetryPolicy() {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(MAX_RETRIES);
    return retryPolicy;
  }

  @Test
  public void testSingleBackoff() throws Exception {
    ExponentialBackOffPolicy backOffPolicy = makeBackoffPolicy();
    RetrySimulator simulator = new RetrySimulator(backOffPolicy, makeRetryPolicy());
    RetrySimulation simulation = simulator.executeSimulation(1);

    List<Long> sleeps = simulation.getLongestTotalSleepSequence().getSleeps();
    System.out.println("Single trial of " + backOffPolicy + ": sleeps=" + sleeps);
    assertEquals(MAX_RETRIES - 1, sleeps.size());
    long initialInterval = backOffPolicy.getInitialInterval();
    for (int i = 0; i < sleeps.size(); i++) {
      long expectedMaxValue = 2 * (long) (initialInterval
              + initialInterval * Math.max(1, Math.pow(backOffPolicy.getMultiplier(), i)));
      assertTrue("Found a sleep [" + sleeps.get(i) + "] which exceeds our max expected value of "
              + expectedMaxValue + " at interval " + i, sleeps.get(i) < expectedMaxValue);
    }
  }

  @Test
  public void testMaxInterval() throws Exception {
    ExponentialBackOffPolicy backOffPolicy = makeBackoffPolicy();
    backOffPolicy.setInitialInterval(3000);
    long maxInterval = backOffPolicy.getMaxInterval();

    RetrySimulator simulator = new RetrySimulator(backOffPolicy, makeRetryPolicy());
    RetrySimulation simulation = simulator.executeSimulation(1);

    List<Long> sleeps = simulation.getLongestTotalSleepSequence().getSleeps();
    System.out.println("Single trial of " + backOffPolicy + ": sleeps=" + sleeps);
    assertEquals(MAX_RETRIES - 1, sleeps.size());
    long initialInterval = backOffPolicy.getInitialInterval();
    for (int i = 0; i < sleeps.size(); i++) {
      long expectedMaxValue = 2 * (long) (initialInterval
              + initialInterval * Math.max(1, Math.pow(backOffPolicy.getMultiplier(), i)));
      assertTrue("Found a sleep [" + sleeps.get(i) + "] which exceeds our max interval value of "
              + expectedMaxValue + " at interval " + i, sleeps.get(i) <= maxInterval);
    }
  }

  @Test
  public void testMultiBackOff() throws Exception {
    ExponentialBackOffPolicy backOffPolicy = makeBackoffPolicy();
    RetrySimulator simulator = new RetrySimulator(backOffPolicy, makeRetryPolicy());
    RetrySimulation simulation = simulator.executeSimulation(NUM_TRIALS);

    System.out.println("Ran " + NUM_TRIALS + " backoff trials.  Each trial retried " + MAX_RETRIES + " times");
    System.out.println("Policy: " + backOffPolicy);
    System.out.println("All generated backoffs:");
    System.out.println("    " + simulation.getPercentiles());

    System.out.println("Backoff frequencies:");
    System.out.print("    " + simulation.getPercentiles());

  }

}
