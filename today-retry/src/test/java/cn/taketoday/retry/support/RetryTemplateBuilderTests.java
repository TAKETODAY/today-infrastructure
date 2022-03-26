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

package cn.taketoday.retry.support;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.classify.BinaryExceptionClassifier;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.backoff.ExponentialBackOffPolicy;
import cn.taketoday.retry.backoff.ExponentialRandomBackOffPolicy;
import cn.taketoday.retry.backoff.NoBackOffPolicy;
import cn.taketoday.retry.backoff.UniformRandomBackOffPolicy;
import cn.taketoday.retry.policy.AlwaysRetryPolicy;
import cn.taketoday.retry.policy.BinaryExceptionClassifierRetryPolicy;
import cn.taketoday.retry.policy.CompositeRetryPolicy;
import cn.taketoday.retry.policy.MapRetryContextCache;
import cn.taketoday.retry.policy.MaxAttemptsRetryPolicy;
import cn.taketoday.retry.policy.TimeoutRetryPolicy;
import cn.taketoday.retry.util.test.TestUtils;

import static cn.taketoday.retry.util.test.TestUtils.getPropertyValue;
import static org.mockito.Mockito.mock;

/**
 * The goal of the builder is to build proper instance. So, this test inspects instance
 * structure instead behaviour. Writing more integrative test is also encouraged.
 * Accessing of private fields is performed via {@link TestUtils#getPropertyValue} to
 * follow project's style.
 *
 * @author Aleksandr Shamukov
 * @author Kim In Hoi
 */
public class RetryTemplateBuilderTests {

  /* ---------------- Mixed tests -------------- */

  @Test
  public void testDefaultBehavior() {
    RetryTemplate template = RetryTemplate.builder().build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);
    assertDefaultClassifier(policyTuple);
    Assert.assertTrue(policyTuple.baseRetryPolicy instanceof MaxAttemptsRetryPolicy);
    assertDefaultClassifier(policyTuple);

    Assert.assertFalse(getPropertyValue(template, "throwLastExceptionOnExhausted", Boolean.class));
    Assert.assertTrue(getPropertyValue(template, "retryContextCache") instanceof MapRetryContextCache);
    Assert.assertEquals(0, getPropertyValue(template, "listeners", RetryListener[].class).length);

