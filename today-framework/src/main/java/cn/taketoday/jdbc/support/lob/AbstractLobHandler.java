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
 * Abstract base class for {@link LobHandler} implementations.
 *
 * <p>Implements all accessor methods for column names through a column lookup
 * and delegating to the corresponding accessor that takes a column index.
 *
 * @author Juergen Hoeller
 * @see ResultSet#findColumn
 * @since 4.0
 */
public abstract class AbstractLobHandler implements LobHandler {

  @Override
  @Nullable
  public byte[] getBlobAsBytes(ResultSet rs, String columnName) throws SQLException {
    return getBlobAsBytes(rs, rs.findColumn(columnName));
  }

  @Override
  @Nullable
  public InputStream getBlobAsBinaryStream(ResultSet rs, String columnName) throws SQLException {
    return getBlobAsBinaryStream(rs, rs.findColumn(columnName));
  }

  @Override
  @Nullable
  public String getClobAsString(ResultSet rs, String columnName) throws SQLException {
    return getClobAsString(rs, rs.findColumn(columnName));
  }

  @Override
  @Nullable
  public InputStream getClobAsAsciiStream(ResultSet rs, String columnName) throws SQLException {
    return getClobAsAsciiStream(rs, rs.findColumn(columnName));
  }

  @Override
  public Reader getClobAsCharacterStream(ResultSet rs, String columnName) throws SQLException {
    return getClobAsCharacterStream(rs, rs.findColumn(columnName));
  }

}
