/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.io.Serializable;
import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.util.ObjectUtils;

/**
 * Abstract class that implements a Pointcut that matches if the underlying
 * {@link TransactionAttributeSource} has an attribute for a given method.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
abstract class TransactionAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

  protected TransactionAttributeSourcePointcut() {
    setClassFilter(new TransactionAttributeSourceClassFilter());
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    TransactionAttributeSource tas = getTransactionAttributeSource();
    return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TransactionAttributeSourcePointcut otherPc)) {
      return false;
    }
    return ObjectUtils.nullSafeEquals(getTransactionAttributeSource(), otherPc.getTransactionAttributeSource());
  }

  @Override
  public int hashCode() {
    return TransactionAttributeSourcePointcut.class.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + getTransactionAttributeSource();
  }

  /**
   * Obtain the underlying TransactionAttributeSource (may be {@code null}).
   * To be implemented by subclasses.
   */
  @Nullable
  protected abstract TransactionAttributeSource getTransactionAttributeSource();

  /**
   * {@link ClassFilter} that delegates to {@link TransactionAttributeSource#isCandidateClass}
   * for filtering classes whose methods are not worth searching to begin with.
   */
  private class TransactionAttributeSourceClassFilter implements ClassFilter {

    @Override
    public boolean matches(Class<?> clazz) {
      if (TransactionalProxy.class.isAssignableFrom(clazz) ||
              PlatformTransactionManager.class.isAssignableFrom(clazz) ||
              PersistenceExceptionTranslator.class.isAssignableFrom(clazz)) {
        return false;
      }
      TransactionAttributeSource tas = getTransactionAttributeSource();
      return (tas == null || tas.isCandidateClass(clazz));
    }
  }

}
