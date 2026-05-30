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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import infra.messaging.Message;
import infra.messaging.MessageHeaders;
import infra.messaging.simp.SimpMessageHeaderAccessor;
import infra.messaging.simp.SimpMessageType;
import infra.messaging.support.MessageBuilder;
import infra.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for
 * {@link AbstractMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
class MessageConverterTests {

  private TestMessageConverter converter = new TestMessageConverter();

  @Test
  void supportsTargetClass() {
    Message<String> message = MessageBuilder.withPayload("ABC").build();

    assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
    assertThat(this.converter.fromMessage(message, Integer.class)).isNull();
  }

  @Test
  void supportsMimeType() {
    Message<String> message = MessageBuilder.withPayload(
            "ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeType.TEXT_PLAIN).build();

    assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
  }

  @Test
  void supportsMimeTypeNotSupported() {
    Message<String> message = MessageBuilder.withPayload(
            "ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeType.APPLICATION_JSON).build();

    assertThat(this.converter.fromMessage(message, String.class)).isNull();
  }

  @Test
  void supportsMimeTypeNotSpecified() {
    Message<String> message = MessageBuilder.withPayload("ABC").build();
    assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
  }

  @Test
  void supportsMimeTypeNoneConfigured() {
    Message<String> message = MessageBuilder.withPayload(
            "ABC").setHeader(MessageHeaders.CONTENT_TYPE, MimeType.APPLICATION_JSON).build();
    this.converter = new TestMessageConverter(new MimeType[0]);

    assertThat(this.converter.fromMessage(message, String.class)).isEqualTo("success-from");
  }

  @Test
  void canConvertFromStrictContentTypeMatch() {
    this.converter = new TestMessageConverter(MimeType.TEXT_PLAIN);
    this.converter.setStrictContentTypeMatch(true);

    Message<String> message = MessageBuilder.withPayload("ABC").build();
    assertThat(this.converter.canConvertFrom(message, String.class)).isFalse();

    message = MessageBuilder.withPayload("ABC")
            .setHeader(MessageHeaders.CONTENT_TYPE, MimeType.TEXT_PLAIN).build();
    assertThat(this.converter.canConvertFrom(message, String.class)).isTrue();

  }

  @Test
  void setStrictContentTypeMatchWithNoSupportedMimeTypes() {
    this.converter = new TestMessageConverter(new MimeType[0]);
    assertThatIllegalArgumentException().isThrownBy(() -> this.converter.setStrictContentTypeMatch(true));
  }

  @Test
  void toMessageWithHeaders() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", "bar");
    MessageHeaders headers = new MessageHeaders(map);
    Message<?> message = this.converter.toMessage("ABC", headers);

    assertThat(message.getHeaders().getId()).isNotNull();
    assertThat(message.getHeaders().getTimestamp()).isNotNull();
    assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeType.TEXT_PLAIN);
    assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
  }

  @Test
  void toMessageWithMutableMessageHeaders() {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setHeader("foo", "bar");
    accessor.setNativeHeader("fooNative", "barNative");
    accessor.setLeaveMutable(true);

    MessageHeaders headers = accessor.getMessageHeaders();
    Message<?> message = this.converter.toMessage("ABC", headers);

    assertThat(message.getHeaders()).isSameAs(headers);
    assertThat(message.getHeaders().getId()).isNull();
    assertThat(message.getHeaders().getTimestamp()).isNull();
    assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeType.TEXT_PLAIN);
  }

  @Test
  void toMessageContentTypeHeader() {
    Message<?> message = this.converter.toMessage("ABC", null);
    assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeType.TEXT_PLAIN);
  }

  @Test
    // gh-29768
  void toMessageDefaultContentType() {
    DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
    resolver.setDefaultMimeType(MimeType.TEXT_PLAIN);

    TestMessageConverter converter = new TestMessageConverter();
    converter.setContentTypeResolver(resolver);
    converter.setStrictContentTypeMatch(true);

    Message<?> message = converter.toMessage("ABC", null);
    assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeType.TEXT_PLAIN);
  }

  private static class TestMessageConverter extends AbstractMessageConverter {

    public TestMessageConverter() {
      super(MimeType.TEXT_PLAIN);
    }

    public TestMessageConverter(MimeType... supportedMimeTypes) {
      super(supportedMimeTypes);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
      return String.class.equals(clazz);
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object hint) {
      return "success-from";
    }

    @Override
    protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object hint) {
      return "success-to";
    }
  }

}
