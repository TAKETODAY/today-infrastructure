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

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.classify.Classifier;
import cn.taketoday.classify.ClassifierSupport;
import cn.taketoday.classify.SubclassClassifier;
import cn.taketoday.lang.Assert;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.context.RetryContextSupport;

/**
 * A {@link RetryPolicy} that dynamically adapts to one of a set of injected policies
 * according to the value of the latest exception.
 *
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class ExceptionClassifierRetryPolicy implements RetryPolicy {

  private Classifier<Throwable, RetryPolicy> exceptionClassifier = new ClassifierSupport<Throwable, RetryPolicy>(
          new NeverRetryPolicy());

  /**
   * Setter for policy map used to create a classifier. Either this property or the
   * exception classifier directly should be set, but not both.
   *
   * @param policyMap a map of Throwable class to {@link RetryPolicy} that will be used
   * to create a {@link Classifier} to locate a policy.
   */
  public void setPolicyMap(Map<Class<? extends Throwable>, RetryPolicy> policyMap) {
    this.exceptionClassifier = new SubclassClassifier<Throwable, RetryPolicy>(policyMap, new NeverRetryPolicy());
  }

  /**
   * Setter for an exception classifier. The classifier is responsible for translating
   * exceptions to concrete retry policies. Either this property or the policy map
   * should be used, but not both.
   *
   * @param exceptionClassifier ExceptionClassifier to use
   */
  public void setExceptionClassifier(Classifier<Throwable, RetryPolicy> exceptionClassifier) {
    this.exceptionClassifier = exceptionClassifier;
  }

  /**
   * Delegate to the policy currently activated in the context.
   *
   * @see RetryPolicy#canRetry(RetryContext)
   */
  public boolean canRetry(RetryContext context) {
    RetryPolicy policy = (RetryPolicy) context;
    return policy.canRetry(context);
  }

  /**
   * Delegate to the policy currently activated in the context.
   *
   * @see RetryPolicy#close(RetryContext)
   */
  public void close(RetryContext context) {
    RetryPolicy policy = (RetryPolicy) context;
    policy.close(context);
  }

  /**
   * Create an active context that proxies a retry policy by choosing a target from the
   * policy map.
   *
   * @see RetryPolicy#open(RetryContext)
   */
  public RetryContext open(RetryContext parent) {
    return new ExceptionClassifierRetryContext(parent, exceptionClassifier).open(parent);
  }

  /**
   * Delegate to the policy currently activated in the context.
   *
   * @see RetryPolicy#registerThrowable(RetryContext,
   * Throwable)
   */
  public void registerThrowable(RetryContext context, Throwable throwable) {
    RetryPolicy policy = (RetryPolicy) context;
    policy.registerThrowable(context, throwable);
    ((RetryContextSupport) context).registerThrowable(throwable);
  }

  private static class ExceptionClassifierRetryContext extends RetryContextSupport implements RetryPolicy {

    final private Classifier<Throwable, RetryPolicy> exceptionClassifier;

    // Dynamic: depends on the latest exception:
    private RetryPolicy policy;

    // Dynamic: depends on the policy:
    private RetryContext context;

    final private Map<RetryPolicy, RetryContext> contexts = new HashMap<RetryPolicy, RetryContext>();

    public ExceptionClassifierRetryContext(RetryContext parent,
            Classifier<Throwable, RetryPolicy> exceptionClassifier) {
      super(parent);
      this.exceptionClassifier = exceptionClassifier;
    }

    public boolean canRetry(RetryContext context) {
      return this.context == null || policy.canRetry(this.context);
    }

    public void close(RetryContext context) {
      // Only close those policies that have been used (opened):
      for (RetryPolicy policy : contexts.keySet()) {
        policy.close(getContext(policy, context.getParent()));
      }
    }

    public RetryContext open(RetryContext parent) {
      return this;
    }

    public void registerThrowable(RetryContext context, Throwable throwable) {
      policy = exceptionClassifier.classify(throwable);
      Assert.notNull(policy, "Could not locate policy for exception=[" + throwable + "].");
      this.context = getContext(policy, context.getParent());
      policy.registerThrowable(this.context, throwable);
    }

    private RetryContext getContext(RetryPolicy policy, RetryContext parent) {
      RetryContext context = contexts.get(policy);
      if (context == null) {
        context = policy.open(parent);
        contexts.put(policy, context);
      }
      return context;
    }

  }

}
