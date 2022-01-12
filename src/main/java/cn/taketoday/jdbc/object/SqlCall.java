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

package cn.taketoday.jdbc.object;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.jdbc.core.CallableStatementCreator;
import cn.taketoday.jdbc.core.CallableStatementCreatorFactory;
import cn.taketoday.jdbc.core.ParameterMapper;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * RdbmsOperation using a JdbcTemplate and representing an SQL-based
 * call such as a stored procedure or a stored function.
 *
 * <p>Configures a CallableStatementCreatorFactory based on the declared
 * parameters.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see CallableStatementCreatorFactory
 */
public abstract class SqlCall extends RdbmsOperation {
  private static final Logger log = LoggerFactory.getLogger(SqlCall.class);

  /**
   * Flag used to indicate that this call is for a function and to
   * use the {? = call get_invoice_count(?)} syntax.
   */
  private boolean function = false;

  /**
   * Flag used to indicate that the sql for this call should be used exactly as
   * it is defined. No need to add the escape syntax and parameter place holders.
   */
  private boolean sqlReadyForUse = false;

  /**
   * Call string as defined in java.sql.CallableStatement.
   * String of form {call add_invoice(?, ?, ?)} or {? = call get_invoice_count(?)}
   * if isFunction is set to true. Updated after each parameter is added.
   */
  @Nullable
  private String callString;

  /**
   * Object enabling us to create CallableStatementCreators
   * efficiently, based on this class's declared parameters.
   */
  @Nullable
  private CallableStatementCreatorFactory callableStatementFactory;

  /**
   * Constructor to allow use as a JavaBean.
   * A DataSource, SQL and any parameters must be supplied before
   * invoking the {@code compile} method and using this object.
   *
   * @see #setDataSource
   * @see #setSql
   * @see #compile
   */
  public SqlCall() { }

  /**
   * Create a new SqlCall object with SQL, but without parameters.
   * Must add parameters or settle with none.
   *
   * @param ds the DataSource to obtain connections from
   * @param sql the SQL to execute
   */
  public SqlCall(DataSource ds, String sql) {
    setDataSource(ds);
    setSql(sql);
  }

  /**
   * Set whether this call is for a function.
   */
  public void setFunction(boolean function) {
    this.function = function;
  }

  /**
   * Return whether this call is for a function.
   */
  public boolean isFunction() {
    return this.function;
  }

  /**
   * Set whether the SQL can be used as is.
   */
  public void setSqlReadyForUse(boolean sqlReadyForUse) {
    this.sqlReadyForUse = sqlReadyForUse;
  }

  /**
   * Return whether the SQL can be used as is.
   */
  public boolean isSqlReadyForUse() {
    return this.sqlReadyForUse;
  }

  /**
   * Overridden method to configure the CallableStatementCreatorFactory
   * based on our declared parameters.
   *
   * @see RdbmsOperation#compileInternal()
   */
  @Override
  protected final void compileInternal() {
    if (isSqlReadyForUse()) {
      this.callString = resolveSql();
    }
    else {
      StringBuilder callString = new StringBuilder(32);
      List<SqlParameter> parameters = getDeclaredParameters();
      int parameterCount = 0;
      if (isFunction()) {
        callString.append("{? = call ").append(resolveSql()).append('(');
        parameterCount = -1;
      }
      else {
        callString.append("{call ").append(resolveSql()).append('(');
      }
      for (SqlParameter parameter : parameters) {
        if (!parameter.isResultsParameter()) {
          if (parameterCount > 0) {
            callString.append(", ");
          }
          if (parameterCount >= 0) {
            callString.append('?');
          }
          parameterCount++;
        }
      }
      callString.append(")}");
      this.callString = callString.toString();
    }
    if (log.isDebugEnabled()) {
      log.debug("Compiled stored procedure. Call string is [{}]", callString);
    }

    this.callableStatementFactory = new CallableStatementCreatorFactory(this.callString, getDeclaredParameters());
    this.callableStatementFactory.setResultSetType(getResultSetType());
    this.callableStatementFactory.setUpdatableResults(isUpdatableResults());

    onCompileInternal();
  }

  /**
   * Hook method that subclasses may override to react to compilation.
   * This implementation does nothing.
   */
  protected void onCompileInternal() { }

  /**
   * Get the call string.
   */
  @Nullable
  public String getCallString() {
    return this.callString;
  }

  /**
   * Return a CallableStatementCreator to perform an operation
   * with this parameters.
   *
   * @param inParams parameters. May be {@code null}.
   */
  protected CallableStatementCreator newCallableStatementCreator(@Nullable Map<String, ?> inParams) {
    Assert.state(this.callableStatementFactory != null, "No CallableStatementFactory available");
    return this.callableStatementFactory.newCallableStatementCreator(inParams);
  }

  /**
   * Return a CallableStatementCreator to perform an operation
   * with the parameters returned from this ParameterMapper.
   *
   * @param inParamMapper parametermapper. May not be {@code null}.
   */
  protected CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
    Assert.state(this.callableStatementFactory != null, "No CallableStatementFactory available");
    return this.callableStatementFactory.newCallableStatementCreator(inParamMapper);
  }

}
