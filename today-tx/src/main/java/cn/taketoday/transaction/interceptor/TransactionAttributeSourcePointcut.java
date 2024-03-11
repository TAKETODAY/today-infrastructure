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

package cn.taketoday.transaction.interceptor;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.util.ObjectUtils;

/**
 * Abstract class that implements a Pointcut that matches if the underlying
 * {@link TransactionAttributeSource} has an attribute for a given method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private TransactionAttributeSource transactionAttributeSource;

  public TransactionAttributeSourcePointcut() {
    setClassFilter(new TransactionAttributeSourceClassFilter());
  }

  public void setTransactionAttributeSource(@Nullable TransactionAttributeSource transactionAttributeSource) {
    this.transactionAttributeSource = transactionAttributeSource;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return (this.transactionAttributeSource == null ||
            this.transactionAttributeSource.hasTransactionAttribute(method, targetClass));
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof TransactionAttributeSourcePointcut that &&
            ObjectUtils.nullSafeEquals(this.transactionAttributeSource, that.transactionAttributeSource)));
  }

  @Override
  public int hashCode() {
    return TransactionAttributeSourcePointcut.class.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.transactionAttributeSource;
  }

  /**
   * {@link ClassFilter} that delegates to {@link TransactionAttributeSource#isCandidateClass}
   * for filtering classes whose methods are not worth searching to begin with.
   */
  private final class TransactionAttributeSourceClassFilter implements ClassFilter {

    @Override
    public boolean matches(Class<?> clazz) {
      if (TransactionalProxy.class.isAssignableFrom(clazz) ||
              TransactionManager.class.isAssignableFrom(clazz) ||
              PersistenceExceptionTranslator.class.isAssignableFrom(clazz)) {
        return false;
      }
      return (transactionAttributeSource == null || transactionAttributeSource.isCandidateClass(clazz));
    }

    @Nullable
    private TransactionAttributeSource getTransactionAttributeSource() {
      return transactionAttributeSource;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof TransactionAttributeSourceClassFilter that &&
              ObjectUtils.nullSafeEquals(getTransactionAttributeSource(), that.getTransactionAttributeSource())));
    }

    @Override
    public int hashCode() {
      return TransactionAttributeSourceClassFilter.class.hashCode();
    }

    @Override
    public String toString() {
      return TransactionAttributeSourceClassFilter.class.getName() + ": " + getTransactionAttributeSource();
    }
  }

}
