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

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * Abstraction for handling large binary fields and large text fields in
 * specific databases, no matter if represented as simple types or Large OBjects.
 *
 * <p>Provides accessor methods for BLOBs and CLOBs, and acts as factory for
 * LobCreator instances, to be used as sessions for creating BLOBs or CLOBs.
 * LobCreators are typically instantiated for each statement execution or for
 * each transaction; they are not thread-safe because they might track
 * allocated database resources in order to free them after execution.
 *
 * <p>Most databases/drivers should be able to work with {@link DefaultLobHandler},
 * which by default delegates to JDBC's direct accessor methods, avoiding the
 * {@code java.sql.Blob} and {@code java.sql.Clob} API completely.
 * {@link DefaultLobHandler} can also be configured to access LOBs using
 * {@code PreparedStatement.setBlob/setClob} (e.g. for PostgreSQL), through
 * setting the {@link DefaultLobHandler#setWrapAsLob "wrapAsLob"} property.
 *
 * <p>Of course, you need to declare different field types for each database.
 * In Oracle, any binary content needs to go into a BLOB, and all character content
 * beyond 4000 bytes needs to go into a CLOB. In MySQL, there is no notion of a
 * CLOB type but rather a LONGTEXT type that behaves like a VARCHAR. For complete
 * portability, use a LobHandler for fields that might typically require LOBs on
 * some database because of the field size (take Oracle's numbers as a guideline).
 *
 * <p><b>Summarizing the recommended options (for actual LOB fields):</b>
 * <ul>
 * <li><b>JDBC 4.0 driver (including Oracle 11g driver):</b> Use {@link DefaultLobHandler},
 * potentially with {@code streamAsLob=true} if your database driver requires that
 * hint when populating a LOB field. Fall back to {@code createTemporaryLob=true}
 * if you happen to run into LOB size limitations with your (Oracle) database setup.
 * <li><b>Oracle 10g driver:</b> Use {@link DefaultLobHandler} with standard setup.
 * On Oracle 10.1, set the "SetBigStringTryClob" connection property; as of Oracle 10.2,
 * DefaultLobHandler should work with standard setup out of the box.
 * <li><b>PostgreSQL:</b> Configure {@link DefaultLobHandler} with {@code wrapAsLob=true},
 * and use that LobHandler to access OID columns (but not BYTEA) in your database tables.
 * <li>For all other database drivers (and for non-LOB fields that might potentially
 * turn into LOBs on some databases): Simply use a plain {@link DefaultLobHandler}.
 * </ul>
 *
 * @author Juergen Hoeller
 * @see DefaultLobHandler
 * @see ResultSet#getBlob
 * @see ResultSet#getClob
 * @see ResultSet#getBytes
 * @see ResultSet#getBinaryStream
 * @see ResultSet#getString
 * @see ResultSet#getAsciiStream
 * @see ResultSet#getCharacterStream
 * @since 4.0
 */
public interface LobHandler {

  /**
   * Retrieve the given column as bytes from the given ResultSet.
   * Might simply invoke {@code ResultSet.getBytes} or work with
   * {@code ResultSet.getBlob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnName the column name to use
   * @return the content as byte array, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getBytes
   */
  @Nullable
  byte[] getBlobAsBytes(ResultSet rs, String columnName) throws SQLException;

  /**
   * Retrieve the given column as bytes from the given ResultSet.
   * Might simply invoke {@code ResultSet.getBytes} or work with
   * {@code ResultSet.getBlob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnIndex the column index to use
   * @return the content as byte array, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getBytes
   */
  @Nullable
  byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * Retrieve the given column as binary stream from the given ResultSet.
   * Might simply invoke {@code ResultSet.getBinaryStream} or work with
   * {@code ResultSet.getBlob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnName the column name to use
   * @return the content as binary stream, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getBinaryStream
   */
  @Nullable
  InputStream getBlobAsBinaryStream(ResultSet rs, String columnName) throws SQLException;

  /**
   * Retrieve the given column as binary stream from the given ResultSet.
   * Might simply invoke {@code ResultSet.getBinaryStream} or work with
   * {@code ResultSet.getBlob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnIndex the column index to use
   * @return the content as binary stream, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getBinaryStream
   */
  @Nullable
  InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * Retrieve the given column as String from the given ResultSet.
   * Might simply invoke {@code ResultSet.getString} or work with
   * {@code ResultSet.getClob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnName the column name to use
   * @return the content as String, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getString
   */
  @Nullable
  String getClobAsString(ResultSet rs, String columnName) throws SQLException;

  /**
   * Retrieve the given column as String from the given ResultSet.
   * Might simply invoke {@code ResultSet.getString} or work with
   * {@code ResultSet.getClob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnIndex the column index to use
   * @return the content as String, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getString
   */
  @Nullable
  String getClobAsString(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * Retrieve the given column as ASCII stream from the given ResultSet.
   * Might simply invoke {@code ResultSet.getAsciiStream} or work with
   * {@code ResultSet.getClob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnName the column name to use
   * @return the content as ASCII stream, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getAsciiStream
   */
  @Nullable
  InputStream getClobAsAsciiStream(ResultSet rs, String columnName) throws SQLException;

  /**
   * Retrieve the given column as ASCII stream from the given ResultSet.
   * Might simply invoke {@code ResultSet.getAsciiStream} or work with
   * {@code ResultSet.getClob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnIndex the column index to use
   * @return the content as ASCII stream, or {@code null} in case of SQL NULL
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getAsciiStream
   */
  @Nullable
  InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * Retrieve the given column as character stream from the given ResultSet.
   * Might simply invoke {@code ResultSet.getCharacterStream} or work with
   * {@code ResultSet.getClob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnName the column name to use
   * @return the content as character stream
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getCharacterStream
   */
  Reader getClobAsCharacterStream(ResultSet rs, String columnName) throws SQLException;

  /**
   * Retrieve the given column as character stream from the given ResultSet.
   * Might simply invoke {@code ResultSet.getCharacterStream} or work with
   * {@code ResultSet.getClob}, depending on the database and driver.
   *
   * @param rs the ResultSet to retrieve the content from
   * @param columnIndex the column index to use
   * @return the content as character stream
   * @throws SQLException if thrown by JDBC methods
   * @see ResultSet#getCharacterStream
   */
  Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException;

  /**
   * Create a new {@link LobCreator} instance, i.e. a session for creating BLOBs
   * and CLOBs. Needs to be closed after the created LOBs are not needed anymore -
   * typically after statement execution or transaction completion.
   *
   * @return the new LobCreator instance
   * @see LobCreator#close()
   */
  LobCreator getLobCreator();

}
