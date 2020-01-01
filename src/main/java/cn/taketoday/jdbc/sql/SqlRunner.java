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
package cn.taketoday.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SqlRunner {

    public static final int NO_GENERATED_KEY = Integer.MIN_VALUE + 1001;

    private final Connection connection;
    private boolean useGeneratedKeySupport;

    public SqlRunner(Connection connection) {
        this.connection = connection;
    }

    public void setUseGeneratedKeySupport(boolean useGeneratedKeySupport) {
        this.useGeneratedKeySupport = useGeneratedKeySupport;
    }

    /**
     * Executes a SELECT statement that returns one row.
     *
     * @param sql
     *            The SQL
     * @param args
     *            The arguments to be set on the statement.
     * @return The row expected.
     * @throws SQLException
     *             If less or more than one row is returned
     */
    public Map<String, Object> query(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> results = queryAll(sql, args);
        if (results.size() != 1) {
            throw new SQLException("Statement returned " + results.size() + " results where exactly one (1) was expected.");
        }
        return results.get(0);
    }

    /**
     * Executes a SELECT statement that returns multiple rows.
     *
     * @param sql
     *            The SQL
     * @param args
     *            The arguments to be set on the statement.
     * @return The list of rows expected.
     * @throws SQLException
     *             If statement preparation or execution fails
     */
    public List<Map<String, Object>> queryAll(String sql, Object... args) throws SQLException {

        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            prepareStatement(ps, args);
            ResultSet rs = ps.executeQuery();
            return getResults(rs);
        } finally {
            try {
                ps.close();
            }
            catch (SQLException e) {
                // ignore
            }
        }
    }

    /**
     * Executes an INSERT statement.
     *
     * @param sql
     *            The SQL
     * @param args
     *            The arguments to be set on the statement.
     * @return The number of rows impacted or BATCHED_RESULTS if the statements are
     *         being batched.
     * @throws SQLException
     *             If statement preparation or execution fails
     */
    public int insert(String sql, Object... args) throws SQLException {
        PreparedStatement ps;
        if (useGeneratedKeySupport) {
            ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }
        else {
            ps = connection.prepareStatement(sql);
        }

        try {
            prepareStatement(ps, args);
            ps.executeUpdate();
            if (useGeneratedKeySupport) {
                List<Map<String, Object>> keys = getResults(ps.getGeneratedKeys());
                if (keys.size() == 1) {
                    Map<String, Object> key = keys.get(0);
                    Iterator<Object> i = key.values().iterator();
                    if (i.hasNext()) {
                        Object genkey = i.next();
                        if (genkey != null) {
                            try {
                                return Integer.parseInt(genkey.toString());
                            }
                            catch (NumberFormatException e) {
                                // ignore, no numeric key support
                            }
                        }
                    }
                }
            }
            return NO_GENERATED_KEY;
        } finally {
            try {
                ps.close();
            }
            catch (SQLException e) {
                // ignore
            }
        }
    }

    /**
     * Executes an UPDATE statement.
     *
     * @param sql
     *            The SQL
     * @param args
     *            The arguments to be set on the statement.
     * @return The number of rows impacted or BATCHED_RESULTS if the statements are
     *         being batched.
     * @throws SQLException
     *             If statement preparation or execution fails
     */
    public int update(String sql, Object... args) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            prepareStatement(ps, args);
            return ps.executeUpdate();
        } finally {
            try {
                ps.close();
            }
            catch (SQLException e) {
                // ignore
            }
        }
    }

    /**
     * Executes a DELETE statement.
     *
     * @param sql
     *            The SQL
     * @param args
     *            The arguments to be set on the statement.
     * @return The number of rows impacted or BATCHED_RESULTS if the statements are
     *         being batched.
     * @throws SQLException
     *             If statement preparation or execution fails
     */
    public int delete(String sql, Object... args) throws SQLException {
        return update(sql, args);
    }

    /**
     * Executes any string as a JDBC Statement. Good for DDL
     *
     * @param sql
     *            The SQL
     * @throws SQLException
     *             If statement preparation or execution fails
     */
    public void run(String sql) throws SQLException {
        Statement stmt = connection.createStatement();
        try {
            stmt.execute(sql);
        } finally {
            try {
                stmt.close();
            }
            catch (SQLException e) {
                // ignore
            }
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        }
        catch (SQLException e) {
            // ignore
        }
    }

    protected void prepareStatement(PreparedStatement ps, Object... args) throws SQLException {

    }

    protected List<Map<String, Object>> getResults(ResultSet rs) throws SQLException {

        return null;
    }

}
