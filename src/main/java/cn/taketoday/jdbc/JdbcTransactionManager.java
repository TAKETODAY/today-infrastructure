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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.transaction.ConnectionHolder;
import cn.taketoday.transaction.JdbcTransactionObject;
import cn.taketoday.transaction.ResourceTransactionManager;
import cn.taketoday.transaction.SynchronizationManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.manager.AbstractTransactionManager;
import cn.taketoday.transaction.manager.DefaultTransactionStatus;
import cn.taketoday.transaction.manager.TransactionException;
import cn.taketoday.transaction.utils.DataSourceUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author TODAY <br>
 *         2018-11-13 19:05
 */
@Setter
@Getter
@MissingBean
@SuppressWarnings("serial")
public class JdbcTransactionManager extends //
        AbstractTransactionManager implements ResourceTransactionManager, InitializingBean {
    
    private static final Logger log = LoggerFactory.getLogger(JdbcTransactionManager.class);

    @Autowired
    private DataSource dataSource;

    private boolean enforceReadOnly = false;

    /**
     * Create a new DataSourceTransactionManager instance. A DataSource has to be
     * set to be able to use it.
     * 
     * @see #setDataSource
     */
    public JdbcTransactionManager() {
        setNestedTransactionAllowed(true);
    }

    /**
     * Return the JDBC DataSource that this instance manages transactions for.
     */
    public final DataSource getDataSource() {
        return this.dataSource;
    }

    /**
     * Specify whether to enforce the read-only nature of a transaction (as
     * indicated by {@link TransactionDefinition#isReadOnly()} through an explicit
     * statement on the transactional connection: "SET TRANSACTION READ ONLY" as
     * understood by Oracle, MySQL and Postgres.
     * <p>
     * The exact treatment, including any SQL statement executed on the connection,
     * can be customized through through {@link #prepareTransactionalConnection}.
     * <p>
     * This mode of read-only handling goes beyond the
     * {@link Connection#setReadOnly} hint that Spring applies by default. In
     * contrast to that standard JDBC hint, "SET TRANSACTION READ ONLY" enforces an
     * isolation-level-like connection mode where data manipulation statements are
     * strictly disallowed. Also, on Oracle, this read-only mode provides read
     * consistency for the entire transaction.
     * <p>
     * Note that older Oracle JDBC drivers (9i, 10g) used to enforce this read-only
     * mode even for {@code Connection.setReadOnly(true}. However, with recent
     * drivers, this strong enforcement needs to be applied explicitly, e.g. through
     * this flag.
     * 
     * @since 4.3.7
     * @see #prepareTransactionalConnection
     */
    public void setEnforceReadOnly(boolean enforceReadOnly) {
        this.enforceReadOnly = enforceReadOnly;
    }

    /**
     * Return whether to enforce the read-only nature of a transaction through an
     * explicit statement on the transactional connection.
     * 
     * @see #setEnforceReadOnly
     */
    public boolean isEnforceReadOnly() {
        return this.enforceReadOnly;
    }

    @Override
    public void afterPropertiesSet() {
        if (getDataSource() == null) {
            throw new IllegalArgumentException("dataSource is required");
        }
    }

    @Override
    public Object getResourceFactory() {
        return dataSource;
    }

    @Override
    protected Object doGetTransaction() {

        final DataSourceTransactionObject txObject = new DataSourceTransactionObject();
        txObject.setSavepointAllowed(isNestedTransactionAllowed());

        final ConnectionHolder conHolder = (ConnectionHolder) SynchronizationManager.getResource(dataSource);

        txObject.setConnectionHolder(conHolder, false);

        return txObject;
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
    }

    /**
     * This implementation sets the isolation level but ignores the timeout.
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {

        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        Connection con = null;

        try {

            if (!txObject.hasConnectionHolder() || txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
                Connection newCon = dataSource.getConnection();
                log.debug("Acquired Connection [{}] for JDBC transaction", newCon);
                txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
            }

            final ConnectionHolder connectionHolder = txObject.getConnectionHolder();

            connectionHolder.setSynchronizedWithTransaction(true);
            con = connectionHolder.getConnection();

            int previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
            txObject.setPreviousIsolationLevel(previousIsolationLevel);

            // Switch to manual commit if necessary. This is very expensive in some JDBC
            // drivers,
            // so we don't want to do it unnecessarily (for example if we've explicitly
            // configured the connection pool to set it already).
            if (con.getAutoCommit()) {
                txObject.setMustRestoreAutoCommit(true);
                log.debug("Switching JDBC Connection [{}] to manual commit", con);
                con.setAutoCommit(false);
            }

            prepareTransactionalConnection(con, definition);
            connectionHolder.setTransactionActive(true);

            int timeout = determineTimeout(definition);
            if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
                connectionHolder.setTimeoutInSeconds(timeout);
            }

            // Bind the connection holder to the thread.
            if (txObject.isNewConnectionHolder()) {
                SynchronizationManager.bindResource(dataSource, txObject.getConnectionHolder());
            }
        } //
        catch (Throwable ex) {
            if (txObject.isNewConnectionHolder()) {
                DataSourceUtils.releaseConnection(con, dataSource);
                txObject.setConnectionHolder(null, false);
            }
            throw new TransactionException("Could not open JDBC Connection for transaction", ex);
        }
    }

    @Override
    protected Object doSuspend(Object transaction) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        txObject.setConnectionHolder(null);
        return SynchronizationManager.unbindResource(dataSource);
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        SynchronizationManager.bindResource(dataSource, suspendedResources);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {

        DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();

        //      log.debug("Committing JDBC transaction on Connection [{}]", con);
        try {
            con.commit();
        }
        catch (SQLException ex) {
            throw new TransactionException("Could not commit JDBC transaction", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
        Connection con = txObject.getConnectionHolder().getConnection();

        //      log.debug("Rolling back JDBC transaction on Connection [{}]", con);

        try {

            con.rollback();
        } //
        catch (SQLException ex) {
            throw new TransactionException("Could not roll back JDBC transaction With msg:[" + ex.getMessage() + "]", ex);
        }
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
        //      log.debug("Setting JDBC transaction [{}] rollback-only", txObject.getConnectionHolder().getConnection());
        txObject.setRollbackOnly();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {

        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;

        // Remove the connection holder from the thread, if exposed.
        if (txObject.isNewConnectionHolder()) {
            SynchronizationManager.unbindResource(dataSource);
        }

        // Reset connection.
        Connection con = txObject.getConnectionHolder().getConnection();
        try {

            if (txObject.isMustRestoreAutoCommit()) {
                con.setAutoCommit(true);
            }
            DataSourceUtils.resetConnectionAfterTransaction(con, txObject.getPreviousIsolationLevel());
        }
        catch (Throwable ex) {
            log.error("Could not reset JDBC Connection after transaction", ex);
            throw new TransactionException(ex);
        }

        if (txObject.isNewConnectionHolder()) {
            //          log.debug("Releasing JDBC Connection [{}] after transaction", con);
            DataSourceUtils.releaseConnection(con, this.dataSource);
        }

        txObject.getConnectionHolder().clear();
    }

    /**
     * Prepare the transactional {@code Connection} right after transaction begin.
     * <p>
     * The default implementation executes a "SET TRANSACTION READ ONLY" statement
     * if the {@link #setEnforceReadOnly "enforceReadOnly"} flag is set to
     * {@code true} and the transaction definition indicates a read-only
     * transaction.
     * <p>
     * The "SET TRANSACTION READ ONLY" is understood by Oracle, MySQL and Postgres
     * and may work with other databases as well. If you'd like to adapt this
     * treatment, override this method accordingly.
     * 
     * @param con
     *            the transactional JDBC Connection
     * @param definition
     *            the current transaction definition
     * @throws SQLException
     *             if thrown by JDBC API
     * @since 4.3.7
     * @see #setEnforceReadOnly
     */
    protected void prepareTransactionalConnection(Connection con, //
                                                  TransactionDefinition definition) throws SQLException //
    {
        if (isEnforceReadOnly() && definition.isReadOnly()) {
            Statement stmt = con.createStatement();
            try {

                stmt.executeUpdate("SET TRANSACTION READ ONLY");
            }
            finally {
                stmt.close();
            }
        }
    }

    /**
     * DataSource transaction object, representing a ConnectionHolder. Used as
     * transaction object by DataSourceTransactionManager.
     */
    private static class DataSourceTransactionObject extends JdbcTransactionObject {

        private boolean newConnectionHolder;

        private boolean mustRestoreAutoCommit;

        public void setConnectionHolder(ConnectionHolder connectionHolder, boolean newConnectionHolder) {
            super.setConnectionHolder(connectionHolder);
            this.newConnectionHolder = newConnectionHolder;
        }

        public boolean isNewConnectionHolder() {
            return this.newConnectionHolder;
        }

        public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
            this.mustRestoreAutoCommit = mustRestoreAutoCommit;
        }

        public boolean isMustRestoreAutoCommit() {
            return this.mustRestoreAutoCommit;
        }

        public void setRollbackOnly() {
            getConnectionHolder().setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            return getConnectionHolder().isRollbackOnly();
        }

        @Override
        public void flush() {
            if (SynchronizationManager.isSynchronizationActive()) {
                SynchronizationManager.triggerFlush();
            }
        }
    }

}
