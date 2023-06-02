/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.jdbc.core.DisposableSqlTypeValue;
import cn.taketoday.jdbc.support.lob.DefaultLobHandler;
import cn.taketoday.jdbc.support.lob.LobCreator;
import cn.taketoday.jdbc.support.lob.LobHandler;
import cn.taketoday.lang.Nullable;

/**
 * Object to represent an SQL BLOB/CLOB value parameter. BLOBs can either be an
 * InputStream or a byte array. CLOBs can be in the form of a Reader, InputStream,
 * or String. Each CLOB/BLOB value will be stored together with its length.
 * The type is based on which constructor is used. Instances of this class are
 * stateful and immutable: use them and discard them.
 *
 * <p>This class holds a reference to a {@link LobCreator} that must be closed after
 * the update has completed. This is done via a call to the {@link #cleanup()} method.
 * All handling of the {@code LobCreator} is done by the framework classes that use it -
 * no need to set or close the {@code LobCreator} for end users of this class.
 *
 * <p>A usage example:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.update(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new Object[] {
 *       name,
 *       new SqlLobValue(contentStream, contentLength, lobHandler),
 *       new SqlLobValue(description, lobHandler)
 *     },
 *     new int[] {Types.VARCHAR, Types.BLOB, Types.CLOB});
 * </pre>
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see cn.taketoday.jdbc.support.lob.LobHandler
 * @see cn.taketoday.jdbc.support.lob.LobCreator
 * @see cn.taketoday.jdbc.core.JdbcTemplate#update(String, Object[], int[])
 * @see cn.taketoday.jdbc.object.SqlUpdate#update(Object[])
 * @see cn.taketoday.jdbc.object.StoredProcedure#execute(java.util.Map)
 * @since 4.0
 */
public class SqlLobValue implements DisposableSqlTypeValue {

  @Nullable
  private final Object content;

  private final int length;

  /**
   * Reference to the LobCreator - so we can close it once the update is done.
   */
  private final LobCreator lobCreator;

  /**
   * Create a new BLOB value with the given byte array,
   * using a DefaultLobHandler.
   *
   * @param bytes the byte array containing the BLOB value
   * @see cn.taketoday.jdbc.support.lob.DefaultLobHandler
   */
  public SqlLobValue(@Nullable byte[] bytes) {
    this(bytes, new DefaultLobHandler());
  }

  /**
   * Create a new BLOB value with the given byte array.
   *
   * @param bytes the byte array containing the BLOB value
   * @param lobHandler the LobHandler to be used
   */
  public SqlLobValue(@Nullable byte[] bytes, LobHandler lobHandler) {
    this.content = bytes;
    this.length = (bytes != null ? bytes.length : 0);
    this.lobCreator = lobHandler.getLobCreator();
  }

  /**
   * Create a new CLOB value with the given content string,
   * using a DefaultLobHandler.
   *
   * @param content the String containing the CLOB value
   * @see cn.taketoday.jdbc.support.lob.DefaultLobHandler
   */
  public SqlLobValue(@Nullable String content) {
    this(content, new DefaultLobHandler());
  }

  /**
   * Create a new CLOB value with the given content string.
   *
   * @param content the String containing the CLOB value
   * @param lobHandler the LobHandler to be used
   */
  public SqlLobValue(@Nullable String content, LobHandler lobHandler) {
    this.content = content;
    this.length = (content != null ? content.length() : 0);
    this.lobCreator = lobHandler.getLobCreator();
  }

  /**
   * Create a new BLOB/CLOB value with the given stream,
   * using a DefaultLobHandler.
   *
   * @param stream the stream containing the LOB value
   * @param length the length of the LOB value
   * @see cn.taketoday.jdbc.support.lob.DefaultLobHandler
   */
  public SqlLobValue(InputStream stream, int length) {
    this(stream, length, new DefaultLobHandler());
  }

  /**
   * Create a new BLOB/CLOB value with the given stream.
   *
   * @param stream the stream containing the LOB value
   * @param length the length of the LOB value
   * @param lobHandler the LobHandler to be used
   */
  public SqlLobValue(InputStream stream, int length, LobHandler lobHandler) {
    this.content = stream;
    this.length = length;
    this.lobCreator = lobHandler.getLobCreator();
  }

  /**
   * Create a new CLOB value with the given character stream,
   * using a DefaultLobHandler.
   *
   * @param reader the character stream containing the CLOB value
   * @param length the length of the CLOB value
   * @see cn.taketoday.jdbc.support.lob.DefaultLobHandler
   */
  public SqlLobValue(Reader reader, int length) {
    this(reader, length, new DefaultLobHandler());
  }

  /**
   * Create a new CLOB value with the given character stream.
   *
   * @param reader the character stream containing the CLOB value
   * @param length the length of the CLOB value
   * @param lobHandler the LobHandler to be used
   */
  public SqlLobValue(Reader reader, int length, LobHandler lobHandler) {
    this.content = reader;
    this.length = length;
    this.lobCreator = lobHandler.getLobCreator();
  }

  /**
   * Set the specified content via the LobCreator.
   */
  @Override
  public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
          throws SQLException {

    if (sqlType == Types.BLOB) {
      if (this.content instanceof byte[] || this.content == null) {
        this.lobCreator.setBlobAsBytes(ps, paramIndex, (byte[]) this.content);
      }
      else if (this.content instanceof String) {
        this.lobCreator.setBlobAsBytes(ps, paramIndex, ((String) this.content).getBytes());
      }
      else if (this.content instanceof InputStream) {
        this.lobCreator.setBlobAsBinaryStream(ps, paramIndex, (InputStream) this.content, this.length);
      }
      else {
        throw new IllegalArgumentException(
                "Content type [" + this.content.getClass().getName() + "] not supported for BLOB columns");
      }
    }
    else if (sqlType == Types.CLOB) {
      if (this.content instanceof String || this.content == null) {
        this.lobCreator.setClobAsString(ps, paramIndex, (String) this.content);
      }
      else if (this.content instanceof InputStream) {
        this.lobCreator.setClobAsAsciiStream(ps, paramIndex, (InputStream) this.content, this.length);
      }
      else if (this.content instanceof Reader) {
        this.lobCreator.setClobAsCharacterStream(ps, paramIndex, (Reader) this.content, this.length);
      }
      else {
        throw new IllegalArgumentException(
                "Content type [" + this.content.getClass().getName() + "] not supported for CLOB columns");
      }
    }
    else {
      throw new IllegalArgumentException("SqlLobValue only supports SQL types BLOB and CLOB");
    }
  }

  /**
   * Close the LobCreator, if any.
   */
  @Override
  public void cleanup() {
    this.lobCreator.close();
  }

}
