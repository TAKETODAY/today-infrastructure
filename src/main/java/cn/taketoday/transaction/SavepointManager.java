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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction;

/**
 * Interface that specifies an API to programmatically manage transaction
 * savepoints in a generic fashion. Extended by TransactionStatus to
 * expose savepoint management functionality for a specific transaction.
 *
 * <p>Note that savepoints can only work within an active transaction.
 * Just use this programmatic savepoint handling for advanced needs;
 * else, a subtransaction with PROPAGATION_NESTED is preferable.
 *
 * <p>This interface is inspired by JDBC 3.0's Savepoint mechanism
 * but is independent from any specific persistence technology.
 *
 * @author Juergen Hoeller
 * @author TODAY
 * @see TransactionStatus
 * @see TransactionDefinition#PROPAGATION_NESTED
 * @see java.sql.Savepoint
 * @since 2018-11-16 21:25
 */
public interface SavepointManager {

  /**
   * Create a new savepoint. You can roll back to a specific savepoint
   * via {@code rollbackToSavepoint}, and explicitly release a savepoint
   * that you don't need anymore via {@code releaseSavepoint}.
   * <p>Note that most transaction managers will automatically release
   * savepoints at transaction completion.
   *
   * @return a savepoint object, to be passed into
   * {@link #rollbackToSavepoint} or {@link #releaseSavepoint}
   * @throws NestedTransactionNotSupportedException if the underlying
   * transaction does not support savepoints
   * @throws TransactionException if the savepoint could not be created,
   * for example because the transaction is not in an appropriate state
   * @see java.sql.Connection#setSavepoint
   */
  Object createSavepoint() throws TransactionException;

  /**
   * Roll back to the given savepoint.
   * <p>The savepoint will <i>not</i> be automatically released afterwards.
   * You may explicitly call {@link #releaseSavepoint(Object)} or rely on
   * automatic release on transaction completion.
   *
   * @param savepoint the savepoint to roll back to
   * @throws NestedTransactionNotSupportedException if the underlying
   * transaction does not support savepoints
   * @throws TransactionException if the rollback failed
   * @see java.sql.Connection#rollback(java.sql.Savepoint)
   */
  void rollbackToSavepoint(Object savepoint) throws TransactionException;

  /**
   * Explicitly release the given savepoint.
   * <p>Note that most transaction managers will automatically release
   * savepoints on transaction completion.
   * <p>Implementations should fail as silently as possible if proper
   * resource cleanup will eventually happen at transaction completion.
   *
   * @param savepoint the savepoint to release
   * @throws NestedTransactionNotSupportedException if the underlying
   * transaction does not support savepoints
   * @throws TransactionException if the release failed
   * @see java.sql.Connection#releaseSavepoint
   */
  void releaseSavepoint(Object savepoint) throws TransactionException;

}
