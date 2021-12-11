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

package cn.taketoday.jdbc.core.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.jdbc.core.InterruptibleBatchPreparedStatementSetter;

/**
 * Abstract implementation of the {@link InterruptibleBatchPreparedStatementSetter}
 * interface, combining the check for available values and setting of those
 * into a single callback method {@link #setValuesIfAvailable}.
 *
 * @author Juergen Hoeller
 * @see #setValuesIfAvailable
 * @since 4.0
 */
public abstract class AbstractInterruptibleBatchPreparedStatementSetter
        implements InterruptibleBatchPreparedStatementSetter {

  private boolean exhausted;

  /**
   * This implementation calls {@link #setValuesIfAvailable}
   * and sets this instance's exhaustion flag accordingly.
   */
  @Override
  public final void setValues(PreparedStatement ps, int i) throws SQLException {
    this.exhausted = !setValuesIfAvailable(ps, i);
  }

  /**
   * This implementation return this instance's current exhaustion flag.
   */
  @Override
  public final boolean isBatchExhausted(int i) {
    return this.exhausted;
  }

  /**
   * This implementation returns {@code Integer.MAX_VALUE}.
   * Can be overridden in subclasses to lower the maximum batch size.
   */
  @Override
  public int getBatchSize() {
    return Integer.MAX_VALUE;
  }

  /**
   * Check for available values and set them on the given PreparedStatement.
   * If no values are available anymore, return {@code false}.
   *
   * @param ps the PreparedStatement we'll invoke setter methods on
   * @param i index of the statement we're issuing in the batch, starting from 0
   * @return whether there were values to apply (that is, whether the applied
   * parameters should be added to the batch and this method should be called
   * for a further iteration)
   * @throws SQLException if an SQLException is encountered
   * (i.e. there is no need to catch SQLException)
   */
  protected abstract boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException;

}
