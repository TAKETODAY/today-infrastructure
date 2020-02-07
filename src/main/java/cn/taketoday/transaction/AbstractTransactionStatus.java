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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction;

/**
 * @author TODAY <br>
 *         2018-11-16 21:28
 */
public abstract class AbstractTransactionStatus implements TransactionStatus {

    private boolean rollbackOnly = false;

    private boolean completed = false;

    private Object savepoint;

    // ---------------------------------------------------------------------
    // Handling of current transaction state
    // ---------------------------------------------------------------------

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    /**
     * Determine the rollback-only flag via checking both the local rollback-only
     * flag of this TransactionStatus and the global rollback-only flag of the
     * underlying transaction, if any.
     * 
     * @see #isLocalRollbackOnly()
     * @see #isGlobalRollbackOnly()
     */
    @Override
    public boolean isRollbackOnly() {
        return (isLocalRollbackOnly() || isGlobalRollbackOnly());
    }

    /**
     * Determine the rollback-only flag via checking this TransactionStatus.
     * <p>
     * Will only return "true" if the application called {@code setRollbackOnly} on
     * this TransactionStatus object.
     */
    public boolean isLocalRollbackOnly() {
        return this.rollbackOnly;
    }

    /**
     * Template method for determining the global rollback-only flag of the
     * underlying transaction, if any.
     * <p>
     * This implementation always returns {@code false}.
     */
    public boolean isGlobalRollbackOnly() {
        return false;
    }

    /**
     * Mark this transaction as completed, that is, committed or rolled back.
     */
    public void setCompleted() {
        this.completed = true;
    }

    @Override
    public boolean isCompleted() {
        return this.completed;
    }

    // ---------------------------------------------------------------------
    // Handling of current savepoint state
    // ---------------------------------------------------------------------

    /**
     * Set a savepoint for this transaction. Useful for PROPAGATION_NESTED.
     * 
     * @see TransactionDefinition#PROPAGATION_NESTED
     */
    protected void setSavepoint(Object savepoint) {
        this.savepoint = savepoint;
    }

    /**
     * Get the savepoint for this transaction, if any.
     */
    protected Object getSavepoint() {
        return this.savepoint;
    }

    @Override
    public boolean hasSavepoint() {
        return (this.savepoint != null);
    }

    /**
     * Create a savepoint and hold it for the transaction.
     * 
     * @throws TransactionException
     *             if the underlying transaction does not support savepoints
     */
    public void createAndHoldSavepoint() throws TransactionException {
        setSavepoint(getSavepointManager().createSavepoint());
    }

    /**
     * Roll back to the savepoint that is held for the transaction and release the
     * savepoint right afterwards.
     */
    public void rollbackToHeldSavepoint() throws TransactionException {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionException("Cannot roll back to savepoint - no savepoint associated with current transaction");
        }
        getSavepointManager().rollbackToSavepoint(savepoint);
        getSavepointManager().releaseSavepoint(savepoint);
        setSavepoint(null);
    }

    /**
     * Release the savepoint that is held for the transaction.
     */
    public void releaseHeldSavepoint() throws TransactionException {
        Object savepoint = getSavepoint();
        if (savepoint == null) {
            throw new TransactionException("Cannot release savepoint - no savepoint associated with current transaction");
        }
        getSavepointManager().releaseSavepoint(savepoint);
        setSavepoint(null);
    }

    // Implementation of SavepointManager
    // ---------------------------------------------------------------------

    /**
     * This implementation delegates to a SavepointManager for the underlying
     * transaction, if possible.
     * 
     * @see #getSavepointManager()
     * @see SavepointManager#createSavepoint()
     */
    @Override
    public Object createSavepoint() throws TransactionException {
        return getSavepointManager().createSavepoint();
    }

    /**
     * This implementation delegates to a SavepointManager for the underlying
     * transaction, if possible.
     * 
     * @see #getSavepointManager()
     * @see SavepointManager#rollbackToSavepoint(Object)
     */
    @Override
    public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().rollbackToSavepoint(savepoint);
    }

    /**
     * This implementation delegates to a SavepointManager for the underlying
     * transaction, if possible.
     * 
     * @see #getSavepointManager()
     * @see SavepointManager#releaseSavepoint(Object)
     */
    @Override
    public void releaseSavepoint(Object savepoint) throws TransactionException {
        getSavepointManager().releaseSavepoint(savepoint);
    }

    /**
     * Return a SavepointManager for the underlying transaction, if possible.
     * <p>
     */
    protected SavepointManager getSavepointManager() {
        throw new TransactionException("This transaction does not support savepoints");
    }

}
