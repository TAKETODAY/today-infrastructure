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

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import infra.beans.factory.InitializingBean;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.SqlParameter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.jdbc.support.SQLExceptionTranslator;

/**
 * An "RDBMS operation" is a multi-threaded, reusable object representing a query,
 * update, or stored procedure call. An RDBMS operation is <b>not</b> a command,
 * as a command is not reusable. However, execute methods may take commands as
 * arguments. Subclasses should be JavaBeans, allowing easy configuration.
 *
 * <p>This class and subclasses throw runtime exceptions, defined in the
 * {@code infra.dao} package (and as thrown by the
 * {@code infra.jdbc.core} package, which the classes
 * in this package use under the hood to perform raw JDBC operations).
 *
 * <p>Subclasses should set SQL and add parameters before invoking the
 * {@link #compile()} method. The order in which parameters are added is
 * significant. The appropriate {@code execute} or {@code update}
 * method can then be invoked.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SqlQuery
 * @see SqlUpdate
 * @see StoredProcedure
 * @see JdbcTemplate
 * @since 4.0
 */
public abstract class RdbmsOperation implements InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(RdbmsOperation.class);

  /** Lower-level class used to execute SQL. */
  private JdbcTemplate jdbcTemplate = new JdbcTemplate();

  private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

  private boolean updatableResults = false;

  private boolean returnGeneratedKeys = false;

  @Nullable
  private String[] generatedKeysColumnNames;

  @Nullable
  private String sql;

  private final ArrayList<SqlParameter> declaredParameters = new ArrayList<>();

  /**
   * Has this operation been compiled? Compilation means at
   * least checking that a DataSource and sql have been provided,
   * but subclasses may also implement their own custom validation.
   */
  private volatile boolean compiled;

  /**
   * An alternative to the more commonly used {@link #setDataSource} when you want to
   * use the same {@link JdbcTemplate} in multiple {@code RdbmsOperations}. This is
   * appropriate if the {@code JdbcTemplate} has special configuration such as a
   * {@link SQLExceptionTranslator} to be reused.
   */
  public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Return the {@link JdbcTemplate} used by this operation object.
   */
  public JdbcTemplate getJdbcTemplate() {
    return this.jdbcTemplate;
  }

  /**
   * Set the JDBC {@link DataSource} to obtain connections from.
   *
   * @see JdbcTemplate#setDataSource
   */
  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate.setDataSource(dataSource);
  }

  /**
   * Set the fetch size for this RDBMS operation. This is important for processing
   * large result sets: Setting this higher than the default value will increase
   * processing speed at the cost of memory consumption; setting this lower can
   * avoid transferring row data that will never be read by the application.
   * <p>Default is -1, indicating to use the driver's default.
   *
   * @see JdbcTemplate#setFetchSize
   */
  public void setFetchSize(int fetchSize) {
    this.jdbcTemplate.setFetchSize(fetchSize);
  }

  /**
   * Set the maximum number of rows for this RDBMS operation. This is important
   * for processing subsets of large result sets, avoiding to read and hold
   * the entire result set in the database or in the JDBC driver.
   * <p>Default is -1, indicating to use the driver's default.
   *
   * @see JdbcTemplate#setMaxRows
   */
  public void setMaxRows(int maxRows) {
    this.jdbcTemplate.setMaxRows(maxRows);
  }

  /**
   * Set the query timeout for statements that this RDBMS operation executes.
   * <p>Default is -1, indicating to use the JDBC driver's default.
   * <p>Note: Any timeout specified here will be overridden by the remaining
   * transaction timeout when executing within a transaction that has a
   * timeout specified at the transaction level.
   */
  public void setQueryTimeout(int queryTimeout) {
    this.jdbcTemplate.setQueryTimeout(queryTimeout);
  }

  /**
   * Set whether to use statements that return a specific type of ResultSet.
   *
   * @param resultSetType the ResultSet type
   * @see ResultSet#TYPE_FORWARD_ONLY
   * @see ResultSet#TYPE_SCROLL_INSENSITIVE
   * @see ResultSet#TYPE_SCROLL_SENSITIVE
   * @see java.sql.Connection#prepareStatement(String, int, int)
   */
  public void setResultSetType(int resultSetType) {
    this.resultSetType = resultSetType;
  }

  /**
   * Return whether statements will return a specific type of ResultSet.
   */
  public int getResultSetType() {
    return this.resultSetType;
  }

  /**
   * Set whether to use statements that are capable of returning
   * updatable ResultSets.
   *
   * @see java.sql.Connection#prepareStatement(String, int, int)
   */
  public void setUpdatableResults(boolean updatableResults) {
    if (isCompiled()) {
      throw new InvalidDataAccessApiUsageException(
              "The updateableResults flag must be set before the operation is compiled");
    }
    this.updatableResults = updatableResults;
  }

  /**
   * Return whether statements will return updatable ResultSets.
   */
  public boolean isUpdatableResults() {
    return this.updatableResults;
  }

  /**
   * Set whether prepared statements should be capable of returning
   * auto-generated keys.
   *
   * @see java.sql.Connection#prepareStatement(String, int)
   */
  public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
    if (isCompiled()) {
      throw new InvalidDataAccessApiUsageException(
              "The returnGeneratedKeys flag must be set before the operation is compiled");
    }
    this.returnGeneratedKeys = returnGeneratedKeys;
  }

  /**
   * Return whether statements should be capable of returning
   * auto-generated keys.
   */
  public boolean isReturnGeneratedKeys() {
    return this.returnGeneratedKeys;
  }

  /**
   * Set the column names of the auto-generated keys.
   *
   * @see java.sql.Connection#prepareStatement(String, String[])
   */
  public void setGeneratedKeysColumnNames(@Nullable String... names) {
    if (isCompiled()) {
      throw new InvalidDataAccessApiUsageException(
              "The column names for the generated keys must be set before the operation is compiled");
    }
    this.generatedKeysColumnNames = names;
  }

  /**
   * Return the column names of the auto generated keys.
   */
  @Nullable
  public String[] getGeneratedKeysColumnNames() {
    return this.generatedKeysColumnNames;
  }

  /**
   * Set the SQL executed by this operation.
   */
  public void setSql(@Nullable String sql) {
    this.sql = sql;
  }

  /**
   * Subclasses can override this to supply dynamic SQL if they wish, but SQL is
   * normally set by calling the {@link #setSql} method or in a subclass constructor.
   */
  @Nullable
  public String getSql() {
    return this.sql;
  }

  /**
   * Resolve the configured SQL for actual use.
   *
   * @return the SQL (never {@code null})
   */
  protected String resolveSql() {
    String sql = getSql();
    Assert.state(sql != null, "No SQL set");
    return sql;
  }

  /**
   * Add anonymous parameters, specifying only their SQL types
   * as defined in the {@code java.sql.Types} class.
   * <p>Parameter ordering is significant. This method is an alternative
   * to the {@link #declareParameter} method, which should normally be preferred.
   *
   * @param types array of SQL types as defined in the
   * {@code java.sql.Types} class
   * @throws InvalidDataAccessApiUsageException if the operation is already compiled
   */
  public void setTypes(@Nullable int[] types) throws InvalidDataAccessApiUsageException {
    if (isCompiled()) {
      throw new InvalidDataAccessApiUsageException("Cannot add parameters once query is compiled");
    }
    if (types != null) {
      for (int type : types) {
        declareParameter(new SqlParameter(type));
      }
    }
  }

  /**
   * Declare a parameter for this operation.
   * <p>The order in which this method is called is significant when using
   * positional parameters. It is not significant when using named parameters
   * with named SqlParameter objects here; it remains significant when using
   * named parameters in combination with unnamed SqlParameter objects here.
   *
   * @param param the SqlParameter to add. This will specify SQL type and (optionally)
   * the parameter's name. Note that you typically use the {@link SqlParameter} class
   * itself here, not any of its subclasses.
   * @throws InvalidDataAccessApiUsageException if the operation is already compiled,
   * and hence cannot be configured further
   */
  public void declareParameter(SqlParameter param) throws InvalidDataAccessApiUsageException {
    if (isCompiled()) {
      throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
    }
    this.declaredParameters.add(param);
  }

  /**
   * Add one or more declared parameters. Used for configuring this operation
   * when used in a bean factory.  Each parameter will specify SQL type and (optionally)
   * the parameter's name.
   *
   * @param parameters an array containing the declared {@link SqlParameter} objects
   * @see #declaredParameters
   */
  public void setParameters(SqlParameter... parameters) {
    if (isCompiled()) {
      throw new InvalidDataAccessApiUsageException("Cannot add parameters once the query is compiled");
    }
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i] != null) {
        this.declaredParameters.add(parameters[i]);
      }
      else {
        throw new InvalidDataAccessApiUsageException("Cannot add parameter at index %s from %s since it is 'null'"
                .formatted(i, Arrays.asList(parameters)));
      }
    }
  }

  /**
   * Return a list of the declared {@link SqlParameter} objects.
   */
  protected List<SqlParameter> getDeclaredParameters() {
    return this.declaredParameters;
  }

  /**
   * Ensures compilation if used in a bean factory.
   */
  @Override
  public void afterPropertiesSet() {
    compile();
  }

  /**
   * Compile this query.
   * Ignores subsequent attempts to compile.
   *
   * @throws InvalidDataAccessApiUsageException if the object hasn't
   * been correctly initialized, for example if no DataSource has been provided
   */
  public final void compile() throws InvalidDataAccessApiUsageException {
    if (!isCompiled()) {
      if (getSql() == null) {
        throw new InvalidDataAccessApiUsageException("Property 'sql' is required");
      }

      try {
        this.jdbcTemplate.afterPropertiesSet();
      }
      catch (IllegalArgumentException ex) {
        throw new InvalidDataAccessApiUsageException(ex.getMessage());
      }

      compileInternal();
      this.compiled = true;

      if (log.isDebugEnabled()) {
        log.debug("RdbmsOperation with SQL [{}] compiled", getSql());
      }
    }
  }

  /**
   * Is this operation "compiled"? Compilation, as in JDO,
   * means that the operation is fully configured, and ready to use.
   * The exact meaning of compilation will vary between subclasses.
   *
   * @return whether this operation is compiled and ready to use
   */
  public boolean isCompiled() {
    return this.compiled;
  }

  /**
   * Check whether this operation has been compiled already;
   * lazily compile it if not already compiled.
   * <p>Automatically called by {@code validateParameters}.
   *
   * @see #validateParameters
   */
  protected void checkCompiled() {
    if (!isCompiled()) {
      log.debug("SQL operation not compiled before execution - invoking compile");
      compile();
    }
  }

  /**
   * Validate the parameters passed to an execute method based on declared parameters.
   * Subclasses should invoke this method before every {@code executeQuery()}
   * or {@code update()} method.
   *
   * @param parameters the parameters supplied (may be {@code null})
   * @throws InvalidDataAccessApiUsageException if the parameters are invalid
   */
  protected void validateParameters(@Nullable Object[] parameters) throws InvalidDataAccessApiUsageException {
    checkCompiled();
    int declaredInParameters = 0;
    for (SqlParameter param : this.declaredParameters) {
      if (param.isInputValueProvided()) {
        if (!supportsLobParameters() &&
                (param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
          throw new InvalidDataAccessApiUsageException(
                  "BLOB or CLOB parameters are not allowed for this kind of operation");
        }
        declaredInParameters++;
      }
    }
    validateParameterCount((parameters != null ? parameters.length : 0), declaredInParameters);
  }

  /**
   * Validate the named parameters passed to an execute method based on declared parameters.
   * Subclasses should invoke this method before every {@code executeQuery()} or
   * {@code update()} method.
   *
   * @param parameters parameter Map supplied (may be {@code null})
   * @throws InvalidDataAccessApiUsageException if the parameters are invalid
   */
  protected void validateNamedParameters(@Nullable Map<String, ?> parameters) throws InvalidDataAccessApiUsageException {
    checkCompiled();
    Map<String, ?> paramsToUse = (parameters != null ? parameters : Collections.emptyMap());
    int declaredInParameters = 0;
    for (SqlParameter param : this.declaredParameters) {
      if (param.isInputValueProvided()) {
        if (!supportsLobParameters() &&
                (param.getSqlType() == Types.BLOB || param.getSqlType() == Types.CLOB)) {
          throw new InvalidDataAccessApiUsageException(
                  "BLOB or CLOB parameters are not allowed for this kind of operation");
        }
        if (param.getName() != null && !paramsToUse.containsKey(param.getName())) {
          throw new InvalidDataAccessApiUsageException("The parameter named '%s' was not among the parameters supplied: %s"
                  .formatted(param.getName(), paramsToUse.keySet()));
        }
        declaredInParameters++;
      }
    }
    validateParameterCount(paramsToUse.size(), declaredInParameters);
  }

  /**
   * Validate the given parameter count against the given declared parameters.
   *
   * @param suppliedParamCount the number of actual parameters given
   * @param declaredInParamCount the number of input parameters declared
   */
  private void validateParameterCount(int suppliedParamCount, int declaredInParamCount) {
    if (suppliedParamCount < declaredInParamCount) {
      throw new InvalidDataAccessApiUsageException("%s parameters were supplied, but %s in parameters were declared in class [%s]"
              .formatted(suppliedParamCount, declaredInParamCount, getClass().getName()));
    }
    if (suppliedParamCount > this.declaredParameters.size() && !allowsUnusedParameters()) {
      throw new InvalidDataAccessApiUsageException("%s parameters were supplied, but %s parameters were declared in class [%s]"
              .formatted(suppliedParamCount, declaredInParamCount, getClass().getName()));
    }
  }

  /**
   * Subclasses must implement this template method to perform their own compilation.
   * Invoked after this base class's compilation is complete.
   * <p>Subclasses can assume that SQL and a DataSource have been supplied.
   *
   * @throws InvalidDataAccessApiUsageException if the subclass hasn't been
   * properly configured
   */
  protected abstract void compileInternal() throws InvalidDataAccessApiUsageException;

  /**
   * Return whether BLOB/CLOB parameters are supported for this kind of operation.
   * <p>The default is {@code true}.
   */
  protected boolean supportsLobParameters() {
    return true;
  }

  /**
   * Return whether this operation accepts additional parameters that are
   * given but not actually used. Applies in particular to parameter Maps.
   * <p>The default is {@code false}.
   *
   * @see StoredProcedure
   */
  protected boolean allowsUnusedParameters() {
    return false;
  }

}
