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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Dave Syer
 * @author Gary Russell
 */
public class ExponentialBackOffPolicyTests {

  private final DummySleeper sleeper = new DummySleeper();

  @Test
  public void testSetMaxInterval() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setMaxInterval(1000);
    assertThat(strategy.toString()).contains("maxInterval=1000");
    strategy.setMaxInterval(0);
    // The minimum value for the max interval is 1
    assertThat(strategy.toString()).contains("maxInterval=1");
  }

  @Test
  public void testSetInitialInterval() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setInitialInterval(10000);
    assertThat(strategy.toString()).contains("initialInterval=10000,");
    strategy.setInitialInterval(0);
    assertThat(strategy.toString()).contains("initialInterval=1,");
  }

  @Test
  public void testSetMultiplier() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setMultiplier(3.);
    assertThat(strategy.toString()).contains("multiplier=3.");
    strategy.setMultiplier(.5);
    assertThat(strategy.toString()).contains("multiplier=1.");
  }

  @Test
  public void testSingleBackOff() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setSleeper(sleeper);
    BackOffContext context = strategy.start(null);
    strategy.backOff(context);
    assertThat(sleeper.getLastBackOff()).isEqualTo(ExponentialBackOffPolicy.DEFAULT_INITIAL_INTERVAL);
  }

  @Test
  public void testMaximumBackOff() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setMaxInterval(50);
    strategy.setSleeper(sleeper);
    BackOffContext context = strategy.start(null);
    strategy.backOff(context);
    assertThat(sleeper.getLastBackOff()).isEqualTo(50);
  }

  @Test
  public void testMultiBackOff() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    long seed = 40;
    double multiplier = 1.2;
    strategy.setInitialInterval(seed);
    strategy.setMultiplier(multiplier);
    strategy.setSleeper(sleeper);
    BackOffContext context = strategy.start(null);
    for (int x = 0; x < 5; x++) {
      strategy.backOff(context);
      assertThat(sleeper.getLastBackOff()).isEqualTo(seed);
      seed *= multiplier;
    }
  }

  @Test
  public void testMultiBackOffWithInitialDelaySupplier() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    long seed = 40;
    double multiplier = 1.2;
    strategy.initialIntervalSupplier(() -> 40L);
    strategy.setMultiplier(multiplier);
    strategy.setSleeper(sleeper);
    BackOffContext context = strategy.start(null);
    for (int x = 0; x < 5; x++) {
      strategy.backOff(context);
      assertThat(sleeper.getLastBackOff()).isEqualTo(seed);
      seed *= multiplier;
    }
  }

  @Test
  public void testInterruptedStatusIsRestored() {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setSleeper(backOffPeriod -> {
      throw new InterruptedException("foo");
    });
    BackOffContext context = strategy.start(null);
    assertThatExceptionOfType(BackOffInterruptedException.class).isThrownBy(() -> strategy.backOff(context));
    assertThat(Thread.interrupted()).isTrue();
  }

}
