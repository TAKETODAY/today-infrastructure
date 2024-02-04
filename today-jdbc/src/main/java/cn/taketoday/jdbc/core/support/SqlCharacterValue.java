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

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.jdbc.core.SqlTypeValue;
import cn.taketoday.lang.Nullable;

/**
 * Object to represent a character-based parameter value for a SQL statement,
 * e.g. a character stream for a CLOB/NCLOB or a LONGVARCHAR column.
 *
 * <p>Designed for use with {@link cn.taketoday.jdbc.core.JdbcTemplate}
 * as well as {@link cn.taketoday.jdbc.core.simple.JdbcClient}, to be
 * passed in as a parameter value wrapping the target content value. Can be
 * combined with {@link cn.taketoday.jdbc.core.SqlParameterValue} for
 * specifying a SQL type, e.g.
 * {@code new SqlParameterValue(Types.CLOB, new SqlCharacterValue(myContent))}.
 * With most database drivers, the type hint is not actually necessary.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SqlBinaryValue
 * @see cn.taketoday.jdbc.core.SqlParameterValue
 * @since 4.0
 */
public class SqlCharacterValue implements SqlTypeValue {

  private final Object content;

  private final long length;

  /**
   * Create a new CLOB value with the given content string.
   *
   * @param string the content as a String or other CharSequence
   */
  public SqlCharacterValue(CharSequence string) {
    this.content = string;
    this.length = string.length();
  }

  /**
   * Create a new {@code SqlCharacterValue} for the given content.
   *
   * @param characters the content as a character array
   */
  public SqlCharacterValue(char[] characters) {
    this.content = characters;
    this.length = characters.length;
  }

  /**
   * Create a new {@code SqlCharacterValue} for the given content.
   *
   * @param reader the content reader
   * @param length the length of the content
   */
  public SqlCharacterValue(Reader reader, long length) {
    this.content = reader;
    this.length = length;
  }

  /**
   * Create a new {@code SqlCharacterValue} for the given content.
   *
   * @param asciiStream the content as ASCII stream
   * @param length the length of the content
   */
  public SqlCharacterValue(InputStream asciiStream, long length) {
    this.content = asciiStream;
    this.length = length;
  }

  @Override
  public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
          throws SQLException {

    if (this.content instanceof CharSequence) {
      setString(ps, paramIndex, sqlType, this.content.toString());
    }
    else if (this.content instanceof char[] chars) {
      setReader(ps, paramIndex, sqlType, new CharArrayReader(chars), this.length);
    }
    else if (this.content instanceof Reader reader) {
      setReader(ps, paramIndex, sqlType, reader, this.length);
    }
    else if (this.content instanceof InputStream asciiStream) {
      ps.setAsciiStream(paramIndex, asciiStream, this.length);
    }
    else {
      throw new IllegalArgumentException("Illegal content type: " + this.content.getClass().getName());
    }
  }

  private void setString(PreparedStatement ps, int paramIndex, int sqlType, String string)
          throws SQLException {

    if (sqlType == Types.CLOB) {
      ps.setClob(paramIndex, new StringReader(string), string.length());
    }
    else if (sqlType == Types.NCLOB) {
      ps.setNClob(paramIndex, new StringReader(string), string.length());
    }
    else {
      ps.setString(paramIndex, string);
    }
  }

  private void setReader(PreparedStatement ps, int paramIndex, int sqlType, Reader reader, long length)
          throws SQLException {

    if (sqlType == Types.CLOB) {
      ps.setClob(paramIndex, reader, length);
    }
    else if (sqlType == Types.NCLOB) {
      ps.setNClob(paramIndex, reader, length);
    }
    else {
      ps.setCharacterStream(paramIndex, reader, length);
    }
  }

}
