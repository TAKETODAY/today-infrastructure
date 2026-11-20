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
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PassThroughClobTests {

  @Test
  void adaptsStringAndReaderToAscii() throws Exception {
    var text = "café";
    var source = new StringReader(text);
    var readerClob = new PassThroughClob(source, text.length());
    var stringClob = new PassThroughClob(text);

    assertThat(readerClob.length()).isEqualTo(4);
    assertThat(readerClob.getCharacterStream()).isSameAs(source);
    try (var fromReader = readerClob.getAsciiStream(); var fromString = stringClob.getAsciiStream()) {
      assertThat(fromReader.readAllBytes()).isEqualTo("caf?".getBytes(StandardCharsets.US_ASCII));
      assertThat(fromString.readAllBytes()).isEqualTo("caf?".getBytes(StandardCharsets.US_ASCII));
    }
  }

  @Test
  void exposesAsciiStreamAndDecodesItAsCharacters() throws Exception {
    var source = new ByteArrayInputStream("hello".getBytes(StandardCharsets.US_ASCII));
    var clob = new PassThroughClob(source, 5);

    assertThat(clob.length()).isEqualTo(5);
    assertThat(clob.getAsciiStream()).isSameAs(source);
    try (var reader = clob.getCharacterStream(); var text = new StringWriter()) {
      reader.transferTo(text);
      assertThat(text.toString()).isEqualTo("hello");
    }
  }

  @Test
  void reportsReaderFailureAsSqlException() throws Exception {
    Reader reader = new Reader() {

      @Override
      public int read(char[] buffer, int offset, int length) throws IOException {
        throw new IOException("broken reader");
      }

      @Override
      public void close() {
      }
    };
    var clob = new PassThroughClob(reader, 5);

    assertThatExceptionOfType(SQLException.class).isThrownBy(clob::getAsciiStream)
            .withMessageContaining("broken reader");
  }

  @Test
  void doesNotSupportRandomAccessOrMutation() throws SQLException {
    var clob = new PassThroughClob("content");

    assertThatThrownBy(() -> clob.getCharacterStream(1, 2)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.getSubString(1, 2)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.position("co", 1)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.position(new PassThroughClob("co"), 1)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.setCharacterStream(1)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.setAsciiStream(1)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.setString(1, "a")).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.setString(1, "abc", 1, 1)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> clob.truncate(2)).isInstanceOf(UnsupportedOperationException.class);
    clob.free();
  }

}
