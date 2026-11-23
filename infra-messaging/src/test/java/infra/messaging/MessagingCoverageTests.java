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

package infra.messaging;

import org.junit.jupiter.api.Test;

import infra.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingCoverageTests {

  @Test
  void messageChannelDefaultSendUsesIndefiniteTimeout() {
    GenericMessage<String> message = new GenericMessage<>("payload");
    MessageChannel channel = new MessageChannel() {
      @Override
      public boolean send(Message<?> actual, long timeout) {
        assertThat(actual).isSameAs(message);
        assertThat(timeout).isEqualTo(MessageChannel.INDEFINITE_TIMEOUT);
        return true;
      }
    };

    assertThat(channel.send(message)).isTrue();
  }

  @Test
  void messagingExceptionRetainsFailedMessageAndCause() {
    Message<String> message = new GenericMessage<>("payload");
    Throwable cause = new IllegalStateException("boom");

    MessagingException withoutMessage = new MessagingException("description");
    MessagingException withCause = new MessagingException("description", cause);
    MessagingException withMessage = new MessagingException(message);
    MessagingException withDescription = new MessagingException(message, "description");
    MessagingException withMessageAndCause = new MessagingException(message, cause);
    MessagingException withAll = new MessagingException(message, "description", cause);

    assertThat(withoutMessage.getFailedMessage()).isNull();
    assertThat(withCause).hasCause(cause);
    assertThat(withMessage.getFailedMessage()).isSameAs(message);
    assertThat(withDescription).hasMessage("description");
    assertThat(withMessageAndCause).hasCause(cause);
    assertThat(withAll).hasCause(cause).hasMessage("description");
    assertThat(withMessage.toString()).contains("failedMessage");
    assertThat(withoutMessage.toString()).doesNotContain("failedMessage");
  }

  @Test
  void derivedMessagingExceptionsRetainMessage() {
    Message<String> message = new GenericMessage<>("payload");
    Throwable cause = new RuntimeException("cause");

    MessageHandlingException handling = new MessageHandlingException(message, "handling", cause);
    MessageDeliveryException delivery = new MessageDeliveryException(message, "delivery", cause);
    MessageDeliveryException descriptionOnly = new MessageDeliveryException("description");
    MessageDeliveryException messageOnly = new MessageDeliveryException(message);
    MessageDeliveryException causeOnly = new MessageDeliveryException(message, cause);

    assertThat(handling).hasMessage("handling").hasCause(cause);
    assertThat(delivery).hasMessage("delivery").hasCause(cause);
    assertThat(descriptionOnly.getFailedMessage()).isNull();
    assertThat(messageOnly.getFailedMessage()).isSameAs(message);
    assertThat(causeOnly).hasCause(cause);
  }

}
