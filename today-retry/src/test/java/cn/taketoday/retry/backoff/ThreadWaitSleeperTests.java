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

import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
public class ThreadWaitSleeperTests {

  @Test
  public void testSingleBackOff() throws Exception {
    long backOffPeriod = 50;
    ThreadWaitSleeper strategy = new ThreadWaitSleeper();
    long before = System.currentTimeMillis();
    strategy.sleep(backOffPeriod);
    long after = System.currentTimeMillis();
    assertEqualsApprox(backOffPeriod, after - before, 25);
  }

  private void assertEqualsApprox(long desired, long actual, long variance) {
    long lower = desired - variance;
    long upper = desired + 2 * variance;
    assertTrue("Expected value to be between '" + lower + "' and '" + upper + "' but was '" + actual + "'",
            lower <= actual);
  }

}
