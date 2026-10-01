/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.transaction.jta;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import infra.util.Assert;
import infra.util.ReflectionUtils;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

/**
 * Adapter for a JTA UserTransaction handle, taking a JTA
 * {@link TransactionManager} reference and creating
 * a JTA {@link UserTransaction} handle for it.
 *
 * <p>The JTA UserTransaction interface is an exact subset of the JTA
 * TransactionManager interface. Unfortunately, it does not serve as
 * super-interface of TransactionManager, though, which requires an
 * adapter such as this class to be used when intending to talk to
 * a TransactionManager handle through the UserTransaction interface.
 *
 * <p>Used internally by Framework's {@link JtaTransactionManager} for certain
 * scenarios. Not intended for direct use in application code.
 * <p>As of Infra 5.0, this adapter also supports the JTA 2.1 read-only methods,
 * while remaining compatible with earlier JTA APIs.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class UserTransactionAdapter implements UserTransaction {

  private static final @Nullable Method beginWithReadOnlyMethod =
          ReflectionUtils.getMethodIfAvailable(TransactionManager.class, "begin", boolean.class);

  private static final @Nullable Method isReadOnlyMethod =
          ReflectionUtils.getMethodIfAvailable(Transaction.class, "isReadOnly");

  private final TransactionManager transactionManager;

  /**
   * Create a new UserTransactionAdapter for the given TransactionManager.
   *
   * @param transactionManager the JTA TransactionManager to wrap
   */
  public UserTransactionAdapter(@Nullable TransactionManager transactionManager) {
    Assert.notNull(transactionManager, "TransactionManager is required");
    this.transactionManager = transactionManager;
  }

  /**
   * Return the JTA TransactionManager that this adapter delegates to.
   */
  public final TransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  @Override
  public void setTransactionTimeout(int timeout) throws SystemException {
    this.transactionManager.setTransactionTimeout(timeout);
  }

  @Override
  public void begin() throws NotSupportedException, SystemException {
    this.transactionManager.begin();
  }

  /**
   * Begin a transaction with the JTA 2.1 read-only flag.
   *
   * @param isReadOnly whether the transaction is read-only
   * @throws NotSupportedException if an earlier JTA API does not support a read-only
   * transaction, or the provider rejects the transaction
   * @throws SystemException if the provider encounters a system error
   * @since 5.0
   */
  public void begin(boolean isReadOnly) throws NotSupportedException, SystemException {
    if (beginWithReadOnlyMethod == null) {
      if (isReadOnly) {
        throw new NotSupportedException("begin(true) requires JTA 2.1");
      }
      this.transactionManager.begin();
      return;
    }
    try {
      beginWithReadOnlyMethod.invoke(this.transactionManager, isReadOnly);
    }
    catch (Exception ex) {
      if (ex instanceof InvocationTargetException ite) {
        if (ite.getTargetException() instanceof NotSupportedException nse) {
          throw nse;
        }
        if (ite.getTargetException() instanceof SystemException se) {
          throw se;
        }
      }
      ReflectionUtils.handleReflectionException(ex);
    }
  }

  /**
   * Return the read-only status of the current JTA 2.1 transaction.
   * <p>Returns {@code false} with earlier JTA APIs.
   *
   * @return whether the transaction is read-only
   * @throws SystemException if the provider encounters a system error
   * @since 5.0
   */
  public boolean isReadOnly() throws SystemException {
    if (isReadOnlyMethod != null) {
      Transaction transaction = this.transactionManager.getTransaction();
      try {
        return (Boolean) isReadOnlyMethod.invoke(transaction);
      }
      catch (Exception ex) {
        if (ex instanceof InvocationTargetException ite && ite.getTargetException() instanceof SystemException se) {
          throw se;
        }
        ReflectionUtils.handleReflectionException(ex);
      }
    }
    return false;
  }

  @Override
  public void commit()
          throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
          SecurityException, SystemException {
    this.transactionManager.commit();
  }

  @Override
  public void rollback() throws SecurityException, SystemException {
    this.transactionManager.rollback();
  }

  @Override
  public void setRollbackOnly() throws SystemException {
    this.transactionManager.setRollbackOnly();
  }

  @Override
  public int getStatus() throws SystemException {
    return this.transactionManager.getStatus();
  }

}
