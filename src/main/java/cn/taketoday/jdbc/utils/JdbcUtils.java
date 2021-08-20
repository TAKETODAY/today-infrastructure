/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.jdbc.utils;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import cn.taketoday.util.ConvertUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.jdbc.PersistenceException;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

/**
 * @author TODAY <br>
 * 2019-08-19 07:42
 */
public abstract class JdbcUtils {
  private static final Logger log = LoggerFactory.getLogger(JdbcUtils.class);

  /**
   * @throws PersistenceException
   *         if a database access error occurs or this method is called on a closed result set
   */
  public static ResultSetMetaData getMetaData(final ResultSet rs) {
    try {
      return rs.getMetaData();
    }
    catch (SQLException ex) {
      throw new PersistenceException("Database error: " + ex.getMessage(), ex);
    }
  }

  public static String getColumnName(ResultSetMetaData resultSetMetaData, int index) throws SQLException {
    final String name = resultSetMetaData.getColumnLabel(index);
    if (StringUtils.isEmpty(name)) {
      return resultSetMetaData.getColumnName(index);
    }
    return name;
  }

  public static Object getResultSetValue(ResultSet rs, int index, Class<?> requiredType) throws SQLException {

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
    else if (double.class == requiredType || Double.class == requiredType || Number.class == requiredType) {
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
        return ConvertUtils.convert(obj, requiredType);
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
      if ("LocalDate".equals(typeName)) {
        return rs.getDate(index);
      }
      else if ("LocalTime".equals(typeName)) {
        return rs.getTime(index);
      }
      else if ("LocalDateTime".equals(typeName)) {
        return rs.getTimestamp(index);
      }

      // Fall back to getObject without type specification, again
      // left up to the caller to convert the value if necessary.
      return getResultSetValue(rs, index);
    }

    // Perform was-null check if necessary (for results that the JDBC driver returns
    // as primitives).
    return (rs.wasNull() ? null : value);
  }

