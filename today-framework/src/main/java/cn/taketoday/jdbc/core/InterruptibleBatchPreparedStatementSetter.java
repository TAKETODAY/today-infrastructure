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

package cn.taketoday.jdbc.core;

/**
 * Extension of the {@link BatchPreparedStatementSetter} interface,
 * adding a batch exhaustion check.
 *
 * <p>This interface allows you to signal the end of a batch rather than
 * having to determine the exact batch size upfront. Batch size is still
 * being honored but it is now the maximum size of the batch.
 *
 * <p>The {@link #isBatchExhausted} method is called after each call to
 * {@link #setValues} to determine whether there were some values added,
 * or if the batch was determined to be complete and no additional values
 * were provided during the last call to {@code setValues}.
 *
 * <p>Consider extending the
 * {@link cn.taketoday.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter}
 * base class instead of implementing this interface directly, using a single
 * {@code setValuesIfAvailable} callback method that checks for available
 * values and sets them, returning whether values have actually been provided.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
 * @see cn.taketoday.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter
 * @since 4.0
 */
public interface InterruptibleBatchPreparedStatementSetter extends BatchPreparedStatementSetter {

  /**
   * Return whether the batch is complete, that is, whether there were no
   * additional values added during the last {@code setValues} call.
   * <p><b>NOTE:</b> If this method returns {@code true}, any parameters
   * that might have been set during the last {@code setValues} call will
   * be ignored! Make sure that you set a corresponding internal flag if you
   * detect exhaustion <i>at the beginning</i> of your {@code setValues}
   * implementation, letting this method return {@code true} based on the flag.
   *
   * @param i index of the statement we're issuing in the batch, starting from 0
   * @return whether the batch is already exhausted
   * @see #setValues
   * @see cn.taketoday.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter#setValuesIfAvailable
   */
  boolean isBatchExhausted(int i);

}
