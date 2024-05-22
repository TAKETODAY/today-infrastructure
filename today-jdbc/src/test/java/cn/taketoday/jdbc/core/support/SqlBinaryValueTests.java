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
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.jdbc.support.JdbcUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/4 21:58
 */
class SqlBinaryValueTests {

  @Test
  void withByteArray() throws SQLException {
    byte[] content = new byte[] { 0, 1, 2 };
    SqlBinaryValue value = new SqlBinaryValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setBytes(1, content);
  }

  @Test
  void withByteArrayForBlob() throws SQLException {
    byte[] content = new byte[] { 0, 1, 2 };
    SqlBinaryValue value = new SqlBinaryValue(content);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.BLOB, null);
    verify(ps).setBlob(eq(1), any(ByteArrayInputStream.class), eq(3L));
  }

  @Test
  void withInputStream() throws SQLException {
    InputStream content = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
    SqlBinaryValue value = new SqlBinaryValue(content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setBinaryStream(1, content, 3L);
  }

  @Test
  void withInputStreamForBlob() throws SQLException {
    InputStream content = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
    SqlBinaryValue value = new SqlBinaryValue(content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.BLOB, null);
    verify(ps).setBlob(1, content, 3L);
  }

  @Test
  void withInputStreamSource() throws SQLException {
    InputStream content = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
    SqlBinaryValue value = new SqlBinaryValue(() -> content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setBinaryStream(1, content, 3L);
  }

  @Test
  void withInputStreamSourceForBlob() throws SQLException {
    InputStream content = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
    SqlBinaryValue value = new SqlBinaryValue(() -> content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.BLOB, null);
    verify(ps).setBlob(1, content, 3L);
  }

  @Test
  void withResource() throws SQLException {
    byte[] content = new byte[] { 0, 1, 2 };
    SqlBinaryValue value = new SqlBinaryValue(new ByteArrayResource(content));
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
    verify(ps).setBinaryStream(eq(1), any(ByteArrayInputStream.class), eq(3L));
  }

  @Test
  void withResourceForBlob() throws SQLException {
    InputStream content = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
    SqlBinaryValue value = new SqlBinaryValue(() -> content, 3);
    PreparedStatement ps = mock();
    value.setTypeValue(ps, 1, Types.BLOB, null);
    verify(ps).setBlob(eq(1), any(ByteArrayInputStream.class), eq(3L));
  }

}