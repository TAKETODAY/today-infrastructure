/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.core.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.jdbc.core.SqlTypeValue;
import cn.taketoday.lang.Nullable;

/**
 * Object to represent a binary parameter value for a SQL statement, e.g.
 * a binary stream for a BLOB or a LONGVARBINARY or PostgreSQL BYTEA column.
 *
 * <p>Designed for use with {@link cn.taketoday.jdbc.core.JdbcTemplate}
 * as well as {@link cn.taketoday.jdbc.core.simple.JdbcClient}, to be
 * passed in as a parameter value wrapping the target content value. Can be
 * combined with {@link cn.taketoday.jdbc.core.SqlParameterValue} for
 * specifying a SQL type, e.g.
 * {@code new SqlParameterValue(Types.BLOB, new SqlBinaryValue(myContent))}.
 * With most database drivers, the type hint is not actually necessary.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SqlCharacterValue
 * @see cn.taketoday.jdbc.core.SqlParameterValue
 * @since 4.0
 */
public class SqlBinaryValue implements SqlTypeValue {

  private final Object content;

  private final long length;

  /**
   * Create a new {@code SqlBinaryValue} for the given content.
   *
   * @param bytes the content as a byte array
   */
  public SqlBinaryValue(byte[] bytes) {
    this.content = bytes;
    this.length = bytes.length;
  }

  /**
   * Create a new {@code SqlBinaryValue} for the given content.
   *
   * @param stream the content stream
   * @param length the length of the content
   */
  public SqlBinaryValue(InputStream stream, long length) {
    this.content = stream;
    this.length = length;
  }

  /**
   * Create a new {@code SqlBinaryValue} for the given content.
   * <p>Consider specifying a {@link Resource} with content length support
   * when available: {@link SqlBinaryValue#SqlBinaryValue(Resource)}.
   *
   * @param resource the resource to obtain a content stream from
   * @param length the length of the content
   */
  public SqlBinaryValue(InputStreamSource resource, long length) {
    this.content = resource;
    this.length = length;
  }

  /**
   * Create a new {@code SqlBinaryValue} for the given content.
   * <p>The length will get derived from {@link Resource#contentLength()}.
   *
   * @param resource the resource to obtain a content stream from
   */
  public SqlBinaryValue(Resource resource) {
    this.content = resource;
    this.length = -1;
  }

  @Override
  public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
          throws SQLException {

    if (this.content instanceof byte[] bytes) {
      setByteArray(ps, paramIndex, sqlType, bytes);
    }
    else if (this.content instanceof InputStream inputStream) {
      setInputStream(ps, paramIndex, sqlType, inputStream, this.length);
    }
    else if (this.content instanceof Resource resource) {
      try {
        setInputStream(ps, paramIndex, sqlType, resource.getInputStream(), resource.contentLength());
      }
      catch (IOException ex) {
        throw new IllegalArgumentException("Cannot open binary stream for JDBC value: " + resource, ex);
      }
    }
    else if (this.content instanceof InputStreamSource resource) {
      try {
        setInputStream(ps, paramIndex, sqlType, resource.getInputStream(), this.length);
      }
      catch (IOException ex) {
        throw new IllegalArgumentException("Cannot open binary stream for JDBC value: " + resource, ex);
      }
    }
    else {
      throw new IllegalArgumentException("Illegal content type: " + this.content.getClass().getName());
    }
  }

  private void setByteArray(PreparedStatement ps, int paramIndex, int sqlType, byte[] bytes)
          throws SQLException {

    if (sqlType == Types.BLOB) {
      ps.setBlob(paramIndex, new ByteArrayInputStream(bytes), bytes.length);
    }
    else {
      ps.setBytes(paramIndex, bytes);
    }
  }

  private void setInputStream(PreparedStatement ps, int paramIndex, int sqlType, InputStream is, long length)
          throws SQLException {

    if (sqlType == Types.BLOB) {
      ps.setBlob(paramIndex, is, length);
    }
    else {
      ps.setBinaryStream(paramIndex, is, length);
    }
  }

}
