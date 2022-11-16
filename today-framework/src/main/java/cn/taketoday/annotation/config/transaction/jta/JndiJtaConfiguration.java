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

package cn.taketoday.annotation.config.transaction.jta;

import cn.taketoday.annotation.config.transaction.TransactionManagerCustomizers;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnJndi;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.transaction.jta.JtaTransactionManager;

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

  @Bean
  JtaTransactionManager transactionManager(
          ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(jtaTransactionManager));
    return jtaTransactionManager;
  }

}