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

package cn.taketoday.jdbc.support;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;

import javax.sql.DataSource;

import cn.taketoday.jdbc.CannotGetJdbcConnectionException;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.NumberUtils;
import cn.taketoday.util.StringUtils;

/**
 * Generic utility methods for working with JDBC. Mainly for internal use
 * within the framework, but also useful for custom JDBC access code.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public abstract class JdbcUtils {
  private static final Logger log = LoggerFactory.getLogger(JdbcUtils.class);

  /**
   * Constant that indicates an unknown (or unspecified) SQL type.
   *
   * @see Types
   */
  public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;

  private static final HashMap<Integer, String> typeNames = new HashMap<>();

  static {
    try {
      for (Field field : Types.class.getFields()) {
        typeNames.put((Integer) field.get(null), field.getName());
      }
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to resolve JDBC Types constants", ex);
    }
  }

  /**
   * Close the given JDBC Connection and ignore any thrown exception.
   * This is useful for typical finally blocks in manual JDBC code.
   *
   * @param con the JDBC Connection to close (may be {@code null})
   */
  public static void closeConnection(@Nullable Connection con) {
    if (con != null) {
      try {
        con.close();
      }
      catch (SQLException ex) {
        log.debug("Could not close JDBC Connection", ex);
      }
      catch (Throwable ex) {
        // We don't trust the JDBC driver: It might throw RuntimeException or Error.
        log.debug("Unexpected exception on closing JDBC Connection", ex);
      }
    }
  }

  /**
   * Close the given JDBC Statement and ignore any thrown exception.
   * This is useful for typical finally blocks in manual JDBC code.
   *
   * @param stmt the JDBC Statement to close (may be {@code null})
   */
  public static void closeStatement(@Nullable Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      }
      catch (SQLException ex) {
        log.trace("Could not close JDBC Statement", ex);
      }
      catch (Throwable ex) {
        // We don't trust the JDBC driver: It might throw RuntimeException or Error.
        log.trace("Unexpected exception on closing JDBC Statement", ex);
      }
    }
  }

  /**
   * Close the given JDBC ResultSet and ignore any thrown exception.
   * This is useful for typical finally blocks in manual JDBC code.
   *
   * @param rs the JDBC ResultSet to close (may be {@code null})
   */
  public static void closeResultSet(@Nullable ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      }
      catch (SQLException ex) {
        log.trace("Could not close JDBC ResultSet", ex);
      }
      catch (Throwable ex) {
        // We don't trust the JDBC driver: It might throw RuntimeException or Error.
        log.trace("Unexpected exception on closing JDBC ResultSet", ex);
      }
    }
  }

  /**
   * Retrieve a JDBC column value from a ResultSet, using the specified value type.
   * <p>Uses the specifically typed ResultSet accessor methods, falling back to
   * {@link #getResultSetValue(ResultSet, int)} for unknown types.
   * <p>Note that the returned value may not be assignable to the specified
   * required type, in case of an unknown type. Calling code needs to deal
   * with this case appropriately, e.g. throwing a corresponding exception.
   *
   * @param rs is the ResultSet holding the data
   * @param index is the column index
   * @param requiredType the required value type (may be {@code null})
   * @return the value object (possibly not of the specified required type,
   * with further conversion steps necessary)
   * @throws SQLException if thrown by the JDBC API
   * @see #getResultSetValue(ResultSet, int)
   */
  @Nullable
  public static Object getResultSetValue(ResultSet rs, int index, @Nullable Class<?> requiredType) throws SQLException {
    if (requiredType == null) {
      return getResultSetValue(rs, index);
    }

    Object value;

    // Explicitly extract typed value, as far as possible.
    if (String.class == requiredType) {
      return rs.getString(index);
    }
    else if (boolean.class == requiredType || Boolean.class == requiredType) {
      value = rs.getBoolean(index);
    }
    else if (byte.class == requiredType || Byte.class == requiredType) {
      value = rs.getByte(index);
    }
    else if (short.class == requiredType || Short.class == requiredType) {
      value = rs.getShort(index);
    }
    else if (int.class == requiredType || Integer.class == requiredType) {
      value = rs.getInt(index);
    }
    else if (long.class == requiredType || Long.class == requiredType) {
      value = rs.getLong(index);
    }
    else if (float.class == requiredType || Float.class == requiredType) {
      value = rs.getFloat(index);
    }
    else if (double.class == requiredType || Double.class == requiredType ||
            Number.class == requiredType) {
      value = rs.getDouble(index);
    }
    else if (BigDecimal.class == requiredType) {
      return rs.getBigDecimal(index);
    }
    else if (java.sql.Date.class == requiredType) {
      return rs.getDate(index);
    }
    else if (java.sql.Time.class == requiredType) {
      return rs.getTime(index);
    }
    else if (java.sql.Timestamp.class == requiredType || java.util.Date.class == requiredType) {
      return rs.getTimestamp(index);
    }
    else if (byte[].class == requiredType) {
      return rs.getBytes(index);
    }
    else if (Blob.class == requiredType) {
      return rs.getBlob(index);
    }
    else if (Clob.class == requiredType) {
      return rs.getClob(index);
    }
    else if (requiredType.isEnum()) {
      // Enums can either be represented through a String or an enum index value:
      // leave enum type conversion up to the caller (e.g. a ConversionService)
      // but make sure that we return nothing other than a String or an Integer.
      Object obj = rs.getObject(index);
      if (obj instanceof String) {
        return obj;
      }
      else if (obj instanceof Number) {
        // Defensively convert any Number to an Integer (as needed by our
        // ConversionService's IntegerToEnumConverterFactory) for use as index
        return NumberUtils.convertNumberToTargetClass((Number) obj, Integer.class);
      }
      else {
        // e.g. on Postgres: getObject returns a PGObject but we need a String
        return rs.getString(index);
      }
    }

    else {
      // Some unknown type desired -> rely on getObject.
      try {
        return rs.getObject(index, requiredType);
      }
      catch (AbstractMethodError err) {
        log.debug("JDBC driver does not implement JDBC 4.1 'getObject(int, Class)' method", err);
      }
      catch (SQLFeatureNotSupportedException ex) {
        log.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
      }
      catch (SQLException ex) {
        log.debug("JDBC driver has limited support for JDBC 4.1 'getObject(int, Class)' method", ex);
      }

      // Corresponding SQL types for JSR-310 / Joda-Time types, left up
      // to the caller to convert them (e.g. through a ConversionService).
      String typeName = requiredType.getSimpleName();
      return switch (typeName) {
        case "LocalDate" -> rs.getDate(index);
        case "LocalTime" -> rs.getTime(index);
        case "LocalDateTime" -> rs.getTimestamp(index);
        default ->
                // Fall back to getObject without type specification, again
                // left up to the caller to convert the value if necessary.
                getResultSetValue(rs, index);
      };

    }

    // Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
    return (rs.wasNull() ? null : value);
  }

  /**
   * Retrieve a JDBC column value from a ResultSet, using the most appropriate
   * value type. The returned value should be a detached value object, not having
   * any ties to the active ResultSet: in particular, it should not be a Blob or
   * Clob object but rather a byte array or String representation, respectively.
   * <p>Uses the {@code getObject(index)} method, but includes additional "hacks"
   * to get around Oracle 10g returning a non-standard object for its TIMESTAMP
   * datatype and a {@code java.sql.Date} for DATE columns leaving out the
   * time portion: These columns will explicitly be extracted as standard
   * {@code java.sql.Timestamp} object.
   *
   * @param rs is the ResultSet holding the data
   * @param index is the column index
   * @return the value object
   * @throws SQLException if thrown by the JDBC API
   * @see Blob
   * @see Clob
   * @see java.sql.Timestamp
   */
  @Nullable
  public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {
    Object obj = rs.getObject(index);
    String className = null;
    if (obj != null) {
      className = obj.getClass().getName();
    }
    if (obj instanceof Blob blob) {
      obj = blob.getBytes(1, (int) blob.length());
    }
    else if (obj instanceof Clob clob) {
      obj = clob.getSubString(1, (int) clob.length());
    }
    else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
      obj = rs.getTimestamp(index);
    }
    else if (className != null && className.startsWith("oracle.sql.DATE")) {
      String metaDataClassName = rs.getMetaData().getColumnClassName(index);
      if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
        obj = rs.getTimestamp(index);
      }
      else {
        obj = rs.getDate(index);
      }
    }
    else if (obj instanceof java.sql.Date) {
      if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
        obj = rs.getTimestamp(index);
      }
    }
    return obj;
  }

  /**
   * Extract database meta-data via the given DatabaseMetaDataCallback.
   * <p>This method will open a connection to the database and retrieve its meta-data.
   * Since this method is called before the exception translation feature is configured
   * for a DataSource, this method can not rely on SQLException translation itself.
   * <p>Any exceptions will be wrapped in a MetaDataAccessException. This is a checked
   * exception and any calling code should catch and handle this exception. You can just
   * log the error and hope for the best, but there is probably a more serious error that
   * will reappear when you try to access the database again.
   *
   * @param dataSource the DataSource to extract meta-data for
   * @param action callback that will do the actual work
   * @return object containing the extracted information, as returned by
   * the DatabaseMetaDataCallback's {@code processMetaData} method
   * @throws MetaDataAccessException if meta-data access failed
   * @see DatabaseMetaData
   */
  public static <T> T extractDatabaseMetaData(
          DataSource dataSource, DatabaseMetaDataCallback<T> action) throws MetaDataAccessException {

    Connection con = null;
    try {
      con = DataSourceUtils.getConnection(dataSource);
      DatabaseMetaData metaData;
      try {
        metaData = con.getMetaData();
      }
      catch (SQLException ex) {
        if (DataSourceUtils.isConnectionTransactional(con, dataSource)) {
          // Probably a closed thread-bound Connection - retry against fresh Connection
          DataSourceUtils.releaseConnection(con, dataSource);
          con = null;
          log.debug("Failed to obtain DatabaseMetaData from transactional Connection - retrying against fresh Connection", ex);
          con = dataSource.getConnection();
          metaData = con.getMetaData();
        }
        else {
          throw ex;
        }
      }
      if (metaData == null) {
        // should only happen in test environments
        throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + con + "] was null");
      }
      return action.processMetaData(metaData);
    }
    catch (CannotGetJdbcConnectionException ex) {
      throw new MetaDataAccessException("Could not get Connection for extracting meta-data", ex);
    }
    catch (SQLException ex) {
      throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
    }
    catch (AbstractMethodError err) {
      throw new MetaDataAccessException(
              "JDBC DatabaseMetaData method not implemented by JDBC driver - upgrade your driver", err);
    }
    finally {
      DataSourceUtils.releaseConnection(con, dataSource);
    }
  }

  /**
   * Return whether the given JDBC driver supports JDBC 2.0 batch updates.
   * <p>Typically invoked right before execution of a given set of statements:
   * to decide whether the set of SQL statements should be executed through
   * the JDBC 2.0 batch mechanism or simply in a traditional one-by-one fashion.
   * <p>Logs a warning if the "supportsBatchUpdates" methods throws an exception
   * and simply returns {@code false} in that case.
   *
   * @param con the Connection to check
   * @return whether JDBC 2.0 batch updates are supported
   * @see DatabaseMetaData#supportsBatchUpdates()
   */
  public static boolean supportsBatchUpdates(Connection con) {
    try {
      DatabaseMetaData dbmd = con.getMetaData();
      if (dbmd != null) {
        if (dbmd.supportsBatchUpdates()) {
          log.debug("JDBC driver supports batch updates");
          return true;
        }
        else {
          log.debug("JDBC driver does not support batch updates");
        }
      }
    }
    catch (SQLException ex) {
      log.debug("JDBC driver 'supportsBatchUpdates' method threw exception", ex);
    }
    return false;
  }

  /**
   * Extract a common name for the target database in use even if
   * various drivers/platforms provide varying names at runtime.
   *
   * @param source the name as provided in database meta-data
   * @return the common name to be used (e.g. "DB2" or "Sybase")
   */
  @Nullable
  public static String commonDatabaseName(@Nullable String source) {
    String name = source;
    if (source != null && source.startsWith("DB2")) {
      name = "DB2";
    }
    else if ("MariaDB".equals(source)) {
      name = "MySQL";
    }
    else if ("Sybase SQL Server".equals(source)
            || "Adaptive Server Enterprise".equals(source)
            || "ASE".equals(source)
            || "sql server".equalsIgnoreCase(source)) {
      name = "Sybase";
    }
    return name;
  }

  /**
   * Check whether the given SQL type is numeric.
   *
   * @param sqlType the SQL type to be checked
   * @return whether the type is numeric
   */
  public static boolean isNumeric(int sqlType) {
    return (Types.BIT == sqlType || Types.BIGINT == sqlType || Types.DECIMAL == sqlType ||
            Types.DOUBLE == sqlType || Types.FLOAT == sqlType || Types.INTEGER == sqlType ||
            Types.NUMERIC == sqlType || Types.REAL == sqlType || Types.SMALLINT == sqlType ||
            Types.TINYINT == sqlType);
  }

  /**
   * Resolve the standard type name for the given SQL type, if possible.
   *
   * @param sqlType the SQL type to resolve
   * @return the corresponding constant name in {@link Types}
   * (e.g. "VARCHAR"/"NUMERIC"), or {@code null} if not resolvable
   * @since 4.0
   */
  @Nullable
  public static String resolveTypeName(int sqlType) {
    return typeNames.get(sqlType);
  }

  /**
   * Determine the column name to use. The column name is determined based on a
   * lookup using ResultSetMetaData.
   * <p>This method implementation takes into account recent clarifications
   * expressed in the JDBC 4.0 specification:
   * <p><i>columnLabel - the label for the column specified with the SQL AS clause.
   * If the SQL AS clause was not specified, then the label is the name of the column</i>.
   *
   * @param resultSetMetaData the current meta-data to use
   * @param columnIndex the index of the column for the look up
   * @return the column name to use
   * @throws SQLException in case of lookup failure
   */
  public static String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
    String name = resultSetMetaData.getColumnLabel(columnIndex);
    if (StringUtils.isEmpty(name)) {
      name = resultSetMetaData.getColumnName(columnIndex);
    }
    return name;
  }

  /**
   * Convert a column name with underscores to the corresponding property name using "camel case".
   * A name like "customer_number" would match a "customerNumber" property name.
   *
   * @param name the column name to be converted
   * @return the name using "camel case"
   */
  public static String convertUnderscoreNameToPropertyName(@Nullable String name) {
    StringBuilder result = new StringBuilder();
    boolean nextIsUpper = false;
    if (name != null && name.length() > 0) {
      if (name.length() > 1 && name.charAt(1) == '_') {
        result.append(Character.toUpperCase(name.charAt(0)));
      }
      else {
        result.append(Character.toLowerCase(name.charAt(0)));
      }
      for (int i = 1; i < name.length(); i++) {
        char c = name.charAt(i);
        if (c == '_') {
          nextIsUpper = true;
        }
        else {
          if (nextIsUpper) {
            result.append(Character.toUpperCase(c));
            nextIsUpper = false;
          }
          else {
            result.append(Character.toLowerCase(c));
          }
        }
      }
    }
    return result.toString();
  }

  /**
   * Close a <code>Connection</code>, avoid closing if null.
   *
   * @param conn Connection to close.
   * @throws SQLException If a database access error occurs
   */
  public static void close(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      conn.close();
    }
  }

  /**
   * Close a <code>ResultSet</code>, avoid closing if null.
   *
   * @param rs ResultSet to close.
   * @throws SQLException If a database access error occurs
   */
  public static void close(@Nullable ResultSet rs) throws SQLException {
    if (rs != null) {
      rs.close();
    }
  }

  /**
   * Close a <code>Statement</code>, avoid closing if null.
   *
   * @param stmt Statement to close.
   * @throws SQLException If a database access error occurs
   */
  public static void close(@Nullable Statement stmt) throws SQLException {
    if (stmt != null) {
      stmt.close();
    }
  }

  /**
   * Close a <code>Connection</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param conn Connection to close.
   */
  public static void closeQuietly(@Nullable Connection conn) {
    try {
      close(conn);
    }
    catch (SQLException e) {
      log.warn("Could not close connection. connection: {}", conn, e);
    }
  }

  /**
   * Close a <code>Connection</code>, <code>Statement</code> and
   * <code>ResultSet</code>. Avoid closing if null and hide any SQLExceptions that
   * occur.
   *
   * @param conn Connection to close.
   * @param stmt Statement to close.
   * @param rs ResultSet to close.
   */
  public static void closeQuietly(@Nullable Connection conn, @Nullable Statement stmt, @Nullable ResultSet rs) {
    try {
      closeQuietly(rs);
    }
    finally {
      try {
        closeQuietly(stmt);
      }
      finally {
        closeQuietly(conn);
      }
    }
  }

  /**
   * Close a <code>ResultSet</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param rs ResultSet to close.
   */
  public static void closeQuietly(@Nullable ResultSet rs) {
    try {
      close(rs);
    }
    catch (SQLException e) {
      log.warn("Could not close ResultSet. result-set: {}", rs, e);
    }
  }

  /**
   * Close a <code>Statement</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param stmt Statement to close.
   */
  public static void closeQuietly(@Nullable Statement stmt) {
    try {
      close(stmt);
    }
    catch (SQLException e) {
      log.warn("Could not close statement. statement: {}", stmt, e);
    }
  }

  /**
   * Commits a <code>Connection</code> then closes it, avoid closing if null.
   *
   * @param conn Connection to close.
   * @throws SQLException If a database access error occurs
   */
  public static void commitAndClose(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      try (conn) {
        conn.commit();
      }
    }
  }

  /**
   * Commits a <code>Connection</code> then closes it, avoid closing if null and
   * hide any SQLExceptions that occur.
   *
   * @param conn Connection to close.
   */
  public static void commitAndCloseQuietly(@Nullable Connection conn) {
    try {
      commitAndClose(conn);
    }
    catch (SQLException e) {
      log.warn("Could not commit and close. Connection: {}", conn, e);
    }
  }

  /**
   * Print the stack trace for a SQLException to STDERR.
   *
   * @param e SQLException to print stack trace of
   */
  public static void printStackTrace(SQLException e) {
    printStackTrace(e, new PrintWriter(System.err));
  }

  /**
   * Print the stack trace for a SQLException to a specified PrintWriter.
   *
   * @param e SQLException to print stack trace of
   * @param pw PrintWriter to print to
   */
  public static void printStackTrace(SQLException e, PrintWriter pw) {

    SQLException next = e;
    while (next != null) {
      next.printStackTrace(pw);
      next = next.getNextException();
      if (next != null) {
        pw.println("Next SQLException:");
      }
    }
  }

  /**
   * Print warnings on a Connection to STDERR.
   *
   * @param conn Connection to print warnings from
   */
  public static void printWarnings(Connection conn) {
    printWarnings(conn, new PrintWriter(System.err));
  }

  /**
   * Print warnings on a Connection to a specified PrintWriter.
   *
   * @param conn Connection to print warnings from
   * @param pw PrintWriter to print to
   */
  public static void printWarnings(@Nullable Connection conn, PrintWriter pw) {
    if (conn != null) {
      try {
        printStackTrace(conn.getWarnings(), pw);
      }
      catch (SQLException e) {
        printStackTrace(e, pw);
      }
    }
  }

  /**
   * Rollback any changes made on the given connection.
   *
   * @param conn Connection to rollback. A null value is legal.
   * @throws SQLException If a database access error occurs
   */
  public static void rollback(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      conn.rollback();
    }
  }

  /**
   * Performs a rollback on the <code>Connection</code> then closes it, avoid
   * closing if null.
   *
   * @param conn Connection to rollback. A null value is legal.
   * @throws SQLException If a database access error occurs
   */
  public static void rollbackAndClose(@Nullable Connection conn) throws SQLException {
    if (conn != null) {
      try (conn) {
        conn.rollback();
      }
    }
  }

  /**
   * Performs a rollback on the <code>Connection</code> then closes it, avoid
   * closing if null and hide any SQLExceptions that occur.
   *
   * @param conn Connection to rollback. A null value is legal.
   */
  public static void rollbackAndCloseQuietly(Connection conn) {
    try {
      rollbackAndClose(conn);
    }
    catch (SQLException e) {
      log.warn("Could not rollback and close. Connection: {}", conn, e);
    }
  }


}
