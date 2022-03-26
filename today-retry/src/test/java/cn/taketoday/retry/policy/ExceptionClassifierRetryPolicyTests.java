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

import cn.taketoday.classify.Classifier;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ExceptionClassifierRetryPolicyTests {

  private ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();

  @Test
  public void testDefaultPolicies() throws Exception {
    RetryContext context = policy.open(null);
    assertNotNull(context);
  }

  @Test
  public void testTrivialPolicies() throws Exception {
    policy.setPolicyMap(Collections.<Class<? extends Throwable>, RetryPolicy>singletonMap(Exception.class,
            new MockRetryPolicySupport()));
    RetryContext context = policy.open(null);
    assertNotNull(context);
    assertTrue(policy.canRetry(context));
  }

  @Test
  public void testNullPolicies() throws Exception {
    policy.setPolicyMap(new HashMap<Class<? extends Throwable>, RetryPolicy>());
    RetryContext context = policy.open(null);
    assertNotNull(context);
  }

  @Test
  public void testNullContext() throws Exception {
    policy.setPolicyMap(Collections.<Class<? extends Throwable>, RetryPolicy>singletonMap(Exception.class,
            new NeverRetryPolicy()));

    RetryContext context = policy.open(null);
    assertNotNull(context);

    assertTrue(policy.canRetry(context));
  }

  @SuppressWarnings("serial")
  @Test
  public void testClassifierOperates() throws Exception {

    RetryContext context = policy.open(null);
    assertNotNull(context);

    assertTrue(policy.canRetry(context));
    policy.registerThrowable(context, new IllegalArgumentException());
    assertFalse(policy.canRetry(context)); // NeverRetryPolicy is the
    // default

    policy.setExceptionClassifier(new Classifier<Throwable, RetryPolicy>() {
      public RetryPolicy classify(Throwable throwable) {
        if (throwable != null) {
          return new AlwaysRetryPolicy();
        }
        return new NeverRetryPolicy();
      }
    });

    // The context saves the classifier, so changing it now has no effect
    assertFalse(policy.canRetry(context));
    policy.registerThrowable(context, new IllegalArgumentException());
    assertFalse(policy.canRetry(context));

    // But now the classifier will be active in the new context...
    context = policy.open(null);
    assertTrue(policy.canRetry(context));
    policy.registerThrowable(context, new IllegalArgumentException());
    assertTrue(policy.canRetry(context));

  }

  int count = 0;

  @SuppressWarnings("serial")
  @Test
  public void testClose() throws Exception {
    policy.setExceptionClassifier(new Classifier<Throwable, RetryPolicy>() {
      public RetryPolicy classify(Throwable throwable) {
        return new MockRetryPolicySupport() {
          public void close(RetryContext context) {
            count++;
          }
        };
      }
    });
    RetryContext context = policy.open(null);

    // The mapped (child) policy hasn't been used yet, so if we close now
    // we don't incur the possible expense of creating the child context.
    policy.close(context);
    assertEquals(0, count); // not classified yet
    // This forces a child context to be created and the child policy is
    // then closed
    policy.registerThrowable(context, new IllegalStateException());
    policy.close(context);
    assertEquals(1, count); // now classified
  }

  @Test
  public void testRetryCount() throws Exception {
    ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();
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
    ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertNotSame(child, context);
    assertSame(context, child.getParent());
  }

}
