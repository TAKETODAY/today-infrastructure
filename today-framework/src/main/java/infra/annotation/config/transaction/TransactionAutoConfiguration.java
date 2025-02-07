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

package infra.annotation.config.transaction;

import java.util.Collection;

import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.context.properties.EnableConfigurationProperties;
import infra.stereotype.Component;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.ReactiveTransactionManager;
import infra.transaction.TransactionManager;
import infra.transaction.annotation.AbstractTransactionManagementConfiguration;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.reactive.TransactionalOperator;
import infra.transaction.support.TransactionOperations;
import infra.transaction.support.TransactionTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Infra transaction.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Lazy
@DisableDIAutoConfiguration
@ConditionalOnClass(PlatformTransactionManager.class)
@EnableConfigurationProperties(TransactionProperties.class)
public class TransactionAutoConfiguration {

  private TransactionAutoConfiguration() {
  }

  @Component
  @ConditionalOnMissingBean
  public static TransactionManagerCustomizers platformTransactionManagerCustomizers(
          Collection<PlatformTransactionManagerCustomizer<?>> customizers) {
    return new TransactionManagerCustomizers(customizers);
  }

  @Component
  @ConditionalOnMissingBean
  @ConditionalOnSingleCandidate(ReactiveTransactionManager.class)
  public static TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
    return TransactionalOperator.create(transactionManager);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate(PlatformTransactionManager.class)
  public static class TransactionTemplateConfiguration {

    @Component
    @ConditionalOnMissingBean(TransactionOperations.class)
    static TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
      return new TransactionTemplate(transactionManager);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(TransactionManager.class)
  @ConditionalOnMissingBean(AbstractTransactionManagementConfiguration.class)
  public static class EnableTransactionManagementConfiguration {

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = false)
    @ConditionalOnProperty(prefix = "infra.aop", name = "proxy-target-class", havingValue = "false")
    public static class JdkDynamicAutoProxyConfiguration {

    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    @ConditionalOnProperty(prefix = "infra.aop", name = "proxy-target-class", havingValue = "true", matchIfMissing = true)
    public static class CglibAutoProxyConfiguration {

    }

  }

}
