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

import java.util.Arrays;

import cn.taketoday.classify.Classifier;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryOperations;
import cn.taketoday.retry.RetryState;
import cn.taketoday.retry.policy.NeverRetryPolicy;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@link MethodInterceptor} that can be used to automatically retry calls to a method
 * on a service if it fails. The argument to the service method is treated as an item to
 * be remembered in case the call fails. So the retry operation is stateful, and the item
 * that failed is tracked by its unique key (via {@link MethodArgumentsKeyGenerator})
 * until the retry is exhausted, at which point the {@link MethodInvocationRecoverer} is
 * called.
 *
 * The main use case for this is where the service is transactional, via a transaction
 * interceptor on the interceptor chain. In this case the retry (and recovery on
 * exhausted) always happens in a new transaction.
 *
 * The injected {@link RetryOperations} is used to control the number of retries. By
 * default it will retry a fixed number of times, according to the defaults in
 * {@link RetryTemplate}.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @since 4.0
 */
public class StatefulRetryOperationsInterceptor implements MethodInterceptor {
  private static final Logger log = LoggerFactory.getLogger(StatefulRetryOperationsInterceptor.class);

  @Nullable
  private MethodArgumentsKeyGenerator keyGenerator;

  private MethodInvocationRecoverer<?> recoverer;

  private NewMethodArgumentsIdentifier newMethodArgumentsIdentifier;

  private RetryOperations retryOperations;

  private String label;

  @Nullable
  private Classifier<? super Throwable, Boolean> rollbackClassifier;

  private boolean useRawKey;

  public StatefulRetryOperationsInterceptor() {
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(new NeverRetryPolicy());
    this.retryOperations = retryTemplate;
  }

  public void setRetryOperations(RetryOperations retryTemplate) {
    Assert.notNull(retryTemplate, "retryOperations is required");
    this.retryOperations = retryTemplate;
  }

  /**
   * Public setter for the {@link MethodInvocationRecoverer} to use if the retry is
   * exhausted. The recoverer should be able to return an object of the same type as the
   * target object because its return value will be used to return to the caller in the
   * case of a recovery.
   *
   * @param recoverer the {@link MethodInvocationRecoverer} to set
   */
  public void setRecoverer(MethodInvocationRecoverer<?> recoverer) {
    this.recoverer = recoverer;
  }

  /**
   * Rollback classifier for the retry state. Default to null (meaning rollback for
   * all).
   *
   * @param rollbackClassifier the rollbackClassifier to set
   */
  public void setRollbackClassifier(@Nullable Classifier<? super Throwable, Boolean> rollbackClassifier) {
    this.rollbackClassifier = rollbackClassifier;
  }

  public void setKeyGenerator(@Nullable MethodArgumentsKeyGenerator keyGenerator) {
    this.keyGenerator = keyGenerator;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Public setter for the {@link NewMethodArgumentsIdentifier}. Only set this if the
   * arguments to the intercepted method can be inspected to find out if they have never
   * been processed before.
   *
   * @param newMethodArgumentsIdentifier the {@link NewMethodArgumentsIdentifier} to set
   */
  public void setNewItemIdentifier(NewMethodArgumentsIdentifier newMethodArgumentsIdentifier) {
    this.newMethodArgumentsIdentifier = newMethodArgumentsIdentifier;
  }

  /**
   * Set to true to use the raw key generated by the key generator. Should only be set
   * to true for cases where the key is guaranteed to be unique in all cases. When
   * false, a compound key is used, including invocation metadata. Default: false.
   *
   * @param useRawKey the useRawKey to set.
   */
  public void setUseRawKey(boolean useRawKey) {
    this.useRawKey = useRawKey;
  }

  /**
   * Wrap the method invocation in a stateful retry with the policy and other helpers
   * provided. If there is a failure the exception will generally be re-thrown. The only
   * time it is not re-thrown is when retry is exhausted and the recovery path is taken
   * (though the {@link MethodInvocationRecoverer} provided if there is one). In that
   * case the value returned from the method invocation will be the value returned by
   * the recoverer (so the return type for that should be the same as the intercepted
   * method).
   *
   * @see MethodInterceptor#invoke(MethodInvocation)
   * @see MethodInvocationRecoverer#recover(Object[], Throwable)
   */
  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    if (log.isDebugEnabled()) {
      log.debug("Executing proxied method in stateful retry: {}({})",
              invocation.getStaticPart(), ObjectUtils.getIdentityHexString(invocation));
    }

    Object key = createKey(invocation);
    Object[] args = invocation.getArguments();
    RetryState retryState = new DefaultRetryState(key,
            newMethodArgumentsIdentifier != null && newMethodArgumentsIdentifier.isNew(args),
            rollbackClassifier);

    Object result = retryOperations.execute(new StatefulMethodInvocationRetryCallback(invocation, label),
            recoverer != null ? new ItemRecovererCallback(args, recoverer) : null, retryState);

    if (log.isDebugEnabled()) {
      log.debug("Exiting proxied method in stateful retry with result: ({})", result);
    }

    return result;
  }

  /**
   * @return the key for the state to allow this retry attempt to be recognised
   */
  @Nullable
  private Object createKey(final MethodInvocation invocation) {
    Object generatedKey;
    if (keyGenerator != null) {
      generatedKey = keyGenerator.getKey(invocation.getArguments());
    }
    else {
      // compute default key
      generatedKey = computeDefaultKey(invocation);
    }

    if (generatedKey == null) {
      // If there's a generator and he still says the key is null, that means he
      // really doesn't want to retry.
      return null;
    }
    if (this.useRawKey) {
      return generatedKey;
    }
    String name = StringUtils.hasText(label) ? label : invocation.getMethod().toGenericString();
    return Arrays.asList(name, generatedKey);
  }

  /**
   * compute default key, sub-classes can override
   *
   * @param invocation target
   * @return default key
   */
  @Nullable
  protected Object computeDefaultKey(MethodInvocation invocation) {
    Object[] args = invocation.getArguments();
    if (args.length == 1) {
      return args[0]; // may be null
    }
    else {
      return Arrays.asList(args);
    }
  }

  /**
   * @author Dave Syer
   */
  private static final class StatefulMethodInvocationRetryCallback
          extends MethodInvocationRetryCallback<Object, Throwable> {

    private StatefulMethodInvocationRetryCallback(MethodInvocation invocation, String label) {
      super(invocation, label);
    }

    @Override
    public Object doWithRetry(RetryContext context) throws Exception {
      context.setAttribute(RetryContext.NAME, label);
      try {
        return this.invocation.proceed();
      }
      catch (Exception | Error e) {
        throw e;
      }
      catch (Throwable e) {
        throw new IllegalStateException(e);
      }
    }

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
