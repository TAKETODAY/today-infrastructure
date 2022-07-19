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

package cn.taketoday.jdbc.core;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.jdbc.support.SqlValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Utility methods for PreparedStatementSetter/Creator and CallableStatementCreator
 * implementations, providing sophisticated parameter management (including support
 * for LOB values).
 *
 * <p>Used by PreparedStatementCreatorFactory and CallableStatementCreatorFactory,
 * but also available for direct use in custom setter/creator implementations.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see PreparedStatementSetter
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see PreparedStatementCreatorFactory
 * @see CallableStatementCreatorFactory
 * @see SqlParameter
 * @see SqlTypeValue
 * @see cn.taketoday.jdbc.core.support.SqlLobValue
 * @since 4.0
 */
public abstract class StatementCreatorUtils {

  /**
   * System property that instructs Framework to ignore {@link java.sql.ParameterMetaData#getParameterType}
   * completely, i.e. to never even attempt to retrieve {@link PreparedStatement#getParameterMetaData()}
   * for {@link StatementCreatorUtils#setNull} calls.
   * <p>The default is "false", trying {@code getParameterType} calls first and falling back to
   * {@link PreparedStatement#setNull} / {@link PreparedStatement#setObject} calls based on
   * well-known behavior of common databases.
   * <p>Consider switching this flag to "true" if you experience misbehavior at runtime,
   * e.g. with connection pool issues in case of an exception thrown from {@code getParameterType}
   * (as reported on JBoss AS 7) or in case of performance problems (as reported on PostgreSQL).
   */
  public static final String IGNORE_GETPARAMETERTYPE_PROPERTY_NAME = "today.jdbc.getParameterType.ignore";

  static boolean shouldIgnoreGetParameterType = TodayStrategies.getFlag(IGNORE_GETPARAMETERTYPE_PROPERTY_NAME);

  private static final Logger log = LoggerFactory.getLogger(StatementCreatorUtils.class);

  private static final Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<>(32);

  static {
    javaTypeToSqlTypeMap.put(boolean.class, Types.BOOLEAN);
    javaTypeToSqlTypeMap.put(Boolean.class, Types.BOOLEAN);
    javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
    javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
    javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
    javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
    javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
    javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
    javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
    javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
    javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
    javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
    javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
    javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
    javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
    javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
    javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
    javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
    javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
    javaTypeToSqlTypeMap.put(LocalDate.class, Types.DATE);
    javaTypeToSqlTypeMap.put(LocalTime.class, Types.TIME);
    javaTypeToSqlTypeMap.put(LocalDateTime.class, Types.TIMESTAMP);
    javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
    javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
  }

