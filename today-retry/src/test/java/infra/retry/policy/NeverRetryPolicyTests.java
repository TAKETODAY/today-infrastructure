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

package infra.retry.policy;

import org.junit.jupiter.api.Test;

import infra.retry.RetryContext;

import static org.assertj.core.api.Assertions.assertThat;

public class NeverRetryPolicyTests {

  @Test
  public void testSimpleOperations() {
    NeverRetryPolicy policy = new NeverRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    // We can retry until the first exception is registered...
    assertThat(policy.canRetry(context)).isTrue();
    assertThat(policy.canRetry(context)).isTrue();
    policy.registerThrowable(context, null);
    assertThat(policy.canRetry(context)).isFalse();
    policy.close(context);
    assertThat(policy.canRetry(context)).isFalse();
  }

  @Test
  public void testRetryCount() {
    NeverRetryPolicy policy = new NeverRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    policy.registerThrowable(context, null);
    assertThat(context.getRetryCount()).isEqualTo(0);
    policy.registerThrowable(context, new RuntimeException("foo"));
    assertThat(context.getRetryCount()).isEqualTo(1);
    assertThat(context.getLastThrowable().getMessage()).isEqualTo("foo");
  }

  @Test
  public void testParent() {
    NeverRetryPolicy policy = new NeverRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertThat(context).isNotSameAs(child);
    assertThat(child.getParent()).isSameAs(context);
  }

}
