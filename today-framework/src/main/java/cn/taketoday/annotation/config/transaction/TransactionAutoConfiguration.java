/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.transaction;

import cn.taketoday.annotation.config.jdbc.DataSourceTransactionManagerAutoConfiguration;
import cn.taketoday.annotation.config.jpa.HibernateJpaAutoConfiguration;
import cn.taketoday.annotation.config.transaction.jta.JtaAutoConfiguration;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.transaction.annotation.AbstractTransactionManagementConfiguration;
import cn.taketoday.transaction.annotation.EnableTransactionManagement;
import cn.taketoday.transaction.reactive.TransactionalOperator;
import cn.taketoday.transaction.support.TransactionOperations;
import cn.taketoday.transaction.support.TransactionTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Infra transaction.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration(after = {
        JtaAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@ConditionalOnClass(PlatformTransactionManager.class)
@EnableConfigurationProperties(TransactionProperties.class)
public class TransactionAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public TransactionManagerCustomizers platformTransactionManagerCustomizers(
          ObjectProvider<PlatformTransactionManagerCustomizer<?>> customizers) {
    return new TransactionManagerCustomizers(customizers.orderedStream().toList());
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnSingleCandidate(ReactiveTransactionManager.class)
  public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
    return TransactionalOperator.create(transactionManager);
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate(PlatformTransactionManager.class)
  public static class TransactionTemplateConfiguration {

    @Bean
    @ConditionalOnMissingBean(TransactionOperations.class)
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
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
