/*
 * Copyright 2017 - 2024 the original author or authors.
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
