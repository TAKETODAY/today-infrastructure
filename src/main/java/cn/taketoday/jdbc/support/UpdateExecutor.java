/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.jdbc.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

/**
 * @author TODAY <br>
 * 2019-08-18 20:21
 */
public class UpdateExecutor extends Executor implements UpdateOperation {

  public UpdateExecutor() { }

  public UpdateExecutor(final DataSource dataSource) {
    setDataSource(dataSource);
  }

  @Override
  public int update(final String sql) throws SQLException {

    if (log.isDebugEnabled()) {
      log.debug("Executing SQL update [{}]", sql);
    }

    return execute((StatementCallback<Integer>) (stmt) -> {
      final int rows = stmt.executeUpdate(sql);
      if (log.isDebugEnabled()) {
        log.debug("SQL update affected {} rows", rows);
      }
      return rows;
    });
  }

  @Override
  public int update(final String sql, final Object[] args) throws SQLException {

    if (log.isDebugEnabled()) {
      log.debug("Executing SQL update [{}]", sql);
    }
    return execute(sql, (PreparedStatementCallback<Integer>) (ps) -> {
      applyParameters(ps, args);
      return ps.executeUpdate();
    });
  }

  @Override
  public int[] batchUpdate(final String... sql) throws SQLException {

    if (log.isDebugEnabled()) {
      log.debug("Executing SQL batch update of [{}] statements", sql.length);
    }

    return execute((StatementCallback<int[]>) (stmt) -> {
      for (final String sqlStmt : sql) {
        stmt.addBatch(sqlStmt);
      }
      return stmt.executeBatch();
    });
  }

  @Override
  public int[] batchUpdate(final String sql, final List<Object[]> batchArgs) throws SQLException {

    return execute((ConnectionCallback<int[]>) (con) -> {
      try (final PreparedStatement ps = con.prepareStatement(sql)) {

        for (final Object[] params : batchArgs) {

          applyStatementSettings(ps, params);
          ps.addBatch();
        }
        return ps.executeBatch();
      }
    });

  }

}
