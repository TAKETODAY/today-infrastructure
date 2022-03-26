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

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.retry.RetryContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleRetryPolicyTests {

  @Test
  public void testCanRetryIfNoException() throws Exception {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    assertTrue(policy.canRetry(context));
  }

  @Test
  public void testEmptyExceptionsNeverRetry() throws Exception {

    // We can't retry any exceptions...
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3,
            Collections.<Class<? extends Throwable>, Boolean>emptyMap());
    RetryContext context = policy.open(null);

    // ...so we can't retry this one...
    policy.registerThrowable(context, new IllegalStateException());
    assertFalse(policy.canRetry(context));
  }

  @Test
  public void testWithExceptionDefaultAlwaysRetry() throws Exception {

    // We retry any exceptions except...
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3,
            Collections.<Class<? extends Throwable>, Boolean>singletonMap(IllegalStateException.class, false), true,
            true);
    RetryContext context = policy.open(null);

    // ...so we can't retry this one...
    policy.registerThrowable(context, new IllegalStateException());
    assertFalse(policy.canRetry(context));

    // ...and we can retry this one...
    policy.registerThrowable(context, new IllegalArgumentException());
    assertTrue(policy.canRetry(context));
  }

  @Test
  public void testRetryLimitInitialState() throws Exception {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    assertTrue(policy.canRetry(context));
    policy.setMaxAttempts(0);
    context = policy.open(null);
    assertFalse(policy.canRetry(context));
  }

  @Test
  public void testRetryLimitSubsequentState() throws Exception {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    policy.setMaxAttempts(2);
    assertTrue(policy.canRetry(context));
    policy.registerThrowable(context, new Exception());
    assertTrue(policy.canRetry(context));
    policy.registerThrowable(context, new Exception());
    assertFalse(policy.canRetry(context));
  }

  @Test
  public void testRetryCount() throws Exception {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    assertNotNull(context);
    policy.registerThrowable(context, null);
    assertEquals(0, context.getRetryCount());
    policy.registerThrowable(context, new RuntimeException("foo"));
    assertEquals(1, context.getRetryCount());
    assertEquals("foo", context.getLastThrowable().getMessage());
  }

  @Test
  public void testFatalOverridesRetryable() throws Exception {
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
    map.put(Exception.class, false);
    map.put(RuntimeException.class, true);
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map);
    RetryContext context = policy.open(null);
    assertNotNull(context);
    policy.registerThrowable(context, new RuntimeException("foo"));
    assertTrue(policy.canRetry(context));
  }

  @Test
  public void testRetryableWithCause() throws Exception {
    Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>();
    map.put(RuntimeException.class, true);
    SimpleRetryPolicy policy = new SimpleRetryPolicy(3, map, true);
    RetryContext context = policy.open(null);
    assertNotNull(context);
    policy.registerThrowable(context, new Exception(new RuntimeException("foo")));
    assertTrue(policy.canRetry(context));
  }

  @Test
  public void testParent() throws Exception {
    SimpleRetryPolicy policy = new SimpleRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertNotSame(child, context);
    assertSame(context, child.getParent());
  }

}
