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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.lang.Assert;
import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.support.RetrySynchronizationManager;
import cn.taketoday.retry.support.RetryTemplate;
import cn.taketoday.util.StringUtils;

/**
 * A {@link MethodInterceptor} that can be used to automatically retry calls to a method
 * on a service if it fails. The injected {@link RetryOperations} is used to control the
 * number of retries. By default it will retry a fixed number of times, according to the
 * defaults in {@link RetryTemplate}.
 *
 * Hint about transaction boundaries. If you want to retry a failed transaction you need
 * to make sure that the transaction boundary is inside the retry, otherwise the
 * successful attempt will roll back with the whole transaction. If the method being
 * intercepted is also transactional, then use the ordering hints in the advice
 * declarations to ensure that this one is before the transaction interceptor in the
 * advice chain.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @since 4.0
 */
public class RetryOperationsInterceptor implements MethodInterceptor {

  private RetryOperations retryOperations = new RetryTemplate();

  private MethodInvocationRecoverer<?> recoverer;

  private String label;

  public void setLabel(String label) {
    this.label = label;
  }

  public void setRetryOperations(RetryOperations retryTemplate) {
    Assert.notNull(retryTemplate, "'retryOperations' cannot be null.");
    this.retryOperations = retryTemplate;
  }

  public void setRecoverer(MethodInvocationRecoverer<?> recoverer) {
    this.recoverer = recoverer;
  }

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {

    String name;
    if (StringUtils.hasText(this.label)) {
      name = this.label;
    }
    else {
      name = invocation.getMethod().toGenericString();
    }

    final String label = name;

    var retryCallback = new MethodInvocationRetryCallback<>(invocation, label) {

      @Override
      public Object doWithRetry(RetryContext context) throws Exception {
        context.setAttribute(RetryContext.NAME, this.label);

        /*
         * If we don't copy the invocation carefully it won't keep a reference to
         * the other interceptors in the chain. We don't have a choice here but to
         * specialise to ReflectiveMethodInvocation (but how often would another
         * implementation come along?).
         */
        if (this.invocation instanceof ProxyMethodInvocation proxyInv) {
          context.setAttribute("___proxy___", proxyInv.getProxy());
          try {
            return proxyInv.invocableClone().proceed();
          }
          catch (Exception e) {
            throw e;
          }
          catch (Error e) {
            throw e;
          }
          catch (Throwable e) {
            throw new IllegalStateException(e);
          }
        }
        else {
          throw new IllegalStateException(
                  "MethodInvocation of the wrong type detected - this should not happen with Spring AOP, "
                          + "so please raise an issue if you see this exception");
        }
      }

    };

    if (this.recoverer != null) {
      var recoveryCallback = new ItemRecovererCallback(invocation.getArguments(), this.recoverer);
      try {
        return retryOperations.execute(retryCallback, recoveryCallback);
      }
      finally {
        RetryContext context = RetrySynchronizationManager.getContext();
        if (context != null) {
          context.removeAttribute("__proxy__");
        }
      }
    }

    return retryOperations.execute(retryCallback);

  }

  /**
   * @author Dave Syer
   */
  private record ItemRecovererCallback(Object[] args, MethodInvocationRecoverer<?> recoverer)
          implements RecoveryCallback<Object> {

    /**
     * @param args the item that failed.
     */
    private ItemRecovererCallback(Object[] args, MethodInvocationRecoverer<?> recoverer) {
      this.args = args.clone();
      this.recoverer = recoverer;
    }

    @Override
    public Object recover(RetryContext context) {
      return this.recoverer.recover(this.args, context.getLastThrowable());
    }

  }

}
