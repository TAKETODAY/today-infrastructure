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

package infra.jdbc.object;

import infra.jdbc.core.PreparedStatementCreator;
import infra.jdbc.core.PreparedStatementCreatorFactory;
import infra.jdbc.core.PreparedStatementSetter;
import infra.jdbc.core.namedparam.NamedParameterUtils;
import infra.jdbc.core.namedparam.ParsedSql;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Operation object representing an SQL-based operation such as a query or update,
 * as opposed to a stored procedure.
 *
 * <p>Configures a {@link PreparedStatementCreatorFactory}
 * based on the declared parameters.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class SqlOperation extends RdbmsOperation {

  /**
   * Object enabling us to create PreparedStatementCreators efficiently,
   * based on this class's declared parameters.
   */
  @Nullable
  private PreparedStatementCreatorFactory preparedStatementFactory;

  /** Parsed representation of the SQL statement. */
  @Nullable
  private ParsedSql cachedSql;

  /** Monitor for locking the cached representation of the parsed SQL statement. */
  private final Object parsedSqlMonitor = new Object();

  /**
   * Overridden method to configure the PreparedStatementCreatorFactory
   * based on our declared parameters.
   */
  @Override
  protected final void compileInternal() {
    this.preparedStatementFactory = new PreparedStatementCreatorFactory(resolveSql(), getDeclaredParameters());
    this.preparedStatementFactory.setResultSetType(getResultSetType());
    this.preparedStatementFactory.setUpdatableResults(isUpdatableResults());
    this.preparedStatementFactory.setReturnGeneratedKeys(isReturnGeneratedKeys());
    if (getGeneratedKeysColumnNames() != null) {
      this.preparedStatementFactory.setGeneratedKeysColumnNames(getGeneratedKeysColumnNames());
    }

    onCompileInternal();
  }

  /**
   * Hook method that subclasses may override to post-process compilation.
   * This implementation does nothing.
   *
   * @see #compileInternal
   */
  protected void onCompileInternal() {
  }

  /**
   * Obtain a parsed representation of this operation's SQL statement.
   * <p>Typically used for named parameter parsing.
   */
  protected ParsedSql getParsedSql() {
    synchronized(this.parsedSqlMonitor) {
      if (this.cachedSql == null) {
        this.cachedSql = NamedParameterUtils.parseSqlStatement(resolveSql());
      }
      return this.cachedSql;
    }
  }

  /**
   * Return a PreparedStatementSetter to perform an operation
   * with the given parameters.
   *
   * @param params the parameter array (may be {@code null})
   */
  protected final PreparedStatementSetter newPreparedStatementSetter(@Nullable Object[] params) {
    Assert.state(this.preparedStatementFactory != null, "No PreparedStatementFactory available");
    return this.preparedStatementFactory.newPreparedStatementSetter(params);
  }

  /**
   * Return a PreparedStatementCreator to perform an operation
   * with the given parameters.
   *
   * @param params the parameter array (may be {@code null})
   */
  protected final PreparedStatementCreator newPreparedStatementCreator(@Nullable Object[] params) {
    Assert.state(this.preparedStatementFactory != null, "No PreparedStatementFactory available");
    return this.preparedStatementFactory.newPreparedStatementCreator(params);
  }

  /**
   * Return a PreparedStatementCreator to perform an operation
   * with the given parameters.
   *
   * @param sqlToUse the actual SQL statement to use (if different from
   * the factory's, for example because of named parameter expanding)
   * @param params the parameter array (may be {@code null})
   */
  protected final PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, @Nullable Object[] params) {
    Assert.state(this.preparedStatementFactory != null, "No PreparedStatementFactory available");
    return this.preparedStatementFactory.newPreparedStatementCreator(sqlToUse, params);
  }

}
