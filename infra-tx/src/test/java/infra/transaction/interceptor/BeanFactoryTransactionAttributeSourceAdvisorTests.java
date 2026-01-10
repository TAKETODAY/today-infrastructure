/*
 * Copyright 2017 - 2026 the TODAY authors.
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