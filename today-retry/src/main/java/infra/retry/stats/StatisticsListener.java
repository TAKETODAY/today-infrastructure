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

package infra.retry.stats;

import infra.core.AttributeAccessor;
import infra.retry.RetryCallback;
import infra.retry.RetryContext;
import infra.retry.RetryListener;
import infra.retry.RetryStatistics;
import infra.retry.policy.CircuitBreakerRetryPolicy;

/**
 * @author Dave Syer
 * @since 4.0
 */
public class StatisticsListener implements RetryListener {

  private final StatisticsRepository repository;

  public StatisticsListener(StatisticsRepository repository) {
    this.repository = repository;
  }

  @Override
  public <T, E extends Throwable> void close(
          RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    String name = getName(context);
    if (name != null) {
      if (!isExhausted(context) || isGlobal(context)) {
        // If exhausted and stateful then the retry callback was not called. If
        // exhausted and stateless it was called, but the started counter was
        // already incremented.
        repository.addStarted(name);
      }
      if (isRecovered(context)) {
        repository.addRecovery(name);
      }
      else if (isExhausted(context)) {
        repository.addAbort(name);
      }
      else if (isClosed(context)) {
        repository.addComplete(name);
      }
      RetryStatistics stats = repository.findOne(name);
      if (stats instanceof AttributeAccessor accessor) {
        for (String key : new String[] { CircuitBreakerRetryPolicy.CIRCUIT_OPEN,
                CircuitBreakerRetryPolicy.CIRCUIT_SHORT_COUNT }) {
          if (context.hasAttribute(key)) {
            accessor.setAttribute(key, context.getAttribute(key));
          }
        }
      }
    }
  }

  @Override
  public <T, E extends Throwable> void onError(
          RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
    String name = getName(context);
    if (name != null) {
      if (!hasState(context)) {
        // Stateless retry involves starting the retry callback once per error
        // without closing the context, so we need to increment the started count
        repository.addStarted(name);
      }
      repository.addError(name);
    }
  }

  private boolean isGlobal(RetryContext context) {
    return context.hasAttribute("state.global");
  }

  private boolean isExhausted(RetryContext context) {
    return context.hasAttribute(RetryContext.EXHAUSTED);
  }

  private boolean isClosed(RetryContext context) {
    return context.hasAttribute(RetryContext.CLOSED);
  }

  private boolean isRecovered(RetryContext context) {
    return context.hasAttribute(RetryContext.RECOVERED);
  }

  private boolean hasState(RetryContext context) {
    return context.hasAttribute(RetryContext.STATE_KEY);
  }

  private String getName(RetryContext context) {
    return (String) context.getAttribute(RetryContext.NAME);
  }

}
