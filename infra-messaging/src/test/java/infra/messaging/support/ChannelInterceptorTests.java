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

package infra.messaging.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import infra.messaging.Message;
import infra.messaging.MessageChannel;
import infra.messaging.MessageHandler;
import infra.messaging.MessageDeliveryException;
import infra.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for the use of {@link ChannelInterceptor}s.
 *
 * @author Rossen Stoyanchev
 */
class ChannelInterceptorTests {

  private ExecutorSubscribableChannel channel;

  private TestMessageHandler messageHandler;

  @BeforeEach
  void setup() {
    this.channel = new ExecutorSubscribableChannel();
    this.messageHandler = new TestMessageHandler();
    this.channel.subscribe(this.messageHandler);
  }

  @Test
  void preSendInterceptorReturningModifiedMessage() {
    Message<?> expected = mock();
    PreSendInterceptor interceptor = new PreSendInterceptor();
    interceptor.setMessageToReturn(expected);
    this.channel.addInterceptor(interceptor);
    this.channel.send(MessageBuilder.withPayload("test").build());

    assertThat(this.messageHandler.getMessages()).hasSize(1);
    Message<?> result = this.messageHandler.getMessages().get(0);

    assertThat(result).isNotNull();
    assertThat(result).isSameAs(expected);
    assertThat(interceptor.wasAfterCompletionInvoked()).isTrue();
  }

  @Test
  void preSendInterceptorReturningNull() {
    PreSendInterceptor interceptor1 = new PreSendInterceptor();
    NullReturningPreSendInterceptor interceptor2 = new NullReturningPreSendInterceptor();
    this.channel.addInterceptor(interceptor1);
    this.channel.addInterceptor(interceptor2);
    Message<?> message = MessageBuilder.withPayload("test").build();
    this.channel.send(message);

    assertThat(interceptor1.getCounter().get()).isEqualTo(1);
    assertThat(interceptor2.getCounter().get()).isEqualTo(1);
    assertThat(this.messageHandler.getMessages()).isEmpty();
    assertThat(interceptor1.wasAfterCompletionInvoked()).isTrue();
    assertThat(interceptor2.wasAfterCompletionInvoked()).isFalse();
  }

