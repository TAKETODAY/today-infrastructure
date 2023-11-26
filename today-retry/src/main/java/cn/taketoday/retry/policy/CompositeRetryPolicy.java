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

package cn.taketoday.retry.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.context.RetryContextSupport;

/**
 * A {@link RetryPolicy} that composes a list of other policies and delegates calls to
 * them in order.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CompositeRetryPolicy implements RetryPolicy {

  RetryPolicy[] policies = new RetryPolicy[0];

  private boolean optimistic = false;

  /**
   * Setter for optimistic.
   *
   * @param optimistic should this retry policy be optimistic
   */
  public void setOptimistic(boolean optimistic) {
    this.optimistic = optimistic;
  }

  /**
   * Setter for policies.
   *
   * @param policies the {@link RetryPolicy} policies
   */
  public void setPolicies(RetryPolicy[] policies) {
    this.policies = policies.clone();
  }

  /**
   * Delegate to the policies that were in operation when the context was created. If
   * any of them cannot retry then return false, otherwise return true.
   *
   * @param context the {@link RetryContext}
   * @see RetryPolicy#canRetry(RetryContext)
   */
  @Override
  public boolean canRetry(RetryContext context) {
    RetryContext[] contexts = ((CompositeRetryContext) context).contexts;
    RetryPolicy[] policies = ((CompositeRetryContext) context).policies;

    boolean retryable = true;

    if (this.optimistic) {
      retryable = false;
      for (int i = 0; i < contexts.length; i++) {
        if (policies[i].canRetry(contexts[i])) {
          retryable = true;
        }
      }
    }
    else {
      for (int i = 0; i < contexts.length; i++) {
        if (!policies[i].canRetry(contexts[i])) {
          retryable = false;
        }
      }
    }

    return retryable;
  }

  /**
   * Delegate to the policies that were in operation when the context was created. If
   * any of them fails to close the exception is propagated (and those later in the
   * chain are closed before re-throwing).
   *
   * @param context the {@link RetryContext}
   * @see RetryPolicy#close(RetryContext)
   */
  @Override
  public void close(RetryContext context) {
    RetryContext[] contexts = ((CompositeRetryContext) context).contexts;
    RetryPolicy[] policies = ((CompositeRetryContext) context).policies;
    RuntimeException exception = null;
    for (int i = 0; i < contexts.length; i++) {
      try {
        policies[i].close(contexts[i]);
      }
      catch (RuntimeException e) {
        if (exception == null) {
          exception = e;
        }
      }
    }
    if (exception != null) {
      throw exception;
    }
  }

  /**
   * Creates a new context that copies the existing policies and keeps a list of the
   * contexts from each one.
   *
   * @see RetryPolicy#open(RetryContext)
   */
  @Override
  public RetryContext open(RetryContext parent) {
    ArrayList<RetryContext> list = new ArrayList<>();
    for (RetryPolicy policy : this.policies) {
      list.add(policy.open(parent));
    }
    return new CompositeRetryContext(parent, list, this.policies);
  }

  /**
   * Delegate to the policies that were in operation when the context was created.
   *
   * @see RetryPolicy#close(RetryContext)
   */
  @Override
  public void registerThrowable(RetryContext context, Throwable throwable) {
    RetryContext[] contexts = ((CompositeRetryContext) context).contexts;
    RetryPolicy[] policies = ((CompositeRetryContext) context).policies;
    for (int i = 0; i < contexts.length; i++) {
      policies[i].registerThrowable(contexts[i], throwable);
    }
    ((RetryContextSupport) context).registerThrowable(throwable);
  }

  /**
   * @return the lower 'maximum number of attempts before failure' between all policies
   * that have a 'maximum number of attempts before failure' set, if at least one is
   * present among the policies, return {@link RetryPolicy#NO_MAXIMUM_ATTEMPTS_SET}
   * otherwise
   */
  @Override
  public int getMaxAttempts() {
    return Arrays.stream(policies)
            .map(RetryPolicy::getMaxAttempts)
            .filter(maxAttempts -> maxAttempts != NO_MAXIMUM_ATTEMPTS_SET)
            .sorted()
            .findFirst()
            .orElse(NO_MAXIMUM_ATTEMPTS_SET);
  }

  private static class CompositeRetryContext extends RetryContextSupport {

    RetryContext[] contexts;

    RetryPolicy[] policies;

    public CompositeRetryContext(RetryContext parent, List<RetryContext> contexts, RetryPolicy[] policies) {
      super(parent);
      this.contexts = contexts.toArray(new RetryContext[contexts.size()]);
      this.policies = policies;
    }

  }

}
