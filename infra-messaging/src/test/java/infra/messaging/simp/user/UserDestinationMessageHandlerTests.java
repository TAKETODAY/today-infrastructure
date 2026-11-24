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

package infra.messaging.simp.user;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import infra.core.testfixture.security.TestPrincipal;
import infra.messaging.Message;
import infra.messaging.StubMessageChannel;
import infra.messaging.SubscribableChannel;
import infra.messaging.simp.SimpMessageHeaderAccessor;
import infra.messaging.simp.SimpMessageType;
import infra.messaging.simp.broker.OrderedMessageChannelDecorator;
import infra.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import infra.messaging.simp.stomp.StompCommand;
import infra.messaging.simp.stomp.StompHeaderAccessor;
import infra.messaging.support.ExecutorSubscribableChannel;
import infra.messaging.support.MessageBuilder;

import static infra.messaging.simp.SimpMessageHeaderAccessor.ORIGINAL_DESTINATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link UserDestinationMessageHandler}.
 */
class UserDestinationMessageHandlerTests {

  private static final String SESSION_ID = "123";

  private final SimpUserRegistry registry = mock();

  private final SubscribableChannel brokerChannel = mock();

  private final UserDestinationMessageHandler handler = new UserDestinationMessageHandler(
          new StubMessageChannel(), this.brokerChannel, new DefaultUserDestinationResolver(this.registry));

  @Test
  @SuppressWarnings("rawtypes")
  void handleSubscribe() {
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
    this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.brokerChannel).send(captor.capture());

