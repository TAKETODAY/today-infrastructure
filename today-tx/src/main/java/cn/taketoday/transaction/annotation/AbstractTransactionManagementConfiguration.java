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

package cn.taketoday.transaction.annotation;

import java.util.Collection;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.ImportAware;
import cn.taketoday.context.annotation.Role;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.transaction.config.TransactionManagementConfigUtils;
import cn.taketoday.transaction.event.TransactionalEventListenerFactory;
import cn.taketoday.transaction.interceptor.RollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttributeSource;
import cn.taketoday.util.CollectionUtils;

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
   * Default transaction manager, as configured through a {@link cn.taketoday.transaction.annotation.TransactionManagementConfigurer}.
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
