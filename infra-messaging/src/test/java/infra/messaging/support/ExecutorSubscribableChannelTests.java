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

package infra.messaging.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import infra.core.task.TaskExecutor;
import infra.logging.Logger;
import infra.messaging.Message;
import infra.messaging.MessageChannel;
import infra.messaging.MessageDeliveryException;
import infra.messaging.MessageHandler;
import infra.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link ExecutorSubscribableChannel}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class ExecutorSubscribableChannelTests {

  private ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();

  @Mock
  private MessageHandler handler;

  @Captor
  private ArgumentCaptor<Runnable> runnableCaptor;

  private final Object payload = new Object();

  private final Message<Object> message = MessageBuilder.withPayload(this.payload).build();

  @Test
  void messageMustNotBeNull() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.channel.send(null))
            .withMessageContaining("Message is required");
  }

  @Test
  void sendWithoutExecutor() {
    BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
    this.channel.addInterceptor(interceptor);
    this.channel.subscribe(this.handler);
    this.channel.send(this.message);
    verify(this.handler).handleMessage(this.message);
    assertThat(interceptor.getCounter().get()).isEqualTo(1);
    assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
  }

  @Test
  void sendWithExecutor() {
    BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
    TaskExecutor executor = mock();
    ExecutorSubscribableChannel testChannel = new ExecutorSubscribableChannel(executor);
    testChannel.addInterceptor(interceptor);
    testChannel.subscribe(this.handler);
    testChannel.send(this.message);
    verify(executor).execute(this.runnableCaptor.capture());
    verify(this.handler, never()).handleMessage(this.message);
    this.runnableCaptor.getValue().run();
    verify(this.handler).handleMessage(this.message);
    assertThat(interceptor.getCounter().get()).isEqualTo(1);
    assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
  }

  @Test
  void deliversLocallyWhenExecutorRejectsTheTask() {
    Executor executor = task -> {
      throw new RejectedExecutionException("stopping");
    };
    var channel = new ExecutorSubscribableChannel(executor);
    var interceptor = new BeforeHandleInterceptor();
    channel.setInterceptors(List.of(new ChannelInterceptor() { }));
    channel.addInterceptor(0, interceptor);
    channel.subscribe(this.handler);

    assertThat(channel.getExecutor()).isSameAs(executor);
    assertThat(channel.send(this.message)).isTrue();
    verify(this.handler).handleMessage(this.message);
    assertThat(interceptor.getCounter()).hasValue(1);
    assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
  }

  @Test
  void preservesMessagingExceptionsAndReportsFatalSubscriberFailuresToInterceptors() {
    var interceptor = new BeforeHandleInterceptor();
    this.channel.setInterceptors(List.of(interceptor));
    this.channel.subscribe(this.handler);
    var deliveryFailure = new MessageDeliveryException(this.message, "delivery failed");
    var fatalFailure = new AssertionError("subscriber failed");
    willThrow(deliveryFailure, fatalFailure).given(this.handler).handleMessage(this.message);

    assertThatExceptionOfType(MessagingException.class).isThrownBy(() -> this.channel.send(this.message))
            .isSameAs(deliveryFailure);
    assertThatExceptionOfType(MessageDeliveryException.class).isThrownBy(() -> this.channel.send(this.message))
            .withCause(fatalFailure).satisfies(ex -> assertThat(ex.getFailedMessage()).isSameAs(this.message));
    assertThat(interceptor.getCounter()).hasValue(2);
    assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
  }

  @Test
  void logsCleanupFailureWithoutPreventingEarlierInterceptorCleanup() {
    Logger logger = mock();
    this.channel.setLogger(logger);
    var first = new BeforeHandleInterceptor();
    ExecutorChannelInterceptor last = mock();
    var failure = new IllegalStateException("cleanup failed");
    given(last.preSend(this.message, this.channel)).willAnswer(invocation -> this.message);
    given(last.beforeHandle(this.message, this.channel, this.handler)).willAnswer(invocation -> this.message);
    willThrow(failure).given(last).afterMessageHandled(this.message, this.channel, this.handler, null);
    this.channel.setInterceptors(List.of(first, last));
    this.channel.subscribe(this.handler);

    assertThat(this.channel.send(this.message)).isTrue();
    verify(logger).error("Exception from afterMessageHandled in " + last, failure);
    assertThat(first.wasAfterHandledInvoked()).isTrue();
  }

  @Test
  void subscribeTwice() {
    assertThat(this.channel.subscribe(this.handler)).isTrue();
    assertThat(this.channel.subscribe(this.handler)).isFalse();
    this.channel.send(this.message);
    verify(this.handler, times(1)).handleMessage(this.message);
  }

  @Test
  void unsubscribeTwice() {
    this.channel.subscribe(this.handler);
    assertThat(this.channel.unsubscribe(this.handler)).isTrue();
    assertThat(this.channel.unsubscribe(this.handler)).isFalse();
    this.channel.send(this.message);
    verify(this.handler, never()).handleMessage(this.message);
  }

  @Test
  void failurePropagates() {
    RuntimeException ex = new RuntimeException();
    willThrow(ex).given(this.handler).handleMessage(this.message);
    MessageHandler secondHandler = mock();
    this.channel.subscribe(this.handler);
    this.channel.subscribe(secondHandler);
    try {
      this.channel.send(message);
    }
    catch (MessageDeliveryException actualException) {
      assertThat(actualException.getCause()).isEqualTo(ex);
    }
    verifyNoInteractions(secondHandler);
  }

  @Test
  void concurrentModification() {
    this.channel.subscribe(message1 -> channel.unsubscribe(handler));
    this.channel.subscribe(this.handler);
    this.channel.send(this.message);
    verify(this.handler).handleMessage(this.message);
  }

  @Test
  void interceptorWithModifiedMessage() {
    Message<?> expected = mock();
    BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
    interceptor.setMessageToReturn(expected);
    this.channel.addInterceptor(interceptor);
    this.channel.subscribe(this.handler);
    this.channel.send(this.message);
    verify(this.handler).handleMessage(expected);
    assertThat(interceptor.getCounter().get()).isEqualTo(1);
    assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
  }

  @Test
  void interceptorWithNull() {
    BeforeHandleInterceptor interceptor1 = new BeforeHandleInterceptor();
    NullReturningBeforeHandleInterceptor interceptor2 = new NullReturningBeforeHandleInterceptor();
    this.channel.addInterceptor(interceptor1);
    this.channel.addInterceptor(interceptor2);
    this.channel.subscribe(this.handler);
    this.channel.send(this.message);
    verifyNoMoreInteractions(this.handler);
    assertThat(interceptor1.getCounter().get()).isEqualTo(1);
    assertThat(interceptor2.getCounter().get()).isEqualTo(1);
    assertThat(interceptor1.wasAfterHandledInvoked()).isTrue();
  }

  @Test
  void interceptorWithException() {
    IllegalStateException expected = new IllegalStateException("Fake exception");
    willThrow(expected).given(this.handler).handleMessage(this.message);
    BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
    this.channel.addInterceptor(interceptor);
    this.channel.subscribe(this.handler);
    try {
      this.channel.send(this.message);
    }
    catch (MessageDeliveryException actual) {
      assertThat(actual.getCause()).isSameAs(expected);
    }
    verify(this.handler).handleMessage(this.message);
    assertThat(interceptor.getCounter().get()).isEqualTo(1);
    assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
  }

  private abstract static class AbstractTestInterceptor implements ChannelInterceptor, ExecutorChannelInterceptor {

    private AtomicInteger counter = new AtomicInteger();

    private volatile boolean afterHandledInvoked;

    public AtomicInteger getCounter() {
      return this.counter;
    }

    public boolean wasAfterHandledInvoked() {
      return this.afterHandledInvoked;
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
      assertThat(message).isNotNull();
      counter.incrementAndGet();
      return message;
    }

    @Override
    public void afterMessageHandled(
            Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {

      this.afterHandledInvoked = true;
    }
  }

  private static class BeforeHandleInterceptor extends AbstractTestInterceptor {

    private Message<?> messageToReturn;

    private RuntimeException exceptionToRaise;

    public void setMessageToReturn(Message<?> messageToReturn) {
      this.messageToReturn = messageToReturn;
    }

    // TODO Determine why setExceptionToRaise() is unused.
    @SuppressWarnings("unused")
    public void setExceptionToRaise(RuntimeException exception) {
      this.exceptionToRaise = exception;
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
      super.beforeHandle(message, channel, handler);
      if (this.exceptionToRaise != null) {
        throw this.exceptionToRaise;
      }
      return (this.messageToReturn != null ? this.messageToReturn : message);
    }
  }

  private static class NullReturningBeforeHandleInterceptor extends AbstractTestInterceptor {

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
      super.beforeHandle(message, channel, handler);
      return null;
    }
  }

}
