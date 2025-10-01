/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jdbc.core.support;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.dao.DataAccessException;
import infra.dao.EmptyResultDataAccessException;
import infra.dao.IncorrectResultSizeDataAccessException;
import infra.jdbc.LobRetrievalFailureException;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.support.lob.LobHandler;
import infra.util.FileCopyUtils;

/**
 * Abstract ResultSetExtractor implementation that assumes streaming of LOB data.
 * Typically used as inner class, with access to surrounding method arguments.
 *
 * <p>Delegates to the {@code streamData} template method for streaming LOB
 * content to some OutputStream, typically using a LobHandler. Converts an
 * IOException thrown during streaming to a LobRetrievalFailureException.
 *
 * <p>A usage example with JdbcTemplate:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * final LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.query(
 * 		 "SELECT content FROM imagedb WHERE image_name=?", new Object[] {name},
 * 		 new AbstractLobStreamingResultSetExtractor() {
 * 			 public void streamData(ResultSet rs) throws SQLException, IOException {
 * 				 FileCopyUtils.copy(lobHandler.getBlobAsBinaryStream(rs, 1), contentStream);
 *             }
 *         }
 * );</pre>
 *
 * @param <T> the result type
 * @author Juergen Hoeller
 * @see LobHandler
 * @see LobRetrievalFailureException
 * @since 4.0
 */
public abstract class AbstractLobStreamingResultSetExtractor<T> implements ResultSetExtractor<T> {

  /**
   * Delegates to handleNoRowFound, handleMultipleRowsFound and streamData,
   * according to the ResultSet state. Converts an IOException thrown by
   * streamData to a LobRetrievalFailureException.
   *
   * @see #handleNoRowFound
   * @see #handleMultipleRowsFound
   * @see #streamData
   * @see LobRetrievalFailureException
   */
  @Override
  @Nullable
  public final T extractData(ResultSet rs) throws SQLException, DataAccessException {
    if (!rs.next()) {
      handleNoRowFound();
    }
    else {
      try {
        streamData(rs);
        if (rs.next()) {
          handleMultipleRowsFound();
        }
      }
      catch (IOException ex) {
        throw new LobRetrievalFailureException("Could not stream LOB content", ex);
      }
    }
    return null;
  }

  /**
   * Handle the case where the ResultSet does not contain a row.
   *
   * @throws DataAccessException a corresponding exception,
   * by default an EmptyResultDataAccessException
   * @see infra.dao.EmptyResultDataAccessException
   */
  protected void handleNoRowFound() throws DataAccessException {
    throw new EmptyResultDataAccessException(
            "LobStreamingResultSetExtractor did not find row in database", 1);
  }

  /**
   * Handle the case where the ResultSet contains multiple rows.
   *
   * @throws DataAccessException a corresponding exception,
   * by default an IncorrectResultSizeDataAccessException
   * @see infra.dao.IncorrectResultSizeDataAccessException
   */
  protected void handleMultipleRowsFound() throws DataAccessException {
    throw new IncorrectResultSizeDataAccessException(
            "LobStreamingResultSetExtractor found multiple rows in database", 1);
  }

  /**
   * Stream LOB content from the given ResultSet to some OutputStream.
   * <p>Typically used as inner class, with access to surrounding method arguments
   * and to a LobHandler instance variable of the surrounding class.
   *
   * @param rs the ResultSet to take the LOB content from
   * @throws SQLException if thrown by JDBC methods
   * @throws IOException if thrown by stream access methods
   * @throws DataAccessException in case of custom exceptions
   * @see LobHandler#getBlobAsBinaryStream
   * @see FileCopyUtils
   */
  protected abstract void streamData(ResultSet rs) throws SQLException, IOException, DataAccessException;

}
