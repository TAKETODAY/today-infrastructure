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

package cn.taketoday.retry.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.listener.MethodInvocationRetryListenerSupport;
import cn.taketoday.util.StringUtils;

/**
 * Callback class for a Spring AOP reflective `MethodInvocation` that can be retried using
 * a {@link RetryOperations}.
 *
 * In a concrete {@link RetryListener} implementation, the
 * `MethodInvocation` can be analysed for providing insights on the method called as well
 * as its parameter values which could then be used for monitoring purposes.
 *
 * @param <T> the type of object returned by the callback
 * @param <E> the type of exception it declares may be thrown
 * @author Marius Grama
 * @see StatefulRetryOperationsInterceptor
 * @see RetryOperationsInterceptor
 * @see MethodInvocationRetryListenerSupport
 * @since 4.0
 */
public abstract class MethodInvocationRetryCallback<T, E extends Throwable> implements RetryCallback<T, E> {

  protected final MethodInvocation invocation;

  protected final String label;

  /**
   * Constructor for the class.
   *
   * @param invocation the method invocation
   * @param label a unique label for statistics reporting.
   */
  public MethodInvocationRetryCallback(MethodInvocation invocation, String label) {
    this.invocation = invocation;
    if (StringUtils.hasText(label)) {
      this.label = label;
    }
    else {
      this.label = invocation.getMethod().toGenericString();
    }
  }

  public MethodInvocation getInvocation() {
    return invocation;
  }

  public String getLabel() {
    return label;
  }

}
