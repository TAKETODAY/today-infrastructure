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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.support.lob.DefaultLobHandler;
import cn.taketoday.jdbc.support.lob.LobCreator;
import cn.taketoday.jdbc.support.lob.LobHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 17.12.2003
 */
public class DefaultLobHandlerTests {

  private ResultSet rs = mock(ResultSet.class);

  private PreparedStatement ps = mock(PreparedStatement.class);

  private LobHandler lobHandler = new DefaultLobHandler();

  private LobCreator lobCreator = lobHandler.getLobCreator();

  @Test
  public void testGetBlobAsBytes() throws SQLException {
    lobHandler.getBlobAsBytes(rs, 1);
    verify(rs).getBytes(1);
  }

  @Test
  public void testGetBlobAsBinaryStream() throws SQLException {
    lobHandler.getBlobAsBinaryStream(rs, 1);
    verify(rs).getBinaryStream(1);
  }

  @Test
  public void testGetClobAsString() throws SQLException {
    lobHandler.getClobAsString(rs, 1);
    verify(rs).getString(1);
  }

  @Test
  public void testGetClobAsAsciiStream() throws SQLException {
    lobHandler.getClobAsAsciiStream(rs, 1);
    verify(rs).getAsciiStream(1);
  }

  @Test
  public void testGetClobAsCharacterStream() throws SQLException {
    lobHandler.getClobAsCharacterStream(rs, 1);
    verify(rs).getCharacterStream(1);
  }

  @Test
  public void testSetBlobAsBytes() throws SQLException {
    byte[] content = "testContent".getBytes();
    lobCreator.setBlobAsBytes(ps, 1, content);
    verify(ps).setBytes(1, content);
  }

  @Test
  public void testSetBlobAsBinaryStream() throws SQLException, IOException {
    InputStream bis = new ByteArrayInputStream("testContent".getBytes());
    lobCreator.setBlobAsBinaryStream(ps, 1, bis, 11);
    verify(ps).setBinaryStream(1, bis, 11);
  }

  @Test
  public void testSetBlobAsBinaryStreamWithoutLength() throws SQLException, IOException {
    InputStream bis = new ByteArrayInputStream("testContent".getBytes());
    lobCreator.setBlobAsBinaryStream(ps, 1, bis, -1);
    verify(ps).setBinaryStream(1, bis);
  }

  @Test
  public void testSetClobAsString() throws SQLException, IOException {
    String content = "testContent";
    lobCreator.setClobAsString(ps, 1, content);
    verify(ps).setString(1, content);
  }

  @Test
  public void testSetClobAsAsciiStream() throws SQLException, IOException {
    InputStream bis = new ByteArrayInputStream("testContent".getBytes());
    lobCreator.setClobAsAsciiStream(ps, 1, bis, 11);
    verify(ps).setAsciiStream(1, bis, 11);
  }

  @Test
  public void testSetClobAsAsciiStreamWithoutLength() throws SQLException, IOException {
    InputStream bis = new ByteArrayInputStream("testContent".getBytes());
    lobCreator.setClobAsAsciiStream(ps, 1, bis, -1);
    verify(ps).setAsciiStream(1, bis);
  }

  @Test
  public void testSetClobAsCharacterStream() throws SQLException, IOException {
    Reader str = new StringReader("testContent");
    lobCreator.setClobAsCharacterStream(ps, 1, str, 11);
    verify(ps).setCharacterStream(1, str, 11);
  }

  @Test
  public void testSetClobAsCharacterStreamWithoutLength() throws SQLException, IOException {
    Reader str = new StringReader("testContent");
    lobCreator.setClobAsCharacterStream(ps, 1, str, -1);
    verify(ps).setCharacterStream(1, str);
  }

}
