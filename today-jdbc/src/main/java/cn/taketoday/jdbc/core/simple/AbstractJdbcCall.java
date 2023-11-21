/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.core.simple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.core.CallableStatementCreator;
import cn.taketoday.jdbc.core.CallableStatementCreatorFactory;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.core.RowMapper;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.jdbc.core.metadata.CallMetaDataContext;
import cn.taketoday.jdbc.core.namedparam.SqlParameterSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Abstract class to provide base functionality for easy stored procedure calls
 * based on configuration options and database meta-data.
 *
 * <p>This class provides the base SPI for {@link SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractJdbcCall {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /** Lower-level class used to execute SQL. */
  private final JdbcTemplate jdbcTemplate;

  /** Context used to retrieve and manage database meta-data. */
  private final CallMetaDataContext callMetaDataContext = new CallMetaDataContext();

  /** List of SqlParameter objects. */
  private final List<SqlParameter> declaredParameters = new ArrayList<>();

  /** List of RefCursor/ResultSet RowMapper objects. */
  private final Map<String, RowMapper<?>> declaredRowMappers = new LinkedHashMap<>();

  /**
   * Has this operation been compiled? Compilation means at least checking
   * that a DataSource or JdbcTemplate has been provided.
   */
  private volatile boolean compiled;

  /** The generated string used for call statement. */
  @Nullable
  private String callString;

  /**
   * A delegate enabling us to create CallableStatementCreators
   * efficiently, based on this class's declared parameters.
   */
  @Nullable
  private CallableStatementCreatorFactory callableStatementFactory;

  /**
   * Constructor to be used when initializing using a {@link DataSource}.
   *
   * @param dataSource the DataSource to be used
   */
  protected AbstractJdbcCall(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Constructor to be used when initializing using a {@link JdbcTemplate}.
   *
   * @param jdbcTemplate the JdbcTemplate to use
   */
  protected AbstractJdbcCall(JdbcTemplate jdbcTemplate) {
    Assert.notNull(jdbcTemplate, "JdbcTemplate is required");
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Get the configured {@link JdbcTemplate}.
   */
  public JdbcTemplate getJdbcTemplate() {
    return this.jdbcTemplate;
  }

  /**
   * Set the name of the stored procedure.
   */
  public void setProcedureName(@Nullable String procedureName) {
    this.callMetaDataContext.setProcedureName(procedureName);
  }

  /**
   * Get the name of the stored procedure.
   */
  @Nullable
  public String getProcedureName() {
    return this.callMetaDataContext.getProcedureName();
  }

  /**
   * Set the names of in parameters to be used.
   */
  public void setInParameterNames(Set<String> inParameterNames) {
    this.callMetaDataContext.setLimitedInParameterNames(inParameterNames);
  }

  /**
   * Get the names of in parameters to be used.
   */
  public Set<String> getInParameterNames() {
    return this.callMetaDataContext.getLimitedInParameterNames();
  }

  /**
   * Set the catalog name to use.
   */
  public void setCatalogName(@Nullable String catalogName) {
    this.callMetaDataContext.setCatalogName(catalogName);
  }

  /**
   * Get the catalog name used.
   */
  @Nullable
  public String getCatalogName() {
    return this.callMetaDataContext.getCatalogName();
  }

  /**
   * Set the schema name to use.
   */
  public void setSchemaName(@Nullable String schemaName) {
    this.callMetaDataContext.setSchemaName(schemaName);
  }

  /**
   * Get the schema name used.
   */
  @Nullable
  public String getSchemaName() {
    return this.callMetaDataContext.getSchemaName();
  }

  /**
   * Specify whether this call is a function call.
   * The default is {@code false}.
   */
  public void setFunction(boolean function) {
    this.callMetaDataContext.setFunction(function);
  }

  /**
   * Is this call a function call?
   */
  public boolean isFunction() {
    return this.callMetaDataContext.isFunction();
  }

  /**
   * Specify whether the call requires a return value.
   * The default is {@code false}.
   */
  public void setReturnValueRequired(boolean returnValueRequired) {
    this.callMetaDataContext.setReturnValueRequired(returnValueRequired);
  }

  /**
   * Does the call require a return value?
   */
  public boolean isReturnValueRequired() {
    return this.callMetaDataContext.isReturnValueRequired();
  }

  /**
   * Specify whether parameters should be bound by name.
   * The default is {@code false}.
   *
   * @since 4.0
   */
  public void setNamedBinding(boolean namedBinding) {
    this.callMetaDataContext.setNamedBinding(namedBinding);
  }

  /**
   * Should parameters be bound by name?
   *
   * @since 4.0
   */
  public boolean isNamedBinding() {
    return this.callMetaDataContext.isNamedBinding();
  }

  /**
   * Specify whether the parameter meta-data for the call should be used.
   * The default is {@code true}.
   */
  public void setAccessCallParameterMetaData(boolean accessCallParameterMetaData) {
    this.callMetaDataContext.setAccessCallParameterMetaData(accessCallParameterMetaData);
  }

  /**
   * Get the call string that should be used based on parameters and meta-data.
   */
  @Nullable
  public String getCallString() {
    return this.callString;
  }

  /**
   * Get the {@link CallableStatementCreatorFactory} being used.
   */
  protected CallableStatementCreatorFactory getCallableStatementFactory() {
    Assert.state(this.callableStatementFactory != null, "No CallableStatementCreatorFactory available");
    return this.callableStatementFactory;
  }

  /**
   * Add a declared parameter to the list of parameters for the call.
   * <p>Only parameters declared as {@code SqlParameter} and {@code SqlInOutParameter} will
   * be used to provide input values. This is different from the {@code StoredProcedure}
   * class which - for backwards compatibility reasons - allows input values to be provided
   * for parameters declared as {@code SqlOutParameter}.
   *
   * @param parameter the {@link SqlParameter} to add
   */
  public void addDeclaredParameter(SqlParameter parameter) {
    Assert.notNull(parameter, "The supplied parameter is required");
    if (StringUtils.isBlank(parameter.getName())) {
      throw new InvalidDataAccessApiUsageException(
              "You must specify a parameter name when declaring parameters for \"" + getProcedureName() + "\"");
    }
    this.declaredParameters.add(parameter);
    if (logger.isDebugEnabled()) {
      logger.debug("Added declared parameter for [" + getProcedureName() + "]: " + parameter.getName());
    }
  }

  /**
   * Add a {@link RowMapper} for the specified parameter or column.
   *
   * @param parameterName name of parameter or column
   * @param rowMapper the RowMapper implementation to use
   */
  public void addDeclaredRowMapper(String parameterName, RowMapper<?> rowMapper) {
    this.declaredRowMappers.put(parameterName, rowMapper);
    if (logger.isDebugEnabled()) {
      logger.debug("Added row mapper for [" + getProcedureName() + "]: " + parameterName);
    }
  }

  //-------------------------------------------------------------------------
  // Methods handling compilation issues
  //-------------------------------------------------------------------------

  /**
   * Compile this JdbcCall using provided parameters and meta-data plus other settings.
   * <p>This finalizes the configuration for this object and subsequent attempts to compile are
   * ignored. This will be implicitly called the first time an un-compiled call is executed.
   *
   * @throws cn.taketoday.dao.InvalidDataAccessApiUsageException if the object hasn't
   * been correctly initialized, for example if no DataSource has been provided
   */
  public final synchronized void compile() throws InvalidDataAccessApiUsageException {
    if (!isCompiled()) {
      if (getProcedureName() == null) {
        throw new InvalidDataAccessApiUsageException("Procedure or Function name is required");
      }
      try {
        this.jdbcTemplate.afterPropertiesSet();
      }
      catch (IllegalArgumentException ex) {
        throw new InvalidDataAccessApiUsageException(ex.getMessage());
      }
      compileInternal();
      this.compiled = true;
      if (logger.isDebugEnabled()) {
        logger.debug("SqlCall for " + (isFunction() ? "function" : "procedure") +
                " [" + getProcedureName() + "] compiled");
      }
    }
  }

  /**
   * Delegate method to perform the actual compilation.
   * <p>Subclasses can override this template method to perform their own compilation.
   * Invoked after this base class's compilation is complete.
   */
  protected void compileInternal() {
    DataSource dataSource = getJdbcTemplate().getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    this.callMetaDataContext.initializeMetaData(dataSource);

    // Iterate over the declared RowMappers and register the corresponding SqlParameter
    this.declaredRowMappers.forEach((key, value) -> this.declaredParameters.add(this.callMetaDataContext.createReturnResultSetParameter(key, value)));
    this.callMetaDataContext.processParameters(this.declaredParameters);

    this.callString = this.callMetaDataContext.createCallString();
    if (logger.isDebugEnabled()) {
      logger.debug("Compiled stored procedure. Call string is [" + this.callString + "]");
    }

    this.callableStatementFactory = new CallableStatementCreatorFactory(
            this.callString, this.callMetaDataContext.getCallParameters());

    onCompileInternal();
  }

  /**
   * Hook method that subclasses may override to react to compilation.
   * This implementation does nothing.
   */
  protected void onCompileInternal() {
  }

  /**
   * Is this operation "compiled"?
   *
   * @return whether this operation is compiled and ready to use
   */
  public boolean isCompiled() {
    return this.compiled;
  }

  /**
   * Check whether this operation has been compiled already;
   * lazily compile it if not already compiled.
   * <p>Automatically called by all {@code doExecute(...)} methods.
   */
  protected void checkCompiled() {
    if (!isCompiled()) {
      logger.debug("JdbcCall call not compiled before execution - invoking compile");
      compile();
    }
  }

  //-------------------------------------------------------------------------
  // Methods handling execution
  //-------------------------------------------------------------------------

  /**
   * Delegate method that executes the call using the passed-in {@link SqlParameterSource}.
   *
   * @param parameterSource parameter names and values to be used in call
   * @return a Map of out parameters
   */
  protected Map<String, Object> doExecute(SqlParameterSource parameterSource) {
    checkCompiled();
    Map<String, Object> params = matchInParameterValuesWithCallParameters(parameterSource);
    return executeCallInternal(params);
  }

  /**
   * Delegate method that executes the call using the passed-in array of parameters.
   *
   * @param args array of parameter values. The order of values must match the order
   * declared for the stored procedure.
   * @return a Map of out parameters
   */
  protected Map<String, Object> doExecute(Object... args) {
    checkCompiled();
    Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
    return executeCallInternal(params);
  }

  /**
   * Delegate method that executes the call using the passed-in Map of parameters.
   *
   * @param args a Map of parameter name and values
   * @return a Map of out parameters
   */
  protected Map<String, Object> doExecute(Map<String, ?> args) {
    checkCompiled();
    Map<String, ?> params = matchInParameterValuesWithCallParameters(args);
    return executeCallInternal(params);
  }

  /**
   * Delegate method to perform the actual call processing.
   */
  private Map<String, Object> executeCallInternal(Map<String, ?> args) {
    CallableStatementCreator csc = getCallableStatementFactory().newCallableStatementCreator(args);
    if (logger.isDebugEnabled()) {
      logger.debug("The following parameters are used for call " + getCallString() + " with " + args);
      int i = 1;
      for (SqlParameter param : getCallParameters()) {
        logger.debug(i + ": " + param.getName() + ", SQL type " + param.getSqlType() + ", type name " +
                param.getTypeName() + ", parameter class [" + param.getClass().getName() + "]");
        i++;
      }
    }
    return getJdbcTemplate().call(csc, getCallParameters());
  }

  /**
   * Get the name of a single out parameter or return value.
   * Used for functions or procedures with one out parameter.
   */
  @Nullable
  protected String getScalarOutParameterName() {
    return this.callMetaDataContext.getScalarOutParameterName();
  }

  /**
   * Get a List of all the call parameters to be used for call.
   * This includes any parameters added based on meta-data processing.
   */
  protected List<SqlParameter> getCallParameters() {
    return this.callMetaDataContext.getCallParameters();
  }

  /**
   * Match the provided in parameter values with registered parameters and
   * parameters defined via meta-data processing.
   *
   * @param parameterSource the parameter values provided as a {@link SqlParameterSource}
   * @return a Map with parameter names and values
   */
  protected Map<String, Object> matchInParameterValuesWithCallParameters(SqlParameterSource parameterSource) {
    return this.callMetaDataContext.matchInParameterValuesWithCallParameters(parameterSource);
  }

  /**
   * Match the provided in parameter values with registered parameters and
   * parameters defined via meta-data processing.
   *
   * @param args the parameter values provided as an array
   * @return a Map with parameter names and values
   */
  private Map<String, ?> matchInParameterValuesWithCallParameters(Object[] args) {
    return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
  }

  /**
   * Match the provided in parameter values with registered parameters and
   * parameters defined via meta-data processing.
   *
   * @param args the parameter values provided in a Map
   * @return a Map with parameter names and values
   */
  protected Map<String, ?> matchInParameterValuesWithCallParameters(Map<String, ?> args) {
    return this.callMetaDataContext.matchInParameterValuesWithCallParameters(args);
  }

}
