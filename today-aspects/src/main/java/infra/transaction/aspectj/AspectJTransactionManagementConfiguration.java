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

package infra.transaction.aspectj;

import infra.beans.factory.config.BeanDefinition;
import infra.transaction.annotation.AbstractTransactionManagementConfiguration;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.annotation.TransactionManagementConfigurationSelector;
import infra.transaction.config.TransactionManagementConfigUtils;
import infra.transaction.interceptor.TransactionAttributeSource;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.stereotype.Component;

/**
 * {@code @Configuration} class that registers the infrastructure beans necessary
 * to enable AspectJ-based annotation-driven transaction management for Framework's own
 * {@link infra.transaction.annotation.Transactional} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableTransactionManagement
 * @see TransactionManagementConfigurationSelector
 * @see AspectJJtaTransactionManagementConfiguration
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {

  @Component(TransactionManagementConfigUtils.TRANSACTION_ASPECT_BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public AnnotationTransactionAspect transactionAspect(TransactionAttributeSource transactionAttributeSource) {
    AnnotationTransactionAspect txAspect = AnnotationTransactionAspect.aspectOf();
    txAspect.setTransactionAttributeSource(transactionAttributeSource);
    if (this.txManager != null) {
      txAspect.setTransactionManager(this.txManager);
    }
    return txAspect;
  }

}
