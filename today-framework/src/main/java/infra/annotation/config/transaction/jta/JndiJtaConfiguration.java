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

package infra.annotation.config.transaction.jta;

import infra.annotation.config.transaction.TransactionManagerCustomizers;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnJndi;
import infra.context.condition.ConditionalOnMissingBean;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.transaction.TransactionManager;
import infra.transaction.jta.JtaTransactionManager;

/**
 * JTA Configuration for a JNDI-managed {@link JtaTransactionManager}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JtaTransactionManager.class)
@ConditionalOnJndi({ JtaTransactionManager.DEFAULT_USER_TRANSACTION_NAME, "java:comp/TransactionManager",
        "java:appserver/TransactionManager", "java:pm/TransactionManager", "java:/TransactionManager" })
@ConditionalOnMissingBean(TransactionManager.class)
class JndiJtaConfiguration {

  @Component
  static JtaTransactionManager transactionManager(
          @Nullable TransactionManagerCustomizers transactionManagerCustomizers) {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    if (transactionManagerCustomizers != null) {
      transactionManagerCustomizers.customize(jtaTransactionManager);
    }
    return jtaTransactionManager;
  }

}