  @Test
  void postSendInterceptorMessageWasSent() {
    final AtomicBoolean preSendInvoked = new AtomicBoolean();
    final AtomicBoolean completionInvoked = new AtomicBoolean();
    this.channel.addInterceptor(new ChannelInterceptor() {
      @Override
      public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        assertInput(message, channel, sent);
        preSendInvoked.set(true);
      }

      @Override
      public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        assertInput(message, channel, sent);
        completionInvoked.set(true);
      }

      private void assertInput(Message<?> message, MessageChannel channel, boolean sent) {
        assertThat(message).isNotNull();
        assertThat(channel).isNotNull();
        assertThat(channel).isSameAs(ChannelInterceptorTests.this.channel);
        assertThat(sent).isTrue();
      }
    });
    this.channel.send(MessageBuilder.withPayload("test").build());
    assertThat(preSendInvoked.get()).isTrue();
    assertThat(completionInvoked.get()).isTrue();
  }

  @Test
  void postSendInterceptorMessageWasNotSent() {
    final AbstractMessageChannel testChannel = new AbstractMessageChannel() {
      @Override
      protected boolean sendInternal(Message<?> message, long timeout) {
        return false;
      }
    };
    final AtomicBoolean preSendInvoked = new AtomicBoolean();
    final AtomicBoolean completionInvoked = new AtomicBoolean();
    testChannel.addInterceptor(new ChannelInterceptor() {
      @Override
      public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        assertInput(message, channel, sent);
        preSendInvoked.set(true);
      }

      @Override
      public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        assertInput(message, channel, sent);
        completionInvoked.set(true);
      }

      private void assertInput(Message<?> message, MessageChannel channel, boolean sent) {
        assertThat(message).isNotNull();
        assertThat(channel).isNotNull();
        assertThat(channel).isSameAs(testChannel);
        assertThat(sent).isFalse();
      }
    });
    testChannel.send(MessageBuilder.withPayload("test").build());
    assertThat(preSendInvoked.get()).isTrue();
    assertThat(completionInvoked.get()).isTrue();
  }

  @Test
  void afterCompletionWithSendException() {
    final AbstractMessageChannel testChannel = new AbstractMessageChannel() {
      @Override
      protected boolean sendInternal(Message<?> message, long timeout) {
        throw new RuntimeException("Simulated exception");
      }
    };
    PreSendInterceptor interceptor1 = new PreSendInterceptor();
    PreSendInterceptor interceptor2 = new PreSendInterceptor();
    testChannel.addInterceptor(interceptor1);
    testChannel.addInterceptor(interceptor2);
    try {
      testChannel.send(MessageBuilder.withPayload("test").build());
    }
    catch (Exception ex) {
      assertThat(ex.getCause().getMessage()).isEqualTo("Simulated exception");
    }
    assertThat(interceptor1.wasAfterCompletionInvoked()).isTrue();
    assertThat(interceptor2.wasAfterCompletionInvoked()).isTrue();
  }

  @Test
  void afterCompletionWithPreSendException() {
    PreSendInterceptor interceptor1 = new PreSendInterceptor();
    PreSendInterceptor interceptor2 = new PreSendInterceptor();
    interceptor2.setExceptionToRaise(new RuntimeException("Simulated exception"));
    this.channel.addInterceptor(interceptor1);
    this.channel.addInterceptor(interceptor2);
    try {
      this.channel.send(MessageBuilder.withPayload("test").build());
    }
    catch (Exception ex) {
      assertThat(ex.getCause().getMessage()).isEqualTo("Simulated exception");
    }
    assertThat(interceptor1.wasAfterCompletionInvoked()).isTrue();
    assertThat(interceptor2.wasAfterCompletionInvoked()).isFalse();
  }

  @Test
  void channelConfigurationAndInterceptorCollection() {
    AbstractMessageChannel testChannel = new TestChannel();
    testChannel.setBeanName("custom");
    assertThat(testChannel.getBeanName()).isEqualTo("custom");
    assertThat(testChannel.toString()).contains("custom");
    assertThat(testChannel.getLogger()).isNotNull();
    testChannel.setLogger(mock());
    assertThat(testChannel.getLogger()).isNotNull();

    ChannelInterceptor first = mock();
    ChannelInterceptor second = mock();
    testChannel.setInterceptors(List.of(first, second));
    assertThat(testChannel.getInterceptors()).containsExactly(first, second);
    assertThat(testChannel.removeInterceptor(first)).isTrue();
    testChannel.addInterceptor(0, first);
    assertThat(testChannel.removeInterceptor(0)).isSameAs(first);
    assertThat(testChannel.removeInterceptor(mock())).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> testChannel.addInterceptor(null));
    assertThatIllegalArgumentException().isThrownBy(() -> testChannel.setInterceptors(Arrays.asList(first, null)));
  }

  @Test
  void wrapsErrorsAndPreservesMessagingExceptions() {
    AbstractMessageChannel runtimeFailure = new AbstractMessageChannel() {
      @Override
      protected boolean sendInternal(Message<?> message, long timeout) {
        throw new RuntimeException("failure");
      }
    };
    assertThatExceptionOfType(MessageDeliveryException.class)
            .isThrownBy(() -> runtimeFailure.send(MessageBuilder.withPayload("test").build()))
            .withCauseInstanceOf(RuntimeException.class);

    MessagingException messagingException = new MessagingException("messaging failure");
    AbstractMessageChannel messagingFailure = new AbstractMessageChannel() {
      @Override
      protected boolean sendInternal(Message<?> message, long timeout) {
        throw messagingException;
      }
    };
    assertThatExceptionOfType(MessagingException.class)
            .isThrownBy(() -> messagingFailure.send(MessageBuilder.withPayload("test").build()))
            .isSameAs(messagingException);

    AbstractMessageChannel errorFailure = new AbstractMessageChannel() {
      @Override
      protected boolean sendInternal(Message<?> message, long timeout) {
        throw new AssertionError("error");
      }
    };
    assertThatExceptionOfType(MessageDeliveryException.class)
            .isThrownBy(() -> errorFailure.send(MessageBuilder.withPayload("test").build()))
            .withCauseInstanceOf(AssertionError.class);
  }

  @Test
  void interceptorCompletionErrorsDoNotHideSendResult() {
    AbstractMessageChannel testChannel = new TestChannel();
    ChannelInterceptor interceptor = new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        return message;
      }

      @Override
      public void afterSendCompletion(Message<?> message, MessageChannel channel,
              boolean sent, Exception ex) {
        throw new RuntimeException("cleanup");
      }
    };
    testChannel.addInterceptor(interceptor);
    assertThat(testChannel.send(MessageBuilder.withPayload("test").build())).isTrue();
  }

  @Test
  void receiveInterceptorChain() {
    TestChannel testChannel = new TestChannel();
    AtomicBoolean firstAfterReceive = new AtomicBoolean();
    AtomicBoolean secondAfterReceive = new AtomicBoolean();
    testChannel.addInterceptor(new ChannelInterceptor() {
      @Override
      public boolean preReceive(MessageChannel channel) {
        return true;
      }

      @Override
      public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        return MessageBuilder.withPayload(message.getPayload() + "-changed").build();
      }

      @Override
      public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        firstAfterReceive.set(true);
      }
    });
    testChannel.addInterceptor(new ChannelInterceptor() {
      @Override
      public boolean preReceive(MessageChannel channel) {
        return false;
      }

      @Override
      public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        return null;
      }

      @Override
      public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        secondAfterReceive.set(true);
      }
    });

    assertThat(testChannel.applyPreReceive()).isFalse();
    assertThat(secondAfterReceive).isFalse();
    assertThat(firstAfterReceive).isTrue();
    assertThat(testChannel.applyPostReceive(MessageBuilder.withPayload("test").build())).isNull();
  }

  private static class TestChannel extends AbstractMessageChannel {

    @Override
    protected boolean sendInternal(Message<?> message, long timeout) {
      return true;
    }

    boolean applyPreReceive() {
      return new ChannelInterceptorChain().applyPreReceive(this);
    }

    Message<?> applyPostReceive(Message<?> message) {
      return new ChannelInterceptorChain().applyPostReceive(message, this);
    }
  }

  private static class TestMessageHandler implements MessageHandler {

    private final List<Message<?>> messages = new ArrayList<>();

    public List<Message<?>> getMessages() {
      return this.messages;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
      this.messages.add(message);
    }
  }

  private abstract static class AbstractTestInterceptor implements ChannelInterceptor {

    private AtomicInteger counter = new AtomicInteger();

    private volatile boolean afterCompletionInvoked;

    public AtomicInteger getCounter() {
      return this.counter;
    }

    public boolean wasAfterCompletionInvoked() {
      return this.afterCompletionInvoked;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
      assertThat(message).isNotNull();
      counter.incrementAndGet();
      return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
      this.afterCompletionInvoked = true;
    }
  }

  private static class PreSendInterceptor extends AbstractTestInterceptor {

    private Message<?> messageToReturn;

    private RuntimeException exceptionToRaise;

    public void setMessageToReturn(Message<?> messageToReturn) {
      this.messageToReturn = messageToReturn;
    }

    public void setExceptionToRaise(RuntimeException exception) {
      this.exceptionToRaise = exception;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
      super.preSend(message, channel);
      if (this.exceptionToRaise != null) {
        throw this.exceptionToRaise;
      }
      return (this.messageToReturn != null ? this.messageToReturn : message);
    }
  }

  private static class NullReturningPreSendInterceptor extends AbstractTestInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
      super.preSend(message, channel);
      return null;
    }
  }

}
