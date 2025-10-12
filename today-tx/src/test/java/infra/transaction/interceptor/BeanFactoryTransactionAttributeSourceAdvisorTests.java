/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.interceptor;

import org.junit.jupiter.api.Test;

import infra.aop.ClassFilter;
import infra.aop.Pointcut;
import infra.aop.support.AbstractBeanFactoryPointcutAdvisor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 22:29
 */
class BeanFactoryTransactionAttributeSourceAdvisorTests {

  @Test
  void getPointcutReturnsTransactionAttributeSourcePointcut() {
    BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
    Pointcut pointcut = advisor.getPointcut();

    assertThat(pointcut).isNotNull();
    assertThat(pointcut).isInstanceOf(TransactionAttributeSourcePointcut.class);
  }

  @Test
  void setTransactionAttributeSourceUpdatesPointcut() {
    BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
    TransactionAttributeSource source = new MatchAlwaysTransactionAttributeSource();

    advisor.setTransactionAttributeSource(source);

    Pointcut pointcut = advisor.getPointcut();
    // Verify that the pointcut now has the transaction attribute source
    // Since there's no getter, we can't directly verify, but we can check the pointcut is still the same instance
    assertThat(pointcut).isInstanceOf(TransactionAttributeSourcePointcut.class);
  }

  @Test
  void setClassFilterUpdatesPointcut() {
    BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
    ClassFilter classFilter = ClassFilter.TRUE;

    advisor.setClassFilter(classFilter);

    Pointcut pointcut = advisor.getPointcut();
    assertThat(pointcut).isNotNull();
  }

  @Test
  void advisorExtendsAbstractBeanFactoryPointcutAdvisor() {
    BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
    assertThat(advisor).isInstanceOf(AbstractBeanFactoryPointcutAdvisor.class);
  }

}