  public static Object getResultSetValue(ResultSet rs, int index) throws SQLException {

    Object obj = rs.getObject(index);

    if (obj instanceof Blob) {
      Blob blob = (Blob) obj;
      return blob.getBytes(1, (int) blob.length());
    }
    else if (obj instanceof Clob) {
      Clob clob = (Clob) obj;
      return clob.getSubString(1, (int) clob.length());
    }

    String className = null;

    if (obj != null) {
      className = obj.getClass().getName();
    }
    if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
      return rs.getTimestamp(index);
    }
    if (className != null && className.startsWith("oracle.sql.DATE")) {
      String metaDataClassName = rs.getMetaData().getColumnClassName(index);
      if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
        return rs.getTimestamp(index);
      }
      return rs.getDate(index);
    }

    if (obj instanceof java.sql.Date && "java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
      return rs.getTimestamp(index);
    }
    return obj;
  }

  // -------------------

  // -------------------

  /**
   * Close a <code>Connection</code>, avoid closing if null.
   *
   * @param conn
   *         Connection to close.
   *
   * @throws SQLException
   *         If a database access error occurs
   */
  public static void close(Connection conn) throws SQLException {
    if (conn != null) {
      conn.close();
    }
  }

  /**
   * Close a <code>ResultSet</code>, avoid closing if null.
   *
   * @param rs
   *         ResultSet to close.
   *
   * @throws SQLException
   *         If a database access error occurs
   */
  public static void close(final ResultSet rs) throws SQLException {
    if (rs != null) {
      rs.close();
    }
  }

  /**
   * Close a <code>Statement</code>, avoid closing if null.
   *
   * @param stmt
   *         Statement to close.
   *
   * @throws SQLException
   *         If a database access error occurs
   */
  public static void close(Statement stmt) throws SQLException {
    if (stmt != null) {
      stmt.close();
    }
  }

  /**
   * Close a <code>Connection</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param conn
   *         Connection to close.
   */
  public static void closeQuietly(Connection conn) {
    try {
      close(conn);
    }
    catch (SQLException e) { // NOPMD
      // quiet
    }
  }

  /**
   * Close a <code>Connection</code>, <code>Statement</code> and
   * <code>ResultSet</code>. Avoid closing if null and hide any SQLExceptions that
   * occur.
   *
   * @param conn
   *         Connection to close.
   * @param stmt
   *         Statement to close.
   * @param rs
   *         ResultSet to close.
   */
  public static void closeQuietly(Connection conn, Statement stmt, ResultSet rs) {

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
   * @param rs
   *         ResultSet to close.
   */
  public static void closeQuietly(ResultSet rs) {
    try {
      close(rs);
    }
    catch (SQLException e) { // NOPMD
      // quiet
    }
  }

  /**
   * Close a <code>Statement</code>, avoid closing if null and hide any
   * SQLExceptions that occur.
   *
   * @param stmt
   *         Statement to close.
   */
  public static void closeQuietly(Statement stmt) {
    try {
      close(stmt);
    }
    catch (SQLException e) { // NOPMD
      // quiet
    }
  }

  /**
   * Commits a <code>Connection</code> then closes it, avoid closing if null.
   *
   * @param conn
   *         Connection to close.
   *
   * @throws SQLException
   *         If a database access error occurs
   */
  public static void commitAndClose(Connection conn) throws SQLException {
    if (conn != null) {
      try {
        conn.commit();
      }
      finally {
        conn.close();
      }
    }
  }

  /**
   * Commits a <code>Connection</code> then closes it, avoid closing if null and
   * hide any SQLExceptions that occur.
   *
   * @param conn
   *         Connection to close.
   */
  public static void commitAndCloseQuietly(Connection conn) {
    try {
      commitAndClose(conn);
    }
    catch (SQLException e) { // NOPMD
      // quiet
    }
  }

  /**
   * Print the stack trace for a SQLException to STDERR.
   *
   * @param e
   *         SQLException to print stack trace of
   */
  public static void printStackTrace(SQLException e) {
    printStackTrace(e, new PrintWriter(System.err));
  }

  /**
   * Print the stack trace for a SQLException to a specified PrintWriter.
   *
   * @param e
   *         SQLException to print stack trace of
   * @param pw
   *         PrintWriter to print to
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
   * @param conn
   *         Connection to print warnings from
   */
  public static void printWarnings(Connection conn) {
    printWarnings(conn, new PrintWriter(System.err));
  }

  /**
   * Print warnings on a Connection to a specified PrintWriter.
   *
   * @param conn
   *         Connection to print warnings from
   * @param pw
   *         PrintWriter to print to
   */
  public static void printWarnings(Connection conn, PrintWriter pw) {
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
   * @param conn
   *         Connection to rollback. A null value is legal.
   *
   * @throws SQLException
   *         If a database access error occurs
   */
  public static void rollback(Connection conn) throws SQLException {
    if (conn != null) {
      conn.rollback();
    }
  }

  /**
   * Performs a rollback on the <code>Connection</code> then closes it, avoid
   * closing if null.
   *
   * @param conn
   *         Connection to rollback. A null value is legal.
   *
   * @throws SQLException
   *         If a database access error occurs
   */
  public static void rollbackAndClose(Connection conn) throws SQLException {
    if (conn != null) {
      try {
        conn.rollback();
      }
      finally {
        conn.close();
      }
    }
  }

  /**
   * Performs a rollback on the <code>Connection</code> then closes it, avoid
   * closing if null and hide any SQLExceptions that occur.
   *
   * @param conn
   *         Connection to rollback. A null value is legal.
   */
  public static void rollbackAndCloseQuietly(Connection conn) {
    try {
      rollbackAndClose(conn);
    }
    catch (SQLException e) { // NOPMD
      // quiet
    }
  }

}
