/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;

/** @author Arjen Poutsma */
public class ByteArrayHttpMessageConverterTests {

  private ByteArrayHttpMessageConverter converter;

  @BeforeEach
  public void setUp() {
    converter = new ByteArrayHttpMessageConverter();
  }

  @Test
  public void canRead() {
    assertThat(converter.canRead(byte[].class, new MediaType("application", "octet-stream"))).isTrue();
  }

  @Test
  public void canWrite() {
    assertThat(converter.canWrite(byte[].class, new MediaType("application", "octet-stream"))).isTrue();
    assertThat(converter.canWrite(byte[].class, MediaType.ALL)).isTrue();
  }

  @Test
  public void read() throws IOException {
    byte[] body = new byte[] { 0x1, 0x2 };
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(new MediaType("application", "octet-stream"));
    byte[] result = converter.read(byte[].class, inputMessage);
    assertThat(result).as("Invalid result").isEqualTo(body);
  }

  @Test
  public void readWithContentLengthHeaderSet() throws IOException {
    byte[] body = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5 };
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(new MediaType("application", "octet-stream"));
    inputMessage.getHeaders().setContentLength(body.length);
    byte[] result = converter.read(byte[].class, inputMessage);
    assertThat(result).as("Invalid result").isEqualTo(body);
  }

  @Test
  public void write() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    byte[] body = new byte[] { 0x1, 0x2 };
    converter.write(body, null, outputMessage);
    assertThat(outputMessage.getBodyAsBytes()).as("Invalid result").isEqualTo(body);
    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(2);
  }

  @Test
  public void repeatableWrites() throws IOException {
    MockHttpOutputMessage outputMessage1 = new MockHttpOutputMessage();
    byte[] body = new byte[] { 0x1, 0x2 };
    assertThat(converter.supportsRepeatableWrites(body)).isTrue();

    converter.write(body, null, outputMessage1);
    assertThat(outputMessage1.getBodyAsBytes()).isEqualTo(body);

    MockHttpOutputMessage outputMessage2 = new MockHttpOutputMessage();
    converter.write(body, null, outputMessage2);
    assertThat(outputMessage2.getBodyAsBytes()).isEqualTo(body);
  }

}
