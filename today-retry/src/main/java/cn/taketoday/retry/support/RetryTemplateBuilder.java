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

package cn.taketoday.retry.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.classify.BinaryExceptionClassifier;
import cn.taketoday.classify.BinaryExceptionClassifierBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.backoff.BackOffPolicy;
import cn.taketoday.retry.backoff.ExponentialBackOffPolicy;
import cn.taketoday.retry.backoff.ExponentialRandomBackOffPolicy;
import cn.taketoday.retry.backoff.FixedBackOffPolicy;
import cn.taketoday.retry.backoff.NoBackOffPolicy;
import cn.taketoday.retry.backoff.UniformRandomBackOffPolicy;
import cn.taketoday.retry.policy.AlwaysRetryPolicy;
import cn.taketoday.retry.policy.BinaryExceptionClassifierRetryPolicy;
import cn.taketoday.retry.policy.CompositeRetryPolicy;
import cn.taketoday.retry.policy.MaxAttemptsRetryPolicy;
import cn.taketoday.retry.policy.TimeoutRetryPolicy;

/**
 * Builder that provides a fluent API to configure new instances of {@link RetryTemplate}.
 *
 * <p>
 * By default, the builder configures a {@link BinaryExceptionClassifier} that acts upon
 * {@link Exception} and its subclasses without traversing causes, a
 * {@link NoBackOffPolicy} and a {@link MaxAttemptsRetryPolicy} that attempts actions
 * {@link MaxAttemptsRetryPolicy#DEFAULT_MAX_ATTEMPTS} times.
 *
 * <p>
 * The builder is not thread-safe.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * RetryTemplate.builder()
 *      .maxAttempts(10)
 *      .exponentialBackoff(100, 2, 10000)
 *      .retryOn(IOException.class)
 *      .traversingCauses()
 *      .build();
 *
 * RetryTemplate.builder()
 *      .fixedBackoff(10)
 *      .withinMillis(3000)
 *      .build();
 *
 * RetryTemplate.builder()
 *      .infiniteRetry()
 *      .retryOn(IOException.class)
 *      .uniformRandomBackoff(1000, 3000)
 *      .build();
 * }</pre>
 *
 * @author Aleksandr Shamukov
 * @author Artem Bilan
 * @author Kim In Hoi
 * @author Andreas Ahlenstorf
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RetryTemplateBuilder {

  private RetryPolicy baseRetryPolicy;

  private BackOffPolicy backOffPolicy;

  private List<RetryListener> listeners;

  private BinaryExceptionClassifierBuilder classifierBuilder;

  /* ---------------- Configure retry policy -------------- */

  /**
   * Attempt an action no more than {@code maxAttempts} times.
   *
   * @param maxAttempts how many times an action should be attempted. A value of 3 would
   * result in an initial attempt and two retries.
   * @return this
   * @throws IllegalArgumentException if {@code maxAttempts} is 0 or less, or if another
   * retry policy has already been selected.
   * @see MaxAttemptsRetryPolicy
   */
  public RetryTemplateBuilder maxAttempts(int maxAttempts) {
    Assert.isTrue(maxAttempts > 0, "Number of attempts should be positive");
    Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
    this.baseRetryPolicy = new MaxAttemptsRetryPolicy(maxAttempts);
    return this;
  }

  /**
   * Retry until {@code timeoutMillis} has passed since the initial attempt.
   *
   * @param timeoutMillis timeout in milliseconds
   * @return this
   * @throws IllegalArgumentException if timeout is {@literal <=} 0 or if another retry
   * policy has already been selected.
   * @see TimeoutRetryPolicy
   */
  public RetryTemplateBuilder withTimeout(long timeoutMillis) {
    Assert.isTrue(timeoutMillis > 0, "timeoutMillis should be greater than 0");
    Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
    this.baseRetryPolicy = new TimeoutRetryPolicy(timeoutMillis);
    return this;
  }

  /**
   * Retry until {@code timeout} has passed since the initial attempt.
   *
   * @param timeout duration for how long retries should be attempted
   * @return this
   * @throws IllegalArgumentException if timeout is {@code null} or 0, or if another
   * retry policy has already been selected.
   * @see TimeoutRetryPolicy
   */
  public RetryTemplateBuilder withTimeout(Duration timeout) {
    Assert.notNull(timeout, "timeout is required");
    return withTimeout(timeout.toMillis());
  }

  /**
   * Retry actions infinitely.
   *
   * @return this
   * @throws IllegalArgumentException if another retry policy has already been selected.
   * @see AlwaysRetryPolicy
   */
  public RetryTemplateBuilder infiniteRetry() {
    Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
    this.baseRetryPolicy = new AlwaysRetryPolicy();
    return this;
  }

  /**
   * Use the provided {@link RetryPolicy}.
   *
   * @param policy {@link RetryPolicy} to use
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, if any argument is {@code null}, or if another retry policy has already
   * been selected.
   */
  public RetryTemplateBuilder customPolicy(RetryPolicy policy) {
    Assert.notNull(policy, "Policy should not be null");
    Assert.isNull(this.baseRetryPolicy, "You have already selected another retry policy");
    this.baseRetryPolicy = policy;
    return this;
  }

  /* ---------------- Configure backoff policy -------------- */

  /**
   * Use an exponential backoff policy. The formula for the backoff period is:
   * <p>
   * {@code currentInterval = Math.min(initialInterval * Math.pow(multiplier, retryNum), maxInterval)}
   * <p>
   * For the first attempt, {@code retryNum = 0}.
   *
   * @param initialInterval initial sleep duration in milliseconds
   * @param multiplier backoff interval multiplier
   * @param maxInterval maximum backoff duration in milliseconds
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, if {@code initialInterval} is {@literal <} 1, if {@code multiplier} is
   * {@literal <=} 1, or if {@code maxInterval} {@literal <=} {@code initialInterval}.
   * @see ExponentialBackOffPolicy
   */
  public RetryTemplateBuilder exponentialBackoff(long initialInterval, double multiplier, long maxInterval) {
    return exponentialBackoff(initialInterval, multiplier, maxInterval, false);
  }

  /**
   * Use an exponential backoff policy. The formula for the backoff period is:
   * <p>
   * {@code currentInterval = Math.min(initialInterval * Math.pow(multiplier, retryNum), maxInterval)}
   * <p>
   * For the first attempt, {@code retryNum = 0}.
   *
   * @param initialInterval initial sleep duration
   * @param multiplier backoff interval multiplier
   * @param maxInterval maximum backoff duration
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, if {@code initialInterval} is {@code null} or less than 1 millisecond,
   * multiplier is {@literal <=} 1, or if {@code maxInterval} is {@code null} or
   * {@literal <=} {@code initialInterval}.
   * @see ExponentialBackOffPolicy
   */
  public RetryTemplateBuilder exponentialBackoff(Duration initialInterval, double multiplier, Duration maxInterval) {
    Assert.notNull(initialInterval, "initialInterval is required");
    Assert.notNull(maxInterval, "maxInterval is required");
    return exponentialBackoff(initialInterval.toMillis(), multiplier, maxInterval.toMillis(), false);
  }

  /**
   * Use an exponential backoff policy. The formula for the backoff period is (without
   * randomness):
   * <p>
   * {@code currentInterval = Math.min(initialInterval * Math.pow(multiplier, retryNum), maxInterval)}
   * <p>
   * For the first attempt, {@code retryNum = 0}.
   *
   * @param initialInterval initial sleep duration in milliseconds
   * @param multiplier backoff interval multiplier
   * @param maxInterval maximum backoff duration in milliseconds
   * @param withRandom whether to use a {@link ExponentialRandomBackOffPolicy} (if
   * {@code true}) or not
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, if {@code initialInterval} is {@literal <} 1, if {@code multiplier} is
   * {@literal <=} 1, or if {@code maxInterval} {@literal <=} {@code initialInterval}.
   * @see ExponentialBackOffPolicy
   * @see ExponentialRandomBackOffPolicy
   */
  public RetryTemplateBuilder exponentialBackoff(
          long initialInterval, double multiplier, long maxInterval, boolean withRandom) {
    Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
    Assert.isTrue(initialInterval >= 1, "Initial interval should be >= 1");
    Assert.isTrue(multiplier > 1, "Multiplier should be > 1");
    Assert.isTrue(maxInterval > initialInterval, "Max interval should be > than initial interval");
    ExponentialBackOffPolicy policy = withRandom ? new ExponentialRandomBackOffPolicy()
                                                 : new ExponentialBackOffPolicy();
    policy.setInitialInterval(initialInterval);
    policy.setMultiplier(multiplier);
    policy.setMaxInterval(maxInterval);
    this.backOffPolicy = policy;
    return this;
  }

  /**
   * Use an exponential backoff policy. The formula for the backoff period is (without
   * randomness):
   * <p>
   * {@code currentInterval = Math.min(initialInterval * Math.pow(multiplier, retryNum), maxInterval)}
   * <p>
   * For the first attempt, {@code retryNum = 0}.
   *
   * @param initialInterval initial sleep duration
   * @param multiplier backoff interval multiplier
   * @param maxInterval maximum backoff duration
   * @param withRandom whether to use a {@link ExponentialRandomBackOffPolicy} (if
   * {@code true}) or not
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, if {@code initialInterval} is {@code null} or less than 1 millisecond, if
   * {@code multiplier} is {@literal <=} 1, or if {@code maxInterval} is {@code null} or
   * {@literal <=} {@code initialInterval}.
   * @see ExponentialBackOffPolicy
   * @see ExponentialRandomBackOffPolicy
   */
  public RetryTemplateBuilder exponentialBackoff(Duration initialInterval,
          double multiplier, Duration maxInterval, boolean withRandom) {
    Assert.notNull(initialInterval, "initialInterval most not be null");
    Assert.notNull(maxInterval, "maxInterval is required");
    return this.exponentialBackoff(initialInterval.toMillis(), multiplier, maxInterval.toMillis(), withRandom);
  }

  /**
   * Perform each retry after a fixed amount of time.
   *
   * @param interval fixed interval in milliseconds
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, or if {@code interval} is {@literal <} 1.
   * @see FixedBackOffPolicy
   */
  public RetryTemplateBuilder fixedBackoff(long interval) {
    Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
    Assert.isTrue(interval >= 1, "Interval should be >= 1");
    FixedBackOffPolicy policy = new FixedBackOffPolicy();
    policy.setBackOffPeriod(interval);
    this.backOffPolicy = policy;
    return this;
  }

  /**
   * Perform each retry after fixed amount of time.
   *
   * @param interval fixed backoff duration
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, or if {@code interval} is {@code null} or less than 1 millisecond
   * @see FixedBackOffPolicy
   */
  public RetryTemplateBuilder fixedBackoff(Duration interval) {
    Assert.notNull(interval, "interval is required");
    long millis = interval.toMillis();
    Assert.isTrue(millis >= 1, "interval is less than 1 millisecond");
    return fixedBackoff(millis);
  }

  /**
   * Use {@link UniformRandomBackOffPolicy}.
   *
   * @param minInterval minimal interval in milliseconds
   * @param maxInterval maximal interval in milliseconds
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, if {@code minInterval} is {@literal <} 1, {@code maxInterval} is
   * {@literal <} 1, or if {@code maxInterval} is {@literal <=} {@code minInterval}.
   * @see UniformRandomBackOffPolicy
   */
  public RetryTemplateBuilder uniformRandomBackoff(long minInterval, long maxInterval) {
    Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
    Assert.isTrue(minInterval >= 1, "Min interval should be >= 1");
    Assert.isTrue(maxInterval >= 1, "Max interval should be >= 1");
    Assert.isTrue(maxInterval > minInterval, "Max interval should be > than min interval");
    UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
    policy.setMinBackOffPeriod(minInterval);
    policy.setMaxBackOffPeriod(maxInterval);
    this.backOffPolicy = policy;
    return this;
  }

  /**
   * Use {@link UniformRandomBackOffPolicy}.
   *
   * @param minInterval minimum backoff duration
   * @param maxInterval maximum backoff duration
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected, if {@code minInterval} is {@code null} or {@literal <} 1,
   * {@code maxInterval} is {@code null} or less than 1 millisecond, or if
   * {@code maxInterval} {@literal <=} {@code minInterval}.
   * @see UniformRandomBackOffPolicy
   */
  public RetryTemplateBuilder uniformRandomBackoff(Duration minInterval, Duration maxInterval) {
    Assert.notNull(minInterval, "minInterval is required");
    Assert.notNull(maxInterval, "maxInterval is required");
    return uniformRandomBackoff(minInterval.toMillis(), maxInterval.toMillis());
  }

  /**
   * Retry immediately without pausing between attempts.
   *
   * @return this
   * @throws IllegalArgumentException if another backoff policy has already been
   * selected.
   * @see NoBackOffPolicy
   */
  public RetryTemplateBuilder noBackoff() {
    Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
    this.backOffPolicy = new NoBackOffPolicy();
    return this;
  }

  /**
   * Use the provided {@link BackOffPolicy}.
   *
   * @param backOffPolicy {@link BackOffPolicy} to use
   * @return this
   * @throws IllegalArgumentException if {@code backOffPolicy} is null or if another
   * backoff policy has already been selected.
   */
  public RetryTemplateBuilder customBackoff(BackOffPolicy backOffPolicy) {
    Assert.isNull(this.backOffPolicy, "You have already selected backoff policy");
    Assert.notNull(backOffPolicy, "You should provide non null custom policy");
    this.backOffPolicy = backOffPolicy;
    return this;
  }

  /* ---------------- Configure exception classifier -------------- */

  /**
   * Add the {@link Throwable} and its subclasses to the list of exceptions that cause a
   * retry.
   * <p>
   * {@code retryOn()} and {@code notRetryOn()} are mutually exclusive. Trying to use
   * both causes an {@link IllegalArgumentException}.
   *
   * @param throwable that causes a retry
   * @return this
   * @throws IllegalArgumentException if {@code throwable} is {@code null}, or if
   * {@link #notRetryOn} has already been used.
   * @see BinaryExceptionClassifierBuilder#retryOn
   * @see BinaryExceptionClassifier
   */
  public RetryTemplateBuilder retryOn(Class<? extends Throwable> throwable) {
    classifierBuilder().retryOn(throwable);
    return this;
  }

  /**
   * Add the {@link Throwable} and its subclasses to the list of exceptions that do not
   * cause a retry.
   * <p>
   * {@code retryOn()} and {@code notRetryOn()} are mutually exclusive. Trying to use
   * both causes an {@link IllegalArgumentException}.
   *
   * @param throwable that does not cause a retry
   * @return this
   * @throws IllegalArgumentException if {@code throwable} is {@code null}, or if
   * {@link #retryOn} has already been used.
   * @see BinaryExceptionClassifierBuilder#notRetryOn
   * @see BinaryExceptionClassifier
   */
  public RetryTemplateBuilder notRetryOn(Class<? extends Throwable> throwable) {
    classifierBuilder().notRetryOn(throwable);
    return this;
  }

  /**
   * Add the list of {@link Throwable} classes and their subclasses to the list of
   * exceptions that cause a retry.
   * <p>
   * {@code retryOn()} and {@code notRetryOn()} are mutually exclusive. Trying to use
   * both causes an {@link IllegalArgumentException}.
   *
   * @param throwables that cause a retry
   * @return this
   * @throws IllegalArgumentException if {@link #notRetryOn} has already been used.
   * @see BinaryExceptionClassifierBuilder#retryOn
   * @see BinaryExceptionClassifier
   */
  public RetryTemplateBuilder retryOn(List<Class<? extends Throwable>> throwables) {
    for (Class<? extends Throwable> throwable : throwables) {
      classifierBuilder().retryOn(throwable);
    }
    return this;
  }

  /**
   * Add the list of {@link Throwable} classes and their subclasses to the list of
   * exceptions that do not cause a retry.
   * <p>
   * {@code retryOn()} and {@code notRetryOn()} are mutually exclusive. Trying to use
   * both causes an {@link IllegalArgumentException}.
   *
   * @param throwables that do not cause a retry
   * @return this
   * @throws IllegalArgumentException if {@link #retryOn} has already been used.
   * @see BinaryExceptionClassifierBuilder#notRetryOn
   * @see BinaryExceptionClassifier
   */
  public RetryTemplateBuilder notRetryOn(List<Class<? extends Throwable>> throwables) {
    for (Class<? extends Throwable> throwable : throwables) {
      classifierBuilder().notRetryOn(throwable);
    }
    return this;
  }

  /**
   * Enable examining exception causes for {@link Throwable} instances that cause a
   * retry.
   * <p>
   * Suppose the following {@code RetryTemplate}: <pre>{@code
   * RetryTemplate.builder()
   *          .retryOn(IOException.class)
   *          .build()
   * }</pre>
   * <p>
   * It will act on code that throws an {@link java.io.IOException}, for example,
   * {@code throw new IOException()}. But it will not retry the action if the
   * {@link java.io.IOException} is wrapped in another exception, for example,
   * {@code throw new MyException(new IOException())}. However, this
   * {@link RetryTemplate} will: <pre>{@code
   * RetryTemplate.builder()
   *          .retryOn(IOException.class)
   *          .traversingCauses()
   *          .build()
   * }</pre>
   *
   * @return this
   * @see BinaryExceptionClassifier
   */
  public RetryTemplateBuilder traversingCauses() {
    classifierBuilder().traversingCauses();
    return this;
  }

  /* ---------------- Add listeners -------------- */

  /**
   * Append the provided {@code listener} to {@link RetryTemplate}'s list of listeners.
   *
   * @param listener to be appended
   * @return this
   * @throws IllegalArgumentException if {@code listener} is {@code null}.
   * @see RetryListener
   */
  public RetryTemplateBuilder withListener(RetryListener listener) {
    Assert.notNull(listener, "Listener should not be null");
    listenersList().add(listener);
    return this;
  }

  /**
   * Append all provided {@code listeners} to {@link RetryTemplate}'s list of listeners.
   *
   * @param listeners to be appended
   * @return this
   * @throws IllegalArgumentException if any of the {@code listeners} is {@code null}.
   * @see RetryListener
   */
  public RetryTemplateBuilder withListeners(List<RetryListener> listeners) {
    for (final RetryListener listener : listeners) {
      Assert.notNull(listener, "Listener should not be null");
    }
    listenersList().addAll(listeners);
    return this;
  }

  /* ---------------- Building -------------- */

  /**
   * Build a new {@link RetryTemplate}.
   * <p>
   * Supports building multiple instances. However, it is not possible to change the
   * configuration between multiple {@code build()} calls.
   * <p>
   * The {@code retryPolicy} of the returned {@link RetryTemplate} is always a
   * {@link CompositeRetryPolicy} that consists of one base policy and of
   * {@link BinaryExceptionClassifierRetryPolicy} to enable exception classification
   * regardless of the base policy.
   *
   * @return new instance of {@link RetryTemplate}
   */
  public RetryTemplate build() {
    RetryTemplate retryTemplate = new RetryTemplate();

    // Exception classifier

    BinaryExceptionClassifier exceptionClassifier =
            classifierBuilder != null
            ? classifierBuilder.build()
            : BinaryExceptionClassifier.defaultClassifier();

    // Retry policy

    if (this.baseRetryPolicy == null) {
      this.baseRetryPolicy = new MaxAttemptsRetryPolicy();
    }

    CompositeRetryPolicy finalPolicy = new CompositeRetryPolicy();
    finalPolicy.setPolicies(new RetryPolicy[] {
            baseRetryPolicy,
            new BinaryExceptionClassifierRetryPolicy(exceptionClassifier)
    });
    retryTemplate.setRetryPolicy(finalPolicy);

    // Backoff policy

    if (this.backOffPolicy == null) {
      this.backOffPolicy = new NoBackOffPolicy();
    }
    retryTemplate.setBackOffPolicy(this.backOffPolicy);

    // Listeners

    if (this.listeners != null) {
      retryTemplate.setListeners(this.listeners.toArray(new RetryListener[0]));
    }

    return retryTemplate;
  }

  /* ---------------- Private utils -------------- */

  private BinaryExceptionClassifierBuilder classifierBuilder() {
    if (this.classifierBuilder == null) {
      this.classifierBuilder = new BinaryExceptionClassifierBuilder();
    }
    return this.classifierBuilder;
  }

  private List<RetryListener> listenersList() {
    if (this.listeners == null) {
      this.listeners = new ArrayList<>();
    }
    return this.listeners;
  }

}
