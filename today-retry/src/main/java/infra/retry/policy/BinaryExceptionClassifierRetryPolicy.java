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

package infra.retry.policy;

import infra.classify.BinaryExceptionClassifier;
import infra.retry.RetryContext;
import infra.retry.RetryPolicy;
import infra.retry.context.RetryContextSupport;

/**
 * A policy, that is based on {@link BinaryExceptionClassifier}. Usually, binary
 * classification is enough for retry purposes. If you need more flexible classification,
 * use {@link ExceptionClassifierRetryPolicy}.
 *
 * @author Aleksandr Shamukov
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BinaryExceptionClassifierRetryPolicy implements RetryPolicy {

  private final BinaryExceptionClassifier exceptionClassifier;

  public BinaryExceptionClassifierRetryPolicy(BinaryExceptionClassifier exceptionClassifier) {
    this.exceptionClassifier = exceptionClassifier;
  }

  public BinaryExceptionClassifier getExceptionClassifier() {
    return exceptionClassifier;
  }

  @Override
  public boolean canRetry(RetryContext context) {
    Throwable t = context.getLastThrowable();
    return t == null || exceptionClassifier.classify(t);
  }

  @Override
  public void close(RetryContext status) {
  }

  @Override
  public void registerThrowable(RetryContext context, Throwable throwable) {
    RetryContextSupport simpleContext = ((RetryContextSupport) context);
    simpleContext.registerThrowable(throwable);
  }

  @Override
  public RetryContext open(RetryContext parent) {
    return new RetryContextSupport(parent);
  }

}
