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

import cn.taketoday.retry.RetryContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeoutRetryPolicyTests {

  @Test
  public void testTimeoutPreventsRetry() throws Exception {
    TimeoutRetryPolicy policy = new TimeoutRetryPolicy();
    policy.setTimeout(100);
    RetryContext context = policy.open(null);
    policy.registerThrowable(context, new Exception());
    assertTrue(policy.canRetry(context));
    Thread.sleep(200);
    assertFalse(policy.canRetry(context));
    policy.close(context);
  }

  @Test
  public void testRetryCount() throws Exception {
    TimeoutRetryPolicy policy = new TimeoutRetryPolicy();
    RetryContext context = policy.open(null);
    assertNotNull(context);
    policy.registerThrowable(context, null);
    assertEquals(0, context.getRetryCount());
    policy.registerThrowable(context, new RuntimeException("foo"));
    assertEquals(1, context.getRetryCount());
    assertEquals("foo", context.getLastThrowable().getMessage());
  }

  @Test
  public void testParent() throws Exception {
    TimeoutRetryPolicy policy = new TimeoutRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertNotSame(child, context);
    assertSame(context, child.getParent());
  }

}
