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

/**
 * @author Rob Harrop
 * @author Dave Syer
 * @since 4.0
 */
public class FixedBackOffPolicyTests {

  private DummySleeper sleeper = new DummySleeper();

  @Test
  public void testSetBackoffPeriodNegative() throws Exception {
    FixedBackOffPolicy strategy = new FixedBackOffPolicy();
    strategy.setBackOffPeriod(-1000L);
    strategy.setSleeper(sleeper);
    strategy.backOff(null);
    // We should see a zero backoff if we try to set it negative
    assertEquals(1, sleeper.getBackOffs().length);
    assertEquals(1, sleeper.getLastBackOff());
  }

  @Test
  public void testSingleBackOff() throws Exception {
    int backOffPeriod = 50;
    FixedBackOffPolicy strategy = new FixedBackOffPolicy();
    strategy.setBackOffPeriod(backOffPeriod);
    strategy.setSleeper(sleeper);
    strategy.backOff(null);
    assertEquals(1, sleeper.getBackOffs().length);
    assertEquals(backOffPeriod, sleeper.getLastBackOff());
  }

  @Test
  public void testManyBackOffCalls() throws Exception {
    int backOffPeriod = 50;
    FixedBackOffPolicy strategy = new FixedBackOffPolicy();
    strategy.setBackOffPeriod(backOffPeriod);
    strategy.setSleeper(sleeper);
    for (int x = 0; x < 10; x++) {
      strategy.backOff(null);
      assertEquals(backOffPeriod, sleeper.getLastBackOff());
    }
    assertEquals(10, sleeper.getBackOffs().length);
  }

}
