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
