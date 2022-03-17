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

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Very simple implementation of TransactionAttributeSource which will always return
 * the same TransactionAttribute for all methods fed to it. The TransactionAttribute
 * may be specified, but will otherwise default to PROPAGATION_REQUIRED. This may be
 * used in the cases where you want to use the same transaction attribute with all
 * methods being handled by a transaction interceptor.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @see cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean
 * @see cn.taketoday.aop.proxy.BeanNameAutoProxyCreator
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MatchAlwaysTransactionAttributeSource implements TransactionAttributeSource, Serializable {

  private TransactionAttribute transactionAttribute = new DefaultTransactionAttribute();

  /**
   * Allows a transaction attribute to be specified, using the String form, for
   * example, "PROPAGATION_REQUIRED".
   *
   * @param transactionAttribute the String form of the transactionAttribute to use.
   * @see TransactionAttribute#parse(String)
   */
  public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
    if (transactionAttribute instanceof DefaultTransactionAttribute dta) {
      dta.resolveAttributeStrings(null);
    }
    this.transactionAttribute = transactionAttribute;
  }

  @Override
  @Nullable
  public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
    return (ClassUtils.isUserLevelMethod(method) ? this.transactionAttribute : null);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MatchAlwaysTransactionAttributeSource otherTas)) {
      return false;
    }
    return ObjectUtils.nullSafeEquals(this.transactionAttribute, otherTas.transactionAttribute);
  }

  @Override
  public int hashCode() {
    return MatchAlwaysTransactionAttributeSource.class.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.transactionAttribute;
  }

}
