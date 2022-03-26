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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Rob Harrop
 * @author Dave Syer
 */
public class ExponentialBackOffPolicyTests {

  private DummySleeper sleeper = new DummySleeper();

  @Test
  public void testSetMaxInterval() throws Exception {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setMaxInterval(1000);
    assertTrue(strategy.toString().indexOf("maxInterval=1000") >= 0);
    strategy.setMaxInterval(0);
    // The minimum value for the max interval is 1
    assertTrue(strategy.toString().indexOf("maxInterval=1") >= 0);
  }

  @Test
  public void testSetInitialInterval() throws Exception {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setInitialInterval(10000);
    assertTrue(strategy.toString().indexOf("initialInterval=10000,") >= 0);
    strategy.setInitialInterval(0);
    assertTrue(strategy.toString().indexOf("initialInterval=1,") >= 0);
  }

  @Test
  public void testSetMultiplier() throws Exception {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setMultiplier(3.);
    assertTrue(strategy.toString().indexOf("multiplier=3.") >= 0);
    strategy.setMultiplier(.5);
    assertTrue(strategy.toString().indexOf("multiplier=1.") >= 0);
  }

  @Test
  public void testSingleBackOff() throws Exception {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setSleeper(sleeper);
    BackOffContext context = strategy.start(null);
    strategy.backOff(context);
    assertEquals(ExponentialBackOffPolicy.DEFAULT_INITIAL_INTERVAL, sleeper.getLastBackOff());
  }

  @Test
  public void testMaximumBackOff() throws Exception {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    strategy.setMaxInterval(50);
    strategy.setSleeper(sleeper);
    BackOffContext context = strategy.start(null);
    strategy.backOff(context);
    assertEquals(50, sleeper.getLastBackOff());
  }

  @Test
  public void testMultiBackOff() throws Exception {
    ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
    long seed = 40;
    double multiplier = 1.2;
    strategy.setInitialInterval(seed);
    strategy.setMultiplier(multiplier);
    strategy.setSleeper(sleeper);
    BackOffContext context = strategy.start(null);
    for (int x = 0; x < 5; x++) {
      strategy.backOff(context);
      assertEquals(seed, sleeper.getLastBackOff());
      seed *= multiplier;
    }
  }

}
