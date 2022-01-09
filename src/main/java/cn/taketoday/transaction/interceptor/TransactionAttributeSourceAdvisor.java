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

import org.aopalliance.aop.Advice;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractPointcutAdvisor;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Advisor driven by a {@link TransactionAttributeSource}, used to include
 * a {@link TransactionInterceptor} only for methods that are transactional.
 *
 * <p>Because the AOP framework caches advice calculations, this is normally
 * faster than just letting the TransactionInterceptor run and find out
 * itself that it has no work to do.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setTransactionInterceptor
 * @see TransactionProxyFactoryBean
 */
@SuppressWarnings("serial")
public class TransactionAttributeSourceAdvisor extends AbstractPointcutAdvisor {

  @Nullable
  private TransactionInterceptor transactionInterceptor;

  private final TransactionAttributeSourcePointcut pointcut = new TransactionAttributeSourcePointcut() {
    @Override
    @Nullable
    protected TransactionAttributeSource getTransactionAttributeSource() {
      return (transactionInterceptor != null ? transactionInterceptor.getTransactionAttributeSource() : null);
    }
  };

  /**
   * Create a new TransactionAttributeSourceAdvisor.
   */
  public TransactionAttributeSourceAdvisor() { }

  /**
   * Create a new TransactionAttributeSourceAdvisor.
   *
   * @param interceptor the transaction interceptor to use for this advisor
   */
  public TransactionAttributeSourceAdvisor(TransactionInterceptor interceptor) {
    setTransactionInterceptor(interceptor);
  }

  /**
   * Set the transaction interceptor to use for this advisor.
   */
  public void setTransactionInterceptor(TransactionInterceptor interceptor) {
    this.transactionInterceptor = interceptor;
  }

  /**
   * Set the {@link ClassFilter} to use for this pointcut.
   * Default is {@link ClassFilter#TRUE}.
   */
  public void setClassFilter(ClassFilter classFilter) {
    this.pointcut.setClassFilter(classFilter);
  }

  @Override
  public Advice getAdvice() {
    Assert.state(this.transactionInterceptor != null, "No TransactionInterceptor set");
    return this.transactionInterceptor;
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}
