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

package infra.messaging.support;

import org.junit.jupiter.api.Test;

import infra.messaging.Message;
import infra.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

class ImmutableMessageChannelInterceptorTests {

  private final ImmutableMessageChannelInterceptor interceptor = new ImmutableMessageChannelInterceptor();

  @Test
  void makesMutableHeadersImmutable() {
    MessageHeaderAccessor accessor = new MessageHeaderAccessor();
    accessor.setLeaveMutable(true);
    Message<?> message = MessageBuilder.createMessage("payload", accessor.getMessageHeaders());

    assertThat(accessor.isMutable()).isTrue();
    assertThat(this.interceptor.preSend(message, ignoredChannel())).isSameAs(message);
    assertThat(accessor.isMutable()).isFalse();
  }

  @Test
  void leavesMessageWithoutAccessorUnchanged() {
    Message<?> message = new GenericMessage<>("payload");
    assertThat(this.interceptor.preSend(message, ignoredChannel())).isSameAs(message);
  }

  private MessageChannel ignoredChannel() {
    return (actual, timeout) -> true;
  }

}
