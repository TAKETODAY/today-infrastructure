/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.jdbc.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Transaction} that makes use of the JDBC commit and rollback facilities
 * directly. It relies on the connection retrieved from the dataSource to manage
 * the scope of the transaction. Delays connection retrieval until
 * getConnection() is called. Ignores commit or rollback requests when
 * autocommit is on.
 *
 * @author Clinton Begin
 *
 * @see JdbcTransactionFactory
 */
@Slf4j
@NoArgsConstructor
public final class JdbcTransaction implements Transaction {

    protected Connection connection;
//	protected DataSource		dataSource;
    protected IsolationLevel isolationLevel = IsolationLevel.NONE;

    protected boolean autoCommmit = true;

    public JdbcTransaction(DataSource dataSource, IsolationLevel isolationLevel, boolean autoCommmit) {
//		this.dataSource = dataSource;
        this.autoCommmit = autoCommmit;
        this.isolationLevel = isolationLevel;
    }

    public JdbcTransaction(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() throws SQLException, TransactionException {
        if (connection == null) {
            openConnection();
        }
        return connection;
    }

    @Override
    public void commit() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            log.debug("Committing JDBC Connection [{}]", connection);
            connection.commit();
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            log.debug("Rolling back JDBC Connection [{}]", connection);
            connection.rollback();
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            resetAutoCommit();
            log.debug("Closing JDBC Connection [{}]", connection);
            connection.close();
        }
    }

    protected void setDesiredAutoCommit(boolean desiredAutoCommit) throws TransactionException {
        try {

            if (connection.getAutoCommit() != desiredAutoCommit) {
                log.debug("Setting autocommit to [{}] on JDBC Connection [{}]", desiredAutoCommit, connection);
                connection.setAutoCommit(desiredAutoCommit);
            }
        }
        catch (SQLException e) {
            throw new TransactionException("Error configuring AutoCommit.  " + "Your driver may not support getAutoCommit() or setAutoCommit(). " + "Requested setting: " + desiredAutoCommit + ".  Cause: " + e, e);
        }
    }

    protected void resetAutoCommit() {
        try {
            if (!connection.getAutoCommit()) {

                log.debug("Resetting autocommit to true on JDBC Connection [{}]", connection);
                connection.setAutoCommit(true);
            }
        }
        catch (SQLException e) {

            log.debug("Error resetting autocommit to true before closing the connection.  Cause: " + e);
        }
    }

    protected void openConnection() throws SQLException, TransactionException {

        log.debug("Opening JDBC Connection");
//		connection = dataSource.getConnection();
        if (isolationLevel != null) {
            connection.setTransactionIsolation(isolationLevel.getLevel());
        }
        setDesiredAutoCommit(autoCommmit);
    }

}