    Message message = captor.getValue();
    assertThat(SimpMessageHeaderAccessor.getDestination(message.getHeaders())).isEqualTo("/queue/foo-user123");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void handleUnsubscribe() {
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
    this.handler.handleMessage(createWith(SimpMessageType.UNSUBSCRIBE, "joe", "123", "/user/queue/foo"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.brokerChannel).send(captor.capture());

    Message message = captor.getValue();
    assertThat(SimpMessageHeaderAccessor.getDestination(message.getHeaders())).isEqualTo("/queue/foo-user123");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void handleMessage() {
    TestSimpUser simpUser = new TestSimpUser("joe");
    simpUser.addSessions(new TestSimpSession("123"));
    given(this.registry.getUser("joe")).willReturn(simpUser);
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
    this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.brokerChannel).send(captor.capture());

    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(captor.getValue());
    assertThat(accessor.getDestination()).isEqualTo("/queue/foo-user123");
    assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void handleMessageWithoutSessionIds() {
    UserDestinationResolver resolver = mock();
    Message message = createWith(SimpMessageType.MESSAGE, "joe", null, "/user/joe/queue/foo");
    UserDestinationResult result = new UserDestinationResult("/queue/foo-user123", Set.of("/queue/foo-user123"), "/user/queue/foo", "joe");
    given(resolver.resolveDestination(message)).willReturn(result);

    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
    UserDestinationMessageHandler handler = new UserDestinationMessageHandler(new StubMessageChannel(), this.brokerChannel, resolver);
    handler.handleMessage(message);

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.brokerChannel).send(captor.capture());

    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(captor.getValue());
    assertThat(accessor.getDestination()).isEqualTo("/queue/foo-user123");
    assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void handleMessageWithoutActiveSession() {
    this.handler.setBroadcastDestination("/topic/unresolved");
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
    this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/user/joe/queue/foo"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.brokerChannel).send(captor.capture());

    Message message = captor.getValue();
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
    assertThat(accessor.getDestination()).isEqualTo("/topic/unresolved");
    assertThat(accessor.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/joe/queue/foo");

    // Should ignore our own broadcast to brokerChannel

    this.handler.handleMessage(message);
    Mockito.verifyNoMoreInteractions(this.brokerChannel);
  }

  @Test
  @SuppressWarnings("rawtypes")
  void handleMessageFromBrokerWithActiveSession() {
    TestSimpUser simpUser = new TestSimpUser("joe");
    simpUser.addSessions(new TestSimpSession("123"));
    given(this.registry.getUser("joe")).willReturn(simpUser);

    this.handler.setBroadcastDestination("/topic/unresolved");
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
    accessor.setSessionId(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
    accessor.setDestination("/topic/unresolved");
    accessor.setNativeHeader(ORIGINAL_DESTINATION, "/user/joe/queue/foo");
    accessor.setNativeHeader("customHeader", "customHeaderValue");
    accessor.setNativeHeader("subscription", "system-subscription");
    accessor.setNativeHeader("message-id", "system-message");
    accessor.setLeaveMutable(true);
    byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
    this.handler.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.brokerChannel).send(captor.capture());
    assertThat(captor.getValue()).isNotNull();
    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(captor.getValue());
    assertThat(headers.getDestination()).isEqualTo("/queue/foo-user123");
    assertThat(headers.getFirstNativeHeader(ORIGINAL_DESTINATION)).isEqualTo("/user/queue/foo");
    assertThat(headers.getFirstNativeHeader("customHeader")).isEqualTo("customHeaderValue");
    assertThat(headers.getFirstNativeHeader("subscription")).isNull();
    assertThat(headers.getFirstNativeHeader("message-id")).isNull();
    assertThat((byte[]) captor.getValue().getPayload()).isEqualTo(payload);
  }

  @Test
  void handleMessageFromBrokerWithoutActiveSession() {
    this.handler.setBroadcastDestination("/topic/unresolved");
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
    accessor.setSessionId(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
    accessor.setDestination("/topic/unresolved");
    accessor.setNativeHeader(ORIGINAL_DESTINATION, "/user/joe/queue/foo");
    accessor.setLeaveMutable(true);
    byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
    this.handler.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

    // No re-broadcast
    verifyNoMoreInteractions(this.brokerChannel);
  }

  @Test
  void reconnectDoesNotWaitForPendingMessagesFromTheDisconnectedSession() {
    ExecutorSubscribableChannel inbound = new ExecutorSubscribableChannel();
    OrderedMessageChannelDecorator.configureInterceptor(inbound, true);
    Queue<Runnable> tasks = new ArrayDeque<>();
    ExecutorSubscribableChannel broker = new ExecutorSubscribableChannel(tasks::add);
    List<String> delivered = new ArrayList<>();
    broker.subscribe(message -> delivered.add(SimpMessageHeaderAccessor.getDestination(message.getHeaders())));
    UserDestinationMessageHandler handler = new UserDestinationMessageHandler(inbound, broker,
            new DefaultUserDestinationResolver(this.registry));
    Message<?> first = createWith(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/foo");
    Message<?> second = createWith(SimpMessageType.SUBSCRIBE, "joe", SESSION_ID, "/user/queue/bar");

    handler.handleMessage(first);
    handler.handleMessage(second);
    assertThat(tasks).hasSize(1);

    handler.handleMessage(createWith(SimpMessageType.DISCONNECT, "joe", SESSION_ID, null));
    handler.handleMessage(first);

    assertThat(tasks).hasSize(2);
    while (!tasks.isEmpty()) {
      tasks.remove().run();
    }
    assertThat(delivered).containsExactly("/queue/foo-user123", "/queue/foo-user123", "/queue/bar-user123");
  }

  @Test
  void ignoreMessage() {

    // no destination
    this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", null));
    verifyNoInteractions(this.brokerChannel);

    // not a user destination
    this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", "123", "/queue/foo"));
    verifyNoInteractions(this.brokerChannel);

    // subscribe + not a user destination
    this.handler.handleMessage(createWith(SimpMessageType.SUBSCRIBE, "joe", "123", "/queue/foo"));
    verifyNoInteractions(this.brokerChannel);

    // no match on message type
    this.handler.handleMessage(createWith(SimpMessageType.CONNECT, "joe", "123", "user/joe/queue/foo"));
    verifyNoInteractions(this.brokerChannel);
  }

  @Test
  void lifecycleAndConfiguration() {
    assertThat(this.handler.getUserDestinationResolver()).isNotNull();
    assertThat(this.handler.getBroadcastDestination()).isNull();
    assertThat(this.handler.getBrokerMessagingTemplate()).isNotNull();
    assertThat(this.handler.isAutoStartup()).isTrue();
    assertThat(this.handler.getPhase()).isEqualTo(Integer.MAX_VALUE);
    assertThat(this.handler.isRunning()).isFalse();

    this.handler.setBroadcastDestination("/topic/unresolved");
    this.handler.setHeaderInitializer(accessor -> accessor.setHeader("initialized", true));
    this.handler.setPhase(10);
    assertThat(this.handler.getBroadcastDestination()).isEqualTo("/topic/unresolved");
    assertThat(this.handler.getHeaderInitializer()).isNotNull();
    assertThat(this.handler.getPhase()).isEqualTo(10);

    this.handler.start();
    assertThat(this.handler.isRunning()).isTrue();
    this.handler.stop(() -> { });
    assertThat(this.handler.isRunning()).isFalse();
    Mockito.verify(this.brokerChannel).subscribe(this.handler);
    Mockito.verify(this.brokerChannel).unsubscribe(this.handler);
  }

  @Test
  void broadcastSkipsNonSystemMessagesAndRebroadcastsUnresolvedMessages() {
    this.handler.setBroadcastDestination("/topic/unresolved");

    StompHeaderAccessor clientMessage = StompHeaderAccessor.create(StompCommand.MESSAGE);
    clientMessage.setSessionId("client-session");
    clientMessage.setDestination("/topic/unresolved");
    clientMessage.setLeaveMutable(true);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], clientMessage.getMessageHeaders());
    this.handler.handleMessage(message);
    verifyNoInteractions(this.registry);

    UserDestinationResolver resolver = mock();
    Message<?> unresolved = createWith(SimpMessageType.MESSAGE, "joe",
            SESSION_ID, "/user/joe/queue/foo");
    given(resolver.resolveDestination(unresolved)).willReturn(new UserDestinationResult(
            "/user/joe/queue/foo", Set.of(), "/user/queue/foo", "joe"));
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
    UserDestinationMessageHandler handler = new UserDestinationMessageHandler(
            new StubMessageChannel(), this.brokerChannel, resolver);
    handler.setBroadcastDestination("/topic/unresolved");
    handler.handleMessage(unresolved);
    Mockito.verify(this.brokerChannel).send(Mockito.any(Message.class));
  }

  @Test
  @SuppressWarnings("rawtypes")
  void headerInitializerAppliedToResolvedMessage() {
    TestSimpUser simpUser = new TestSimpUser("joe");
    simpUser.addSessions(new TestSimpSession(SESSION_ID));
    given(this.registry.getUser("joe")).willReturn(simpUser);
    given(this.brokerChannel.send(Mockito.any(Message.class))).willReturn(true);
    this.handler.setHeaderInitializer(accessor -> accessor.setHeader("initialized", true));

    this.handler.handleMessage(createWith(SimpMessageType.MESSAGE, "joe", SESSION_ID,
            "/user/joe/queue/foo"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    Mockito.verify(this.brokerChannel).send(captor.capture());
    assertThat(captor.getValue().getHeaders().get("initialized")).isEqualTo(true);
  }

  private Message<?> createWith(
          SimpMessageType type, @Nullable String user, @Nullable String sessionId, @Nullable String destination) {

    SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(type);
    if (destination != null) {
      headers.setDestination(destination);
    }
    if (user != null) {
      headers.setUser(new TestPrincipal(user));
    }
    if (sessionId != null) {
      headers.setSessionId(sessionId);
    }
    return MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
  }

}
