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

package infra.retry.backoff;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.retry.policy.SimpleRetryPolicy;
import infra.retry.support.RetrySimulation;
import infra.retry.support.RetrySimulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Jon Travis
 * @author Chase Diem
 * @author Gary Russell
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
  public void testSingleBackoff() {
    ExponentialBackOffPolicy backOffPolicy = makeBackoffPolicy();
    RetrySimulator simulator = new RetrySimulator(backOffPolicy, makeRetryPolicy());
    RetrySimulation simulation = simulator.executeSimulation(1);

    List<Long> sleeps = simulation.getLongestTotalSleepSequence().getSleeps();
    System.out.println("Single trial of " + backOffPolicy + ": sleeps=" + sleeps);
    assertThat(sleeps).hasSize(MAX_RETRIES - 1);
    long initialInterval = backOffPolicy.getInitialInterval();
    for (int i = 0; i < sleeps.size(); i++) {
      long expectedMaxValue = 2 * (long) (initialInterval
              + initialInterval * Math.max(1, Math.pow(backOffPolicy.getMultiplier(), i)));
      assertThat(sleeps.get(i))
              .describedAs("Found a sleep [%d] which exceeds our max expected value of %d at interval %d",
                      sleeps.get(i), expectedMaxValue, i)
              .isLessThan(expectedMaxValue);
    }
  }

  @Test
  public void testMaxInterval() {
    ExponentialBackOffPolicy backOffPolicy = makeBackoffPolicy();
    backOffPolicy.setInitialInterval(3000);
    long maxInterval = backOffPolicy.getMaxInterval();

    RetrySimulator simulator = new RetrySimulator(backOffPolicy, makeRetryPolicy());
    RetrySimulation simulation = simulator.executeSimulation(1);

    List<Long> sleeps = simulation.getLongestTotalSleepSequence().getSleeps();
    System.out.println("Single trial of " + backOffPolicy + ": sleeps=" + sleeps);
    assertThat(sleeps).hasSize(MAX_RETRIES - 1);
    long initialInterval = backOffPolicy.getInitialInterval();
    for (int i = 0; i < sleeps.size(); i++) {
      long expectedMaxValue = 2 * (long) (initialInterval
              + initialInterval * Math.max(1, Math.pow(backOffPolicy.getMultiplier(), i)));
      assertThat(sleeps.get(i))
              .describedAs("Found a sleep [%d] which exceeds our max expected value of %d at interval %d",
                      sleeps.get(i), expectedMaxValue, i)
              .isLessThanOrEqualTo(expectedMaxValue);
    }
  }

  @Test
  public void testMultiBackOff() {
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
