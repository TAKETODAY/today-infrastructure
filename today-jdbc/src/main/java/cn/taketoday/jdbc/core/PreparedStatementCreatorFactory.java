/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.lang.Nullable;

/**
 * Helper class that efficiently creates multiple {@link PreparedStatementCreator}
 * objects with different parameters based on an SQL statement and a single
 * set of parameter declarations.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PreparedStatementCreatorFactory {

  /** The SQL, which won't change when the parameters change. */
  private final String sql;

  /** List of SqlParameter objects (may be {@code null}). */
  @Nullable
  private List<SqlParameter> declaredParameters;

  private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

  private boolean updatableResults = false;

  private boolean returnGeneratedKeys = false;

  @Nullable
  private String[] generatedKeysColumnNames;

  /**
   * Create a new factory. Will need to add parameters via the
   * {@link #addParameter} method or have no parameters.
   *
   * @param sql the SQL statement to execute
   */
  public PreparedStatementCreatorFactory(String sql) {
    this.sql = sql;
  }

  /**
   * Create a new factory with the given SQL and JDBC types.
   *
   * @param sql the SQL statement to execute
   * @param types int array of JDBC types
   */
  public PreparedStatementCreatorFactory(String sql, int... types) {
    this.sql = sql;
    this.declaredParameters = SqlParameter.sqlTypesToAnonymousParameterList(types);
  }

  /**
   * Create a new factory with the given SQL and parameters.
   *
   * @param sql the SQL statement to execute
   * @param declaredParameters list of {@link SqlParameter} objects
   */
  public PreparedStatementCreatorFactory(String sql, List<SqlParameter> declaredParameters) {
    this.sql = sql;
    this.declaredParameters = declaredParameters;
  }

  /**
   * Return the SQL statement to execute.
   */
  public final String getSql() {
    return this.sql;
  }

  /**
   * Add a new declared parameter.
   * <p>Order of parameter addition is significant.
   *
   * @param param the parameter to add to the list of declared parameters
   */
  public void addParameter(SqlParameter param) {
    if (this.declaredParameters == null) {
      this.declaredParameters = new ArrayList<>();
    }
    this.declaredParameters.add(param);
  }

  /**
   * Set whether to use prepared statements that return a specific type of ResultSet.
   *
   * @param resultSetType the ResultSet type
   * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
   * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
   * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
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
   * Set whether prepared statements should be capable of returning auto-generated keys.
   */
  public void setReturnGeneratedKeys(boolean returnGeneratedKeys) {
    this.returnGeneratedKeys = returnGeneratedKeys;
  }

  /**
   * Set the column names of the auto-generated keys.
   */
  public void setGeneratedKeysColumnNames(String... names) {
    this.generatedKeysColumnNames = names;
  }

  /**
   * Return a new PreparedStatementSetter for the given parameters.
   *
   * @param params list of parameters (may be {@code null})
   */
  public PreparedStatementSetter newPreparedStatementSetter(@Nullable List<?> params) {
    return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
  }

  /**
   * Return a new PreparedStatementSetter for the given parameters.
   *
   * @param params the parameter array (may be {@code null})
   */
  public PreparedStatementSetter newPreparedStatementSetter(@Nullable Object[] params) {
    return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
  }

  /**
   * Return a new PreparedStatementCreator for the given parameters.
   *
   * @param params list of parameters (may be {@code null})
   */
  public PreparedStatementCreator newPreparedStatementCreator(@Nullable List<?> params) {
    return new PreparedStatementCreatorImpl(params != null ? params : Collections.emptyList());
  }

  /**
   * Return a new PreparedStatementCreator for the given parameters.
   *
   * @param params the parameter array (may be {@code null})
   */
  public PreparedStatementCreator newPreparedStatementCreator(@Nullable Object[] params) {
    return new PreparedStatementCreatorImpl(params != null ? Arrays.asList(params) : Collections.emptyList());
  }

  /**
   * Return a new PreparedStatementCreator for the given parameters.
   *
   * @param sqlToUse the actual SQL statement to use (if different from
   * the factory's, for example because of named parameter expanding)
   * @param params the parameter array (may be {@code null})
   */
  public PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, @Nullable Object[] params) {
    return new PreparedStatementCreatorImpl(
            sqlToUse, (params != null ? Arrays.asList(params) : Collections.emptyList()));
  }

  /**
   * PreparedStatementCreator implementation returned by this class.
   */
  private class PreparedStatementCreatorImpl
          implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider, ParameterDisposer {

    private final String actualSql;

    private final List<?> parameters;

    public PreparedStatementCreatorImpl(List<?> parameters) {
      this(sql, parameters);
    }

    public PreparedStatementCreatorImpl(String actualSql, List<?> parameters) {
      this.actualSql = actualSql;
      this.parameters = parameters;
      if (declaredParameters != null) {
        int size = parameters.size();
        if (size != declaredParameters.size()) {
          // Account for named parameters being used multiple times
          HashSet<String> names = new HashSet<>();
          for (int i = 0; i < size; i++) {
            Object param = parameters.get(i);
            if (param instanceof SqlParameterValue sqlParameterValue) {
              names.add(sqlParameterValue.getName());
            }
            else {
              names.add("Parameter #" + i);
            }
          }
          if (names.size() != declaredParameters.size()) {
            throw new InvalidDataAccessApiUsageException(
                    "SQL [" + sql + "]: given " + names.size() +
                            " parameters but expected " + declaredParameters.size());
          }
        }
      }
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
      PreparedStatement ps;
      if (generatedKeysColumnNames != null || returnGeneratedKeys) {
        if (generatedKeysColumnNames != null) {
          ps = con.prepareStatement(actualSql, generatedKeysColumnNames);
        }
        else {
          ps = con.prepareStatement(actualSql, Statement.RETURN_GENERATED_KEYS);
        }
      }
      else if (resultSetType == ResultSet.TYPE_FORWARD_ONLY && !updatableResults) {
        ps = con.prepareStatement(actualSql);
      }
      else {
        ps = con.prepareStatement(actualSql, resultSetType,
                updatableResults ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
      }
      setValues(ps);
      return ps;
    }

    @Override
    public void setValues(PreparedStatement ps) throws SQLException {
      // Set arguments: Does nothing if there are no parameters.
      int sqlColIndx = 1;
      int size = parameters.size();
      for (int i = 0; i < size; i++) {
        Object in = parameters.get(i);
        SqlParameter declaredParameter = null;
        // SqlParameterValue overrides declared parameter meta-data, in particular for
        // independence from the declared parameter position in case of named parameters.
        if (in instanceof SqlParameterValue sqlParameterValue) {
          in = sqlParameterValue.getValue();
          declaredParameter = sqlParameterValue;
        }
        else if (declaredParameters != null) {
          if (declaredParameters.size() <= i) {
            throw new InvalidDataAccessApiUsageException(
                    "SQL [" + sql + "]: unable to access parameter number " + (i + 1) +
                            " given only " + declaredParameters.size() + " parameters");

          }
          declaredParameter = declaredParameters.get(i);
        }
        if (declaredParameter == null) {
          StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, SqlTypeValue.TYPE_UNKNOWN, in);
        }
        else if (in instanceof Iterable<?> entries && declaredParameter.getSqlType() != Types.ARRAY) {
          for (Object entry : entries) {
            if (entry instanceof Object[] valueArray) {
              for (Object argValue : valueArray) {
                StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, argValue);
              }
            }
            else {
              StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, entry);
            }
          }
        }
        else {
          StatementCreatorUtils.setParameterValue(ps, sqlColIndx++, declaredParameter, in);
        }
      }
    }

    @Override
    public String getSql() {
      return sql;
    }

    @Override
    public void cleanupParameters() {
      StatementCreatorUtils.cleanupParameters(parameters);
    }

    @Override
    public String toString() {
      return "PreparedStatementCreator: sql=[" + sql + "]; parameters=" + parameters;
    }
  }

}
