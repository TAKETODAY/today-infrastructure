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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.transaction.utils.DataSourceUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-08-18 20:39
 */
@Getter
@Setter
public abstract class Executer implements BasicOperation {

    private DataSource dataSource;

    private Integer maxRows;
    private Integer fetchSize;
    private Integer queryTimeout;

    protected static final Logger log = LoggerFactory.getLogger("cn.taketoday.jdbc.Executer");

    /**
     * Set the fetch size for this JdbcTemplate. This is important for processing
     * large result sets: Setting this higher than the default value will increase
     * processing speed at the cost of memory consumption; setting this lower can
     * avoid transferring row data that will never be read by the application.
     * <p>
     * Default is -1, indicating to use the JDBC driver's default configuration
     * (i.e. to not pass a specific fetch size setting on to the driver).
     * <p>
     * Note: As of 4.3, negative values other than -1 will get passed on to the
     * driver, since e.g. MySQL supports special behavior for
     * {@code Integer.MIN_VALUE}.
     * 
     * @see java.sql.Statement#setFetchSize
     */
    public void setFetchSize(final Integer fetchSize) {
        this.fetchSize = fetchSize;
    }

    /**
     * Return the fetch size specified for this JdbcTemplate.
     */
    public Integer getFetchSize() {
        return this.fetchSize;
    }

    /**
     * Set the maximum number of rows for this JdbcTemplate. This is important for
     * processing subsets of large result sets, avoiding to read and hold the entire
     * result set in the database or in the JDBC driver if we're never interested in
     * the entire result in the first place (for example, when performing searches
     * that might return a large number of matches).
     * <p>
     * Default is -1, indicating to use the JDBC driver's default configuration
     * (i.e. to not pass a specific max rows setting on to the driver).
     * <p>
     * Note: As of 4.3, negative values other than -1 will get passed on to the
     * driver, in sync with {@link #setFetchSize}'s support for special MySQL
     * values.
     * 
     * @see java.sql.Statement#setMaxRows
     */
    public void setMaxRows(final Integer maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * Return the maximum number of rows specified for this JdbcTemplate.
     */
    public Integer getMaxRows() {
        return this.maxRows;
    }

    /**
     * Set the query timeout for statements that this JdbcTemplate executes.
     * <p>
     * Default is -1, indicating to use the JDBC driver's default (i.e. to not pass
     * a specific query timeout setting on the driver).
     * <p>
     * Note: Any timeout specified here will be overridden by the remaining
     * transaction timeout when executing within a transaction that has a timeout
     * specified at the transaction level.
     * 
     * @see java.sql.Statement#setQueryTimeout
     */
    public void setQueryTimeout(final Integer queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    /**
     * Return the query timeout for statements that this JdbcTemplate executes.
     */
    public Integer getQueryTimeout() {
        return this.queryTimeout;
    }

    protected void applyStatementSettings(final Statement stmt) throws SQLException {

        final Integer fetchSize = getFetchSize();
        if (fetchSize != null) {
            stmt.setFetchSize(fetchSize.intValue());
        }

        final Integer maxRows = getMaxRows();
        if (maxRows != null) {
            stmt.setMaxRows(maxRows.intValue());
        }

        DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
    }

    protected void applyParameters(final PreparedStatement ps, final Object... args) throws SQLException {
        int i = 1;
        for (final Object o : args) {
            ps.setObject(i++, o);
        }
    }

    protected void applyStatementSettings(final PreparedStatement stmt, final Object... args) throws SQLException {
        applyStatementSettings(stmt);
        if (args != null) {
            applyParameters(stmt, args);
        }
    }

    // BasicOperation
    // -----------------------------

    @Override
    public <T> T execute(final ConnectionCallback<T> action) throws SQLException {

        final DataSource dataSource = getDataSource();

        final Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            return action.doInConnection(con);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    @Override
    public <T> T execute(final StatementCallback<T> action) throws SQLException {

        return execute((ConnectionCallback<T>) (con) -> {

            try (final Statement statement = con.createStatement()) {
                applyStatementSettings(statement);
                return action.doInStatement(statement);
            }
        });
    }

    @Override
    public <T> T execute(final String sql, final PreparedStatementCallback<T> action) throws SQLException {

        return execute((ConnectionCallback<T>) (conn) -> {

            try (final PreparedStatement statement = conn.prepareStatement(sql)) {
                applyStatementSettings(statement);
                return action.doInPreparedStatement(statement);
            }
        });
    }

    @Override
    public void execute(final String sql) throws SQLException {

        if (log.isDebugEnabled()) {
            log.debug("Executing SQL statement [{}]", sql);
        }

        execute((StatementCallback<Object>) (s) -> {
            return s.execute(sql);
        });
    }

    @Override
    public <T> T execute(final String sql, final CallableStatementCallback<T> action) throws SQLException {

        return execute((ConnectionCallback<T>) (conn) -> {
            return action.doInCallableStatement(conn.prepareCall(sql));
        });
    }

}
