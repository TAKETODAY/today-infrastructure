/*
 * Copyright 2012-present the original author or authors.
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

package infra.transaction.config;

import java.util.Collection;

import infra.app.LazyInitializationExcludeFilter;
import infra.context.annotation.Bean;
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
import infra.transaction.aspectj.AbstractTransactionAspect;
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
public final class TransactionAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  public static TransactionManagerCustomizers platformTransactionManagerCustomizers(
          Collection<TransactionManagerCustomizer<?>> customizers) {
    return new TransactionManagerCustomizers(customizers);
  }

  @Component
  @ConditionalOnClass(name = "reactor.core.publisher.Mono")
  @ConditionalOnMissingBean
  @ConditionalOnSingleCandidate(ReactiveTransactionManager.class)
  public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
    return TransactionalOperator.create(transactionManager);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate(PlatformTransactionManager.class)
  public static class TransactionTemplateConfiguration {

    @Component
    @ConditionalOnMissingBean(TransactionOperations.class)
    public static TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
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

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(AbstractTransactionAspect.class)
  static class AspectJTransactionManagementConfiguration {

    @Bean
    static LazyInitializationExcludeFilter eagerTransactionAspect() {
      return LazyInitializationExcludeFilter.forBeanTypes(AbstractTransactionAspect.class);
    }

  }

}
