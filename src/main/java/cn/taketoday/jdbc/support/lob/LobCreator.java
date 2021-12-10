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

package cn.taketoday.jdbc.support.lob;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * Interface that abstracts potentially database-specific creation of large binary
 * fields and large text fields. Does not work with {@code java.sql.Blob}
 * and {@code java.sql.Clob} instances in the API, as some JDBC drivers
 * do not support these types as such.
 *
 * <p>The LOB creation part is where {@link LobHandler} implementations usually
 * differ. Possible strategies include usage of
 * {@code PreparedStatement.setBinaryStream/setCharacterStream} but also
 * {@code PreparedStatement.setBlob/setClob} with either a stream argument
 * (requires JDBC 4.0) or {@code java.sql.Blob/Clob} wrapper objects.
 *
 * <p>A LobCreator represents a session for creating BLOBs: It is <i>not</i>
 * thread-safe and needs to be instantiated for each statement execution or for
 * each transaction. Each LobCreator needs to be closed after completion.
 *
 * <p>For convenient working with a PreparedStatement and a LobCreator,
 * consider using {@link cn.taketoday.jdbc.core.JdbcTemplate} with an
 * {@link cn.taketoday.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback}
 * implementation. See the latter's javadoc for details.
 *
 * @author Juergen Hoeller
 * @see #close()
 * @see LobHandler#getLobCreator()
 * @see DefaultLobHandler.DefaultLobCreator
 * @see PreparedStatement#setBlob
 * @see PreparedStatement#setClob
 * @see PreparedStatement#setBytes
 * @see PreparedStatement#setBinaryStream
 * @see PreparedStatement#setString
 * @see PreparedStatement#setAsciiStream
 * @see PreparedStatement#setCharacterStream
 * @since 04.12.2003
 */
public interface LobCreator extends Closeable {

  /**
   * Set the given content as bytes on the given statement, using the given
   * parameter index. Might simply invoke {@code PreparedStatement.setBytes}
   * or create a Blob instance for it, depending on the database and driver.
   *
   * @param ps the PreparedStatement to the set the content on
   * @param paramIndex the parameter index to use
   * @param content the content as byte array, or {@code null} for SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see PreparedStatement#setBytes
   */
  void setBlobAsBytes(PreparedStatement ps, int paramIndex, @Nullable byte[] content)
          throws SQLException;

  /**
   * Set the given content as binary stream on the given statement, using the given
   * parameter index. Might simply invoke {@code PreparedStatement.setBinaryStream}
   * or create a Blob instance for it, depending on the database and driver.
   *
   * @param ps the PreparedStatement to the set the content on
   * @param paramIndex the parameter index to use
   * @param contentStream the content as binary stream, or {@code null} for SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see PreparedStatement#setBinaryStream
   */
  void setBlobAsBinaryStream(
          PreparedStatement ps, int paramIndex, @Nullable InputStream contentStream, int contentLength)
          throws SQLException;

  /**
   * Set the given content as String on the given statement, using the given
   * parameter index. Might simply invoke {@code PreparedStatement.setString}
   * or create a Clob instance for it, depending on the database and driver.
   *
   * @param ps the PreparedStatement to the set the content on
   * @param paramIndex the parameter index to use
   * @param content the content as String, or {@code null} for SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see PreparedStatement#setBytes
   */
  void setClobAsString(PreparedStatement ps, int paramIndex, @Nullable String content)
          throws SQLException;

  /**
   * Set the given content as ASCII stream on the given statement, using the given
   * parameter index. Might simply invoke {@code PreparedStatement.setAsciiStream}
   * or create a Clob instance for it, depending on the database and driver.
   *
   * @param ps the PreparedStatement to the set the content on
   * @param paramIndex the parameter index to use
   * @param asciiStream the content as ASCII stream, or {@code null} for SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see PreparedStatement#setAsciiStream
   */
  void setClobAsAsciiStream(
          PreparedStatement ps, int paramIndex, @Nullable InputStream asciiStream, int contentLength)
          throws SQLException;

  /**
   * Set the given content as character stream on the given statement, using the given
   * parameter index. Might simply invoke {@code PreparedStatement.setCharacterStream}
   * or create a Clob instance for it, depending on the database and driver.
   *
   * @param ps the PreparedStatement to the set the content on
   * @param paramIndex the parameter index to use
   * @param characterStream the content as character stream, or {@code null} for SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see PreparedStatement#setCharacterStream
   */
  void setClobAsCharacterStream(
          PreparedStatement ps, int paramIndex, @Nullable Reader characterStream, int contentLength)
          throws SQLException;

  /**
   * Close this LobCreator session and free its temporarily created BLOBs and CLOBs.
   * Will not need to do anything if using PreparedStatement's standard methods,
   * but might be necessary to free database resources if using proprietary means.
   * <p><b>NOTE</b>: Needs to be invoked after the involved PreparedStatements have
   * been executed or the affected O/R mapping sessions have been flushed.
   * Otherwise, the database resources for the temporary BLOBs might stay allocated.
   */
  @Override
  void close();

}
