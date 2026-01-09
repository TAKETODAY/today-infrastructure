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

package infra.transaction.annotation;

import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.annotation.Role;
import infra.stereotype.Component;
import infra.transaction.config.TransactionManagementConfigUtils;
import infra.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import infra.transaction.interceptor.TransactionAttributeSource;
import infra.transaction.interceptor.TransactionInterceptor;

/**
 * {@code @Configuration} class that registers the Framework infrastructure beans
 * necessary to enable proxy-based annotation-driven transaction management.
 *
 * @author Chris Beams
 * @author Sebastien Deleuze
 * @see EnableTransactionManagement
 * @see TransactionManagementConfigurationSelector
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ImportRuntimeHints(TransactionRuntimeHints.class)
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
  public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(
          TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {

    BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
    advisor.setTransactionAttributeSource(transactionAttributeSource);
    advisor.setAdvice(transactionInterceptor);
    if (this.enableTx != null) {
      advisor.setOrder(this.enableTx.getInt("order"));
    }
    return advisor;
  }

  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public TransactionInterceptor transactionInterceptor(TransactionAttributeSource transactionAttributeSource) {
    TransactionInterceptor interceptor = new TransactionInterceptor();
    interceptor.setTransactionAttributeSource(transactionAttributeSource);
    if (this.txManager != null) {
      interceptor.setTransactionManager(this.txManager);
    }
    return interceptor;
  }

}
