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

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Types;

import javax.sql.DataSource;

import cn.taketoday.jdbc.core.SqlParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 22.02.2005
 */
public class BatchSqlUpdateTests {

  @Test
  public void testBatchUpdateWithExplicitFlush() throws Exception {
    doTestBatchUpdate(false);
  }

  @Test
  public void testBatchUpdateWithFlushThroughBatchSize() throws Exception {
    doTestBatchUpdate(true);
  }

  private void doTestBatchUpdate(boolean flushThroughBatchSize) throws Exception {
    final String sql = "UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?";
    final int[] ids = new int[] { 100, 200 };
    final int[] rowsAffected = new int[] { 1, 2 };

    Connection connection = mock(Connection.class);
    DataSource dataSource = mock(DataSource.class);
    given(dataSource.getConnection()).willReturn(connection);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    given(preparedStatement.getConnection()).willReturn(connection);
    given(preparedStatement.executeBatch()).willReturn(rowsAffected);

    DatabaseMetaData mockDatabaseMetaData = mock(DatabaseMetaData.class);
    given(mockDatabaseMetaData.supportsBatchUpdates()).willReturn(true);
    given(connection.prepareStatement(sql)).willReturn(preparedStatement);
    given(connection.getMetaData()).willReturn(mockDatabaseMetaData);

    BatchSqlUpdate update = new BatchSqlUpdate(dataSource, sql);
    update.declareParameter(new SqlParameter(Types.INTEGER));
    if (flushThroughBatchSize) {
      update.setBatchSize(2);
    }

    update.update(ids[0]);
    update.update(ids[1]);

    if (flushThroughBatchSize) {
      assertThat(update.getQueueCount()).isEqualTo(0);
      assertThat(update.getRowsAffected().length).isEqualTo(2);
    }
    else {
      assertThat(update.getQueueCount()).isEqualTo(2);
      assertThat(update.getRowsAffected().length).isEqualTo(0);
    }

    int[] actualRowsAffected = update.flush();
    assertThat(update.getQueueCount()).isEqualTo(0);

    if (flushThroughBatchSize) {
      assertThat(actualRowsAffected.length == 0).as("flush did not execute updates").isTrue();
    }
    else {
      assertThat(actualRowsAffected.length == 2).as("executed 2 updates").isTrue();
      assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
      assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);
    }

    actualRowsAffected = update.getRowsAffected();
    assertThat(actualRowsAffected.length == 2).as("executed 2 updates").isTrue();
    assertThat(actualRowsAffected[0]).isEqualTo(rowsAffected[0]);
    assertThat(actualRowsAffected[1]).isEqualTo(rowsAffected[1]);

    update.reset();
    assertThat(update.getRowsAffected().length).isEqualTo(0);

    verify(preparedStatement).setObject(1, ids[0], Types.INTEGER);
    verify(preparedStatement).setObject(1, ids[1], Types.INTEGER);
    verify(preparedStatement, times(2)).addBatch();
    verify(preparedStatement).close();
  }
}
