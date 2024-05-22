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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.jdbc.support.JdbcUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/4 21:58
 */
class SqlCharacterValueTests {

  @Test
  void withString() throws SQLException {
    String content = "abc";
    SqlCharacterValue value = new SqlCharacterValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setString(1, content);
  }

  @Test
  void withStringForClob() throws SQLException {
    String content = "abc";
    SqlCharacterValue value = new SqlCharacterValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.CLOB, null);
    verify(ps).setClob(eq(1), any(StringReader.class), eq(3L));
  }

  @Test
  void withStringForNClob() throws SQLException {
    String content = "abc";
    SqlCharacterValue value = new SqlCharacterValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.NCLOB, null);
    verify(ps).setNClob(eq(1), any(StringReader.class), eq(3L));
  }

  @Test
  void withCharArray() throws SQLException {
    char[] content = "abc".toCharArray();
    SqlCharacterValue value = new SqlCharacterValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setCharacterStream(eq(1), any(CharArrayReader.class), eq(3L));
  }

  @Test
  void withCharArrayForClob() throws SQLException {
    char[] content = "abc".toCharArray();
    SqlCharacterValue value = new SqlCharacterValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.CLOB, null);
    verify(ps).setClob(eq(1), any(CharArrayReader.class), eq(3L));
  }

  @Test
  void withCharArrayForNClob() throws SQLException {
    char[] content = "abc".toCharArray();
    SqlCharacterValue value = new SqlCharacterValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.NCLOB, null);
    verify(ps).setNClob(eq(1), any(CharArrayReader.class), eq(3L));
  }

  @Test
  void withReader() throws SQLException {
    Reader content = new StringReader("abc");
    SqlCharacterValue value = new SqlCharacterValue(content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setCharacterStream(1, content, 3L);
  }

  @Test
  void withReaderForClob() throws SQLException {
    Reader content = new StringReader("abc");
    SqlCharacterValue value = new SqlCharacterValue(content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.CLOB, null);
    verify(ps).setClob(1, content, 3L);
  }

  @Test
  void withReaderForNClob() throws SQLException {
    Reader content = new StringReader("abc");
    SqlCharacterValue value = new SqlCharacterValue(content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.NCLOB, null);
    verify(ps).setNClob(1, content, 3L);
  }

  @Test
  void withAsciiStream() throws SQLException {
    InputStream content = new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII));
    SqlCharacterValue value = new SqlCharacterValue(content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setAsciiStream(1, content, 3L);
  }

}