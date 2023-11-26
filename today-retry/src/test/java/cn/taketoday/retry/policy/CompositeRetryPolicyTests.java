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

package cn.taketoday.retry.policy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CompositeRetryPolicyTests {

  @Test
  public void testEmptyPolicies() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  public void testTrivialPolicies() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() });
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertThat(policy.canRetry(context)).isTrue();
  }

  @SuppressWarnings("serial")
  @Test
  public void testNonTrivialPolicies() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() {
      public boolean canRetry(RetryContext context) {
        return false;
      }
    } });
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertThat(policy.canRetry(context)).isFalse();
  }

  @SuppressWarnings("serial")
  @Test
  public void testNonTrivialPoliciesWithThrowable() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() {
      boolean errorRegistered = false;

      public boolean canRetry(RetryContext context) {
        return !errorRegistered;
      }

      public void registerThrowable(RetryContext context, Throwable throwable) {
        errorRegistered = true;
      }
    } });
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertThat(policy.canRetry(context)).isTrue();
    policy.registerThrowable(context, null);
    assertThat(policy.canRetry(context)).describedAs("Should be still able to retry").isFalse();
  }

  @SuppressWarnings("serial")
  @Test
  public void testNonTrivialPoliciesClose() {
    final List<String> list = new ArrayList<>();
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport() {
      public void close(RetryContext context) {
        list.add("1");
      }
    }, new MockRetryPolicySupport() {
      public void close(RetryContext context) {
        list.add("2");
      }
    } });
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    policy.close(context);
    assertThat(list).hasSize(2);
  }

  @SuppressWarnings("serial")
  @Test
  public void testExceptionOnPoliciesClose() {
    final List<String> list = new ArrayList<>();
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport() {
      public void close(RetryContext context) {
        list.add("1");
        throw new RuntimeException("Pah!");
      }
    }, new MockRetryPolicySupport() {
      public void close(RetryContext context) {
        list.add("2");
      }
    } });
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> policy.close(context)).withMessage("Pah!");
    assertThat(list).hasSize(2);
  }

  @Test
  public void testRetryCount() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() });
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
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertThat(context).isNotSameAs(child);
    assertThat(child.getParent()).isSameAs(context);
  }

  @SuppressWarnings("serial")
  @Test
  public void testOptimistic() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setOptimistic(true);
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport() {
      public boolean canRetry(RetryContext context) {
        return false;
      }
    }, new MockRetryPolicySupport() });
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  public void testMaximumAttemptsForNonSuitablePolicies() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setOptimistic(true);
    policy.setPolicies(new RetryPolicy[] { new NeverRetryPolicy(), new NeverRetryPolicy() });

    assertThat(policy.getMaxAttempts()).isEqualTo(RetryPolicy.NO_MAXIMUM_ATTEMPTS_SET);
  }

  @Test
  public void testMaximumAttemptsForSuitablePolicies() {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setOptimistic(true);
    policy.setPolicies(
            new RetryPolicy[] { new SimpleRetryPolicy(6), new SimpleRetryPolicy(3), new SimpleRetryPolicy(4) });

    assertThat(policy.getMaxAttempts()).isEqualTo(3);
  }

}
