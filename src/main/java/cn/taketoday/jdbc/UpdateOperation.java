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
package cn.taketoday.jdbc;

import java.sql.SQLException;
import java.util.List;

/**
 * @author TODAY <br>
 *         2019-08-18 20:09
 */
public interface UpdateOperation {

    // Update
    // ------------------------------------------------
    /**
     * Issue a single SQL update operation (such as an insert, update or delete
     * statement).
     * 
     * @param sql
     *            static SQL to execute
     * @return the number of rows affected @ if there is any problem.
     */
    int update(String sql) throws SQLException;

    /**
     * Issue a single SQL update operation (such as an insert, update or delete
     * statement) via a prepared statement, binding the given arguments.
     * 
     * @param sql
     *            SQL containing bind parameters
     * @param args
     *            arguments to bind to the query (leaving it to the
     *            PreparedStatement to guess the corresponding SQL type); may also
     *            contain {@link SqlParameterValue} objects which indicate not only
     *            the argument value but also the SQL type and optionally the scale
     * @return the number of rows affected @ if there is any problem issuing the
     *         update
     */
    int update(String sql, Object[] args) throws SQLException;

    /**
     * Issue multiple SQL updates on a single JDBC Statement using batching.
     * <p>
     * Will fall back to separate updates on a single Statement if the JDBC driver
     * does not support batch updates.
     * 
     * @param sql
     *            defining an array of SQL statements that will be executed.
     * @return an array of the number of rows affected by each statement @ if there
     *         is any problem executing the batch
     */
    int[] batchUpdate(String... sql) throws SQLException;

    /**
     * Execute a batch using the supplied SQL statement with the batch of supplied
     * arguments.
     * 
     * @param sql
     *            the SQL statement to execute
     * @param batchArgs
     *            the List of Object arrays containing the batch of arguments for
     *            the query
     * @return an array containing the numbers of rows affected by each update in
     *         the batch
     */
    int[] batchUpdate(String sql, List<Object[]> batchArgs) throws SQLException;

}
