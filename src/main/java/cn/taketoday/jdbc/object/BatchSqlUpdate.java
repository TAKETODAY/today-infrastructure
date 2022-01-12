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

package cn.taketoday.jdbc.object;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.core.BatchPreparedStatementSetter;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * SqlUpdate subclass that performs batch update operations. Encapsulates
 * queuing up records to be updated, and adds them as a single batch once
 * {@code flush} is called or the given batch size has been met.
 *
 * <p>Note that this class is a <b>non-thread-safe object</b>, in contrast
 * to all other JDBC operations objects in this package. You need to create
 * a new instance of it for each use, or call {@code reset} before
 * reuse within the same thread.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @see #flush
 * @see #reset
 * @since 4.0
 */
public class BatchSqlUpdate extends SqlUpdate {
  private static final Logger log = LoggerFactory.getLogger(BatchSqlUpdate.class);

  /**
   * Default number of inserts to accumulate before committing a batch (5000).
   */
  public static final int DEFAULT_BATCH_SIZE = 5000;

  private int batchSize = DEFAULT_BATCH_SIZE;

  private boolean trackRowsAffected = true;

  private final Deque<Object[]> parameterQueue = new ArrayDeque<>();

  private final List<Integer> rowsAffected = new ArrayList<>();

  /**
   * Constructor to allow use as a JavaBean. DataSource and SQL
   * must be supplied before compilation and use.
   *
   * @see #setDataSource
   * @see #setSql
   */
  public BatchSqlUpdate() {
    super();
  }

  /**
   * Construct an update object with a given DataSource and SQL.
   *
   * @param ds the DataSource to use to obtain connections
   * @param sql the SQL statement to execute
   */
  public BatchSqlUpdate(DataSource ds, String sql) {
    super(ds, sql);
  }

  /**
   * Construct an update object with a given DataSource, SQL
   * and anonymous parameters.
   *
   * @param ds the DataSource to use to obtain connections
   * @param sql the SQL statement to execute
   * @param types the SQL types of the parameters, as defined in the
   * {@code java.sql.Types} class
   * @see java.sql.Types
   */
  public BatchSqlUpdate(DataSource ds, String sql, int[] types) {
    super(ds, sql, types);
  }

  /**
   * Construct an update object with a given DataSource, SQL,
   * anonymous parameters and specifying the maximum number of rows
   * that may be affected.
   *
   * @param ds the DataSource to use to obtain connections
   * @param sql the SQL statement to execute
   * @param types the SQL types of the parameters, as defined in the
   * {@code java.sql.Types} class
   * @param batchSize the number of statements that will trigger
   * an automatic intermediate flush
   * @see java.sql.Types
   */
  public BatchSqlUpdate(DataSource ds, String sql, int[] types, int batchSize) {
    super(ds, sql, types);
    setBatchSize(batchSize);
  }

  /**
   * Set the number of statements that will trigger an automatic intermediate
   * flush. {@code update} calls or the given statement parameters will
   * be queued until the batch size is met, at which point it will empty the
   * queue and execute the batch.
   * <p>You can also flush already queued statements with an explicit
   * {@code flush} call. Note that you need to this after queueing
   * all parameters to guarantee that all statements have been flushed.
   */
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  /**
   * Set whether to track the rows affected by batch updates performed
   * by this operation object.
   * <p>Default is "true". Turn this off to save the memory needed for
   * the list of row counts.
   *
   * @see #getRowsAffected()
   */
  public void setTrackRowsAffected(boolean trackRowsAffected) {
    this.trackRowsAffected = trackRowsAffected;
  }

  /**
   * BatchSqlUpdate does not support BLOB or CLOB parameters.
   */
  @Override
  protected boolean supportsLobParameters() {
    return false;
  }

  /**
   * Overridden version of {@code update} that adds the given statement
   * parameters to the queue rather than executing them immediately.
   * All other {@code update} methods of the SqlUpdate base class go
   * through this method and will thus behave similarly.
   * <p>You need to call {@code flush} to actually execute the batch.
   * If the specified batch size is reached, an implicit flush will happen;
   * you still need to finally call {@code flush} to flush all statements.
   *
   * @param params array of parameter objects
   * @return the number of rows affected by the update (always -1,
   * meaning "not applicable", as the statement is not actually
   * executed by this method)
   * @see #flush
   */
  @Override
  public int update(Object... params) throws DataAccessException {
    validateParameters(params);
    this.parameterQueue.add(params.clone());

    if (this.parameterQueue.size() == this.batchSize) {
      if (log.isDebugEnabled()) {
        log.debug("Triggering auto-flush because queue reached batch size of {}", this.batchSize);
      }
      flush();
    }

    return -1;
  }

  /**
   * Trigger any queued update operations to be added as a final batch.
   *
   * @return an array of the number of rows affected by each statement
   */
  public int[] flush() {
    if (this.parameterQueue.isEmpty()) {
      return new int[0];
    }

    int[] rowsAffected = getJdbcTemplate().batchUpdate(
            resolveSql(),
            new BatchPreparedStatementSetter() {
              @Override
              public int getBatchSize() {
                return parameterQueue.size();
              }

              @Override
              public void setValues(PreparedStatement ps, int index) throws SQLException {
                Object[] params = parameterQueue.removeFirst();
                newPreparedStatementSetter(params).setValues(ps);
              }
            });

    for (int rowCount : rowsAffected) {
      checkRowsAffected(rowCount);
      if (this.trackRowsAffected) {
        this.rowsAffected.add(rowCount);
      }
    }

    return rowsAffected;
  }

  /**
   * Return the current number of statements or statement parameters
   * in the queue.
   */
  public int getQueueCount() {
    return this.parameterQueue.size();
  }

  /**
   * Return the number of already executed statements.
   */
  public int getExecutionCount() {
    return this.rowsAffected.size();
  }

  /**
   * Return the number of affected rows for all already executed statements.
   * Accumulates all of {@code flush}'s return values until
   * {@code reset} is invoked.
   *
   * @return an array of the number of rows affected by each statement
   * @see #reset
   */
  public int[] getRowsAffected() {
    int size = this.rowsAffected.size();
    int[] result = new int[size];
    for (int i = 0; i < size; i++) {
      result[i] = this.rowsAffected.get(i);
    }
    return result;
  }

  /**
   * Reset the statement parameter queue, the rows affected cache,
   * and the execution count.
   */
  public void reset() {
    this.parameterQueue.clear();
    this.rowsAffected.clear();
  }

}
