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

package infra.jdbc.datasource.lookup;

import java.util.Map;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.transaction.TransactionDefinition;
import infra.transaction.support.TransactionSynchronizationManager;

/**
 * DataSource that routes to one of various target DataSources based on the
 * current transaction isolation level. The target DataSources need to be
 * configured with the isolation level name as key, as defined on the
 * {@link infra.transaction.TransactionDefinition TransactionDefinition}
 * interface.
 *
 * <p>This is particularly useful in combination with JTA transaction management
 * (typically through Infra {@link infra.transaction.jta.JtaTransactionManager}).
 * Standard JTA does not support transaction-specific isolation levels. Some JTA
 * providers support isolation levels as a vendor-specific extension (e.g. WebLogic),
 * which is the preferred way of addressing this. As an alternative (e.g. on WebSphere),
 * the target database can be represented through multiple JNDI DataSources, each
 * configured with a different isolation level (for the entire DataSource).
 * {@code IsolationLevelDataSourceRouter} allows to transparently switch to the
 * appropriate DataSource based on the current transaction's isolation level.
 *
 * <p>For example, the configuration can look like the following, assuming that
 * the target DataSources are defined as individual Infra beans with names
 * "myRepeatableReadDataSource", "mySerializableDataSource", and "myDefaultDataSource":
 *
 * <pre>{@code
 * <bean id="dataSourceRouter" class="infra.jdbc.datasource.lookup.IsolationLevelDataSourceRouter">
 *   <property name="targetDataSources">
 *     <map>
 *       <entry key="ISOLATION_REPEATABLE_READ" value-ref="myRepeatableReadDataSource"/>
 *       <entry key="ISOLATION_SERIALIZABLE" value-ref="mySerializableDataSource"/>
 *     </map>
 *   </property>
 *   <property name="defaultTargetDataSource" ref="myDefaultDataSource"/>
 * </bean>
 * }</pre>
 *
 * Alternatively, the keyed values can also be data source names, to be resolved
 * through a {@link #setDataSourceLookup DataSourceLookup}: by default, JNDI
 * names for a standard JNDI lookup. This allows for a single concise definition
 * without the need for separate DataSource bean definitions.
 *
 * <pre>{@code
 * <bean id="dataSourceRouter" class="infra.jdbc.datasource.lookup.IsolationLevelDataSourceRouter">
 *   <property name="targetDataSources">
 *     <map>
 *       <entry key="ISOLATION_REPEATABLE_READ" value="java:comp/env/jdbc/myrrds"/>
 *       <entry key="ISOLATION_SERIALIZABLE" value="java:comp/env/jdbc/myserds"/>
 *     </map>
 *   </property>
 *   <property name="defaultTargetDataSource" value="java:comp/env/jdbc/mydefds"/>
 * </bean>
 * }</pre>
 *
 * Note: If you are using this router in combination with Infra
 * {@link infra.transaction.jta.JtaTransactionManager},
 * don't forget to switch the "allowCustomIsolationLevels" flag to "true".
 * (By default, JtaTransactionManager will only accept a default isolation level
 * because of the lack of isolation level support in standard JTA itself.)
 *
 * <pre>{@code
 * <bean id="transactionManager" class="infra.transaction.jta.JtaTransactionManager">
 *   <property name="allowCustomIsolationLevels" value="true"/>
 * </bean>
 * }</pre>
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setTargetDataSources
 * @see #setDefaultTargetDataSource
 * @see infra.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
 * @see infra.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
 * @see infra.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
 * @see infra.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
 * @see infra.transaction.jta.JtaTransactionManager
 * @since 4.0
 */
public class IsolationLevelDataSourceRouter extends AbstractRoutingDataSource {

  /**
   * Map of constant names to constant values for the isolation constants
   * defined in {@link TransactionDefinition}.
   */
  static final Map<String, Integer> constants = Map.of(
      "ISOLATION_DEFAULT", TransactionDefinition.ISOLATION_DEFAULT,
      "ISOLATION_READ_UNCOMMITTED", TransactionDefinition.ISOLATION_READ_UNCOMMITTED,
      "ISOLATION_READ_COMMITTED", TransactionDefinition.ISOLATION_READ_COMMITTED,
      "ISOLATION_REPEATABLE_READ", TransactionDefinition.ISOLATION_REPEATABLE_READ,
      "ISOLATION_SERIALIZABLE", TransactionDefinition.ISOLATION_SERIALIZABLE
  );

  /**
   * Supports Integer values for the isolation level constants
   * as well as isolation level names as defined on the
   * {@link infra.transaction.TransactionDefinition TransactionDefinition interface}.
   */
  @Override
  protected Object resolveSpecifiedLookupKey(Object lookupKey) {
    if (lookupKey instanceof Integer isolationLevel) {
      Assert.isTrue(constants.containsValue(isolationLevel),
          "Only values of isolation constants allowed");
      return isolationLevel;
    }
    else if (lookupKey instanceof String constantName) {
      Assert.hasText(constantName, "'lookupKey' must not be null or blank");
      Integer isolationLevel = constants.get(constantName);
      Assert.notNull(isolationLevel, "Only isolation constants allowed");
      return isolationLevel;
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
