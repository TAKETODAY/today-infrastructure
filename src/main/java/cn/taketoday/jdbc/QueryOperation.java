/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author TODAY <br>
 *         2019-08-18 20:08
 */
public interface QueryOperation {

    // Query
    // ------------------------------------------------

    /**
     * Execute a query given static SQL, reading the ResultSet with a
     * ResultSetExtractor.
     * <p>
     * Uses a JDBC Statement, not a PreparedStatement. If you want to execute a
     * static query with a PreparedStatement, use the overloaded {@code query}
     * method with {@code null} as argument array.
     * 
     * @param sql
     *            SQL query to execute
     * @param rse
     *            object that will extract all rows of results
     * @return an arbitrary result object, as returned by the ResultSetExtractor
     * @throws SQLException
     *             if there is any problem executing the query
     * @see #query(String, Object[], ResultSetExtractor)
     */
    default <T> T query(String sql, ResultSetExtractor<T> rse) throws SQLException {
        return query(sql, null, rse);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, reading the ResultSet with a
     * ResultSetExtractor.
     * 
     * @param sql
     *            SQL query to execute
     * @param rse
     *            object that will extract results
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @return an arbitrary result object, as returned by the ResultSetExtractor
     * @throws SQLException
     *             if the query fails
     */
    default <T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws SQLException {
        return query(sql, args, rse);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, reading the ResultSet with a
     * ResultSetExtractor.
     * 
     * @param sql
     *            SQL query to execute
     * @param rse
     *            object that will extract results
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @return an arbitrary result object, as returned by the ResultSetExtractor
     * @throws SQLException
     *             if the query fails
     */
    <T> T query(String sql, Object[] args, ResultSetExtractor<T> rse) throws SQLException;

    /**
     * Execute a query given static SQL, reading the ResultSet on a per-row basis
     * with a RowCallbackHandler.
     * <p>
     * Uses a JDBC Statement, not a PreparedStatement. If you want to execute a
     * static query with a PreparedStatement, use the overloaded {@code query}
     * method with {@code null} as argument array.
     * 
     * @param sql
     *            SQL query to execute
     * @param rch
     *            object that will extract results, one row at a time
     * @throws SQLException
     *             if there is any problem executing the query
     * @see #queryList(String, Object[], ResultSetHandler)
     */
    default void query(String sql, ResultSetHandler rch) throws SQLException {
        query(sql, null, rch);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, reading the ResultSet on a per-row basis with
     * a RowCallbackHandler.
     * 
     * @param sql
     *            SQL query to execute
     * @param rch
     *            object that will extract results, one row at a time
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @throws SQLException
     *             if the query fails
     */
    default void query(String sql, ResultSetHandler rch, Object... args) throws SQLException {
        query(sql, args, rch);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, reading the ResultSet on a per-row basis with
     * a RowCallbackHandler.
     * 
     * @param sql
     *            SQL query to execute
     * @param rch
     *            object that will extract results, one row at a time
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @throws SQLException
     *             if the query fails
     */
    void query(String sql, Object[] args, ResultSetHandler rch) throws SQLException;

    /**
     * Execute a query for a result object, given static SQL.
     * <p>
     * Uses a JDBC Statement, not a PreparedStatement. If you want to execute a
     * static query with a PreparedStatement, use the overloaded
     * {@link #query(String, Class, Object...)} method with {@code null} as argument
     * array.
     * <p>
     * This method is useful for running static SQL with a known outcome. The query
     * is expected to be a single row/single column query; the returned result will
     * be directly mapped to the corresponding object type.
     * 
     * @param sql
     *            SQL query to execute
     * @param requiredType
     *            the type that the result object is expected to match
     * @return the result object of the required type, or {@code null} in case of
     *         SQL NULL
     * @throws SQLException
     *             if there is any problem executing the query
     * @see #query(String, Class, Object[])
     */
    default <T> T query(String sql, Class<T> requiredType) throws SQLException {
        return query(sql, null, requiredType);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, expecting a result object.
     * <p>
     * The query is expected to be a single row/single column query; the returned
     * result will be directly mapped to the corresponding object type.
     * 
     * @param sql
     *            SQL query to execute
     * @param requiredType
     *            the type that the result object is expected to match
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @return the result object of the required type, or {@code null} in case of
     *         SQL NULL
     * @throws SQLException
     *             if the query fails
     * @see #query(String, Class)
     */
    default <T> T query(String sql, Class<T> requiredType, Object... args) throws SQLException {
        return query(sql, args, requiredType);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, expecting a result object.
     * <p>
     * The query is expected to be a single row/single column query; the returned
     * result will be directly mapped to the corresponding object type.
     * 
     * @param sql
     *            SQL query to execute
     * @param requiredType
     *            the type that the result object is expected to match
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @return the result object of the required type, or {@code null} in case of
     *         SQL NULL
     * @throws SQLException
     *             if the query fails
     * @see #query(String, Class)
     */
    <T> T query(String sql, Object[] args, Class<T> requiredType) throws SQLException;

    // List
    // -----------------------------------------------------------

    /**
     * Execute a query given static SQL, mapping each row to a Java object via a
     * RowMapper.
     * <p>
     * Uses a JDBC Statement, not a PreparedStatement. If you want to execute a
     * static query with a PreparedStatement, use the overloaded {@code query}
     * method with {@code null} as argument array.
     * 
     * @param sql
     *            SQL query to execute
     * @param rowMapper
     *            object that will map one object per row
     * @return the result List, containing mapped objects
     * @throws SQLException
     *             if there is any problem executing the query
     * @see #queryList(String, Object[], RowMapper)
     */
    default <T> List<T> queryList(String sql, RowMapper<T> rowMapper) throws SQLException {
        return queryList(sql, null, rowMapper);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, mapping each row to a Java object via a
     * RowMapper.
     * 
     * @param sql
     *            SQL query to execute
     * @param rowMapper
     *            object that will map one object per row
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @return the result List, containing mapped objects
     * @throws SQLException
     *             if the query fails
     */
    default <T> List<T> queryList(String sql, RowMapper<T> rowMapper, Object... args) throws SQLException {
        return queryList(sql, args, rowMapper);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, mapping each row to a Java object via a
     * RowMapper.
     * 
     * @param sql
     *            SQL query to execute
     * @param rowMapper
     *            object that will map one object per row
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @return the result List, containing mapped objects
     * @throws SQLException
     *             if the query fails
     */
    <T> List<T> queryList(String sql, Object[] args, RowMapper<T> rowMapper) throws SQLException;

    /**
     * Query given SQL to create a prepared statement from SQL.
     * <p>
     * The results will be mapped to a List (one entry for each row) of result
     * objects, each of them matching the specified element type.
     * 
     * @param sql
     *            SQL query to execute
     * @param elementType
     *            the required type of element in the result list
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type);
     * @return a List of objects that match the specified element type
     * @throws SQLException
     *             if the query fails
     * @see #queryList(String, Object[], Class)
     */
    default <T> List<T> queryList(String sql, Class<T> elementType) throws SQLException {
        return queryList(sql, null, elementType);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, expecting a result list.
     * <p>
     * The results will be mapped to a List (one entry for each row) of result
     * objects, each of them matching the specified element type.
     * 
     * @param sql
     *            SQL query to execute
     * @param elementType
     *            the required type of element in the result list
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type);
     * @return a List of objects that match the specified element type
     * @throws SQLException
     *             if the query fails
     * @see #queryList(String, Class)
     */
    default <T> List<T> queryList(String sql, Class<T> elementType, Object... args) throws SQLException {
        return queryList(sql, args, elementType);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, expecting a result list.
     * <p>
     * The results will be mapped to a List (one entry for each row) of result
     * objects, each of them matching the specified element type.
     * 
     * @param sql
     *            SQL query to execute
     * @param elementType
     *            the required type of element in the result list
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type);
     * @return a List of objects that match the specified element type
     * @throws SQLException
     *             if the query fails
     * @see #queryList(String, Class)
     */
    <T> List<T> queryList(String sql, Object[] args, Class<T> elementType) throws SQLException;

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, expecting a result list.
     * <p>
     * The results will be mapped to a List (one entry for each row) of Maps (one
     * entry for each column, using the column name as the key). Each element in the
     * list will be of the form returned by this interface's queryMap() methods.
     * 
     * @param sql
     *            SQL query to execute
     * @return a List that contains a Map per row
     * @throws SQLException
     *             if the query fails
     * @see #queryList(String, Object[])
     */
    default List<Map<String, Object>> queryList(String sql) throws SQLException {
        return queryList(sql, (Object[]) null);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, expecting a result list.
     * <p>
     * The results will be mapped to a List (one entry for each row) of Maps (one
     * entry for each column, using the column name as the key). Each element in the
     * list will be of the form returned by this interface's queryMap() methods.
     * 
     * @param sql
     *            SQL query to execute
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type);
     * @return a List that contains a Map per row
     * @throws SQLException
     *             if the query fails
     * @see #queryList(String)
     */
    List<Map<String, Object>> queryList(String sql, Object[] args) throws SQLException;

    // Map
    // -------------------------------------------------------------

    /**
     * Execute a query for a result Map, given static SQL.
     * <p>
     * Uses a JDBC Statement, not a PreparedStatement. If you want to execute a
     * static query with a PreparedStatement, use the overloaded
     * {@link #queryMap(String, Object[])} method with {@code null} as argument
     * array.
     * <p>
     * The query is expected to be a single row query; the result row will be mapped
     * to a Map (one entry for each column, using the column name as the key).
     * 
     * @param sql
     *            SQL query to execute
     * @return the result Map (one entry for each column, using the column name as
     *         the key)
     * @throws SQLException
     *             if there is any problem executing the query
     * @see #queryMap(String, Object[])
     */
    default Map<String, Object> queryMap(String sql) throws SQLException {
        return queryMap(sql, null);
    }

    /**
     * Query given SQL to create a prepared statement from SQL and a list of
     * arguments to bind to the query, expecting a result Map. The queryMap()
     * methods defined by this interface are appropriate when you don't have a
     * domain model. Otherwise, consider using one of the queryObject() methods.
     * <p>
     * The query is expected to be a single row query; the result row will be mapped
     * to a Map (one entry for each column, using the column name as the key).
     * 
     * @param sql
     *            SQL query to execute
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type);
     * @return the result Map (one entry for each column, using the column name as
     *         the key)
     * @throws SQLException
     *             if the query fails
     * @see #queryMap(String)
     */
    Map<String, Object> queryMap(String sql, Object[] args) throws SQLException;

    /**
     */
    @FunctionalInterface
    public interface ResultSetExtractor<T> {

        /**
         * Implementations must implement this method to process the entire ResultSet.
         * 
         * @param rs
         *            ResultSet to extract data from. Implementations should not close
         *            this: it will be closed by the calling JdbcTemplate.
         * @return an arbitrary result object, or {@code null} if none (the extractor
         *         will typically be stateful in the latter case).
         * @throws SQLException
         *             if a SQLException is encountered getting column values or
         *             navigating (that is, there's no need to catch SQLException)
         * @throws SQLException
         *             in case of custom exceptions
         */
        T extractData(final ResultSet rs) throws SQLException;

    }

    @FunctionalInterface
    public interface ResultSetHandler {

        /**
         * Implementations must implement this method to process each row of data in the
         * ResultSet. This method should not call {@code next()} on the ResultSet; it is
         * only supposed to extract values of the current row.
         * <p>
         * Exactly what the implementation chooses to do is up to it: A trivial
         * implementation might simply count rows, while another implementation might
         * build an XML document.
         * 
         * @param rs
         *            the ResultSet to process (pre-initialized for the current row)
         * @throws SQLException
         *             if a SQLException is encountered getting column values (that is,
         *             there's no need to catch SQLException)
         */
        void handleResult(final ResultSet rs) throws SQLException;

    }

    @FunctionalInterface
    public interface RowMapper<T> {

        /**
         * Implementations must implement this method to map each row of data in the
         * ResultSet. This method should not call {@code next()} on the ResultSet; it is
         * only supposed to map values of the current row.
         * 
         * @param rs
         *            the ResultSet to map (pre-initialized for the current row)
         * @param rowNum
         *            the number of the current row
         * @return the result object for the current row (may be {@code null})
         * @throws SQLException
         *             if a SQLException is encountered getting column values (that is,
         *             there's no need to catch SQLException)
         */
        T mapRow(final ResultSet rs, final int rowNum) throws SQLException;

    }
}
