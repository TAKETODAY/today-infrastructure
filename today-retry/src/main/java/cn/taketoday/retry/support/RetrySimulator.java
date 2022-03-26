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
import java.util.List;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.backoff.SleepingBackOffPolicy;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.backoff.ExponentialRandomBackOffPolicy;
import cn.taketoday.retry.backoff.Sleeper;

/**
 * A {@link RetrySimulator} is a tool for exercising retry + backoff operations.
 *
 * When calibrating a set of retry + backoff pairs, it is useful to know the behaviour of
 * the retry for various scenarios.
 *
 * Things you may want to know: - Does a 'maxInterval' of 5000 ms in my backoff even
 * matter? (This is often the case when retry counts are low -- so why set the max
 * interval at something that cannot be achieved?) - What are the typical sleep durations
 * for threads in a retry - What was the longest sleep duration for any retry sequence
 *
 * The simulator provides this information by executing a retry + backoff pair until
 * failure (that is all retries are exhausted). The information about each retry is
 * provided as part of the {@link RetrySimulation}.
 *
 * Note that the impetus for this class was to expose the timings which are possible with
 * {@link ExponentialRandomBackOffPolicy}, which
 * provides random values and must be looked at over a series of trials.
 *
 * @author Jon Travis
 */
public class RetrySimulator {

  private final SleepingBackOffPolicy<?> backOffPolicy;

  private final RetryPolicy retryPolicy;

  public RetrySimulator(SleepingBackOffPolicy<?> backOffPolicy, RetryPolicy retryPolicy) {
    this.backOffPolicy = backOffPolicy;
    this.retryPolicy = retryPolicy;
  }

  /**
   * Execute the simulator for a give # of iterations.
   *
   * @param numSimulations Number of simulations to run
   * @return the outcome of all simulations
   */
  public RetrySimulation executeSimulation(int numSimulations) {
    RetrySimulation simulation = new RetrySimulation();

    for (int i = 0; i < numSimulations; i++) {
      simulation.addSequence(executeSingleSimulation());
    }
    return simulation;
  }

  /**
   * Execute a single simulation
   *
   * @return The sleeps which occurred within the single simulation.
   */
  public List<Long> executeSingleSimulation() {
    StealingSleeper stealingSleeper = new StealingSleeper();
    SleepingBackOffPolicy<?> stealingBackoff = backOffPolicy.withSleeper(stealingSleeper);

    RetryTemplate template = new RetryTemplate();
    template.setBackOffPolicy(stealingBackoff);
    template.setRetryPolicy(retryPolicy);

    try {
      template.execute(new FailingRetryCallback());
    }
    catch (FailingRetryException e) {

    }
    catch (Throwable e) {
      throw new RuntimeException("Unexpected exception", e);
    }

    return stealingSleeper.getSleeps();
  }

  static class FailingRetryCallback implements RetryCallback<Object, Exception> {

    public Object doWithRetry(RetryContext context) throws Exception {
      throw new FailingRetryException();
    }

  }

  @SuppressWarnings("serial")
  static class FailingRetryException extends Exception {

  }

  @SuppressWarnings("serial")
  static class StealingSleeper implements Sleeper {

    private final List<Long> sleeps = new ArrayList<Long>();

    public void sleep(long backOffPeriod) throws InterruptedException {
      sleeps.add(backOffPeriod);
    }

    public List<Long> getSleeps() {
      return sleeps;
    }

  }

}
