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

package cn.taketoday.jdbc.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.lang.Nullable;

/**
 * Helper class that efficiently creates multiple {@link CallableStatementCreator}
 * objects with different parameters based on an SQL statement and a single
 * set of parameter declarations.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class CallableStatementCreatorFactory {

  /** The SQL call string, which won't change when the parameters change. */
  private final String callString;

  /** List of SqlParameter objects. May not be {@code null}. */
  private final List<SqlParameter> declaredParameters;

  private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

  private boolean updatableResults = false;

  /**
   * Create a new factory. Will need to add parameters via the
   * {@link #addParameter} method or have no parameters.
   *
   * @param callString the SQL call string
   */
  public CallableStatementCreatorFactory(String callString) {
    this.callString = callString;
    this.declaredParameters = new ArrayList<>();
  }

  /**
   * Create a new factory with the given SQL and the given parameters.
   *
   * @param callString the SQL call string
   * @param declaredParameters list of {@link SqlParameter} objects
   */
  public CallableStatementCreatorFactory(String callString, List<SqlParameter> declaredParameters) {
    this.callString = callString;
    this.declaredParameters = declaredParameters;
  }

  /**
   * Return the SQL call string.
   *
   * @since 4.0
   */
  public final String getCallString() {
    return this.callString;
  }

  /**
   * Add a new declared parameter.
   * <p>Order of parameter addition is significant.
   *
   * @param param the parameter to add to the list of declared parameters
   */
  public void addParameter(SqlParameter param) {
    this.declaredParameters.add(param);
  }

  /**
   * Set whether to use prepared statements that return a specific type of ResultSet.
   * specific type of ResultSet.
   *
   * @param resultSetType the ResultSet type
   * @see ResultSet#TYPE_FORWARD_ONLY
   * @see ResultSet#TYPE_SCROLL_INSENSITIVE
   * @see ResultSet#TYPE_SCROLL_SENSITIVE
   */
  public void setResultSetType(int resultSetType) {
    this.resultSetType = resultSetType;
  }

  /**
   * Set whether to use prepared statements capable of returning updatable ResultSets.
   */
  public void setUpdatableResults(boolean updatableResults) {
    this.updatableResults = updatableResults;
  }

  /**
   * Return a new CallableStatementCreator instance given this parameters.
   *
   * @param params list of parameters (may be {@code null})
   */
  public CallableStatementCreator newCallableStatementCreator(@Nullable Map<String, ?> params) {
    return new CallableStatementCreatorImpl(params != null ? params : new HashMap<>());
  }

  /**
   * Return a new CallableStatementCreator instance given this parameter mapper.
   *
   * @param inParamMapper the ParameterMapper implementation that will return a Map of parameters
   */
  public CallableStatementCreator newCallableStatementCreator(ParameterMapper inParamMapper) {
    return new CallableStatementCreatorImpl(inParamMapper);
  }

  /**
   * CallableStatementCreator implementation returned by this class.
   */
  private class CallableStatementCreatorImpl implements CallableStatementCreator, SqlProvider, ParameterDisposer {

    @Nullable
    private ParameterMapper inParameterMapper;

    @Nullable
    private Map<String, ?> inParameters;

    /**
     * Create a new CallableStatementCreatorImpl.
     *
     * @param inParamMapper the ParameterMapper implementation for mapping input parameters
     */
    public CallableStatementCreatorImpl(ParameterMapper inParamMapper) {
      this.inParameterMapper = inParamMapper;
    }

    /**
     * Create a new CallableStatementCreatorImpl.
     *
     * @param inParams list of SqlParameter objects
     */
    public CallableStatementCreatorImpl(Map<String, ?> inParams) {
      this.inParameters = inParams;
    }

    @Override
    public CallableStatement createCallableStatement(Connection con) throws SQLException {
      // If we were given a ParameterMapper, we must let the mapper do its thing to create the Map.
      if (this.inParameterMapper != null) {
        this.inParameters = this.inParameterMapper.createMap(con);
      }
      else {
        if (this.inParameters == null) {
          throw new InvalidDataAccessApiUsageException(
                  "A ParameterMapper or a Map of parameters must be provided");
        }
      }

      CallableStatement cs = null;
      if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
        cs = con.prepareCall(callString);
      }
      else {
        cs = con.prepareCall(callString, resultSetType,
                updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
      }

      int sqlColIndx = 1;
      for (SqlParameter declaredParam : declaredParameters) {
        if (!declaredParam.isResultsParameter()) {
          // So, it's a call parameter - part of the call string.
          // Get the value - it may still be null.
          Object inValue = this.inParameters.get(declaredParam.getName());
          if (declaredParam instanceof ResultSetSupportingSqlParameter) {
            // It's an output parameter: SqlReturnResultSet parameters already excluded.
            // It need not (but may be) supplied by the caller.
            if (declaredParam instanceof SqlOutParameter) {
              if (declaredParam.getTypeName() != null) {
                cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getTypeName());
              }
              else {
                if (declaredParam.getScale() != null) {
                  cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType(), declaredParam.getScale());
                }
                else {
                  cs.registerOutParameter(sqlColIndx, declaredParam.getSqlType());
                }
              }
              if (declaredParam.isInputValueProvided()) {
                StatementCreatorUtils.setParameterValue(cs, sqlColIndx, declaredParam, inValue);
              }
            }
          }
          else {
            // It's an input parameter; must be supplied by the caller.
            if (!this.inParameters.containsKey(declaredParam.getName())) {
              throw new InvalidDataAccessApiUsageException(
                      "Required input parameter '" + declaredParam.getName() + "' is missing");
            }
            StatementCreatorUtils.setParameterValue(cs, sqlColIndx, declaredParam, inValue);
          }
          sqlColIndx++;
        }
      }

      return cs;
    }

    @Override
    public String getSql() {
      return callString;
    }

    @Override
    public void cleanupParameters() {
      if (this.inParameters != null) {
        StatementCreatorUtils.cleanupParameters(this.inParameters.values());
      }
    }

    @Override
    public String toString() {
      return "CallableStatementCreator: sql=[" + callString + "]; parameters=" + this.inParameters;
    }
  }

}
