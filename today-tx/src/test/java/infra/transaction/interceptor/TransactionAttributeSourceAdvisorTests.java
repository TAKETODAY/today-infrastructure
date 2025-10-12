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

import java.util.Properties;

import infra.aop.ClassFilter;
import infra.aop.Pointcut;
import infra.aop.support.AbstractPointcutAdvisor;
import infra.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Rod Johnson
 */
public class TransactionAttributeSourceAdvisorTests {

  @Test
  public void serializability() throws Exception {
    TransactionInterceptor ti = new TransactionInterceptor();
    ti.setTransactionAttributes(new Properties());
    TransactionAttributeSourceAdvisor tas = new TransactionAttributeSourceAdvisor(ti);
    SerializationTestUtils.serializeAndDeserialize(tas);
  }

  @Test
  void constructorWithInterceptor() {
    TransactionInterceptor interceptor = new TransactionInterceptor();
    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor(interceptor);

    assertThat(advisor).isNotNull();
    assertThat(advisor.getAdvice()).isEqualTo(interceptor);
  }

  @Test
  void setTransactionInterceptor() {
    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor();
    TransactionInterceptor interceptor = new TransactionInterceptor();

    advisor.setTransactionInterceptor(interceptor);

    assertThat(advisor.getAdvice()).isEqualTo(interceptor);
  }

  @Test
  void setTransactionInterceptorWithNullThrowsException() {
    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor();

    assertThatThrownBy(() -> advisor.setTransactionInterceptor(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TransactionInterceptor is required");
  }

  @Test
  void getAdviceWithoutInterceptorThrowsException() {
    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor();

    assertThatThrownBy(() -> advisor.getAdvice())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No TransactionInterceptor set");
  }

  @Test
  void getPointcutReturnsTransactionAttributeSourcePointcut() {
    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor();

    Pointcut pointcut = advisor.getPointcut();

    assertThat(pointcut).isNotNull();
    assertThat(pointcut).isInstanceOf(TransactionAttributeSourcePointcut.class);
  }

  @Test
  void setClassFilterUpdatesPointcut() {
    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor();
    ClassFilter classFilter = ClassFilter.TRUE;

    advisor.setClassFilter(classFilter);

    Pointcut pointcut = advisor.getPointcut();
    assertThat(pointcut).isNotNull();
  }

  @Test
  void advisorExtendsAbstractPointcutAdvisor() {
    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor();
    assertThat(advisor).isInstanceOf(AbstractPointcutAdvisor.class);
  }

  @Test
  void serializabilityWithInterceptor() throws Exception {
    TransactionInterceptor interceptor = new TransactionInterceptor();
    NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
    source.addTransactionalMethod("test", new DefaultTransactionAttribute());
    interceptor.setTransactionAttributeSource(source);

    TransactionAttributeSourceAdvisor advisor = new TransactionAttributeSourceAdvisor(interceptor);

    SerializationTestUtils.serializeAndDeserialize(advisor);
  }

}
