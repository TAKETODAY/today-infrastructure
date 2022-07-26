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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.1
 */
public class FixedBackOffPolicyTests {

  private final DummySleeper sleeper = new DummySleeper();

  @Test
  public void testSetBackoffPeriodNegative() {
    FixedBackOffPolicy strategy = new FixedBackOffPolicy();
    strategy.setBackOffPeriod(-1000L);
    strategy.setSleeper(sleeper);
    strategy.backOff(null);
    // We should see a zero backoff if we try to set it negative
    assertThat(sleeper.getBackOffs().length).isEqualTo(1);
    assertThat(sleeper.getLastBackOff()).isEqualTo(1);
  }

  @Test
  public void testSingleBackOff() {
    int backOffPeriod = 50;
    FixedBackOffPolicy strategy = new FixedBackOffPolicy();
    strategy.setBackOffPeriod(backOffPeriod);
    strategy.setSleeper(sleeper);
    strategy.backOff(null);
    assertThat(sleeper.getBackOffs().length).isEqualTo(1);
    assertThat(sleeper.getLastBackOff()).isEqualTo(backOffPeriod);
  }

  @Test
  public void testManyBackOffCalls() {
    int backOffPeriod = 50;
    FixedBackOffPolicy strategy = new FixedBackOffPolicy();
    strategy.setBackOffPeriod(backOffPeriod);
    strategy.setSleeper(sleeper);
    for (int x = 0; x < 10; x++) {
      strategy.backOff(null);
      assertThat(sleeper.getLastBackOff()).isEqualTo(backOffPeriod);
    }
    assertThat(sleeper.getBackOffs().length).isEqualTo(10);
  }

}