    Assert.assertTrue(getPropertyValue(template, "backOffPolicy") instanceof NoBackOffPolicy);
  }

  @Test
  public void testBasicCustomization() {
    RetryListener listener1 = mock(RetryListener.class);
    RetryListener listener2 = mock(RetryListener.class);

    RetryTemplate template = RetryTemplate.builder().maxAttempts(10).exponentialBackoff(99, 1.5, 1717)
            .retryOn(IOException.class)
            .retryOn(Collections.<Class<? extends Throwable>>singletonList(IllegalArgumentException.class))
            .traversingCauses().withListener(listener1).withListeners(Collections.singletonList(listener2)).build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);

    BinaryExceptionClassifier classifier = policyTuple.exceptionClassifierRetryPolicy.getExceptionClassifier();
    Assert.assertTrue(classifier.classify(new FileNotFoundException()));
    Assert.assertTrue(classifier.classify(new IllegalArgumentException()));
    Assert.assertFalse(classifier.classify(new RuntimeException()));
    Assert.assertFalse(classifier.classify(new OutOfMemoryError()));

    Assert.assertTrue(policyTuple.baseRetryPolicy instanceof MaxAttemptsRetryPolicy);
    Assert.assertEquals(10, ((MaxAttemptsRetryPolicy) policyTuple.baseRetryPolicy).getMaxAttempts());

    List<RetryListener> listeners = Arrays.asList(getPropertyValue(template, "listeners", RetryListener[].class));
    Assert.assertEquals(2, listeners.size());
    Assert.assertTrue(listeners.contains(listener1));
    Assert.assertTrue(listeners.contains(listener2));

    Assert.assertTrue(getPropertyValue(template, "backOffPolicy") instanceof ExponentialBackOffPolicy);
  }

  /* ---------------- Retry policy -------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testFailOnRetryPoliciesConflict() {
    RetryTemplate.builder().maxAttempts(3).withinMillis(1000).build();
  }

  @Test
  public void testTimeoutPolicy() {
    RetryTemplate template = RetryTemplate.builder().withinMillis(10000).build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);
    assertDefaultClassifier(policyTuple);

    Assert.assertTrue(policyTuple.baseRetryPolicy instanceof TimeoutRetryPolicy);
    Assert.assertEquals(10000, ((TimeoutRetryPolicy) policyTuple.baseRetryPolicy).getTimeout());
  }

  @Test
  public void testInfiniteRetry() {
    RetryTemplate template = RetryTemplate.builder().infiniteRetry().build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);
    assertDefaultClassifier(policyTuple);

    Assert.assertTrue(policyTuple.baseRetryPolicy instanceof AlwaysRetryPolicy);
  }

  @Test
  public void testCustomPolicy() {
    RetryPolicy customPolicy = mock(RetryPolicy.class);

    RetryTemplate template = RetryTemplate.builder().customPolicy(customPolicy).build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);

    assertDefaultClassifier(policyTuple);
    Assert.assertEquals(customPolicy, policyTuple.baseRetryPolicy);
  }

  private void assertDefaultClassifier(PolicyTuple policyTuple) {
    BinaryExceptionClassifier classifier = policyTuple.exceptionClassifierRetryPolicy.getExceptionClassifier();
    Assert.assertTrue(classifier.classify(new Exception()));
    Assert.assertTrue(classifier.classify(new Exception(new Error())));
    Assert.assertFalse(classifier.classify(new Error()));
    Assert.assertFalse(classifier.classify(new Error(new Exception())));
  }

  /* ---------------- Exception classification -------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testFailOnEmptyExceptionClassifierRules() {
    RetryTemplate.builder().traversingCauses().build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailOnNotationMix() {
    RetryTemplate.builder().retryOn(IOException.class).notRetryOn(OutOfMemoryError.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailOnNotationsMix() {
    RetryTemplate.builder().retryOn(Collections.<Class<? extends Throwable>>singletonList(IOException.class))
            .notRetryOn(Collections.<Class<? extends Throwable>>singletonList(OutOfMemoryError.class));
  }

  /* ---------------- BackOff -------------- */

  @Test(expected = IllegalArgumentException.class)
  public void testFailOnBackOffPolicyNull() {
    RetryTemplate.builder().customBackoff(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailOnBackOffPolicyConflict() {
    RetryTemplate.builder().noBackoff().fixedBackoff(1000).build();
  }

  @Test
  public void testUniformRandomBackOff() {
    RetryTemplate template = RetryTemplate.builder().uniformRandomBackoff(10, 100).build();
    Assert.assertTrue(getPropertyValue(template, "backOffPolicy") instanceof UniformRandomBackOffPolicy);
  }

  @Test
  public void testNoBackOff() {
    RetryTemplate template = RetryTemplate.builder().noBackoff().build();
    Assert.assertTrue(getPropertyValue(template, "backOffPolicy") instanceof NoBackOffPolicy);
  }

  @Test
  public void testExpBackOffWithRandom() {
    RetryTemplate template = RetryTemplate.builder().exponentialBackoff(10, 2, 500, true).build();
    Assert.assertTrue(getPropertyValue(template, "backOffPolicy") instanceof ExponentialRandomBackOffPolicy);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateInitAndMax() {
    RetryTemplate.builder().exponentialBackoff(100, 2, 100).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateMeaninglessMultipier() {
    RetryTemplate.builder().exponentialBackoff(100, 1, 200).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateZeroInitInterval() {
    RetryTemplate.builder().exponentialBackoff(0, 2, 200).build();
  }

  /* ---------------- Utils -------------- */

  private static class PolicyTuple {

    RetryPolicy baseRetryPolicy;

    BinaryExceptionClassifierRetryPolicy exceptionClassifierRetryPolicy;

    static PolicyTuple extractWithAsserts(RetryTemplate template) {
      CompositeRetryPolicy compositeRetryPolicy = getPropertyValue(template, "retryPolicy",
              CompositeRetryPolicy.class);
      PolicyTuple res = new PolicyTuple();

      Assert.assertFalse(getPropertyValue(compositeRetryPolicy, "optimistic", Boolean.class));

      for (final RetryPolicy policy : getPropertyValue(compositeRetryPolicy, "policies", RetryPolicy[].class)) {
        if (policy instanceof BinaryExceptionClassifierRetryPolicy) {
          res.exceptionClassifierRetryPolicy = (BinaryExceptionClassifierRetryPolicy) policy;
        }
        else {
          res.baseRetryPolicy = policy;
        }
      }
      Assert.assertNotNull(res.exceptionClassifierRetryPolicy);
      Assert.assertNotNull(res.baseRetryPolicy);
      return res;
    }

  }

}
