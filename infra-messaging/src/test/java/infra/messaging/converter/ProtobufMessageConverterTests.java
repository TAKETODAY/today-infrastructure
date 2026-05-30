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

package infra.messaging.converter;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import infra.messaging.Message;
import infra.messaging.MessageHeaders;
import infra.messaging.protobuf.Msg;
import infra.messaging.protobuf.SecondMsg;
import infra.messaging.support.MessageBuilder;

import static infra.messaging.MessageHeaders.CONTENT_TYPE;
import static infra.util.MimeType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ProtobufMessageConverter}.
 *
 * @author Parviz Rozikov
 * @author Sam Brannen
 */
class ProtobufMessageConverterTests {

  private final ProtobufMessageConverter converter = new ProtobufMessageConverter();

  private Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

  private Message<byte[]> message = MessageBuilder.withPayload(this.testMsg.toByteArray())
          .setHeader(CONTENT_TYPE, ProtobufMessageConverter.PROTOBUF).build();

  private Message<byte[]> messageWithoutContentType = MessageBuilder.withPayload(this.testMsg.toByteArray()).build();

  private final Message<byte[]> messageJson = MessageBuilder.withPayload("""
                  {
                  	"foo": "Foo",
                  	"blah": {
                  		"blah": 123
                  	}
                  }
                  """.getBytes(StandardCharsets.UTF_8))
          .setHeader(CONTENT_TYPE, APPLICATION_JSON)
          .build();

  @Test
  void extensionRegistryNull() {
    ProtobufMessageConverter converter = new ProtobufMessageConverter(null);
    assertThat(converter.extensionRegistry).isNotNull();
  }

  @Test
  void defaultContentType() {
    assertThat(converter.getDefaultContentType(testMsg)).isEqualTo(ProtobufMessageConverter.PROTOBUF);
  }

  @Test
  void canConvertFrom() {
    assertThat(converter.canConvertFrom(message, Msg.class)).isTrue();
    assertThat(converter.canConvertFrom(messageWithoutContentType, Msg.class)).isTrue();
    assertThat(converter.canConvertFrom(messageJson, Msg.class)).isTrue();
  }

  @Test
  void canConvertTo() {
    assertThat(converter.canConvertTo(testMsg, message.getHeaders())).isTrue();
    assertThat(converter.canConvertTo(testMsg, messageWithoutContentType.getHeaders())).isTrue();
    assertThat(converter.canConvertTo(testMsg, messageJson.getHeaders())).isTrue();
  }

  @Test
  void convertFrom() {
    assertThat(converter.fromMessage(message, Msg.class)).isEqualTo(testMsg);
  }

  @Test
  void convertFromNoContentType() {
    assertThat(converter.fromMessage(messageWithoutContentType, Msg.class)).isEqualTo(testMsg);
  }

  @Test
  void convertTo() {
    Message<?> message = converter.toMessage(testMsg, this.message.getHeaders());
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo(this.message.getPayload());
  }

  @Test
  void jsonWithGoogleProtobuf() throws Exception {
    ProtobufMessageConverter converter = new ProtobufMessageConverter(
            new ProtobufMessageConverter.ProtobufJavaUtilSupport(null, null),
            mock());

    //convertTo
    Message<?> message = converter.toMessage(testMsg, new MessageHeaders(Map.of(CONTENT_TYPE, APPLICATION_JSON)));
    assertThat(message).isNotNull();
    assertThat(message.getHeaders().get(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
    JSONAssert.assertEquals(new String(messageJson.getPayload()), message.getPayload().toString(), true);

    //convertFrom
    assertThat(converter.fromMessage(messageJson, Msg.class)).isEqualTo(testMsg);
  }

}
