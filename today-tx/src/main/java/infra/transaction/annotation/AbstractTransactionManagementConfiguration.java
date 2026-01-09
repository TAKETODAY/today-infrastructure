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

import org.jspecify.annotations.Nullable;

import java.util.Collection;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.ImportAware;
import infra.context.annotation.Role;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotationMetadata;
import infra.stereotype.Component;
import infra.transaction.TransactionManager;
import infra.transaction.config.TransactionManagementConfigUtils;
import infra.transaction.event.TransactionalEventListenerFactory;
import infra.transaction.interceptor.RollbackRuleAttribute;
import infra.transaction.interceptor.TransactionAttributeSource;
import infra.util.CollectionUtils;

/**
 * Abstract base {@code @Configuration} class providing common structure for enabling
 * Framework's annotation-driven transaction management capability.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableTransactionManagement
 * @since 4.0
 */
public abstract class AbstractTransactionManagementConfiguration implements ImportAware {

  @Nullable
  protected MergedAnnotation<EnableTransactionManagement> enableTx;

  /**
   * Default transaction manager, as configured through a {@link infra.transaction.annotation.TransactionManagementConfigurer}.
   */
  @Nullable
  protected TransactionManager txManager;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.enableTx = importMetadata.getAnnotation(EnableTransactionManagement.class);
    if (!enableTx.isPresent()) {
      throw new IllegalArgumentException(
              "@EnableTransactionManagement is not present on importing class " + importMetadata.getClassName());
    }
  }

  @Autowired(required = false)
  void setConfigurers(Collection<TransactionManagementConfigurer> configurers) {
    if (CollectionUtils.isEmpty(configurers)) {
      return;
    }
    if (configurers.size() > 1) {
      throw new IllegalStateException("Only one TransactionManagementConfigurer may exist");
    }
    TransactionManagementConfigurer configurer = configurers.iterator().next();
    this.txManager = configurer.annotationDrivenTransactionManager();
  }

  /**
   * @since 5.0
   */
  @Component
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public TransactionAttributeSource transactionAttributeSource() {
    // Accept protected @Transactional methods on CGLIB proxies
    AnnotationTransactionAttributeSource tas = new AnnotationTransactionAttributeSource(false);
    // Apply default rollback rule
    if (this.enableTx != null && this.enableTx.getEnum("rollbackOn", RollbackOn.class) == RollbackOn.ALL_EXCEPTIONS) {
      tas.addDefaultRollbackRule(RollbackRuleAttribute.ROLLBACK_ON_ALL_EXCEPTIONS);
    }
    return tas;
  }

  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @Component(TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)
  public static TransactionalEventListenerFactory transactionalEventListenerFactory() {
    return new RestrictedTransactionalEventListenerFactory();
  }

}
