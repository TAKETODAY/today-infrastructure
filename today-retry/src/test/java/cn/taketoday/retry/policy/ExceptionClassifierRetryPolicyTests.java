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

import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExceptionClassifierRetryPolicyTests {

  private final ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();

  @Test
  public void testDefaultPolicies() {
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
  }

  @Test
  public void testTrivialPolicies() {
    policy.setPolicyMap(Collections.<Class<? extends Throwable>, RetryPolicy>singletonMap(Exception.class,
            new MockRetryPolicySupport()));
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertTrue(policy.canRetry(context));
  }

  @Test
  public void testNullPolicies() {
    policy.setPolicyMap(new HashMap<>());
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
  }

  @Test
  public void testNullContext() {
    policy.setPolicyMap(Collections.<Class<? extends Throwable>, RetryPolicy>singletonMap(Exception.class,
            new NeverRetryPolicy()));

    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();

    assertTrue(policy.canRetry(context));
  }

  @SuppressWarnings("serial")
  @Test
  public void testClassifierOperates() {

    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();

    assertTrue(policy.canRetry(context));
    policy.registerThrowable(context, new IllegalArgumentException());
    assertFalse(policy.canRetry(context)); // NeverRetryPolicy is the
    // default

    policy.setExceptionClassifier(throwable -> {
      if (throwable != null) {
        return new AlwaysRetryPolicy();
      }
      return new NeverRetryPolicy();
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
  public void testClose() {
    policy.setExceptionClassifier(throwable -> new MockRetryPolicySupport() {
      public void close(RetryContext context) {
        count++;
      }
    });
    RetryContext context = policy.open(null);

    // The mapped (child) policy hasn't been used yet, so if we close now
    // we don't incur the possible expense of creating the child context.
    policy.close(context);
    assertThat(count).isEqualTo(0); // not classified yet
    // This forces a child context to be created and the child policy is
    // then closed
    policy.registerThrowable(context, new IllegalStateException());
    policy.close(context);
    assertThat(count).isEqualTo(1); // now classified
  }

  @Test
  public void testRetryCount() {
    ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();
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
    ExceptionClassifierRetryPolicy policy = new ExceptionClassifierRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertThat(context).isNotSameAs(child);
    assertThat(child.getParent()).isSameAs(context);
  }

}
