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

import cn.taketoday.beans.DirectFieldAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Tomaz Fernandes
 * @author Gary Russell
 * @since 1.3.3
 */
public class BackOffPolicyBuilderTests {

  @Test
  public void shouldCreateDefaultBackOffPolicy() {
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newDefaultPolicy();
    assertThat(FixedBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass())).isTrue();
    FixedBackOffPolicy policy = (FixedBackOffPolicy) backOffPolicy;
    assertThat(policy.getBackOffPeriod()).isEqualTo(1000);
  }

  @Test
  public void shouldCreateDefaultBackOffPolicyViaNewBuilder() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().sleeper(mockSleeper).build();
    assertThat(FixedBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass())).isTrue();
    FixedBackOffPolicy policy = (FixedBackOffPolicy) backOffPolicy;
    assertThat(policy.getBackOffPeriod()).isEqualTo(1000);
    assertThat(new DirectFieldAccessor(policy).getPropertyValue("sleeper")).isEqualTo(mockSleeper);
  }

  @Test
  public void shouldCreateFixedBackOffPolicy() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(3500).sleeper(mockSleeper).build();
    assertThat(FixedBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass())).isTrue();
    FixedBackOffPolicy policy = (FixedBackOffPolicy) backOffPolicy;
    assertThat(policy.getBackOffPeriod()).isEqualTo(3500);
    assertThat(new DirectFieldAccessor(policy).getPropertyValue("sleeper")).isEqualTo(mockSleeper);
  }

  @Test
  public void shouldCreateUniformRandomBackOffPolicy() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(1).maxDelay(5000).sleeper(mockSleeper)
            .build();
    assertThat(UniformRandomBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass())).isTrue();
    UniformRandomBackOffPolicy policy = (UniformRandomBackOffPolicy) backOffPolicy;
    assertThat(policy.getMinBackOffPeriod()).isEqualTo(1);
    assertThat(policy.getMaxBackOffPeriod()).isEqualTo(5000);
    assertThat(new DirectFieldAccessor(policy).getPropertyValue("sleeper")).isEqualTo(mockSleeper);
  }

  @Test
  public void shouldCreateExponentialBackOff() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(100).maxDelay(1000).multiplier(2)
            .random(false).sleeper(mockSleeper).build();
    assertThat(ExponentialBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass())).isTrue();
    ExponentialBackOffPolicy policy = (ExponentialBackOffPolicy) backOffPolicy;
    assertThat(policy.getInitialInterval()).isEqualTo(100);
    assertThat(policy.getMaxInterval()).isEqualTo(1000);
    assertThat(policy.getMultiplier()).isEqualTo(2);
    assertThat(new DirectFieldAccessor(policy).getPropertyValue("sleeper")).isEqualTo(mockSleeper);
  }

  @Test
  public void shouldCreateExponentialRandomBackOff() {
    Sleeper mockSleeper = mock(Sleeper.class);
    BackOffPolicy backOffPolicy = BackOffPolicyBuilder.newBuilder().delay(10000).maxDelay(100000).multiplier(10)
            .random(true).sleeper(mockSleeper).build();
    assertThat(ExponentialRandomBackOffPolicy.class.isAssignableFrom(backOffPolicy.getClass())).isTrue();
    ExponentialRandomBackOffPolicy policy = (ExponentialRandomBackOffPolicy) backOffPolicy;
    assertThat(policy.getInitialInterval()).isEqualTo(10000);
    assertThat(policy.getMaxInterval()).isEqualTo(100000);
    assertThat(policy.getMultiplier()).isEqualTo(10);
    assertThat(new DirectFieldAccessor(policy).getPropertyValue("sleeper")).isEqualTo(mockSleeper);
  }

}
