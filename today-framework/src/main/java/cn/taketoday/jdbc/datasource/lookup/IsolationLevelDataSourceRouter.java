/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource.lookup;

import cn.taketoday.core.Constants;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.support.DefaultTransactionDefinition;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * DataSource that routes to one of various target DataSources based on the
 * current transaction isolation level. The target DataSources need to be
 * configured with the isolation level name as key, as defined on the
 * {@link cn.taketoday.transaction.TransactionDefinition TransactionDefinition interface}.
 *
 * <p>This is particularly useful in combination with JTA transaction management
 * (typically through  {@link cn.taketoday.transaction.jta.JtaTransactionManager}).
 * Standard JTA does not support transaction-specific isolation levels. Some JTA
 * providers support isolation levels as a vendor-specific extension (e.g. WebLogic),
 * which is the preferred way of addressing this. As alternative (e.g. on WebSphere),
 * the target database can be represented through multiple JNDI DataSources, each
 * configured with a different isolation level (for the entire DataSource).
 * The present DataSource router allows to transparently switch to the
 * appropriate DataSource based on the current transaction's isolation level.
 *
 * <p>The configuration can for example look like this, assuming that the target
 * DataSources are defined as individual Framework beans with names
 * "myRepeatableReadDataSource", "mySerializableDataSource" and "myDefaultDataSource":
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="cn.taketoday.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value-ref="myRepeatableReadDataSource"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value-ref="mySerializableDataSource"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" ref="myDefaultDataSource"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Alternatively, the keyed values can also be data source names, to be resolved
 * through a {@link #setDataSourceLookup DataSourceLookup}: by default, JNDI
 * names for a standard JNDI lookup. This allows for a single concise definition
 * without the need for separate DataSource bean definitions.
 *
 * <pre class="code">
 * &lt;bean id="dataSourceRouter" class="cn.taketoday.jdbc.datasource.lookup.IsolationLevelDataSourceRouter"&gt;
 *   &lt;property name="targetDataSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value="java:comp/env/jdbc/myrrds"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value="java:comp/env/jdbc/myserds"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetDataSource" value="java:comp/env/jdbc/mydefds"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Note: If you are using this router in combination with
 * {@link cn.taketoday.transaction.jta.JtaTransactionManager},
 * don't forget to switch the "allowCustomIsolationLevels" flag to "true".
 * (By default, JtaTransactionManager will only accept a default isolation level
 * because of the lack of isolation level support in standard JTA itself.)
 *
 * <pre class="code">
 * &lt;bean id="transactionManager" class="cn.taketoday.transaction.jta.JtaTransactionManager"&gt;
 *   &lt;property name="allowCustomIsolationLevels" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @see #setTargetDataSources
 * @see #setDefaultTargetDataSource
 * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
 * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
 * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
 * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
 * @see cn.taketoday.transaction.jta.JtaTransactionManager
 * @since 4.0
 */
public class IsolationLevelDataSourceRouter extends AbstractRoutingDataSource {

  /** Constants instance for TransactionDefinition. */
  private static final Constants constants = new Constants(TransactionDefinition.class);

  /**
   * Supports Integer values for the isolation level constants
   * as well as isolation level names as defined on the
   * {@link cn.taketoday.transaction.TransactionDefinition TransactionDefinition interface}.
   */
  @Override
  protected Object resolveSpecifiedLookupKey(Object lookupKey) {
    if (lookupKey instanceof Integer) {
      return lookupKey;
    }
    else if (lookupKey instanceof String constantName) {
      if (!constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
        throw new IllegalArgumentException("Only isolation constants allowed");
      }
      return constants.asNumber(constantName);
    }
    else {
      throw new IllegalArgumentException(
              "Invalid lookup key - needs to be isolation level Integer or isolation level name String: " + lookupKey);
    }
  }

  @Override
  @Nullable
  protected Object determineCurrentLookupKey() {
    return TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
  }

}
