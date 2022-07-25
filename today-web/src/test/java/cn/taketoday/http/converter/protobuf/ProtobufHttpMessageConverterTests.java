/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.protobuf.Msg;
import cn.taketoday.protobuf.SecondMsg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test suite for {@link ProtobufHttpMessageConverter}.
 *
 * @author Alex Antonov
 * @author Juergen Hoeller
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 */
@SuppressWarnings("deprecation")
public class ProtobufHttpMessageConverterTests {

  private ProtobufHttpMessageConverter converter;

  private ExtensionRegistry extensionRegistry;

  private Msg testMsg;

  @BeforeEach
  public void setup() {
    this.extensionRegistry = mock(ExtensionRegistry.class);
    this.converter = new ProtobufHttpMessageConverter();
    this.testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();
  }

  @Test
  public void extensionRegistryInitializerNull() {
    ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter();
    assertThat(converter.extensionRegistry).isNotNull();
  }

  @Test
  public void extensionRegistryNull() {
    ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter((ExtensionRegistry) null);
    assertThat(converter.extensionRegistry).isNotNull();
  }

  @Test
  public void canRead() {
    assertThat(this.converter.canRead(Msg.class, null)).isTrue();
    assertThat(this.converter.canRead(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
    assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
    assertThat(this.converter.canRead(Msg.class, MediaType.APPLICATION_XML)).isTrue();
    assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_PLAIN)).isTrue();

    // only supported as an output format
    assertThat(this.converter.canRead(Msg.class, MediaType.TEXT_HTML)).isFalse();
  }

  @Test
  public void canWrite() {
    assertThat(this.converter.canWrite(Msg.class, null)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, ProtobufHttpMessageConverter.PROTOBUF)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_JSON)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.APPLICATION_XML)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_PLAIN)).isTrue();
    assertThat(this.converter.canWrite(Msg.class, MediaType.TEXT_HTML)).isTrue();
  }

  @Test
  public void read() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(ProtobufHttpMessageConverter.PROTOBUF);
    Message result = this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  public void readNoContentType() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    Message result = this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  public void writeProtobuf() throws IOException {
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
  public void writeJsonWithGoogleProtobuf() throws IOException {
    this.converter = new ProtobufHttpMessageConverter(
            new ProtobufHttpMessageConverter.ProtobufJavaUtilSupport(null, null),
            this.extensionRegistry);
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = MediaType.APPLICATION_JSON;
    this.converter.write(this.testMsg, contentType, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);

    final String body = outputMessage.getBodyAsString(Charset.forName("UTF-8"));
    assertThat(body.isEmpty()).as("body is empty").isFalse();

    Msg.Builder builder = Msg.newBuilder();
    JsonFormat.parser().merge(body, builder);
    assertThat(builder.build()).isEqualTo(this.testMsg);

    assertThat(outputMessage.getHeaders().getFirst(
            ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER)).isNull();
    assertThat(outputMessage.getHeaders().getFirst(
            ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER)).isNull();
  }

  @Test
  public void writeJsonWithJavaFormat() throws IOException {
    this.converter = new ProtobufHttpMessageConverter(
            new ProtobufHttpMessageConverter.ProtobufJavaFormatSupport(),
            this.extensionRegistry);
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = MediaType.APPLICATION_JSON;
    this.converter.write(this.testMsg, contentType, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);

    final String body = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
    assertThat(body.isEmpty()).as("body is empty").isFalse();

    Msg.Builder builder = Msg.newBuilder();
    JsonFormat.parser().merge(body, builder);
    assertThat(builder.build()).isEqualTo(this.testMsg);

    assertThat(outputMessage.getHeaders().getFirst(
            ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER)).isNull();
    assertThat(outputMessage.getHeaders().getFirst(
            ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER)).isNull();
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