  /**
   * Derive a default SQL type from the given Java type.
   *
   * @param javaType the Java type to translate
   * @return the corresponding SQL type, or {@link SqlTypeValue#TYPE_UNKNOWN} if none found
   */
  public static int javaTypeToSqlParameterType(@Nullable Class<?> javaType) {
    if (javaType == null) {
      return SqlTypeValue.TYPE_UNKNOWN;
    }
    Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
    if (sqlType != null) {
      return sqlType;
    }
    if (Number.class.isAssignableFrom(javaType)) {
      return Types.NUMERIC;
    }
    if (isStringValue(javaType)) {
      return Types.VARCHAR;
    }
    if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
      return Types.TIMESTAMP;
    }
    return SqlTypeValue.TYPE_UNKNOWN;
  }

  /**
   * Set the value for a parameter. The method used is based on the SQL type
   * of the parameter and we can handle complex types like arrays and LOBs.
   *
   * @param ps the prepared statement or callable statement
   * @param paramIndex index of the parameter we are setting
   * @param param the parameter as it is declared including type
   * @param inValue the value to set
   * @throws SQLException if thrown by PreparedStatement methods
   */
  public static void setParameterValue(
          PreparedStatement ps, int paramIndex, SqlParameter param, @Nullable Object inValue) throws SQLException {
    setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param.getScale(), inValue);
  }

  /**
   * Set the value for a parameter. The method used is based on the SQL type
   * of the parameter and we can handle complex types like arrays and LOBs.
   *
   * @param ps the prepared statement or callable statement
   * @param paramIndex index of the parameter we are setting
   * @param sqlType the SQL type of the parameter
   * @param inValue the value to set (plain value or an SqlTypeValue)
   * @throws SQLException if thrown by PreparedStatement methods
   * @see SqlTypeValue
   */
  public static void setParameterValue(
          PreparedStatement ps, int paramIndex, int sqlType, @Nullable Object inValue) throws SQLException {
    setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
  }

  /**
   * Set the value for a parameter. The method used is based on the SQL type
   * of the parameter and we can handle complex types like arrays and LOBs.
   *
   * @param ps the prepared statement or callable statement
   * @param paramIndex index of the parameter we are setting
   * @param sqlType the SQL type of the parameter
   * @param typeName the type name of the parameter
   * (optional, only used for SQL NULL and SqlTypeValue)
   * @param inValue the value to set (plain value or an SqlTypeValue)
   * @throws SQLException if thrown by PreparedStatement methods
   * @see SqlTypeValue
   */
  public static void setParameterValue(
          PreparedStatement ps, int paramIndex, int sqlType, String typeName,
          @Nullable Object inValue) throws SQLException {

    setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
  }

  /**
   * Set the value for a parameter. The method used is based on the SQL type
   * of the parameter and we can handle complex types like arrays and LOBs.
   *
   * @param ps the prepared statement or callable statement
   * @param paramIndex index of the parameter we are setting
   * @param sqlType the SQL type of the parameter
   * @param typeName the type name of the parameter
   * (optional, only used for SQL NULL and SqlTypeValue)
   * @param scale the number of digits after the decimal point
   * (for DECIMAL and NUMERIC types)
   * @param inValue the value to set (plain value or an SqlTypeValue)
   * @throws SQLException if thrown by PreparedStatement methods
   * @see SqlTypeValue
   */
  private static void setParameterValueInternal(
          PreparedStatement ps, int paramIndex, int sqlType,
          @Nullable String typeName, @Nullable Integer scale, @Nullable Object inValue) throws SQLException {

    String typeNameToUse = typeName;
    int sqlTypeToUse = sqlType;
    Object inValueToUse = inValue;

    // override type info?
    if (inValue instanceof SqlParameterValue parameterValue) {
      if (log.isDebugEnabled()) {
        log.debug("Overriding type info with runtime info from SqlParameterValue: column index {}, SQL type {}, type name {}",
                paramIndex, parameterValue.getSqlType(), parameterValue.getTypeName());
      }
      if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
        sqlTypeToUse = parameterValue.getSqlType();
      }
      if (parameterValue.getTypeName() != null) {
        typeNameToUse = parameterValue.getTypeName();
      }
      inValueToUse = parameterValue.getValue();
    }

    if (log.isTraceEnabled()) {
      log.trace("Setting SQL statement parameter value: column index {}, parameter value [{}], value class [{}], SQL type {}",
              paramIndex, inValueToUse, (inValueToUse != null ? inValueToUse.getClass().getName() : "null"),
              (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
    }

    if (inValueToUse == null) {
      setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
    }
    else {
      setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
    }
  }

  /**
   * Set the specified PreparedStatement parameter to null,
   * respecting database-specific peculiarities.
   */
  private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
          throws SQLException {

    if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER && typeName == null)) {
      boolean useSetObject = false;
      Integer sqlTypeToUse = null;
      if (!shouldIgnoreGetParameterType) {
        try {
          sqlTypeToUse = ps.getParameterMetaData().getParameterType(paramIndex);
        }
        catch (SQLException ex) {
          if (log.isDebugEnabled()) {
            log.debug("JDBC getParameterType call failed - using fallback method instead: {}", ex.toString());
          }
        }
      }
      if (sqlTypeToUse == null) {
        // Proceed with database-specific checks
        sqlTypeToUse = Types.NULL;
        DatabaseMetaData dbmd = ps.getConnection().getMetaData();
        String jdbcDriverName = dbmd.getDriverName();
        String databaseProductName = dbmd.getDatabaseProductName();
        if (databaseProductName.startsWith("Informix") ||
                (jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server"))) {
          // "Microsoft SQL Server JDBC Driver 3.0" versus "Microsoft JDBC Driver 4.0 for SQL Server"
          useSetObject = true;
        }
        else if (databaseProductName.startsWith("DB2") ||
                jdbcDriverName.startsWith("jConnect") ||
                jdbcDriverName.startsWith("SQLServer") ||
                jdbcDriverName.startsWith("Apache Derby")) {
          sqlTypeToUse = Types.VARCHAR;
        }
      }
      if (useSetObject) {
        ps.setObject(paramIndex, null);
      }
      else {
        ps.setNull(paramIndex, sqlTypeToUse);
      }
    }
    else if (typeName != null) {
      ps.setNull(paramIndex, sqlType, typeName);
    }
    else {
      ps.setNull(paramIndex, sqlType);
    }
  }

  private static void setValue(
          PreparedStatement ps, int paramIndex, int sqlType,
          @Nullable String typeName, @Nullable Integer scale, Object inValue) throws SQLException {

    if (inValue instanceof SqlTypeValue) {
      ((SqlTypeValue) inValue).setTypeValue(ps, paramIndex, sqlType, typeName);
    }
    else if (inValue instanceof SqlValue) {
      ((SqlValue) inValue).setValue(ps, paramIndex);
    }
    else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR) {
      ps.setString(paramIndex, inValue.toString());
    }
    else if (sqlType == Types.NVARCHAR || sqlType == Types.LONGNVARCHAR) {
      ps.setNString(paramIndex, inValue.toString());
    }
    else if ((sqlType == Types.CLOB || sqlType == Types.NCLOB) && isStringValue(inValue.getClass())) {
      String strVal = inValue.toString();
      if (strVal.length() > 4000) {
        // Necessary for older Oracle drivers, in particular when running against an Oracle 10 database.
        // Should also work fine against other drivers/databases since it uses standard JDBC 4.0 API.
        if (sqlType == Types.NCLOB) {
          ps.setNClob(paramIndex, new StringReader(strVal), strVal.length());
        }
        else {
          ps.setClob(paramIndex, new StringReader(strVal), strVal.length());
        }
      }
      else {
        // Fallback: setString or setNString binding
        if (sqlType == Types.NCLOB) {
          ps.setNString(paramIndex, strVal);
        }
        else {
          ps.setString(paramIndex, strVal);
        }
      }
    }
    else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
      if (inValue instanceof BigDecimal) {
        ps.setBigDecimal(paramIndex, (BigDecimal) inValue);
      }
      else if (scale != null) {
        ps.setObject(paramIndex, inValue, sqlType, scale);
      }
      else {
        ps.setObject(paramIndex, inValue, sqlType);
      }
    }
    else if (sqlType == Types.BOOLEAN) {
      if (inValue instanceof Boolean) {
        ps.setBoolean(paramIndex, (Boolean) inValue);
      }
      else {
        ps.setObject(paramIndex, inValue, Types.BOOLEAN);
      }
    }
    else if (sqlType == Types.DATE) {
      if (inValue instanceof java.util.Date) {
        if (inValue instanceof java.sql.Date) {
          ps.setDate(paramIndex, (java.sql.Date) inValue);
        }
        else {
          ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValue).getTime()));
        }
      }
      else if (inValue instanceof Calendar cal) {
        ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
      }
      else {
        ps.setObject(paramIndex, inValue, Types.DATE);
      }
    }
    else if (sqlType == Types.TIME) {
      if (inValue instanceof java.util.Date) {
        if (inValue instanceof java.sql.Time) {
          ps.setTime(paramIndex, (java.sql.Time) inValue);
        }
        else {
          ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValue).getTime()));
        }
      }
      else if (inValue instanceof Calendar cal) {
        ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
      }
      else {
        ps.setObject(paramIndex, inValue, Types.TIME);
      }
    }
    else if (sqlType == Types.TIMESTAMP) {
      if (inValue instanceof java.util.Date) {
        if (inValue instanceof java.sql.Timestamp) {
          ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValue);
        }
        else {
          ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
        }
      }
      else if (inValue instanceof Calendar cal) {
        ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
      }
      else {
        ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
      }
    }
    else if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER &&
            "Oracle".equals(ps.getConnection().getMetaData().getDatabaseProductName()))) {
      if (isStringValue(inValue.getClass())) {
        ps.setString(paramIndex, inValue.toString());
      }
      else if (isDateValue(inValue.getClass())) {
        ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
      }
      else if (inValue instanceof Calendar cal) {
        ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
      }
      else {
        // Fall back to generic setObject call without SQL type specified.
        ps.setObject(paramIndex, inValue);
      }
    }
    else {
      // Fall back to generic setObject call with SQL type specified.
      ps.setObject(paramIndex, inValue, sqlType);
    }
  }

  /**
   * Check whether the given value can be treated as a String value.
   */
  private static boolean isStringValue(Class<?> inValueType) {
    // Consider any CharSequence (including StringBuffer and StringBuilder) as a String.
    return (CharSequence.class.isAssignableFrom(inValueType) ||
            StringWriter.class.isAssignableFrom(inValueType));
  }

  /**
   * Check whether the given value is a {@code java.util.Date}
   * (but not one of the JDBC-specific subclasses).
   */
  private static boolean isDateValue(Class<?> inValueType) {
    return (java.util.Date.class.isAssignableFrom(inValueType) &&
            !(java.sql.Date.class.isAssignableFrom(inValueType) ||
                    java.sql.Time.class.isAssignableFrom(inValueType) ||
                    java.sql.Timestamp.class.isAssignableFrom(inValueType)));
  }

  /**
   * Clean up all resources held by parameter values which were passed to an
   * execute method. This is for example important for closing LOB values.
   *
   * @param paramValues parameter values supplied. May be {@code null}.
   * @see DisposableSqlTypeValue#cleanup()
   * @see cn.taketoday.jdbc.core.support.SqlLobValue#cleanup()
   */
  public static void cleanupParameters(@Nullable Object... paramValues) {
    if (paramValues != null) {
      cleanupParameters(Arrays.asList(paramValues));
    }
  }

  /**
   * Clean up all resources held by parameter values which were passed to an
   * execute method. This is for example important for closing LOB values.
   *
   * @param paramValues parameter values supplied. May be {@code null}.
   * @see DisposableSqlTypeValue#cleanup()
   * @see cn.taketoday.jdbc.core.support.SqlLobValue#cleanup()
   */
  public static void cleanupParameters(@Nullable Collection<?> paramValues) {
    if (paramValues != null) {
      for (Object inValue : paramValues) {
        // Unwrap SqlParameterValue first...
        if (inValue instanceof SqlParameterValue) {
          inValue = ((SqlParameterValue) inValue).getValue();
        }
        // Check for disposable value types
        if (inValue instanceof SqlValue) {
          ((SqlValue) inValue).cleanup();
        }
        else if (inValue instanceof DisposableSqlTypeValue) {
          ((DisposableSqlTypeValue) inValue).cleanup();
        }
      }
    }
  }

}
