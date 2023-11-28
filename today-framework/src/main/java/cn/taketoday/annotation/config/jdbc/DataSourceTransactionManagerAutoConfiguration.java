/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.jdbc;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.transaction.TransactionAutoConfiguration;
import cn.taketoday.annotation.config.transaction.TransactionManagerCustomizers;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigureOrder;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.support.JdbcTransactionManager;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;
import cn.taketoday.transaction.TransactionManager;

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
@EnableConfigurationProperties(DataSourceProperties.class)
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

