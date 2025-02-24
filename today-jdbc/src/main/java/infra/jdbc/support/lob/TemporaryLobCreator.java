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

package infra.jdbc.support.lob;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.dao.DataAccessResourceFailureException;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.FileCopyUtils;

/**
 * {@link LobCreator} implementation based on temporary LOBs,
 * using JDBC 4.0's {@link java.sql.Connection#createBlob()} /
 * {@link java.sql.Connection#createClob()} mechanism.
 *
 * <p>Used by DefaultLobHandler's {@link DefaultLobHandler#setCreateTemporaryLob} mode.
 * Can also be used directly to reuse the tracking and freeing of temporary LOBs.
 *
 * @author Juergen Hoeller
 * @see DefaultLobHandler#setCreateTemporaryLob
 * @see java.sql.Connection#createBlob()
 * @see java.sql.Connection#createClob()
 * @since 4.0
 */
public class TemporaryLobCreator implements LobCreator {

  protected static final Logger logger = LoggerFactory.getLogger(TemporaryLobCreator.class);

  private final Set<Blob> temporaryBlobs = new LinkedHashSet<>(1);

  private final Set<Clob> temporaryClobs = new LinkedHashSet<>(1);

  @Override
  public void setBlobAsBytes(PreparedStatement ps, int paramIndex, @Nullable byte[] content)
          throws SQLException {

    if (content != null) {
      Blob blob = ps.getConnection().createBlob();
      blob.setBytes(1, content);
      this.temporaryBlobs.add(blob);
      ps.setBlob(paramIndex, blob);
    }
    else {
      ps.setBlob(paramIndex, (Blob) null);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(content != null ? "Copied bytes into temporary BLOB with length " + content.length :
                   "Set BLOB to null");
    }
  }

  @Override
  public void setBlobAsBinaryStream(
          PreparedStatement ps, int paramIndex, @Nullable InputStream binaryStream, int contentLength)
          throws SQLException {

    if (binaryStream != null) {
      Blob blob = ps.getConnection().createBlob();
      try {
        FileCopyUtils.copy(binaryStream, blob.setBinaryStream(1));
      }
      catch (IOException ex) {
        throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
      }
      this.temporaryBlobs.add(blob);
      ps.setBlob(paramIndex, blob);
    }
    else {
      ps.setBlob(paramIndex, (Blob) null);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(binaryStream != null ?
                   "Copied binary stream into temporary BLOB with length " + contentLength :
                   "Set BLOB to null");
    }
  }

  @Override
  public void setClobAsString(PreparedStatement ps, int paramIndex, @Nullable String content)
          throws SQLException {

    if (content != null) {
      Clob clob = ps.getConnection().createClob();
      clob.setString(1, content);
      this.temporaryClobs.add(clob);
      ps.setClob(paramIndex, clob);
    }
    else {
      ps.setClob(paramIndex, (Clob) null);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(content != null ? "Copied string into temporary CLOB with length " + content.length() :
                   "Set CLOB to null");
    }
  }

  @Override
  public void setClobAsAsciiStream(
          PreparedStatement ps, int paramIndex, @Nullable InputStream asciiStream, int contentLength)
          throws SQLException {

    if (asciiStream != null) {
      Clob clob = ps.getConnection().createClob();
      try {
        FileCopyUtils.copy(asciiStream, clob.setAsciiStream(1));
      }
      catch (IOException ex) {
        throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
      }
      this.temporaryClobs.add(clob);
      ps.setClob(paramIndex, clob);
    }
    else {
      ps.setClob(paramIndex, (Clob) null);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(asciiStream != null ?
                   "Copied ASCII stream into temporary CLOB with length " + contentLength :
                   "Set CLOB to null");
    }
  }

  @Override
  public void setClobAsCharacterStream(
          PreparedStatement ps, int paramIndex, @Nullable Reader characterStream, int contentLength)
          throws SQLException {

    if (characterStream != null) {
      Clob clob = ps.getConnection().createClob();
      try {
        FileCopyUtils.copy(characterStream, clob.setCharacterStream(1));
      }
      catch (IOException ex) {
        throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
      }
      this.temporaryClobs.add(clob);
      ps.setClob(paramIndex, clob);
    }
    else {
      ps.setClob(paramIndex, (Clob) null);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(characterStream != null ?
                   "Copied character stream into temporary CLOB with length " + contentLength :
                   "Set CLOB to null");
    }
  }

  @Override
  public void close() {
    for (Blob blob : this.temporaryBlobs) {
      try {
        blob.free();
      }
      catch (SQLException ex) {
        logger.warn("Could not free BLOB", ex);
      }
    }
    for (Clob clob : this.temporaryClobs) {
      try {
        clob.free();
      }
      catch (SQLException ex) {
        logger.warn("Could not free CLOB", ex);
      }
    }
  }

}
