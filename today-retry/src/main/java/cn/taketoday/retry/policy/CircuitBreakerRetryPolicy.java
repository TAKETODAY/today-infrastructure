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

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryPolicy;
import cn.taketoday.retry.context.RetryContextSupport;

/**
 * @author Dave Syer
 */
@SuppressWarnings("serial")
public class CircuitBreakerRetryPolicy implements RetryPolicy {

  public static final String CIRCUIT_OPEN = "circuit.open";

  public static final String CIRCUIT_SHORT_COUNT = "circuit.shortCount";

  private static Logger logger = LoggerFactory.getLogger(CircuitBreakerRetryPolicy.class);

  private final RetryPolicy delegate;

  private long resetTimeout = 20000;

  private long openTimeout = 5000;

  public CircuitBreakerRetryPolicy() {
    this(new SimpleRetryPolicy());
  }

  public CircuitBreakerRetryPolicy(RetryPolicy delegate) {
    this.delegate = delegate;
  }

  /**
   * Timeout for resetting circuit in milliseconds. After the circuit opens it will
   * re-close after this time has elapsed and the context will be restarted.
   *
   * @param timeout the timeout to set in milliseconds
   */
  public void setResetTimeout(long timeout) {
    this.resetTimeout = timeout;
  }

  /**
   * Timeout for tripping the open circuit. If the delegate policy cannot retry and the
   * time elapsed since the context was started is less than this window, then the
   * circuit is opened.
   *
   * @param timeout the timeout to set in milliseconds
   */
  public void setOpenTimeout(long timeout) {
    this.openTimeout = timeout;
  }

  @Override
  public boolean canRetry(RetryContext context) {
    CircuitBreakerRetryContext circuit = (CircuitBreakerRetryContext) context;
    if (circuit.isOpen()) {
      circuit.incrementShortCircuitCount();
      return false;
    }
    else {
      circuit.reset();
    }
    return this.delegate.canRetry(circuit.context);
  }

  @Override
  public RetryContext open(RetryContext parent) {
    return new CircuitBreakerRetryContext(parent, this.delegate, this.resetTimeout, this.openTimeout);
  }

  @Override
  public void close(RetryContext context) {
    CircuitBreakerRetryContext circuit = (CircuitBreakerRetryContext) context;
    this.delegate.close(circuit.context);
  }

  @Override
  public void registerThrowable(RetryContext context, Throwable throwable) {
    CircuitBreakerRetryContext circuit = (CircuitBreakerRetryContext) context;
    circuit.registerThrowable(throwable);
    this.delegate.registerThrowable(circuit.context, throwable);
  }

  static class CircuitBreakerRetryContext extends RetryContextSupport {

    private volatile RetryContext context;

    private final RetryPolicy policy;

    private volatile long start = System.currentTimeMillis();

    private final long timeout;

    private final long openWindow;

    private final AtomicInteger shortCircuitCount = new AtomicInteger();

    public CircuitBreakerRetryContext(RetryContext parent, RetryPolicy policy, long timeout, long openWindow) {
      super(parent);
      this.policy = policy;
      this.timeout = timeout;
      this.openWindow = openWindow;
      this.context = createDelegateContext(policy, parent);
      setAttribute("state.global", true);
    }

    public void reset() {
      shortCircuitCount.set(0);
      setAttribute(CIRCUIT_SHORT_COUNT, shortCircuitCount.get());
    }

    public void incrementShortCircuitCount() {
      shortCircuitCount.incrementAndGet();
      setAttribute(CIRCUIT_SHORT_COUNT, shortCircuitCount.get());
    }

    private RetryContext createDelegateContext(RetryPolicy policy, RetryContext parent) {
      RetryContext context = policy.open(parent);
      reset();
      return context;
    }

    public boolean isOpen() {
      long time = System.currentTimeMillis() - this.start;
      boolean retryable = this.policy.canRetry(this.context);
      if (!retryable) {
        if (time > this.timeout) {
          logger.trace("Closing");
          this.context = createDelegateContext(policy, getParent());
          this.start = System.currentTimeMillis();
          retryable = this.policy.canRetry(this.context);
        }
        else if (time < this.openWindow) {
          if (!hasAttribute(CIRCUIT_OPEN) || (Boolean) getAttribute(CIRCUIT_OPEN) == false) {
            logger.trace("Opening circuit");
            setAttribute(CIRCUIT_OPEN, true);
            this.start = System.currentTimeMillis();
          }

          return true;
        }
      }
      else {
        if (time > this.openWindow) {
          logger.trace("Resetting context");
          this.start = System.currentTimeMillis();
          this.context = createDelegateContext(policy, getParent());
        }
      }
      if (logger.isTraceEnabled()) {
        logger.trace("Open: " + !retryable);
      }
      setAttribute(CIRCUIT_OPEN, !retryable);
      return !retryable;
    }

    @Override
    public int getRetryCount() {
      return this.context.getRetryCount();
    }

    @Override
    public String toString() {
      return this.context.toString();
    }

  }

}
