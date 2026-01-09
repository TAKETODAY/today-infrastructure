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

package infra.transaction.aspectj;

import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Configuration;
import infra.context.annotation.Role;
import infra.stereotype.Component;
import infra.transaction.annotation.AbstractTransactionManagementConfiguration;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.annotation.TransactionManagementConfigurationSelector;
import infra.transaction.config.TransactionManagementConfigUtils;
import infra.transaction.interceptor.TransactionAttributeSource;

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
