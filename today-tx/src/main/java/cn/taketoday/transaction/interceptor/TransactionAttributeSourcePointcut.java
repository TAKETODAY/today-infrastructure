/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.transaction.interceptor;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionManager;

/**
 * Abstract class that implements a Pointcut that matches if the underlying
 * {@link TransactionAttributeSource} has an attribute for a given method.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

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
    return transactionAttributeSource == null
            || transactionAttributeSource.getTransactionAttribute(method, targetClass) != null;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof TransactionAttributeSourcePointcut otherPc &&
            Objects.equals(this.transactionAttributeSource, otherPc.transactionAttributeSource)));
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
  private class TransactionAttributeSourceClassFilter implements ClassFilter {

    @Override
    public boolean matches(Class<?> clazz) {
      if (TransactionalProxy.class.isAssignableFrom(clazz)
              || TransactionManager.class.isAssignableFrom(clazz)
              || PersistenceExceptionTranslator.class.isAssignableFrom(clazz)) {
        return false;
      }
      return transactionAttributeSource == null || transactionAttributeSource.isCandidateClass(clazz);
    }
  }

}
