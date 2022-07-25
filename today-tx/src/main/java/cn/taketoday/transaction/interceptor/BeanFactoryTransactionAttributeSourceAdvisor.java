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

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractBeanFactoryPointcutAdvisor;
import cn.taketoday.lang.Nullable;

/**
 * Advisor driven by a {@link TransactionAttributeSource}, used to include
 * a transaction advice bean for methods that are transactional.
 *
 * @author Juergen Hoeller
 * @see #setAdviceBeanName
 * @see TransactionInterceptor
 * @see TransactionAttributeSourceAdvisor
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanFactoryTransactionAttributeSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

  @Nullable
  private TransactionAttributeSource transactionAttributeSource;

  private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
    @Override
    @Nullable
    protected TransactionAttributeSource getTransactionAttributeSource() {
      return transactionAttributeSource;
    }
  };

  /**
   * Set the transaction attribute source which is used to find transaction
   * attributes. This should usually be identical to the source reference
   * set on the transaction interceptor itself.
   *
   * @see TransactionInterceptor#setTransactionAttributeSource
   */
  public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
    this.transactionAttributeSource = transactionAttributeSource;
  }

  /**
   * Set the {@link ClassFilter} to use for this pointcut.
   * Default is {@link ClassFilter#TRUE}.
   */
  public void setClassFilter(ClassFilter classFilter) {
    this.pointcut.setClassFilter(classFilter);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}
