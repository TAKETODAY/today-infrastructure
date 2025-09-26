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

package infra.annotation.config.jdbc;

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

import infra.annotation.config.transaction.TransactionAutoConfiguration;
import infra.annotation.config.transaction.TransactionManagerCustomizers;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.core.Ordered;
import infra.core.env.Environment;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DataSourceTransactionManager;
import infra.jdbc.support.JdbcTransactionManager;
import infra.stereotype.Component;
import infra.transaction.TransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JdbcTransactionManager}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 11:51
 */
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@DisableDIAutoConfiguration(before = TransactionAutoConfiguration.class)
@ConditionalOnClass({ JdbcTemplate.class, TransactionManager.class })
public class DataSourceTransactionManagerAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnSingleCandidate(DataSource.class)
  static class JdbcTransactionManagerConfiguration {

    @Component
    @ConditionalOnMissingBean(TransactionManager.class)
    static DataSourceTransactionManager transactionManager(Environment environment, DataSource dataSource,
            @Nullable TransactionManagerCustomizers transactionManagerCustomizers) {
      DataSourceTransactionManager transactionManager = createTransactionManager(environment, dataSource);
      if (transactionManagerCustomizers != null) {
        transactionManagerCustomizers.customize(transactionManager);
      }
      return transactionManager;
    }

    private static DataSourceTransactionManager createTransactionManager(Environment environment, DataSource dataSource) {
      return environment.getFlag("dao.exceptiontranslation.enabled", true)
              ? new JdbcTransactionManager(dataSource)
              : new DataSourceTransactionManager(dataSource);
    }

  }

}

