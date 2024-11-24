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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
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
    assertThat(lower).describedAs("Expected value to be between '%d' and '%d' but was '%d'", lower, upper, actual)
            .isLessThanOrEqualTo(actual);
  }

}
