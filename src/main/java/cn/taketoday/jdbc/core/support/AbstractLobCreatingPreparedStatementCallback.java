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

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.core.PreparedStatementCallback;
import cn.taketoday.jdbc.support.lob.LobCreator;
import cn.taketoday.jdbc.support.lob.LobHandler;
import cn.taketoday.lang.Assert;

/**
 * Abstract {@link PreparedStatementCallback} implementation that manages a {@link LobCreator}.
 * Typically used as inner class, with access to surrounding method arguments.
 *
 * <p>Delegates to the {@code setValues} template method for setting values
 * on the PreparedStatement, using a given LobCreator for BLOB/CLOB arguments.
 *
 * <p>A usage example with {@link cn.taketoday.jdbc.core.JdbcTemplate}:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.execute(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new AbstractLobCreatingPreparedStatementCallback(lobHandler) {
 *       protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException {
 *         ps.setString(1, name);
 *         lobCreator.setBlobAsBinaryStream(ps, 2, contentStream, contentLength);
 *         lobCreator.setClobAsString(ps, 3, description);
 *       }
 *     }
 * );</pre>
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.jdbc.support.lob.LobCreator
 * @since 4.0
 */
public abstract class AbstractLobCreatingPreparedStatementCallback implements PreparedStatementCallback<Integer> {

  private final LobHandler lobHandler;

  /**
   * Create a new AbstractLobCreatingPreparedStatementCallback for the
   * given LobHandler.
   *
   * @param lobHandler the LobHandler to create LobCreators with
   */
  public AbstractLobCreatingPreparedStatementCallback(LobHandler lobHandler) {
    Assert.notNull(lobHandler, "LobHandler must not be null");
    this.lobHandler = lobHandler;
  }

  @Override
  public final Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
    try (LobCreator lobCreator = this.lobHandler.getLobCreator()) {
      setValues(ps, lobCreator);
      return ps.executeUpdate();
    }
  }

  /**
   * Set values on the given PreparedStatement, using the given
   * LobCreator for BLOB/CLOB arguments.
   *
   * @param ps the PreparedStatement to use
   * @param lobCreator the LobCreator to use
   * @throws SQLException if thrown by JDBC methods
   * @throws DataAccessException in case of custom exceptions
   */
  protected abstract void setValues(PreparedStatement ps, LobCreator lobCreator)
          throws SQLException, DataAccessException;

}
