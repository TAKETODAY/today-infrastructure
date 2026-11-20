/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.jdbc.support.lob.DefaultLobHandler;
import infra.jdbc.support.lob.LobCreator;
import infra.jdbc.support.lob.LobHandler;
import infra.jdbc.support.lob.TemporaryLobCreator;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

  @Test
  void readsWrappedBlobAndClob() throws SQLException, IOException {
    var handler = new DefaultLobHandler();
    handler.setWrapAsLob(true);
    Blob blob = mock(Blob.class);
    Clob clob = mock(Clob.class);
    byte[] bytes = { 1, 2 };
    given(rs.getBlob(1)).willReturn(blob);
    given(rs.getClob(2)).willReturn(clob);
    given(blob.length()).willReturn(2L);
    given(blob.getBytes(1, 2)).willReturn(bytes);
    given(blob.getBinaryStream()).willReturn(new ByteArrayInputStream(bytes));
    given(clob.length()).willReturn(3L);
    given(clob.getSubString(1, 3)).willReturn("abc");
    given(clob.getAsciiStream()).willReturn(new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII)));
    given(clob.getCharacterStream()).willReturn(new StringReader("abc"));

    assertThat(handler.getBlobAsBytes(rs, 1)).containsExactly(bytes);
    assertThat(FileCopyUtils.copyToByteArray(handler.getBlobAsBinaryStream(rs, 1))).containsExactly(bytes);
    assertThat(handler.getClobAsString(rs, 2)).isEqualTo("abc");
    assertThat(FileCopyUtils.copyToByteArray(handler.getClobAsAsciiStream(rs, 2)))
            .containsExactly("abc".getBytes(StandardCharsets.US_ASCII));
    assertThat(FileCopyUtils.copyToString(handler.getClobAsCharacterStream(rs, 2))).isEqualTo("abc");
  }

  @Test
  void wrapsValuesInJdbcLobs() throws SQLException, IOException {
    var handler = new DefaultLobHandler();
    handler.setWrapAsLob(true);
    byte[] bytes = { 1, 2 };
    var binary = new ByteArrayInputStream(bytes);
    var ascii = new ByteArrayInputStream(new byte[] { 'a' });
    var characters = new StringReader("letters");
    try (LobCreator creator = handler.getLobCreator()) {
      creator.setBlobAsBytes(ps, 1, bytes);
      creator.setBlobAsBinaryStream(ps, 2, binary, 2);
      creator.setClobAsString(ps, 3, "letters");
      creator.setClobAsAsciiStream(ps, 4, ascii, 1);
      creator.setClobAsCharacterStream(ps, 5, characters, 7);
    }

    var blob = ArgumentCaptor.forClass(Blob.class);
    var clob = ArgumentCaptor.forClass(Clob.class);
    verify(ps).setBlob(eq(1), blob.capture());
    assertThat(blob.getValue().length()).isEqualTo(2);
    assertThat(FileCopyUtils.copyToByteArray(blob.getValue().getBinaryStream())).containsExactly(bytes);
    verify(ps).setBlob(eq(2), blob.capture());
    assertThat(blob.getValue().getBinaryStream()).isSameAs(binary);
    verify(ps).setClob(eq(3), clob.capture());
    assertThat(clob.getValue().length()).isEqualTo(7);
    assertThat(FileCopyUtils.copyToString(clob.getValue().getCharacterStream())).isEqualTo("letters");
    verify(ps).setClob(eq(4), clob.capture());
    assertThat(clob.getValue().getAsciiStream()).isSameAs(ascii);
    verify(ps).setClob(eq(5), clob.capture());
    assertThat(clob.getValue().getCharacterStream()).isSameAs(characters);
  }

  @Test
  void streamsValuesWithTheAppropriateJdbcOverload() throws SQLException, IOException {
    var handler = new DefaultLobHandler();
    handler.setStreamAsLob(true);
    byte[] bytes = { 1, 2 };
    byte[] ascii = { 'a', 'b' };
    var binary = new ByteArrayInputStream(bytes);
    var characters = new StringReader("abc");
    try (LobCreator creator = handler.getLobCreator()) {
      creator.setBlobAsBytes(ps, 1, bytes);
      creator.setBlobAsBinaryStream(ps, 2, binary, 2);
      creator.setBlobAsBinaryStream(ps, 3, binary, -1);
      creator.setClobAsString(ps, 4, "abc");
      creator.setClobAsAsciiStream(ps, 5, new ByteArrayInputStream(ascii), 2);
      creator.setClobAsAsciiStream(ps, 6, new ByteArrayInputStream(ascii), -1);
      creator.setClobAsCharacterStream(ps, 7, characters, 3);
      creator.setClobAsCharacterStream(ps, 8, characters, -1);
    }

    var stream = ArgumentCaptor.forClass(InputStream.class);
    var reader = ArgumentCaptor.forClass(Reader.class);
    verify(ps).setBlob(eq(1), stream.capture(), eq(2L));
    assertThat(FileCopyUtils.copyToByteArray(stream.getValue())).containsExactly(bytes);
    verify(ps).setBlob(2, binary, 2L);
    verify(ps).setBlob(3, binary);
    verify(ps).setClob(eq(4), reader.capture(), eq(3L));
    assertThat(FileCopyUtils.copyToString(reader.getValue())).isEqualTo("abc");
    verify(ps).setClob(eq(5), reader.capture(), eq(2L));
    assertThat(FileCopyUtils.copyToString(reader.getValue())).isEqualTo("ab");
    verify(ps).setClob(eq(6), reader.capture());
    assertThat(FileCopyUtils.copyToString(reader.getValue())).isEqualTo("ab");
    verify(ps).setClob(7, characters, 3L);
    verify(ps).setClob(8, characters);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void bindsNullLobsInBothAdaptationModes(boolean streamAsLob) throws SQLException {
    var handler = new DefaultLobHandler();
    handler.setStreamAsLob(streamAsLob);
    handler.setWrapAsLob(!streamAsLob);
    try (LobCreator creator = handler.getLobCreator()) {
      creator.setBlobAsBytes(ps, 1, null);
      creator.setBlobAsBinaryStream(ps, 2, null, -1);
      creator.setClobAsString(ps, 3, null);
      creator.setClobAsAsciiStream(ps, 4, null, -1);
      creator.setClobAsCharacterStream(ps, 5, null, -1);
    }
    verify(ps).setBlob(1, (Blob) null);
    verify(ps).setBlob(2, (Blob) null);
    verify(ps).setClob(3, (Clob) null);
    verify(ps).setClob(4, (Clob) null);
    verify(ps).setClob(5, (Clob) null);
    verifyNoMoreInteractions(ps);
  }

  @Test
  void usesTemporaryLobCreatorWhenEnabled() {
    var handler = new DefaultLobHandler();
    handler.setCreateTemporaryLob(true);
    assertThat(handler.getLobCreator()).isInstanceOf(TemporaryLobCreator.class);
  }

}
