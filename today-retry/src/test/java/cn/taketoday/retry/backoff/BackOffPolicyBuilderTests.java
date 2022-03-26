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

import cn.taketoday.beans.DirectFieldAccessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Tomaz Fernandes
 * @since 4.0
 */
public class BackOffPolicyBuilderTests {

  @Test
  public void shouldCreateDefaultBackOffPolicy() {
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newDefaultPolicy();
    assertTrue(FixedBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass()));
    FixedBackOffPolicy policy = (FixedBackOffPolicy) backOffPolicy;
    assertEquals(1000, policy.getBackOffPeriod());
  }

  @Test
  public void shouldCreateDefaultBackOffPolicyViaNewBuilder() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().sleeper(mockSleeper).build();
    assertTrue(FixedBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass()));
    FixedBackOffPolicy policy = (FixedBackOffPolicy) backOffPolicy;
    assertEquals(1000, policy.getBackOffPeriod());
    assertEquals(mockSleeper, new DirectFieldAccessor(policy).getPropertyValue("sleeper"));
  }

  @Test
  public void shouldCreateFixedBackOffPolicy() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(3500).sleeper(mockSleeper).build();
    assertTrue(FixedBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass()));
    FixedBackOffPolicy policy = (FixedBackOffPolicy) backOffPolicy;
    assertEquals(3500, policy.getBackOffPeriod());
    assertEquals(mockSleeper, new DirectFieldAccessor(policy).getPropertyValue("sleeper"));
  }

  @Test
  public void shouldCreateUniformRandomBackOffPolicy() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(1).maxDelay(5000).sleeper(mockSleeper)
            .build();
    assertTrue(UniformRandomBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass()));
    UniformRandomBackOffPolicy policy = (UniformRandomBackOffPolicy) backOffPolicy;
    assertEquals(1, policy.getMinBackOffPeriod());
    assertEquals(5000, policy.getMaxBackOffPeriod());
    assertEquals(mockSleeper, new DirectFieldAccessor(policy).getPropertyValue("sleeper"));
  }

  @Test
  public void shouldCreateExponentialBackOff() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(100).maxDelay(1000).multiplier(2)
            .random(false).sleeper(mockSleeper).build();
    assertTrue(ExponentialBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass()));
    ExponentialBackOffPolicy policy = (ExponentialBackOffPolicy) backOffPolicy;
    assertEquals(100, policy.getInitialInterval());
    assertEquals(1000, policy.getMaxInterval());
    assertEquals(2, policy.getMultiplier(), 0);
    assertEquals(mockSleeper, new DirectFieldAccessor(policy).getPropertyValue("sleeper"));
  }

  @Test
  public void shouldCreateExponentialRandomBackOff() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(10000).maxDelay(100000).multiplier(10)
            .random(true).sleeper(mockSleeper).build();
    assertTrue(ExponentialRandomBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass()));
    ExponentialRandomBackOffPolicy policy = (ExponentialRandomBackOffPolicy) backOffPolicy;
    assertEquals(10000, policy.getInitialInterval());
    assertEquals(100000, policy.getMaxInterval());
    assertEquals(10, policy.getMultiplier(), 0);
    assertEquals(mockSleeper, new DirectFieldAccessor(policy).getPropertyValue("sleeper"));
  }

}
