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

package cn.taketoday.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * @author TODAY 2021/8/21 00:07
 */
class StreamUtilsTests {

  private byte[] bytes = new byte[StreamUtils.BUFFER_SIZE + 10];

  private String string = "";

  @BeforeEach
  void setup() {
    new Random().nextBytes(bytes);
    while (string.length() < StreamUtils.BUFFER_SIZE + 10) {
      string += UUID.randomUUID().toString();
    }
  }

  @Test
  void copyToByteArray() throws Exception {
    InputStream inputStream = new ByteArrayInputStream(bytes);
    byte[] actual = StreamUtils.copyToByteArray(inputStream);
    assertThat(actual).isEqualTo(bytes);
  }

  @Test
  void copyToString() throws Exception {
    Charset charset = Charset.defaultCharset();
    InputStream inputStream = new ByteArrayInputStream(string.getBytes(charset));
    String actual = StreamUtils.copyToString(inputStream, charset);
    assertThat(actual).isEqualTo(string);
  }

  @Test
  void copyBytes() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamUtils.copy(bytes, out);
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  @Test
  void copyString() throws Exception {
    Charset charset = Charset.defaultCharset();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamUtils.copy(string, charset, out);
    assertThat(out.toByteArray()).isEqualTo(string.getBytes(charset));
  }

  @Test
  void copyStream() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamUtils.copy(new ByteArrayInputStream(bytes), out);
    assertThat(out.toByteArray()).isEqualTo(bytes);
  }

  @Test
  void copyRange() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamUtils.copyRange(new ByteArrayInputStream(bytes), out, 0, 100);
    byte[] range = Arrays.copyOfRange(bytes, 0, 101);
    assertThat(out.toByteArray()).isEqualTo(range);
  }

  @Test
  void nonClosingInputStream() throws Exception {
    InputStream source = mock(InputStream.class);
    InputStream nonClosing = StreamUtils.nonClosing(source);
    nonClosing.read();
    nonClosing.read(bytes);
    nonClosing.read(bytes, 1, 2);
    nonClosing.close();
    InOrder ordered = inOrder(source);
    ordered.verify(source).read();
    ordered.verify(source).read(bytes, 0, bytes.length);
    ordered.verify(source).read(bytes, 1, 2);
    ordered.verify(source, never()).close();
  }

  @Test
  void nonClosingOutputStream() throws Exception {
    OutputStream source = mock(OutputStream.class);
    OutputStream nonClosing = StreamUtils.nonClosing(source);
    nonClosing.write(1);
    nonClosing.write(bytes);
    nonClosing.write(bytes, 1, 2);
    nonClosing.close();
    InOrder ordered = inOrder(source);
    ordered.verify(source).write(1);
    ordered.verify(source).write(bytes, 0, bytes.length);
    ordered.verify(source).write(bytes, 1, 2);
    ordered.verify(source, never()).close();
  }
}
