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

package cn.taketoday.retry.policy;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.retry.RetryContext;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleRetryPolicyTests {

  @Test
  public void testCanRetryIfNoException() {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  public void testEmptyExceptionsNeverRetry() {

    // We can't retry any exceptions...
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3,
            Collections.<Class<? extends Throwable>, Boolean>emptyMap());
    RetryContext context = policy.open(null);

    // ...so we can't retry this one...
    policy.registerThrowable(context, new IllegalStateException());
    assertThat(policy.canRetry(context)).isFalse();
  }

  @Test
  public void testWithExceptionDefaultAlwaysRetry() {

    // We retry any exceptions except...
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3,
            Collections.<Class<? extends Throwable>, Boolean>singletonMap(IllegalStateException.class, false), true,
            true);
    RetryContext context = policy.open(null);

    // ...so we can't retry this one...
    policy.registerThrowable(context, new IllegalStateException());
    assertThat(policy.canRetry(context)).isFalse();

    // ...and we can retry this one...
    policy.registerThrowable(context, new IllegalArgumentException());
    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  public void testRetryLimitInitialState() {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(policy.canRetry(context)).isTrue();
    policy.setMaxAttempts(0);
    context = policy.open(null);
    assertThat(policy.canRetry(context)).isFalse();
  }

  @Test
  public void testRetryLimitSubsequentState() {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    policy.setMaxAttempts(2);
    assertThat(policy.canRetry(context)).isTrue();
    policy.registerThrowable(context, new Exception());
    assertThat(policy.canRetry(context)).isTrue();
    policy.registerThrowable(context, new Exception());
    assertThat(policy.canRetry(context)).isFalse();
  }

  @Test
  public void testRetryCount() {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    policy.registerThrowable(context, null);
    assertThat(context.getRetryCount()).isEqualTo(0);
    policy.registerThrowable(context, new RuntimeException("foo"));
    assertThat(context.getRetryCount()).isEqualTo(1);
    assertThat(context.getLastThrowable().getMessage()).isEqualTo("foo");
  }

  @Test
  public void testFatalOverridesRetryable() {
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
    map.put(Exception.class, false);
    map.put(RuntimeException.class, true);
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map);
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    policy.registerThrowable(context, new RuntimeException("foo"));
    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  public void testRetryableWithCause() {
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<>();
    map.put(RuntimeException.class, true);
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map, true);
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    policy.registerThrowable(context, new Exception(new RuntimeException("foo")));
    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  public void testParent() {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertThat(context).isNotSameAs(child);
    assertThat(child.getParent()).isSameAs(context);
  }

}
