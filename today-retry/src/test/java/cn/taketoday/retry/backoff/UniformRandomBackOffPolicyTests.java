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
 * @author Tomaz Fernandes
 * @author Gary Russell
 * @since 1.3.2
 */
public class UniformRandomBackOffPolicyTests {

  @Test
  public void testSetSleeper() {
    UniformRandomBackOffPolicy backOffPolicy = new UniformRandomBackOffPolicy();
    int minBackOff = 1000;
    int maxBackOff = 10000;
    backOffPolicy.setMinBackOffPeriod(minBackOff);
    backOffPolicy.setMaxBackOffPeriod(maxBackOff);
    UniformRandomBackOffPolicy withSleeper = backOffPolicy.withSleeper(new DummySleeper());

    assertThat(withSleeper.getMinBackOffPeriod()).isEqualTo(minBackOff);
    assertThat(withSleeper.getMaxBackOffPeriod()).isEqualTo(maxBackOff);
  }

  @Test
  public void testInterruptedStatusIsRestored() {
    UniformRandomBackOffPolicy backOffPolicy = new UniformRandomBackOffPolicy();
    int minBackOff = 1000;
    int maxBackOff = 10000;
    backOffPolicy.setMinBackOffPeriod(minBackOff);
    backOffPolicy.setMaxBackOffPeriod(maxBackOff);
    UniformRandomBackOffPolicy withSleeper = backOffPolicy.withSleeper(backOffPeriod -> {
      throw new InterruptedException("foo");
    });

    assertThatExceptionOfType(BackOffInterruptedException.class).isThrownBy(() -> withSleeper.backOff(null));
    assertThat(Thread.interrupted()).isTrue();
  }

}
