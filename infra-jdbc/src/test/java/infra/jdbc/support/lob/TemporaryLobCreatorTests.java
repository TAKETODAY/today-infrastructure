/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc.support.lob;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import infra.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TemporaryLobCreatorTests {

  private final PreparedStatement statement = mock(PreparedStatement.class);

  private final Connection connection = mock(Connection.class);

  private final Blob blob = mock(Blob.class);

  private final Clob clob = mock(Clob.class);

  @Test
  void copiesValuesAndFreesEachTemporaryLob() throws SQLException {
    byte[] bytes = { 1, 2, 3 };
    connect();
    try (var creator = new TemporaryLobCreator()) {
      creator.setBlobAsBytes(statement, 1, bytes);
      creator.setClobAsString(statement, 2, "data");

      verify(blob).setBytes(1, bytes);
      verify(clob).setString(1, "data");
      verify(statement).setBlob(1, blob);
      verify(statement).setClob(2, clob);
    }
    verify(blob).free();
    verify(clob).free();
  }

  @Test
  void copiesStreamsAndContinuesFreeingLobsAfterAnError() throws SQLException {
    connect();
    Clob secondClob = mock(Clob.class);
    given(connection.createClob()).willReturn(clob, secondClob);
    var binaryOutput = new ByteArrayOutputStream();
    var asciiOutput = new ByteArrayOutputStream();
    var characterOutput = new StringWriter();
    given(blob.setBinaryStream(1)).willReturn(binaryOutput);
    given(clob.setAsciiStream(1)).willReturn(asciiOutput);
    given(secondClob.setCharacterStream(1)).willReturn(characterOutput);
    willThrow(new SQLException("blob free failed")).given(blob).free();
    willThrow(new SQLException("clob free failed")).given(clob).free();
    byte[] bytes = { 1, 2, 3 };

    try (var creator = new TemporaryLobCreator()) {
      creator.setBlobAsBinaryStream(statement, 1, new ByteArrayInputStream(bytes), bytes.length);
      creator.setClobAsAsciiStream(statement, 2, new ByteArrayInputStream(new byte[] { 'a' }), 1);
      creator.setClobAsCharacterStream(statement, 3, new StringReader("你好"), -1);

      assertThat(binaryOutput.toByteArray()).containsExactly(bytes);
      assertThat(asciiOutput.toString(StandardCharsets.US_ASCII)).isEqualTo("a");
      assertThat(characterOutput.toString()).isEqualTo("你好");
      verify(statement).setBlob(1, blob);
      verify(statement).setClob(2, clob);
      verify(statement).setClob(3, secondClob);
    }
    verify(blob).free();
    verify(clob).free();
    verify(secondClob).free();
  }

  @Test
  void bindsNullWithoutAllocatingTemporaryLobs() throws SQLException {
    try (var creator = new TemporaryLobCreator()) {
      creator.setBlobAsBytes(statement, 1, null);
      creator.setBlobAsBinaryStream(statement, 2, null, -1);
      creator.setClobAsString(statement, 3, null);
      creator.setClobAsAsciiStream(statement, 4, null, -1);
      creator.setClobAsCharacterStream(statement, 5, null, -1);
    }
    verify(statement).setBlob(1, (Blob) null);
    verify(statement).setBlob(2, (Blob) null);
    verify(statement).setClob(3, (Clob) null);
    verify(statement).setClob(4, (Clob) null);
    verify(statement).setClob(5, (Clob) null);
    verifyNoInteractions(connection, blob, clob);
  }

  @Test
  void translatesStreamCopyFailures() throws SQLException, IOException {
    connect();
    var failure = new IOException("broken stream");
    InputStream stream = mock(InputStream.class);
    given(stream.transferTo(any())).willThrow(failure);
    Reader reader = mock(Reader.class);
    given(reader.read(any(char[].class))).willThrow(failure);
    given(blob.setBinaryStream(1)).willReturn(new ByteArrayOutputStream());
    given(clob.setAsciiStream(1)).willReturn(new ByteArrayOutputStream());
    given(clob.setCharacterStream(1)).willReturn(new StringWriter());

    try (var creator = new TemporaryLobCreator()) {
      assertThatThrownBy(() -> creator.setBlobAsBinaryStream(statement, 1, stream, -1))
              .isInstanceOf(DataAccessResourceFailureException.class).hasCause(failure);
      assertThatThrownBy(() -> creator.setClobAsAsciiStream(statement, 1, stream, -1))
              .isInstanceOf(DataAccessResourceFailureException.class).hasCause(failure);
      assertThatThrownBy(() -> creator.setClobAsCharacterStream(statement, 1, reader, -1))
              .isInstanceOf(DataAccessResourceFailureException.class).hasCause(failure);
    }
  }

  private void connect() throws SQLException {
    given(statement.getConnection()).willReturn(connection);
    given(connection.createBlob()).willReturn(blob);
    given(connection.createClob()).willReturn(clob);
  }

}
