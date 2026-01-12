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

package infra.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.protobuf.Msg;
import infra.protobuf.SecondMsg;

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
class ProtobufHttpMessageConverterTests {

  private ProtobufHttpMessageConverter converter = new ProtobufHttpMessageConverter();

  private final ExtensionRegistry extensionRegistry = mock();

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
  void writeProtobuf() throws IOException {
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
  void writeJsonWithGoogleProtobuf() throws IOException {
    this.converter = new ProtobufHttpMessageConverter(
            new ProtobufHttpMessageConverter.ProtobufJavaUtilSupport(null, null),
            this.extensionRegistry);
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = MediaType.APPLICATION_JSON;
    this.converter.write(this.testMsg, contentType, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);

    final String body = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
    assertThat(body).as("body is empty").isNotEmpty();

    Msg.Builder builder = Msg.newBuilder();
    JsonFormat.parser().merge(body, builder);
    assertThat(builder.build()).isEqualTo(this.testMsg);

    assertThat(outputMessage.getHeaders().getFirst(
            ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER)).isNull();
    assertThat(outputMessage.getHeaders().getFirst(
            ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER)).isNull();
  }

  @Test
  void defaultContentType() {
    assertThat(this.converter.getDefaultContentType(this.testMsg))
            .isEqualTo(ProtobufHttpMessageConverter.PROTOBUF);
  }

  @Test
  void getContentLength() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
    this.converter.write(this.testMsg, contentType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(-1);
  }

  @Test
  void writeTextPlain() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = MediaType.TEXT_PLAIN;
    this.converter.write(this.testMsg, contentType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);
    String body = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
    assertThat(body).contains("foo: \"Foo\"");
    assertThat(body).contains("blah {");
    assertThat(body).contains("blah: 123");
  }

  @Test
  void readTextPlain() throws IOException {
    String text = "foo: \"Foo\"\nblah {\n  blah: 123\n}";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(text.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    Msg result = (Msg) this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void readJsonWithGoogleProtobuf() throws IOException {
    this.converter = new ProtobufHttpMessageConverter(
            new ProtobufHttpMessageConverter.ProtobufJavaUtilSupport(null, null),
            this.extensionRegistry);

    Msg msg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();
    String json = JsonFormat.printer().print(msg);

    MockHttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8));
    inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    Msg result = (Msg) this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(msg);
  }

  @Test
  void writePlusProto() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    this.converter.write(this.testMsg, this.testPlusProtoMediaType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(this.testPlusProtoMediaType);
    Message result = Msg.parseFrom(outputMessage.getBodyAsBytes());
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void readPlusProto() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(this.testPlusProtoMediaType);
    var result = this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void writeWithoutPopulateProtoHeader() throws IOException {
    this.converter.setPopulateProtoHeader(false);
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
    this.converter.write(this.testMsg, contentType, outputMessage);

    String messageHeader = outputMessage.getHeaders().getFirst(ProtobufHttpMessageConverter.X_PROTOBUF_MESSAGE_HEADER);
    String schemaHeader = outputMessage.getHeaders().getFirst(ProtobufHttpMessageConverter.X_PROTOBUF_SCHEMA_HEADER);
    assertThat(messageHeader).isNull();
    assertThat(schemaHeader).isNull();
  }

  @Test
  void supportsMessageOrBuilder() {
    assertThat(this.converter.supports(Msg.class)).isTrue();
    assertThat(this.converter.supports(Msg.Builder.class)).isTrue();
    assertThat(this.converter.supports(Object.class)).isFalse();
  }

  @Test
  void readWithUnknownContentType() throws IOException {
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
    converter.setSupportedMediaTypes(MediaType.APPLICATION_OCTET_STREAM);
    var result = this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void readWithExtensionRegistry() throws IOException {
    ProtobufHttpMessageConverter converterWithRegistry = new ProtobufHttpMessageConverter(this.extensionRegistry);
    byte[] body = this.testMsg.toByteArray();
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(ProtobufHttpMessageConverter.PROTOBUF);
    var result = converterWithRegistry.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void writeMessageBuilder() throws IOException {
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = ProtobufHttpMessageConverter.PROTOBUF;
    Msg.Builder builder = this.testMsg.toBuilder();
    this.converter.write(builder, contentType, outputMessage);
    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);
    Message result = Msg.parseFrom(outputMessage.getBodyAsBytes());
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void writeJsonWithUtf8Charset() throws IOException {
    this.converter = new ProtobufHttpMessageConverter(
            new ProtobufHttpMessageConverter.ProtobufJavaUtilSupport(null, null),
            this.extensionRegistry);
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = new MediaType("application", "json", StandardCharsets.UTF_8);
    this.converter.write(this.testMsg, contentType, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(contentType);

    final String body = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
    assertThat(body).as("body is empty").isNotEmpty();

    Msg.Builder builder = Msg.newBuilder();
    JsonFormat.parser().merge(body, builder);
    assertThat(builder.build()).isEqualTo(this.testMsg);
  }

  @Test
  void canReadWithNullMediaType() {
    assertThat(this.converter.canRead(Msg.class, null)).isTrue();
  }

  @Test
  void canReadWithUnsupportedClass() {
    assertThat(this.converter.canRead(Object.class, ProtobufHttpMessageConverter.PROTOBUF)).isFalse();
  }

  @Test
  void canWriteWithUnsupportedClass() {
    assertThat(this.converter.canWrite(Object.class, ProtobufHttpMessageConverter.PROTOBUF)).isFalse();
  }

  @Test
  void readWithTextPlainAndCharset() throws IOException {
    String text = "foo: \"Foo\"\nblah {\n  blah: 123\n}";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(text.getBytes(StandardCharsets.UTF_8));
    MediaType contentType = new MediaType("text", "plain", StandardCharsets.UTF_8);
    inputMessage.getHeaders().setContentType(contentType);
    Msg result = (Msg) this.converter.read(Msg.class, inputMessage);
    assertThat(result).isEqualTo(this.testMsg);
  }

  @Test
  void supportsRepeatableWrites() {
    assertThat(this.converter.supportsRepeatableWrites(this.testMsg)).isTrue();
  }

}
