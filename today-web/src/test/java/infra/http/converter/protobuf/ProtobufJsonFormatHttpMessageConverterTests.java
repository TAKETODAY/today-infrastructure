/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.converter.protobuf;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.protobuf.Msg;
import infra.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for {@link ProtobufJsonFormatHttpMessageConverter}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
class ProtobufJsonFormatHttpMessageConverterTests {

  private final ProtobufHttpMessageConverter converter = new ProtobufJsonFormatHttpMessageConverter(
          JsonFormat.parser(), JsonFormat.printer());

  private final Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();
  private final MediaType testPlusProtoMediaType = MediaType.parseMediaType("application/vnd.example.public.v1+x-protobuf");

  @Test
  void canRead() {
    assertThat(this.converter.canRead(Msg.class, null)).isTrue();
    assertThat(this.converter.canRead(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
    assertThat(this.converter.canRead(Msg.class, this.testPlusProtoMediaType)).isTrue();
    assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
    assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
  }

  @Test
  void canWrite() {
    assertThat(this.converter.canWrite(Msg.class, null)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, this.testPlusProtoMediaType)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
  }

  @Test
  void read() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(ProtobufHttpMessageConverter.PROTOBUF);
    var result = this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void readNoContentType() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    var result = this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void write() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
    this.converter.write(this.testMsg, contentType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);
    assertThat(outputMessage.getBodyAsBytes().length).isGreaterThan(0);
    Message result = Msg.parseFrom(outputMessage.getBodyAsBytes());
    assertThat(result).isEqualTo(this.testMsg);

    String messageHeader =
            outputMessage.getHeaders().getFirst(ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER);
    assertThat(messageHeader).isEqualTo("Msg");
    String schemaHeader =
            outputMessage.getHeaders().getFirst(ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER);
    assertThat(schemaHeader).isEqualTo("sample.proto");
  }

  @Test
  void defaultContentType() {
    assertThat(this.converter.getDefaultContentType(this.testMsg)).isEqualTo(ProtobufHttpMessageConverter.PROTOBUF);
  }

  @Test
  void getContentLength() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
    this.converter.write(this.testMsg, contentType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(-1);
  }

}
