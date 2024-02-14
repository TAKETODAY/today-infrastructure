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

package cn.taketoday.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.protobuf.Msg;
import cn.taketoday.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for {@link ProtobufJsonFormatHttpMessageConverter}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
@SuppressWarnings("deprecation")
public class ProtobufJsonFormatHttpMessageConverterTests {

  private final ProtobufHttpMessageConverter converter = new ProtobufJsonFormatHttpMessageConverter(
          JsonFormat.parser(), JsonFormat.printer());

  private final Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

  @Test
  public void extensionRegistryInitializer() {
    ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter((ExtensionRegistry) null);
    assertThat(converter).isNotNull();
  }

  @Test
  public void canRead() {
    assertThat(this.converter.canRead(Msg.class, null)).isTrue();
    assertThat(this.converter.canRead(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
    assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
    assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
  }

  @Test
  public void canWrite() {
    assertThat(this.converter.canWrite(Msg.class, null)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
  }

  @Test
  public void read() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(ProtobufHttpMessageConverter.PROTOBUF);
    Message result = (Message) this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  public void readNoContentType() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    Message result = (Message) this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  public void write() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
    this.converter.write(this.testMsg, contentType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);
    assertThat(outputMessage.getBodyAsBytes().length > 0).isTrue();
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
  public void defaultContentType() throws Exception {
    assertThat(this.converter.getDefaultContentType(this.testMsg)).isEqualTo(ProtobufHttpMessageConverter.PROTOBUF);
  }

  @Test
  public void getContentLength() throws Exception {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
    this.converter.write(this.testMsg, contentType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(-1);
  }

}
