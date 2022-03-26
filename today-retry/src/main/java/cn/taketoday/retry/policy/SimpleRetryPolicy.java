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

import java.util.Map;

import cn.taketoday.classify.BinaryExceptionClassifier;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.context.RetryContextSupport;
import cn.taketoday.retry.support.RetryTemplate;
import cn.taketoday.util.ClassUtils;

/**
 * Simple retry policy that retries a fixed number of times for a set of named exceptions
 * (and subclasses). The number of attempts includes the initial try, so e.g.
 *
 * <pre>
 * retryTemplate = new RetryTemplate(new SimpleRetryPolicy(3));
 * retryTemplate.execute(callback);
 * </pre>
 *
 * will execute the callback at least once, and as many as 3 times.
 *
 * Since version 1.3 it is not necessary to use this class. The same behaviour can be
 * achieved by constructing a {@link CompositeRetryPolicy} with
 * {@link MaxAttemptsRetryPolicy} and {@link BinaryExceptionClassifierRetryPolicy} inside,
 * that is actually performed by:
 *
 * <pre>
 * RetryTemplate.newBuilder()
 *                  .maxAttempts(3)
 *                  .retryOn(Exception.class)
 *                  .build();
 * </pre>
 *
 * or by {@link RetryTemplate#defaultInstance()}
 *
 * @author Dave Syer
 * @author Rob Harrop
 * @author Gary Russell
 * @author Aleksandr Shamukov
 */
@SuppressWarnings("serial")
public class SimpleRetryPolicy implements RetryPolicy {

  /**
   * The default limit to the number of attempts for a new policy.
   */
  public final static int DEFAULT_MAX_ATTEMPTS = 3;

  private volatile int maxAttempts;

  private BinaryExceptionClassifier retryableClassifier = new BinaryExceptionClassifier(false);

  /**
   * Create a {@link SimpleRetryPolicy} with the default number of retry attempts,
   * retrying all exceptions.
   */
  public SimpleRetryPolicy() {
    this(DEFAULT_MAX_ATTEMPTS, BinaryExceptionClassifier.defaultClassifier());
  }

  /**
   * Create a {@link SimpleRetryPolicy} with the specified number of retry attempts,
   * retrying all exceptions.
   *
   * @param maxAttempts the maximum number of attempts
   */
  public SimpleRetryPolicy(int maxAttempts) {
    this(maxAttempts, BinaryExceptionClassifier.defaultClassifier());
  }

  /**
   * Create a {@link SimpleRetryPolicy} with the specified number of retry attempts.
   *
   * @param maxAttempts the maximum number of attempts
   * @param retryableExceptions the map of exceptions that are retryable
   */
  public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions) {
    this(maxAttempts, retryableExceptions, false);
  }

  /**
   * Create a {@link SimpleRetryPolicy} with the specified number of retry attempts. If
   * traverseCauses is true, the exception causes will be traversed until a match or the
   * root cause is found.
   *
   * @param maxAttempts the maximum number of attempts
   * @param retryableExceptions the map of exceptions that are retryable based on the
   * map value (true/false).
   * @param traverseCauses true to traverse the exception cause chain until a classified
   * exception is found or the root cause is reached.
   */
  public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
          boolean traverseCauses) {
    this(maxAttempts, retryableExceptions, traverseCauses, false);
  }

  /**
   * Create a {@link SimpleRetryPolicy} with the specified number of retry attempts. If
   * traverseCauses is true, the exception causes will be traversed until a match or the
   * root cause is found. The default value indicates whether to retry or not for
   * exceptions (or super classes thereof) that are not found in the map.
   *
   * @param maxAttempts the maximum number of attempts
   * @param retryableExceptions the map of exceptions that are retryable based on the
   * map value (true/false).
   * @param traverseCauses true to traverse the exception cause chain until a classified
   * exception is found or the root cause is reached.
   * @param defaultValue the default action.
   */
  public SimpleRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
          boolean traverseCauses, boolean defaultValue) {
    super();
    this.maxAttempts = maxAttempts;
    this.retryableClassifier = new BinaryExceptionClassifier(retryableExceptions, defaultValue);
    this.retryableClassifier.setTraverseCauses(traverseCauses);
  }

  /**
   * Create a {@link SimpleRetryPolicy} with the specified number of retry attempts and
   * provided exception classifier.
   *
   * @param maxAttempts the maximum number of attempts
   * @param classifier custom exception classifier
   */
  public SimpleRetryPolicy(int maxAttempts, BinaryExceptionClassifier classifier) {
    super();
    this.maxAttempts = maxAttempts;
    this.retryableClassifier = classifier;
  }

  /**
   * Set the number of attempts before retries are exhausted. Includes the initial
   * attempt before the retries begin so, generally, will be {@code >= 1}. For example
   * setting this property to 3 means 3 attempts total (initial + 2 retries).
   *
   * @param maxAttempts the maximum number of attempts including the initial attempt.
   */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * The maximum number of attempts before failure.
   *
   * @return the maximum number of attempts
   */
  public int getMaxAttempts() {
    return this.maxAttempts;
  }

  /**
   * Test for retryable operation based on the status.
   *
   * @return true if the last exception was retryable and the number of attempts so far
   * is less than the limit.
   * @see RetryPolicy#canRetry(RetryContext)
   */
  @Override
  public boolean canRetry(RetryContext context) {
    Throwable t = context.getLastThrowable();
    return (t == null || retryForException(t)) && context.getRetryCount() < this.maxAttempts;
  }

  /**
   * @see RetryPolicy#close(RetryContext)
   */
  @Override
  public void close(RetryContext status) {
  }

  /**
   * Update the status with another attempted retry and the latest exception.
   *
   * @see RetryPolicy#registerThrowable(RetryContext, Throwable)
   */
  @Override
  public void registerThrowable(RetryContext context, Throwable throwable) {
    SimpleRetryContext simpleContext = ((SimpleRetryContext) context);
    simpleContext.registerThrowable(throwable);
  }

  /**
   * Get a status object that can be used to track the current operation according to
   * this policy. Has to be aware of the latest exception and the number of attempts.
   *
   * @see RetryPolicy#open(RetryContext)
   */
  @Override
  public RetryContext open(RetryContext parent) {
    return new SimpleRetryContext(parent);
  }

  private static class SimpleRetryContext extends RetryContextSupport {

    public SimpleRetryContext(RetryContext parent) {
      super(parent);
    }

  }

  /**
   * Delegates to an exception classifier.
   *
   * @param ex
   * @return true if this exception or its ancestors have been registered as retryable.
   */
  private boolean retryForException(Throwable ex) {
    return this.retryableClassifier.classify(ex);
  }

  @Override
  public String toString() {
    return ClassUtils.getShortName(getClass()) + "[maxAttempts=" + this.maxAttempts + "]";
  }

}
