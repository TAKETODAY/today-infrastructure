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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.retry.backoff.ExponentialBackOffPolicy;
import cn.taketoday.retry.backoff.ExponentialRandomBackOffPolicy;
import cn.taketoday.retry.backoff.FixedBackOffPolicy;
import cn.taketoday.retry.policy.SimpleRetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;

public class RetrySimulationTests {

  @Test
  public void testSimulatorExercisesFixedBackoff() {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(5);

    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    backOffPolicy.setBackOffPeriod(400);

    RetrySimulator simulator = new RetrySimulator(backOffPolicy, retryPolicy);
    RetrySimulation simulation = simulator.executeSimulation(1000);
    System.out.println(backOffPolicy);
    System.out.println("Longest sequence  " + simulation.getLongestTotalSleepSequence());
    System.out.println("Percentiles:       " + simulation.getPercentiles());

    assertThat(simulation.getLongestTotalSleepSequence().getSleeps())
            .isEqualTo(Arrays.asList(400l, 400l, 400l, 400l));
    assertThat(simulation.getPercentiles())
            .isEqualTo(Arrays.asList(400d, 400d, 400d, 400d, 400d, 400d, 400d, 400d, 400d));
    assertThat(simulation.getPercentile(0.5)).isEqualTo(400d);
  }

  @Test
  public void testSimulatorExercisesExponentialBackoff() {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(5);

    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setMultiplier(2);
    backOffPolicy.setMaxInterval(30000);
    backOffPolicy.setInitialInterval(100);

    RetrySimulator simulator = new RetrySimulator(backOffPolicy, retryPolicy);
    RetrySimulation simulation = simulator.executeSimulation(1000);
    System.out.println(backOffPolicy);
    System.out.println("Longest sequence  " + simulation.getLongestTotalSleepSequence());
    System.out.println("Percentiles:       " + simulation.getPercentiles());

    assertThat(simulation.getLongestTotalSleepSequence().getSleeps())
            .isEqualTo(Arrays.asList(100l, 200l, 400l, 800l));
    assertThat(simulation.getPercentiles())
            .isEqualTo(Arrays.asList(100d, 100d, 200d, 200d, 300d, 400d, 400d, 800d, 800d));
    assertThat(simulation.getPercentile(0.5f)).isEqualTo(300d);
  }

  @Test
  public void testSimulatorExercisesRandomExponentialBackoff() {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(5);

    ExponentialBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
    backOffPolicy.setMultiplier(2);
    backOffPolicy.setMaxInterval(30000);
    backOffPolicy.setInitialInterval(100);

    RetrySimulator simulator = new RetrySimulator(backOffPolicy, retryPolicy);
    RetrySimulation simulation = simulator.executeSimulation(10000);
    System.out.println(backOffPolicy);
    System.out.println("Longest sequence  " + simulation.getLongestTotalSleepSequence());
    System.out.println("Percentiles:       " + simulation.getPercentiles());

    assertThat(simulation.getPercentiles().size()).isGreaterThan(4);
  }

}
