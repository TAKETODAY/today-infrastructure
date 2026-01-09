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

package infra.annotation.config.transaction.jta;

import org.jspecify.annotations.Nullable;

import infra.annotation.config.transaction.TransactionManagerCustomizers;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnJndi;
import infra.context.condition.ConditionalOnMissingBean;
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
  public JtaTransactionManager transactionManager(
          @Nullable TransactionManagerCustomizers transactionManagerCustomizers) {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    if (transactionManagerCustomizers != null) {
      transactionManagerCustomizers.customize(jtaTransactionManager);
    }
    return jtaTransactionManager;
  }

}
