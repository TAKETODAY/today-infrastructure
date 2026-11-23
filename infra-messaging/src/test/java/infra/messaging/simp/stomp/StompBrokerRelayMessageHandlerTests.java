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

package infra.messaging.simp.stomp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import infra.core.testfixture.security.TestPrincipal;
import infra.messaging.Message;
import infra.messaging.MessageDeliveryException;
import infra.messaging.MessageHandler;
import infra.messaging.MessageHeaders;
import infra.messaging.StubMessageChannel;
import infra.messaging.simp.SimpMessageHeaderAccessor;
import infra.messaging.simp.SimpMessageType;
import infra.messaging.support.GenericMessage;
import infra.messaging.support.MessageBuilder;
import infra.messaging.support.MessageHeaderAccessor;
import infra.messaging.tcp.ReconnectStrategy;
import infra.messaging.tcp.TcpConnection;
import infra.messaging.tcp.TcpConnectionHandler;
import infra.messaging.tcp.TcpOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link StompBrokerRelayMessageHandler}.
 *
 * @author Rossen Stoyanchev
 */
class StompBrokerRelayMessageHandlerTests {

  private StompBrokerRelayMessageHandler brokerRelay;

  private final StubMessageChannel outboundChannel = new StubMessageChannel();

  private final StubTcpOperations tcpClient = new StubTcpOperations();

  private final ArgumentCaptor<Runnable> messageCountTaskCaptor = ArgumentCaptor.forClass(Runnable.class);

  @BeforeEach
  void setup() {
    this.brokerRelay = new StompBrokerRelayMessageHandler(new StubMessageChannel(),
            this.outboundChannel, new StubMessageChannel(), Collections.singletonList("/topic")) {

      @Override
      protected void startInternal() {
        publishBrokerAvailableEvent(); // Force this, since we'll never actually connect
        super.startInternal();
      }
    };

    this.brokerRelay.setTcpClient(this.tcpClient);

    this.brokerRelay.setTaskScheduler(mock());
  }

  @Test
  void virtualHost() {
    this.brokerRelay.setVirtualHost("ABC");

    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);

    StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
    assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers1.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
    assertThat(headers1.getHost()).isEqualTo("ABC");

    StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
    assertThat(headers2.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers2.getSessionId()).isEqualTo("sess1");
    assertThat(headers2.getHost()).isEqualTo("ABC");
  }

  @Test
  void virtualHostDefault() {
    Message<byte[]> connectMessage = connectMessage("sess1", "joe");
    MessageHeaderAccessor.getAccessor(connectMessage, StompHeaderAccessor.class).setHost("ABC");

    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);

    StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
    assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers1.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
    assertThat(headers1.getHost()).isNull();

    StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
    assertThat(headers2.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers2.getSessionId()).isEqualTo("sess1");
    assertThat(headers2.getHost()).isNull();
  }

  @Test
  void loginAndPasscode() {
    this.brokerRelay.setSystemLogin("syslogin");
    this.brokerRelay.setSystemPasscode("syspasscode");
    this.brokerRelay.setClientLogin("clientlogin");
    this.brokerRelay.setClientPasscode("clientpasscode");

    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);

    StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
    assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers1.getLogin()).isEqualTo("syslogin");
    assertThat(headers1.getPasscode()).isEqualTo("syspasscode");

    StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
    assertThat(headers2.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers2.getLogin()).isEqualTo("clientlogin");
    assertThat(headers2.getPasscode()).isEqualTo("clientpasscode");
  }

  @Test
  void destinationExcluded() {
    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
    accessor.setLeaveMutable(true);
    this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

    accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setSessionId("sess1");
    accessor.setDestination("/user/daisy/foo");
    this.brokerRelay.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);
    StompHeaderAccessor headers = this.tcpClient.getSentHeaders(0);
    assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);

    headers = this.tcpClient.getSentHeaders(1);
    assertThat(headers.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers.getSessionId()).isEqualTo("sess1");
  }

  @Test
    // gh-22822
  void destinationExcludedWithHeartbeat() {
    Message<byte[]> connectMessage = connectMessage("sess1", "joe");
    MessageHeaderAccessor.getAccessor(connectMessage, StompHeaderAccessor.class).setHeartbeat(10000, 10000);

    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage);

    SimpMessageHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
    accessor.setLeaveMutable(true);
    this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

    // Run the messageCountTask to clear the message count
    verify(this.brokerRelay.getTaskScheduler()).scheduleWithFixedDelay(this.messageCountTaskCaptor.capture(), eq(Duration.ofMillis(5000L)));
    this.messageCountTaskCaptor.getValue().run();

    accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setSessionId("sess1");
    accessor.setDestination("/user/daisy/foo");
    this.brokerRelay.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

    assertThat(this.tcpClient.getSentMessages()).hasSize(3);
    assertThat(this.tcpClient.getSentHeaders(2).getMessageType()).isEqualTo(SimpMessageType.HEARTBEAT);
  }

  @Test
  void messageFromBrokerIsEnriched() {
    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);
    assertThat(this.tcpClient.getSentHeaders(0).getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(this.tcpClient.getSentHeaders(1).getCommand()).isEqualTo(StompCommand.CONNECT);

    this.tcpClient.handleMessage(message(StompCommand.MESSAGE, null, null, null));

    Message<byte[]> message = this.outboundChannel.getMessages().get(0);
    StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    assertThat(accessor.getSessionId()).isEqualTo("sess1");
    assertThat(accessor.getUser().getName()).isEqualTo("joe");
  }

  // SPR-12820

  @Test
  void connectWhenBrokerNotAvailable() {
    this.brokerRelay.start();
    this.brokerRelay.stopInternal();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    Message<byte[]> message = this.outboundChannel.getMessages().get(0);
    StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
    assertThat(accessor.getSessionId()).isEqualTo("sess1");
    assertThat(accessor.getUser().getName()).isEqualTo("joe");
    assertThat(accessor.getMessage()).isEqualTo("Broker not available.");
  }

  @Test
  void sendAfterBrokerUnavailable() {
    this.brokerRelay.start();
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);

    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(2);

    this.brokerRelay.stopInternal();
    this.brokerRelay.handleMessage(message(StompCommand.SEND, "sess1", "joe", "/foo"));
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);

    Message<byte[]> message = this.outboundChannel.getMessages().get(0);
    StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
    assertThat(accessor.getSessionId()).isEqualTo("sess1");
    assertThat(accessor.getUser().getName()).isEqualTo("joe");
    assertThat(accessor.getMessage()).isEqualTo("Broker not available.");
  }

  @Test
  @SuppressWarnings("rawtypes")
  void systemSubscription() {
    MessageHandler handler = mock();
    this.brokerRelay.setSystemSubscriptions(Collections.singletonMap("/topic/foo", handler));
    this.brokerRelay.start();

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
    accessor.setLeaveMutable(true);
    MessageHeaders headers = accessor.getMessageHeaders();
    this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], headers));

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);
    assertThat(this.tcpClient.getSentHeaders(0).getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(this.tcpClient.getSentHeaders(1).getCommand()).isEqualTo(StompCommand.SUBSCRIBE);
    assertThat(this.tcpClient.getSentHeaders(1).getDestination()).isEqualTo("/topic/foo");

    Message<byte[]> message = message(StompCommand.MESSAGE, null, null, "/topic/foo");
    this.tcpClient.handleMessage(message);

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(handler).handleMessage(captor.capture());

    Message actual = captor.getValue();
    assertThat(actual).isSameAs(message);

    accessor = StompHeaderAccessor.getAccessor(actual, StompHeaderAccessor.class);
    assertThat(accessor.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);
  }

  @Test
  void alreadyConnected() {
    this.brokerRelay.start();

    Message<byte[]> connect = connectMessage("sess1", "joe");
    this.brokerRelay.handleMessage(connect);

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);

    StompHeaderAccessor headers1 = this.tcpClient.getSentHeaders(0);
    assertThat(headers1.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers1.getSessionId()).isEqualTo(StompBrokerRelayMessageHandler.SYSTEM_SESSION_ID);

    StompHeaderAccessor headers2 = this.tcpClient.getSentHeaders(1);
    assertThat(headers2.getCommand()).isEqualTo(StompCommand.CONNECT);
    assertThat(headers2.getSessionId()).isEqualTo("sess1");

    this.brokerRelay.handleMessage(connect);

    assertThat(this.tcpClient.getSentMessages()).hasSize(2);
    assertThat(this.outboundChannel.getMessages()).isEmpty();
  }

  @Test
  void rejectsServerMessagesWhenBrokerIsUnavailable() {
    this.brokerRelay.start();
    this.brokerRelay.stopInternal();

    assertThatExceptionOfType(MessageDeliveryException.class)
            .isThrownBy(() -> this.brokerRelay.handleMessage(message(StompCommand.SEND, null, null, "/topic/events")))
            .withMessageContaining("Message broker not active");
    assertThat(this.outboundChannel.getMessages()).isEmpty();
  }

  @Test
  void rejectsUnadaptedMessagesAndIgnoresFramesForUnknownSessions() {
    this.brokerRelay.start();
    var plain = new GenericMessage<>("not a STOMP frame");
    var unsupported = MessageBuilder.createMessage("no SIMP headers", new MessageHeaderAccessor().getMessageHeaders());

    assertThatIllegalStateException().isThrownBy(() -> this.brokerRelay.handleMessage(plain))
            .withMessageContaining("No header accessor");
    assertThatIllegalStateException().isThrownBy(() -> this.brokerRelay.handleMessage(unsupported))
            .withMessageContaining("Unexpected header accessor type");
    this.brokerRelay.handleMessage(message(StompCommand.SUBSCRIBE, null, null, "/topic/events"));
    this.brokerRelay.handleMessage(message(StompCommand.SEND, "missing", null, "/topic/events"));
    this.brokerRelay.handleMessage(message(StompCommand.DISCONNECT, "missing", null, null));
    assertThat(this.tcpClient.getSentMessages()).hasSize(1);
    assertThat(this.outboundChannel.getMessages()).isEmpty();
  }

  @Test
  void waitsForConnectedBeforeForwardingAndClosesOnDisconnectWithoutReceipt() {
    this.brokerRelay.start();
    Message<byte[]> connect = connectMessage("sess1", "joe");
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(connect, StompHeaderAccessor.class);
    assertThat(accessor).isNotNull();
    accessor.setImmutable();
    this.brokerRelay.handleMessage(connect);
    var send = message(StompCommand.SEND, "sess1", null, "/topic/events");
    this.brokerRelay.handleMessage(send);
    assertThat(this.tcpClient.getSentMessages()).hasSize(2);

    this.tcpClient.handleMessage(message(StompCommand.CONNECTED, null, null, null));
    this.brokerRelay.handleMessage(send);
    this.brokerRelay.handleMessage(message(StompCommand.DISCONNECT, "sess1", null, null));

    assertThat(this.tcpClient.getSentHeaders(2).getDestination()).isEqualTo("/topic/events");
    assertThat(this.tcpClient.getSentHeaders(3).getCommand()).isEqualTo(StompCommand.DISCONNECT);
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);
    assertThat(this.tcpClient.connection.closed).isTrue();
  }

  @Test
  void keepsConnectionOpenForDisconnectReceiptUntilBrokerClosesIt() {
    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));
    this.tcpClient.handleMessage(message(StompCommand.CONNECTED, null, null, null));
    Message<byte[]> disconnect = message(StompCommand.DISCONNECT, "sess1", null, null);
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(disconnect, StompHeaderAccessor.class);
    assertThat(accessor).isNotNull();
    accessor.setReceipt("close-1");
    this.brokerRelay.handleMessage(disconnect);

    assertThat(this.tcpClient.getSentHeaders(2).getReceipt()).isEqualTo("close-1");
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(2);
    assertThat(this.tcpClient.connection.closed).isFalse();

    this.tcpClient.connectionHandler.afterConnectionClosed();
    this.tcpClient.connectionHandler.afterConnectionClosed();
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);
    assertThat(this.outboundChannel.getMessages()).hasSize(2);
    var error = MessageHeaderAccessor.getAccessor(this.outboundChannel.getMessages().get(1), StompHeaderAccessor.class);
    assertThat(error).isNotNull();
    assertThat(error.getCommand()).isEqualTo(StompCommand.ERROR);
    assertThat(error.getMessage()).isEqualTo("Connection to broker closed.");
  }

  @Test
  void reportsMissingConnectedFrameAndRemovesTheClientSession() {
    this.brokerRelay.setHeaderInitializer(headers -> headers.setHeader("origin", "relay"));
    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    this.tcpClient.connection.readInactivity.run();
    assertThat(this.tcpClient.connection.closed).isTrue();
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);
    assertThat(this.outboundChannel.getMessages()).singleElement().satisfies(message -> {
      StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      assertThat(accessor).isNotNull();
      assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
      assertThat(accessor.getSessionId()).isEqualTo("sess1");
      assertThat(accessor.getMessage()).contains("No CONNECTED frame received");
      assertThat(accessor.getHeader("origin")).isEqualTo("relay");
    });
  }

  @Test
  void reportsClientConnectionFailureAndRemovesTheClientSession() {
    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));

    this.tcpClient.connectionHandler.afterConnectFailure(new IllegalStateException("connection refused"));

    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);
    assertThat(this.outboundChannel.getMessages()).singleElement().satisfies(message -> {
      StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      assertThat(accessor).isNotNull();
      assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
      assertThat(accessor.getSessionId()).isEqualTo("sess1");
      assertThat(accessor.getMessage()).contains("Failed to connect: connection refused");
    });
  }

  @Test
  void reportsForwardFailureAndClosesTheClientConnection() {
    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));
    this.tcpClient.handleMessage(message(StompCommand.CONNECTED, null, null, null));
    this.tcpClient.connection.sendFailure = new IllegalStateException("write failed");

    this.brokerRelay.handleMessage(message(StompCommand.SEND, "sess1", "joe", "/topic/events"));

    assertThat(this.tcpClient.connection.closed).isTrue();
    assertThat(this.outboundChannel.getMessages()).hasSize(2);
    assertThat(this.outboundChannel.getMessages().get(1)).satisfies(message -> {
      StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      assertThat(accessor).isNotNull();
      assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
      assertThat(accessor.getMessage()).contains("failed to forward");
    });
  }

  @Test
  void systemConnectionConfiguresHeartbeatsAndHandlesSystemMessages() {
    MessageHandler handler = mock();
    this.brokerRelay.setSystemSubscriptions(Collections.singletonMap("/topic/system", handler));
    this.brokerRelay.start();

    StompHeaderAccessor connected = StompHeaderAccessor.create(StompCommand.CONNECTED);
    connected.setHeartbeat(100, 100);
    connected.setLeaveMutable(true);
    this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], connected.getMessageHeaders()));

    assertThat(this.tcpClient.connection.writeInactivity).isNotNull();
    assertThat(this.tcpClient.connection.readInactivity).isNotNull();
    assertThat(this.tcpClient.getSentHeaders(1).getCommand()).isEqualTo(StompCommand.SUBSCRIBE);

    this.tcpClient.connection.writeInactivity.run();
    assertThat(this.tcpClient.getSentHeaders(2).getMessageType()).isEqualTo(SimpMessageType.HEARTBEAT);

    this.tcpClient.handleMessage(message(StompCommand.MESSAGE, null, null, null));
    this.tcpClient.handleMessage(message(StompCommand.MESSAGE, null, null, "/topic/unknown"));
    this.tcpClient.handleMessage(message(StompCommand.MESSAGE, null, null, "/topic/system"));
    verify(handler).handleMessage(any());
  }

  @Test
  void systemHeartbeatFailureClosesConnectionAndSystemHandlerExceptionsAreIgnored() {
    MessageHandler handler = mock();
    doThrow(new IllegalStateException("handler failed")).when(handler).handleMessage(any());
    this.brokerRelay.setSystemSubscriptions(Collections.singletonMap("/topic/system", handler));
    this.brokerRelay.start();

    StompHeaderAccessor connected = StompHeaderAccessor.create(StompCommand.CONNECTED);
    connected.setHeartbeat(100, 100);
    connected.setLeaveMutable(true);
    this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], connected.getMessageHeaders()));

    this.tcpClient.handleMessage(message(StompCommand.MESSAGE, null, null, "/topic/system"));
    verify(handler).handleMessage(any());

    this.tcpClient.connection.sendFailure = new IllegalStateException("heartbeat failed");
    this.tcpClient.connection.writeInactivity.run();

    assertThat(this.tcpClient.connection.closed).isTrue();
  }

  @Test
  void missingSystemHeartbeatMakesBrokerUnavailable() {
    this.brokerRelay.start();
    StompHeaderAccessor connected = StompHeaderAccessor.create(StompCommand.CONNECTED);
    connected.setHeartbeat(100, 100);
    connected.setLeaveMutable(true);
    this.tcpClient.handleMessage(MessageBuilder.createMessage(new byte[0], connected.getMessageHeaders()));

    this.tcpClient.connection.readInactivity.run();

    assertThat(this.tcpClient.connection.closed).isTrue();
    assertThat(this.brokerRelay.isBrokerAvailable()).isFalse();
  }

  @Test
  void failureToSubscribeOnSystemConnectionMakesBrokerUnavailable() {
    this.brokerRelay.setSystemSubscriptions(Collections.singletonMap("/topic/system", mock(MessageHandler.class)));
    this.brokerRelay.start();
    this.tcpClient.connection.sendFailure = new IllegalStateException("subscription write failed");

    this.tcpClient.handleMessage(message(StompCommand.CONNECTED, null, null, null));

    assertThat(this.tcpClient.connection.closed).isTrue();
    assertThat(this.brokerRelay.isBrokerAvailable()).isFalse();
  }

  @Test
  void reportsSynchronousSystemSendFailureAndIgnoresNonBrokerDestination() {
    this.brokerRelay.start();
    this.tcpClient.handleMessage(message(StompCommand.CONNECTED, null, null, null));
    this.brokerRelay.handleMessage(message(StompCommand.SEND, null, null, "/app/ignored"));
    assertThat(this.tcpClient.getSentMessages()).hasSize(1);
    this.tcpClient.connection.sendFailure = new IllegalStateException("system write failed");

    assertThatExceptionOfType(MessageDeliveryException.class)
            .isThrownBy(() -> this.brokerRelay.handleMessage(message(StompCommand.SEND, null, null, "/topic/events")))
            .havingRootCause().withMessage("system write failed");
    assertThat(this.brokerRelay.isBrokerAvailable()).isFalse();
  }

  @Test
  void transportFailureNotifiesClientAndCleansUpOnlyOnce() {
    this.brokerRelay.start();
    this.brokerRelay.handleMessage(connectMessage("sess1", "joe"));
    this.tcpClient.handleMessage(message(StompCommand.CONNECTED, null, null, null));

    this.tcpClient.connectionHandler.handleFailure(new IllegalStateException("transport lost"));
    this.tcpClient.connectionHandler.handleFailure(new IllegalStateException("late callback"));

    assertThat(this.tcpClient.connection.closed).isTrue();
    assertThat(this.outboundChannel.getMessages()).hasSize(2);
    StompHeaderAccessor error = StompHeaderAccessor.wrap(this.outboundChannel.getMessages().get(1));
    assertThat(error.getCommand()).isEqualTo(StompCommand.ERROR);
    assertThat(error.getMessage()).contains("transport lost");
  }

  @Test
  void systemConnectionClosePublishesBrokerUnavailableWithoutClosingTwice() {
    this.brokerRelay.start();
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);

    this.tcpClient.handlers.get(0).afterConnectionClosed();

    // The TCP close callback resets the handler's connection reference first;
    // the cleanup path must therefore not close an already closed connection.
    assertThat(this.tcpClient.connection.closed).isFalse();
    assertThat(this.brokerRelay.getConnectionCount()).isEqualTo(1);
    assertThat(this.brokerRelay.isBrokerAvailable()).isFalse();
  }

  @Test
  void stopContinuesWhenTcpClientShutdownFails() {
    this.tcpClient.shutdownFailure = new IllegalStateException("shutdown failed");
    this.brokerRelay.start();

    this.brokerRelay.stop();

    assertThat(this.brokerRelay.getTcpClient()).isSameAs(this.tcpClient);
  }

  @Test
  void rejectsSystemMessageBeforeSystemConnectionIsConnected() {
    this.brokerRelay.start();

    assertThatExceptionOfType(MessageDeliveryException.class)
            .isThrownBy(() -> this.brokerRelay.handleMessage(message(StompCommand.SEND, null, null, "/topic/events")))
            .withCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void configurationAndStatistics() {
    assertThat(this.brokerRelay.getRelayHost()).isEqualTo("127.0.0.1");
    assertThat(this.brokerRelay.getRelayPort()).isEqualTo(61613);
    assertThat(this.brokerRelay.getClientLogin()).isEqualTo("guest");
    assertThat(this.brokerRelay.getClientPasscode()).isEqualTo("guest");
    assertThat(this.brokerRelay.getSystemLogin()).isEqualTo("guest");
    assertThat(this.brokerRelay.getSystemPasscode()).isEqualTo("guest");
    assertThat(this.brokerRelay.getSystemHeartbeatSendInterval()).isEqualTo(10000);
    assertThat(this.brokerRelay.getSystemHeartbeatReceiveInterval()).isEqualTo(10000);
    assertThat(this.brokerRelay.getSystemSubscriptions()).isEmpty();
    assertThat(this.brokerRelay.isPausable()).isFalse();
    assertThat(this.brokerRelay.getTcpClient()).isSameAs(this.tcpClient);

    this.brokerRelay.setRelayHost("broker.example");
    this.brokerRelay.setRelayPort(61614);
    this.brokerRelay.setClientLogin("client");
    this.brokerRelay.setClientPasscode("client-pass");
    this.brokerRelay.setSystemLogin("system");
    this.brokerRelay.setSystemPasscode("system-pass");
    this.brokerRelay.setSystemHeartbeatSendInterval(5);
    this.brokerRelay.setSystemHeartbeatReceiveInterval(6);
    this.brokerRelay.setVirtualHost("vhost");
    this.brokerRelay.setHeaderInitializer(accessor -> accessor.setHeader("origin", "test"));
    this.brokerRelay.setSystemSubscriptions(Collections.emptyMap());

    assertThat(this.brokerRelay.getRelayHost()).isEqualTo("broker.example");
    assertThat(this.brokerRelay.getRelayPort()).isEqualTo(61614);
    assertThat(this.brokerRelay.getClientLogin()).isEqualTo("client");
    assertThat(this.brokerRelay.getClientPasscode()).isEqualTo("client-pass");
    assertThat(this.brokerRelay.getSystemLogin()).isEqualTo("system");
    assertThat(this.brokerRelay.getSystemPasscode()).isEqualTo("system-pass");
    assertThat(this.brokerRelay.getSystemHeartbeatSendInterval()).isEqualTo(5);
    assertThat(this.brokerRelay.getSystemHeartbeatReceiveInterval()).isEqualTo(6);
    assertThat(this.brokerRelay.getVirtualHost()).isEqualTo("vhost");
    assertThat(this.brokerRelay.getHeaderInitializer()).isNotNull();
    assertThat(this.brokerRelay.getStats().getTotalHandlers()).isZero();
    assertThat(this.brokerRelay.getStatsInfo()).contains("0 sessions");

    assertThatIllegalArgumentException().isThrownBy(() -> this.brokerRelay.setRelayHost(" "));
    assertThatIllegalArgumentException().isThrownBy(() -> this.brokerRelay.setClientLogin(""));
    assertThatIllegalArgumentException().isThrownBy(() -> this.brokerRelay.setClientPasscode(" "));
    this.brokerRelay.setTcpClient(null);
    assertThat(this.brokerRelay.isPausable()).isTrue();
    assertThat(this.brokerRelay.toString()).contains("broker.example:61614");
  }

  private Message<byte[]> connectMessage(String sessionId, String user) {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
    headers.setSessionId(sessionId);
    headers.setUser(new TestPrincipal(user));
    headers.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
  }

  private Message<byte[]> message(StompCommand command, String sessionId, String user, String destination) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    if (sessionId != null) {
      accessor.setSessionId(sessionId);
    }
    if (user != null) {
      accessor.setUser(new TestPrincipal(user));
    }
    if (destination != null) {
      accessor.setDestination(destination);
    }
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private static CompletableFuture<Void> getVoidFuture() {
    return CompletableFuture.completedFuture(null);
  }

  private static class StubTcpOperations implements TcpOperations<byte[]> {

    private StubTcpConnection connection = new StubTcpConnection();

    private final List<TcpConnectionHandler<byte[]>> handlers = new ArrayList<>();

    private Throwable shutdownFailure;

    private TcpConnectionHandler<byte[]> connectionHandler;

    public List<Message<byte[]>> getSentMessages() {
      return this.connection.getMessages();
    }

    public StompHeaderAccessor getSentHeaders(int index) {
      assertThat(getSentMessages().size()).as("Size: " + getSentMessages().size()).isGreaterThan(index);
      Message<byte[]> message = getSentMessages().get(index);
      StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      assertThat(accessor).isNotNull();
      return accessor;
    }

    @Override
    public CompletableFuture<Void> connectAsync(TcpConnectionHandler<byte[]> handler) {
      this.handlers.add(handler);
      this.connectionHandler = handler;
      handler.afterConnected(this.connection);
      return getVoidFuture();
    }

    @Override
    public CompletableFuture<Void> connectAsync(TcpConnectionHandler<byte[]> handler,
            ReconnectStrategy reconnectStrategy) {
      this.handlers.add(handler);
      this.connectionHandler = handler;
      handler.afterConnected(this.connection);
      return getVoidFuture();
    }

    @Override
    public CompletableFuture<Void> shutdownAsync() {
      if (this.shutdownFailure != null) {
        return CompletableFuture.failedFuture(this.shutdownFailure);
      }
      return getVoidFuture();
    }

    public void handleMessage(Message<byte[]> message) {
      this.connectionHandler.handleMessage(message);
    }

  }

  private static class StubTcpConnection implements TcpConnection<byte[]> {

    private final List<Message<byte[]>> messages = new ArrayList<>();

    private Runnable readInactivity;

    private Runnable writeInactivity;

    private boolean closed;

    private Throwable sendFailure;

    public List<Message<byte[]>> getMessages() {
      return this.messages;
    }

    @Override
    public CompletableFuture<Void> sendAsync(Message<byte[]> message) {
      this.messages.add(message);
      if (this.sendFailure != null) {
        return CompletableFuture.failedFuture(this.sendFailure);
      }
      return getVoidFuture();
    }

    @Override
    public void onReadInactivity(Runnable runnable, long duration) {
      this.readInactivity = runnable;
    }

    @Override
    public void onWriteInactivity(Runnable runnable, long duration) {
      this.writeInactivity = runnable;
    }

    @Override
    public void close() {
      this.closed = true;
    }
  }

}
