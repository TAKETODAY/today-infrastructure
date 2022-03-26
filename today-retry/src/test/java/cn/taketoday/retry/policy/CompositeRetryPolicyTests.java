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

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CompositeRetryPolicyTests {

  @Test
  public void testEmptyPolicies() throws Exception {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    RetryContext context = policy.open(null);
    assertNotNull(context);
    assertTrue(policy.canRetry(context));
  }

  @Test
  public void testTrivialPolicies() throws Exception {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() });
    RetryContext context = policy.open(null);
    assertNotNull(context);
    assertTrue(policy.canRetry(context));
  }

  @SuppressWarnings("serial")
  @Test
  public void testNonTrivialPolicies() throws Exception {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() {
      public boolean canRetry(RetryContext context) {
        return false;
      }
    } });
    RetryContext context = policy.open(null);
    assertNotNull(context);
    assertFalse(policy.canRetry(context));
  }

  @SuppressWarnings("serial")
  @Test
  public void testNonTrivialPoliciesWithThrowable() throws Exception {
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
    assertNotNull(context);
    assertTrue(policy.canRetry(context));
    policy.registerThrowable(context, null);
    assertFalse("Should be still able to retry", policy.canRetry(context));
  }

  @SuppressWarnings("serial")
  @Test
  public void testNonTrivialPoliciesClose() throws Exception {
    final List<String> list = new ArrayList<String>();
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
    assertNotNull(context);
    policy.close(context);
    assertEquals(2, list.size());
  }

  @SuppressWarnings("serial")
  @Test
  public void testExceptionOnPoliciesClose() throws Exception {
    final List<String> list = new ArrayList<String>();
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
    assertNotNull(context);
    try {
      policy.close(context);
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertEquals("Pah!", e.getMessage());
    }
    assertEquals(2, list.size());
  }

  @Test
  public void testRetryCount() throws Exception {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport(), new MockRetryPolicySupport() });
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
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertNotSame(child, context);
    assertSame(context, child.getParent());
  }

  @SuppressWarnings("serial")
  @Test
  public void testOptimistic() throws Exception {
    CompositeRetryPolicy policy = new CompositeRetryPolicy();
    policy.setOptimistic(true);
    policy.setPolicies(new RetryPolicy[] { new MockRetryPolicySupport() {
      public boolean canRetry(RetryContext context) {
        return false;
      }
    }, new MockRetryPolicySupport() });
    RetryContext context = policy.open(null);
    assertNotNull(context);
    assertTrue(policy.canRetry(context));
  }

}
