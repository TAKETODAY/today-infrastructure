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

package infra.retry.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.classify.BinaryExceptionClassifier;
import infra.retry.RetryListener;
import infra.retry.RetryPolicy;
import infra.retry.backoff.ExponentialBackOffPolicy;
import infra.retry.backoff.ExponentialRandomBackOffPolicy;
import infra.retry.backoff.FixedBackOffPolicy;
import infra.retry.backoff.NoBackOffPolicy;
import infra.retry.backoff.UniformRandomBackOffPolicy;
import infra.retry.policy.AlwaysRetryPolicy;
import infra.retry.policy.BinaryExceptionClassifierRetryPolicy;
import infra.retry.policy.CompositeRetryPolicy;
import infra.retry.policy.MapRetryContextCache;
import infra.retry.policy.MaxAttemptsRetryPolicy;
import infra.retry.policy.TimeoutRetryPolicy;
import infra.retry.util.test.TestUtils;

import static infra.retry.util.test.TestUtils.getPropertyValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * The goal of the builder is to build proper instance. So, this test inspects instance
 * structure instead behaviour. Writing more integrative test is also encouraged.
 * Accessing of private fields is performed via {@link TestUtils#getPropertyValue} to
 * follow project's style.
 *
 * @author Aleksandr Shamukov
 * @author Kim In Hoi
 * @author Gary Russell
 */
public class RetryTemplateBuilderTests {

  /* ---------------- Mixed tests -------------- */

  @Test
  public void testDefaultBehavior() {
    RetryTemplate template = RetryTemplate.builder().build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);
    assertDefaultClassifier(policyTuple);
    Assertions.assertThat(policyTuple.baseRetryPolicy).isInstanceOf(MaxAttemptsRetryPolicy.class);
    assertDefaultClassifier(policyTuple);

    Assertions.assertThat(TestUtils.getPropertyValue(template, "throwLastExceptionOnExhausted", Boolean.class)).isFalse();
    Assertions.assertThat(TestUtils.getPropertyValue(template, "retryContextCache")).isInstanceOf(MapRetryContextCache.class);
    Assertions.assertThat(TestUtils.getPropertyValue(template, "listeners", RetryListener[].class).length).isEqualTo(0);

    Assertions.assertThat(TestUtils.getPropertyValue(template, "backOffPolicy")).isInstanceOf(NoBackOffPolicy.class);
  }

  @Test
  public void testBasicCustomization() {
    RetryListener listener1 = mock(RetryListener.class);
    RetryListener listener2 = mock(RetryListener.class);

    RetryTemplate template = RetryTemplate.builder()
            .maxAttempts(10)
            .exponentialBackoff(99, 1.5, 1717)
            .retryOn(IOException.class)
            .retryOn(Collections.singletonList(IllegalArgumentException.class))
            .traversingCauses()
            .withListener(listener1)
            .withListeners(Collections.singletonList(listener2))
            .build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);

    BinaryExceptionClassifier classifier = policyTuple.exceptionClassifierRetryPolicy.getExceptionClassifier();
    assertThat(classifier.classify(new FileNotFoundException())).isTrue();
    assertThat(classifier.classify(new IllegalArgumentException())).isTrue();
    assertThat(classifier.classify(new RuntimeException())).isFalse();
    assertThat(classifier.classify(new OutOfMemoryError())).isFalse();

    assertThat(policyTuple.baseRetryPolicy instanceof MaxAttemptsRetryPolicy).isTrue();
    assertThat(((MaxAttemptsRetryPolicy) policyTuple.baseRetryPolicy).getMaxAttempts()).isEqualTo(10);

    List<RetryListener> listeners = Arrays.asList(TestUtils.getPropertyValue(template, "listeners", RetryListener[].class));
    assertThat(listeners).hasSize(2);
    assertThat(listeners.contains(listener1)).isTrue();
    assertThat(listeners.contains(listener2)).isTrue();

    Assertions.assertThat(TestUtils.getPropertyValue(template, "backOffPolicy")).isInstanceOf(ExponentialBackOffPolicy.class);
  }

  /* ---------------- Retry policy -------------- */

  @Test
  public void testFailOnRetryPoliciesConflict() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RetryTemplate.builder().maxAttempts(3).withTimeout(1000).build());
  }

  @Test
  public void testTimeoutMillis() {
    RetryTemplate template = RetryTemplate.builder().withTimeout(10000).build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);
    assertDefaultClassifier(policyTuple);

    Assertions.assertThat(policyTuple.baseRetryPolicy).isInstanceOf(TimeoutRetryPolicy.class);
    assertThat(((TimeoutRetryPolicy) policyTuple.baseRetryPolicy).getTimeout()).isEqualTo(10000);
  }

  @Test
  public void testTimeoutDuration() {
    RetryTemplate template = RetryTemplate.builder().withTimeout(Duration.ofSeconds(3)).build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);
    assertDefaultClassifier(policyTuple);

    Assertions.assertThat(policyTuple.baseRetryPolicy).isInstanceOf(TimeoutRetryPolicy.class);
    assertThat(((TimeoutRetryPolicy) policyTuple.baseRetryPolicy).getTimeout()).isEqualTo(3000);
  }

  @Test
  public void testInfiniteRetry() {
    RetryTemplate template = RetryTemplate.builder().infiniteRetry().build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);
    assertDefaultClassifier(policyTuple);

    Assertions.assertThat(policyTuple.baseRetryPolicy).isInstanceOf(AlwaysRetryPolicy.class);
  }

  @Test
  public void testCustomPolicy() {
    RetryPolicy customPolicy = mock(RetryPolicy.class);

    RetryTemplate template = RetryTemplate.builder().customPolicy(customPolicy).build();

    PolicyTuple policyTuple = PolicyTuple.extractWithAsserts(template);

    assertDefaultClassifier(policyTuple);
    assertThat(policyTuple.baseRetryPolicy).isEqualTo(customPolicy);
  }

  private void assertDefaultClassifier(PolicyTuple policyTuple) {
    BinaryExceptionClassifier classifier = policyTuple.exceptionClassifierRetryPolicy.getExceptionClassifier();
    assertThat(classifier.classify(new Exception())).isTrue();
    assertThat(classifier.classify(new Exception(new Error()))).isTrue();
    assertThat(classifier.classify(new Error())).isFalse();
    assertThat(classifier.classify(new Error(new Exception()))).isFalse();
  }

  /* ---------------- Exception classification -------------- */

  @Test
  public void testFailOnEmptyExceptionClassifierRules() {
    assertThatIllegalArgumentException().isThrownBy(() -> RetryTemplate.builder().traversingCauses().build());
  }

  @Test
  public void testFailOnNotationMix() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RetryTemplate.builder().retryOn(IOException.class).notRetryOn(OutOfMemoryError.class));
  }

  @Test
  public void testFailOnNotationsMix() {
    assertThatIllegalArgumentException().isThrownBy(() -> RetryTemplate.builder()
            .retryOn(Collections.singletonList(IOException.class))
            .notRetryOn(Collections.singletonList(OutOfMemoryError.class)));
  }

  /* ---------------- BackOff -------------- */

  @Test
  public void testFailOnBackOffPolicyNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> RetryTemplate.builder().customBackoff(null).build());
  }

  @Test
  public void testFailOnBackOffPolicyConflict() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RetryTemplate.builder().noBackoff().fixedBackoff(1000).build());
  }

  @Test
  public void testFixedBackoff() {
    RetryTemplate template = RetryTemplate.builder().fixedBackoff(200).build();
    FixedBackOffPolicy policy = TestUtils.getPropertyValue(template, "backOffPolicy", FixedBackOffPolicy.class);

    assertThat(policy.getBackOffPeriod()).isEqualTo(200);
  }

  @Test
  public void testFixedBackoffDuration() {
    RetryTemplate template = RetryTemplate.builder().fixedBackoff(Duration.ofSeconds(1)).build();
    FixedBackOffPolicy policy = TestUtils.getPropertyValue(template, "backOffPolicy", FixedBackOffPolicy.class);

    assertThat(policy.getBackOffPeriod()).isEqualTo(1000);
  }

  @Test
  public void testUniformRandomBackOff() {
    RetryTemplate template = RetryTemplate.builder().uniformRandomBackoff(10, 100).build();
    Assertions.assertThat(TestUtils.getPropertyValue(template, "backOffPolicy")).isInstanceOf(UniformRandomBackOffPolicy.class);
  }

  @Test
  public void testUniformRandomBackOffDuration() {
    RetryTemplate template = RetryTemplate.builder()
            .uniformRandomBackoff(Duration.ofSeconds(1), Duration.ofSeconds(2))
            .build();

    UniformRandomBackOffPolicy policy = TestUtils.getPropertyValue(template, "backOffPolicy",
            UniformRandomBackOffPolicy.class);

    assertThat(policy.getMinBackOffPeriod()).isEqualTo(1000);
    assertThat(policy.getMaxBackOffPeriod()).isEqualTo(2000);
  }

  @Test
  public void testNoBackOff() {
    RetryTemplate template = RetryTemplate.builder().noBackoff().build();
    Assertions.assertThat(TestUtils.getPropertyValue(template, "backOffPolicy")).isInstanceOf(NoBackOffPolicy.class);
  }

  @Test
  public void testExponentialBackoff() {
    RetryTemplate template = RetryTemplate.builder().exponentialBackoff(10, 2, 500).build();
    ExponentialBackOffPolicy policy = TestUtils.getPropertyValue(template, "backOffPolicy", ExponentialBackOffPolicy.class);

    assertThat(policy.getInitialInterval()).isEqualTo(10);
    assertThat(policy.getMultiplier()).isEqualTo(2);
    assertThat(policy.getMaxInterval()).isEqualTo(500);
  }

  @Test
  public void testExponentialBackoffDuration() {
    RetryTemplate template = RetryTemplate.builder()
            .exponentialBackoff(Duration.ofSeconds(2), 2, Duration.ofSeconds(3))
            .build();

    ExponentialBackOffPolicy policy = TestUtils.getPropertyValue(template, "backOffPolicy", ExponentialBackOffPolicy.class);

    assertThat(policy.getInitialInterval()).isEqualTo(2000);
    assertThat(policy.getMultiplier()).isEqualTo(2);
    assertThat(policy.getMaxInterval()).isEqualTo(3000);
    assertThat(policy.getMaxInterval()).isEqualTo(3000);
  }

  @Test
  public void testExpBackOffWithRandom() {
    RetryTemplate template = RetryTemplate.builder().exponentialBackoff(10, 2, 500, true).build();
    Assertions.assertThat(TestUtils.getPropertyValue(template, "backOffPolicy")).isInstanceOf(ExponentialRandomBackOffPolicy.class);
  }

  @Test
  public void testExponentialRandomBackoffDuration() {
    RetryTemplate template = RetryTemplate.builder()
            .exponentialBackoff(Duration.ofSeconds(2), 2, Duration.ofSeconds(3), true)
            .build();

    ExponentialRandomBackOffPolicy policy = TestUtils.getPropertyValue(template, "backOffPolicy",
            ExponentialRandomBackOffPolicy.class);

    assertThat(policy.getInitialInterval()).isEqualTo(2000);
    assertThat(policy.getMultiplier()).isEqualTo(2);
    assertThat(policy.getMaxInterval()).isEqualTo(3000);
  }

  @Test
  public void testValidateInitAndMax() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RetryTemplate.builder().exponentialBackoff(100, 2, 100).build());
  }

  @Test
  public void testValidateMeaninglessMultiplier() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RetryTemplate.builder().exponentialBackoff(100, 1, 200).build());
  }

  @Test
  public void testValidateZeroInitInterval() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RetryTemplate.builder().exponentialBackoff(0, 2, 200).build());
  }

  /* ---------------- Utils -------------- */

  private static class PolicyTuple {

    RetryPolicy baseRetryPolicy;

    BinaryExceptionClassifierRetryPolicy exceptionClassifierRetryPolicy;

    static PolicyTuple extractWithAsserts(RetryTemplate template) {
      CompositeRetryPolicy compositeRetryPolicy = TestUtils.getPropertyValue(template, "retryPolicy",
              CompositeRetryPolicy.class);
      PolicyTuple res = new PolicyTuple();

      assertThat(TestUtils.getPropertyValue(compositeRetryPolicy, "optimistic", Boolean.class)).isFalse();

      for (final RetryPolicy policy : TestUtils.getPropertyValue(compositeRetryPolicy, "policies", RetryPolicy[].class)) {
        if (policy instanceof BinaryExceptionClassifierRetryPolicy) {
          res.exceptionClassifierRetryPolicy = (BinaryExceptionClassifierRetryPolicy) policy;
        }
        else {
          res.baseRetryPolicy = policy;
        }
      }
      assertThat(res.exceptionClassifierRetryPolicy).isNotNull();
      assertThat(res.baseRetryPolicy).isNotNull();
      return res;
    }

  }

}
