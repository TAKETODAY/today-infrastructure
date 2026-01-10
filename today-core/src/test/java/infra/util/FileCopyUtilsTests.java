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

package infra.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY 2021/8/21 00:07
 */
class FileCopyUtilsTests {

  @Test
  void copyFromInputStream() throws IOException {
    byte[] content = "content".getBytes();
    ByteArrayInputStream in = new ByteArrayInputStream(content);
    ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
    int count = FileCopyUtils.copy(in, out);
    assertThat(count).isEqualTo(content.length);
    assertThat(Arrays.equals(content, out.toByteArray())).isTrue();
  }

  @Test
  void copyFromByteArray() throws IOException {
    byte[] content = "content".getBytes();
    ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
    FileCopyUtils.copy(content, out);
    assertThat(Arrays.equals(content, out.toByteArray())).isTrue();
  }

  @Test
  void copyToByteArray() throws IOException {
    byte[] content = "content".getBytes();
    ByteArrayInputStream in = new ByteArrayInputStream(content);
    byte[] result = FileCopyUtils.copyToByteArray(in);
    assertThat(Arrays.equals(content, result)).isTrue();
  }

  @Test
  void copyFromReader() throws IOException {
    String content = "content";
    StringReader in = new StringReader(content);
    StringWriter out = new StringWriter();
    int count = FileCopyUtils.copy(in, out);
    assertThat(count).isEqualTo(content.length());
    assertThat(out.toString()).hasToString(content);
  }

  @Test
  void copyFromString() throws IOException {
    String content = "content";
    StringWriter out = new StringWriter();
    FileCopyUtils.copy(content, out);
    assertThat(out.toString()).hasToString(content);
  }

  @Test
  void copyToString() throws IOException {
    String content = "content";
    StringReader in = new StringReader(content);
    String result = FileCopyUtils.copyToString(in);
    assertThat(result).isEqualTo(content);
  }

  @Test
  void copyFromEmptyInputStreamReturnsZero() throws IOException {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int count = FileCopyUtils.copy(in, out);
    assertThat(count).isZero();
    assertThat(out.toByteArray()).isEmpty();
  }

  @Test
  void copyFromNullInputStreamToByteArrayReturnsEmptyArray() throws IOException {
    byte[] result = FileCopyUtils.copyToByteArray((InputStream) null);
    assertThat(result).isEmpty();
  }

  @Test
  void copyFromNullReaderToStringReturnsEmptyString() throws IOException {
    String result = FileCopyUtils.copyToString(null);
    assertThat(result).isEmpty();
  }

  @Test
  void copyWithCustomBufferSize() throws IOException {
    String content = "test content";
    StringReader in = new StringReader(content);
    StringWriter out = new StringWriter();
    int count = FileCopyUtils.copy(in, out, 2);
    assertThat(count).isEqualTo(content.length());
    assertThat(out.toString()).isEqualTo(content);
  }

  @Test
  void copyBetweenFilesWithNonExistentInputThrowsException() {
    File in = new File("nonexistent.txt");
    File out = new File("output.txt");
    assertThatThrownBy(() -> FileCopyUtils.copy(in, out))
            .isInstanceOf(IOException.class);
  }

  @Test
  void copyToByteArrayFromLargeFile() throws IOException {
    byte[] largeContent = new byte[8192];
    Arrays.fill(largeContent, (byte) 'x');
    ByteArrayInputStream in = new ByteArrayInputStream(largeContent);
    byte[] result = FileCopyUtils.copyToByteArray(in);
    assertThat(result).isEqualTo(largeContent);
  }

  @Test
  void copyHandlesUnicodeCharacters() throws IOException {
    String unicode = "Hello 世界 🌍";
    StringReader in = new StringReader(unicode);
    StringWriter out = new StringWriter();
    FileCopyUtils.copy(in, out);
    assertThat(out.toString()).isEqualTo(unicode);
  }

  @Test
  void copyToStringWithCustomBufferSizeHandlesEmptyReader() throws IOException {
    Reader in = new StringReader("");
    String result = FileCopyUtils.copyToString(in, 16);
    assertThat(result).isEmpty();
  }

  @Test
  void copyBetweenStreamsFlushesOutput() throws IOException {
    byte[] content = "test".getBytes();
    ByteArrayInputStream in = new ByteArrayInputStream(content);
    ByteArrayOutputStream out = new ByteArrayOutputStream() {
      private boolean flushed = false;

      @Override
      public void flush() {
        flushed = true;
      }

      @Override
      public byte[] toByteArray() {
        assertThat(flushed).isTrue();
        return super.toByteArray();
      }
    };
    FileCopyUtils.copy(in, out);
  }

  @Test
  void copyHandlesNullByteArray() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    assertThatThrownBy(() -> FileCopyUtils.copy((byte[]) null, out))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void copyWithZeroLengthBufferSize() throws IOException {
    String content = "test";
    StringReader in = new StringReader(content);
    StringWriter out = new StringWriter();

    int copied = FileCopyUtils.copy(in, out, 10);
    assertThat(copied).isEqualTo(content.length());
    assertThat(out.toString()).isEqualTo(content);
  }

  @Test
  void copyBetweenReadersWithPartialReads() throws IOException {
    String content = "test content";
    Reader in = new StringReader(content) {
      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        // Only read 1 char at a time
        return super.read(cbuf, off, Math.min(1, len));
      }
    };

    StringWriter out = new StringWriter();
    int copied = FileCopyUtils.copy(in, out);

    assertThat(copied).isEqualTo(content.length());
    assertThat(out.toString()).isEqualTo(content);
  }

  @Test
  void copyHandlesNullWriter() {
    assertThatThrownBy(() -> FileCopyUtils.copy("test", null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void copyToStringWithNegativeBufferSize() throws IOException {
    Reader in = new StringReader("test");
    assertThatThrownBy(() -> FileCopyUtils.copyToString(in, -1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void copyEmptyStringWriterFlushes() throws IOException {
    StringWriter out = new StringWriter() {
      private boolean flushed = false;

      @Override
      public void flush() {
        flushed = true;
      }

      @Override
      public String toString() {
        assertThat(flushed).isTrue();
        return super.toString();
      }
    };

    FileCopyUtils.copy("", out);
  }
}
