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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * Simple JDBC {@link Blob} adapter that exposes a given byte array or binary stream.
 * Optionally used by {@link DefaultLobHandler}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
class PassThroughBlob implements Blob {

  @Nullable
  private byte[] content;

  @Nullable
  private InputStream binaryStream;

  private final long contentLength;

  public PassThroughBlob(byte[] content) {
    this.content = content;
    this.contentLength = content.length;
  }

  public PassThroughBlob(InputStream binaryStream, long contentLength) {
    this.binaryStream = binaryStream;
    this.contentLength = contentLength;
  }

  @Override
  public long length() throws SQLException {
    return this.contentLength;
  }

  @Override
  public InputStream getBinaryStream() throws SQLException {
    if (this.content != null) {
      return new ByteArrayInputStream(this.content);
    }
    else {
      return (this.binaryStream != null ? this.binaryStream : InputStream.nullInputStream());
    }
  }

  @Override
  public InputStream getBinaryStream(long pos, long length) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream setBinaryStream(long pos) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getBytes(long pos, int length) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int setBytes(long pos, byte[] bytes) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long position(byte[] pattern, long start) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long position(Blob pattern, long start) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void truncate(long len) throws SQLException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void free() throws SQLException {
    // no-op
  }

}
