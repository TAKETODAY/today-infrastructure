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

package infra.jdbc.config;

import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

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
import infra.transaction.config.TransactionAutoConfiguration;
import infra.transaction.config.TransactionManagerCustomizers;

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
public final class DataSourceTransactionManagerAutoConfiguration {

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

