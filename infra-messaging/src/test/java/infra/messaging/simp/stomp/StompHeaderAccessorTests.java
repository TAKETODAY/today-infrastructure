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

package infra.messaging.simp.stomp;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import infra.core.testfixture.security.TestPrincipal;
import infra.messaging.Message;
import infra.messaging.MessageHeaders;
import infra.messaging.simp.SimpMessageHeaderAccessor;
import infra.messaging.simp.SimpMessageType;
import infra.messaging.support.IdTimestampMessageHeaderInitializer;
import infra.messaging.support.MessageBuilder;
import infra.messaging.support.MessageHeaderAccessor;
import infra.util.AlternativeJdkUuidGenerator;
import infra.util.LinkedMultiValueMap;
import infra.util.MimeType;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link StompHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class StompHeaderAccessorTests {

  @Test
  void createWithCommand() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
    assertThat(accessor.getCommand()).isEqualTo(StompCommand.CONNECTED);

    accessor = StompHeaderAccessor.create(StompCommand.CONNECTED, new LinkedMultiValueMap<>());
    assertThat(accessor.getCommand()).isEqualTo(StompCommand.CONNECTED);
  }

  @Test
  void createWithSubscribeNativeHeaders() {
    MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
    extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");
    extHeaders.add(StompHeaderAccessor.STOMP_DESTINATION_HEADER, "/d");

    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE, extHeaders);

    assertThat(headers.getCommand()).isEqualTo(StompCommand.SUBSCRIBE);
    assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.SUBSCRIBE);
    assertThat(headers.getDestination()).isEqualTo("/d");
    assertThat(headers.getSubscriptionId()).isEqualTo("s1");
  }

  @Test
  void createWithUnsubscribeNativeHeaders() {
    MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
    extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");

    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE, extHeaders);

    assertThat(headers.getCommand()).isEqualTo(StompCommand.UNSUBSCRIBE);
    assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.UNSUBSCRIBE);
    assertThat(headers.getSubscriptionId()).isEqualTo("s1");
  }

  @Test
  void createWithMessageFrameNativeHeaders() {
    MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
    extHeaders.add(StompHeaderAccessor.DESTINATION_HEADER, "/d");
    extHeaders.add(StompHeaderAccessor.STOMP_SUBSCRIPTION_HEADER, "s1");
    extHeaders.add(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER, "application/json");

    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE, extHeaders);

    assertThat(headers.getCommand()).isEqualTo(StompCommand.MESSAGE);
    assertThat(headers.getMessageType()).isEqualTo(SimpMessageType.MESSAGE);
    assertThat(headers.getSubscriptionId()).isEqualTo("s1");
  }

  @Test
  void createWithConnectNativeHeaders() {
    MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
    extHeaders.add(StompHeaderAccessor.STOMP_LOGIN_HEADER, "joe");
    extHeaders.add(StompHeaderAccessor.STOMP_PASSCODE_HEADER, "joe123");

    StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.STOMP, extHeaders);

    assertThat(headerAccessor.getCommand()).isEqualTo(StompCommand.STOMP);
    assertThat(headerAccessor.getMessageType()).isEqualTo(SimpMessageType.CONNECT);
    assertThat(headerAccessor.getHeader("stompCredentials")).isNotNull();
    assertThat(headerAccessor.getLogin()).isEqualTo("joe");
    assertThat(headerAccessor.getPasscode()).isEqualTo("joe123");
    assertThat(headerAccessor.toString()).contains("passcode=[PROTECTED]");

    Map<String, List<String>> output = headerAccessor.toNativeHeaderMap();
    assertThat(output.get(StompHeaderAccessor.STOMP_LOGIN_HEADER)).containsExactly("joe");
    assertThat(output.get(StompHeaderAccessor.STOMP_PASSCODE_HEADER)).containsExactly("PROTECTED");
  }

  @Test
  void toNativeHeadersSubscribe() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setSubscriptionId("s1");
    headers.setDestination("/d");

    Map<String, List<String>> actual = headers.toNativeHeaderMap();

    assertThat(actual).hasSize(2);
    assertThat(actual.get(StompHeaderAccessor.STOMP_ID_HEADER)).containsExactly("s1");
    assertThat(actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER)).containsExactly("/d");
  }

  @Test
  void toNativeHeadersUnsubscribe() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
    headers.setSubscriptionId("s1");

    Map<String, List<String>> actual = headers.toNativeHeaderMap();

    assertThat(actual).hasSize(1);
    assertThat(actual.get(StompHeaderAccessor.STOMP_ID_HEADER)).containsExactly("s1");
  }

  @Test
  void toNativeHeadersMessageFrame() {
    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
    headers.setSubscriptionId("s1");
    headers.setDestination("/d");
    headers.setContentType(MimeType.APPLICATION_JSON);
    headers.updateStompCommandAsServerMessage();

    Map<String, List<String>> actual = headers.toNativeHeaderMap();

    assertThat(actual.size()).as(actual.toString()).isEqualTo(4);
    assertThat(actual.get(StompHeaderAccessor.STOMP_SUBSCRIPTION_HEADER)).containsExactly("s1");
    assertThat(actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER)).containsExactly("/d");
    assertThat(actual.get(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER)).containsExactly("application/json");
    assertThat(actual.get(StompHeaderAccessor.STOMP_MESSAGE_ID_HEADER)).singleElement().as("message-id was not created").isNotNull();
  }

  @Test
  void toNativeHeadersContentType() {
    SimpMessageHeaderAccessor simpHeaderAccessor = SimpMessageHeaderAccessor.create();
    simpHeaderAccessor.setContentType(MimeType.valueOf("application/atom+xml"));
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], simpHeaderAccessor.getMessageHeaders());

    StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(message);
    Map<String, List<String>> map = stompHeaderAccessor.toNativeHeaderMap();

    assertThat(map.get(StompHeaderAccessor.STOMP_CONTENT_TYPE_HEADER)).containsExactly("application/atom+xml");
  }

  @Test
  void encodeConnectWithLoginAndPasscode() {
    MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
    extHeaders.add(StompHeaderAccessor.STOMP_LOGIN_HEADER, "joe");
    extHeaders.add(StompHeaderAccessor.STOMP_PASSCODE_HEADER, "joe123");

    StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT, extHeaders);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());
    byte[] bytes = new StompEncoder().encode(message);

    assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("CONNECT\nlogin:joe\npasscode:joe123\n\n\0");
  }

  @Test
  void modifyCustomNativeHeader() {
    MultiValueMap<String, String> extHeaders = new LinkedMultiValueMap<>();
    extHeaders.add(StompHeaderAccessor.STOMP_ID_HEADER, "s1");
    extHeaders.add(StompHeaderAccessor.STOMP_DESTINATION_HEADER, "/d");
    extHeaders.add("accountId", "ABC123");

    StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE, extHeaders);
    String accountId = headers.getFirstNativeHeader("accountId");
    headers.setNativeHeader("accountId", accountId.toLowerCase());

    Map<String, List<String>> actual = headers.toNativeHeaderMap();
    assertThat(actual).hasSize(3);

    assertThat(actual.get(StompHeaderAccessor.STOMP_ID_HEADER)).containsExactly("s1");
    assertThat(actual.get(StompHeaderAccessor.STOMP_DESTINATION_HEADER)).containsExactly("/d");
    assertThat(actual.get("accountId")).element(0).as("abc123").isNotNull();
  }

  @Test
  void messageIdAndTimestampDefaultBehavior() {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SEND);
    MessageHeaders headers = headerAccessor.getMessageHeaders();

    assertThat(headers.getId()).isNull();
    assertThat(headers.getTimestamp()).isNull();
  }

  @Test
  void messageIdAndTimestampEnabled() {
    IdTimestampMessageHeaderInitializer headerInitializer = new IdTimestampMessageHeaderInitializer();
    headerInitializer.setIdGenerator(new AlternativeJdkUuidGenerator());
    headerInitializer.setEnableTimestamp(true);

    StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.SEND);
    headerInitializer.initHeaders(headerAccessor);

    assertThat(headerAccessor.getMessageHeaders().getId()).isNotNull();
    assertThat(headerAccessor.getMessageHeaders().getTimestamp()).isNotNull();
  }

  @Test
  void getAccessor() {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headerAccessor.getMessageHeaders());

    assertThat(MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)).isSameAs(headerAccessor);
  }

  @Test
  void getShortLogMessage() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination("/foo");
    accessor.setContentType(MimeType.APPLICATION_JSON);
    accessor.setSessionId("123");
    String actual = accessor.getShortLogMessage("payload".getBytes(StandardCharsets.UTF_8));
    assertThat(actual).isEqualTo("SEND /foo session=123 application/json payload=payload");

    String string = "a".repeat(80);
    final String payload = string + " > 80";
    actual = accessor.getShortLogMessage(payload.getBytes(StandardCharsets.UTF_8));
    assertThat(actual).isEqualTo(("SEND /foo session=123 application/json payload=" + string + "...(truncated)"));
  }

  @Test
  void headerAccessorsAndHeartbeat() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    assertThat(accessor.getHeartbeat()).containsExactly(0, 0);
    assertThat(accessor.getContentLength()).isNull();
    assertThat(accessor.getReceipt()).isNull();
    assertThat(accessor.getReceiptId()).isNull();
    assertThat(accessor.getMessage()).isNull();
    assertThat(accessor.getMessageId()).isNull();
    assertThat(accessor.getVersion()).isNull();
    assertThat(accessor.getHost()).isNull();

    accessor.setHeartbeat(10, 20);
    accessor.setContentLength(42);
    accessor.setAck("client");
    accessor.setNack("nack-id");
    accessor.setLogin("joe");
    accessor.setPasscode("secret");
    accessor.setReceipt("receipt");
    accessor.setReceiptId("receipt-id");
    accessor.setMessage("message");
    accessor.setMessageId("message-id");
    accessor.setVersion("1.2");
    accessor.setHost("example.org");
    accessor.setUser(new TestPrincipal("joe"));

    assertThat(accessor.getHeartbeat()).containsExactly(10, 20);
    assertThat(accessor.getContentLength()).isEqualTo(42);
    assertThat(accessor.getAck()).isEqualTo("client");
    assertThat(accessor.getNack()).isEqualTo("nack-id");
    assertThat(accessor.getLogin()).isEqualTo("joe");
    assertThat(accessor.getPasscode()).isEqualTo("secret");
    assertThat(accessor.getReceipt()).isEqualTo("receipt");
    assertThat(accessor.getReceiptId()).isEqualTo("receipt-id");
    assertThat(accessor.getMessage()).isEqualTo("message");
    assertThat(accessor.getMessageId()).isEqualTo("message-id");
    assertThat(accessor.getVersion()).isEqualTo("1.2");
    assertThat(accessor.getHost()).isEqualTo("example.org");
    assertThat(accessor.toNativeHeaderMap().get(StompHeaderAccessor.STOMP_PASSCODE_HEADER))
            .containsExactly("PROTECTED");
  }

  @Test
  void updateClientAndServerCommands() {
    StompHeaderAccessor client = StompHeaderAccessor.wrap(MessageBuilder.createMessage(
            "payload", SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE).getMessageHeaders()));
    client.setSessionId("session");
    assertThat(client.updateStompCommandAsClientMessage()).isEqualTo(StompCommand.SEND);
    assertThat(client.updateStompCommandAsClientMessage()).isEqualTo(StompCommand.SEND);
    client.setHeader("stompCommand", StompCommand.SUBSCRIBE);
    assertThatIllegalStateException().isThrownBy(client::updateStompCommandAsServerMessage);

    StompHeaderAccessor server = StompHeaderAccessor.create(StompCommand.SEND);
    server.setSessionId("session");
    server.setSubscriptionId("subscription");
    server.updateStompCommandAsServerMessage();
    assertThat(server.getCommand()).isEqualTo(StompCommand.MESSAGE);
    assertThat(server.getMessageId()).startsWith("session-");
    assertThat(server.getSubscriptionId()).isEqualTo("subscription");
    server.updateStompCommandAsServerMessage();
    assertThat(server.getMessageId()).isNotNull();

    assertThatIllegalStateException().isThrownBy(() ->
            StompHeaderAccessor.create(StompCommand.CONNECT).updateStompCommandAsClientMessage());
    assertThatIllegalStateException().isThrownBy(() ->
            StompHeaderAccessor.create(StompCommand.CONNECT).updateStompCommandAsServerMessage());
    assertThatIllegalStateException().isThrownBy(() ->
            StompHeaderAccessor.create(StompCommand.MESSAGE).updateStompCommandAsClientMessage());
  }

  @Test
  void detailedLogMessagesForHeartbeatsAndCommands() {
    StompHeaderAccessor heartbeat = StompHeaderAccessor.createForHeartbeat();
    assertThat(heartbeat.isHeartbeat()).isTrue();
    assertThat(heartbeat.getDetailedLogMessage(null)).isEqualTo("heart-beat");
    heartbeat.setSessionId("session");
    assertThat(heartbeat.getDetailedLogMessage(null)).isEqualTo("heart-beat in session session");

    StompHeaderAccessor connect = StompHeaderAccessor.create(StompCommand.CONNECT);
    connect.setSessionId("session");
    connect.setUser(new TestPrincipal("joe"));
    assertThat(connect.getShortLogMessage(null)).isEqualTo("CONNECT user=joe session=session");

    StompHeaderAccessor stomp = StompHeaderAccessor.create(StompCommand.STOMP);
    stomp.setSessionId("session");
    assertThat(stomp.getShortLogMessage(null)).isEqualTo("STOMP session=session");

    StompHeaderAccessor unsubscribe = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
    unsubscribe.setSessionId("session");
    unsubscribe.setSubscriptionId("sub");
    assertThat(unsubscribe.getShortLogMessage(null)).isEqualTo("UNSUBSCRIBE id=sub session=session");

    StompHeaderAccessor disconnect = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    disconnect.setSessionId("session");
    disconnect.setReceipt("receipt");
    assertThat(disconnect.getShortLogMessage(null)).isEqualTo("DISCONNECT receipt=receipt session=session");
  }

  @Test
  void detailedLogMessagesCoverAllCommandShapes() {
    StompHeaderAccessor subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    subscribe.setDestination("/topic/foo");
    subscribe.setSubscriptionId("sub-1");
    subscribe.setSessionId("session");
    assertThat(subscribe.getShortLogMessage(null)).isEqualTo("SUBSCRIBE /topic/foo id=sub-1 session=session");

    StompHeaderAccessor connected = StompHeaderAccessor.create(StompCommand.CONNECTED);
    connected.setHeartbeat(10, 20);
    connected.setSessionId("session");
    assertThat(connected.getShortLogMessage(null)).isEqualTo("CONNECTED heart-beat=[10, 20] session=session");

    StompHeaderAccessor message = StompHeaderAccessor.create(StompCommand.MESSAGE);
    message.setSessionId("session");
    message.setUser(new TestPrincipal("joe"));
    message.setNativeHeader("custom", "value");
    message.setContentType(MimeType.TEXT_PLAIN);
    assertThat(message.getDetailedLogMessage("payload".getBytes(StandardCharsets.UTF_8)))
            .contains("MESSAGE", "custom", "user=joe", "payload=payload");

    StompHeaderAccessor noCommand = StompHeaderAccessor.wrap(MessageBuilder.withPayload("payload").build());
    assertThat(noCommand.getDetailedLogMessage("payload")).contains("payload");
    assertThatIllegalStateException().isThrownBy(() -> message.getShortLogMessage("payload"));
  }

  @Test
  void staticHeaderAccessorsAndContentLength() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setPasscode("secret");
    accessor.setContentLength(11);
    Map<String, Object> headers = accessor.getMessageHeaders();

    assertThat(StompHeaderAccessor.getCommand(headers)).isEqualTo(StompCommand.CONNECT);
    assertThat(StompHeaderAccessor.getPasscode(headers)).isEqualTo("secret");
    assertThat(StompHeaderAccessor.getContentLength(accessor.toNativeHeaderMap())).isEqualTo(11);
    assertThat(StompHeaderAccessor.getContentLength(Map.of())).isNull();
    assertThat(StompHeaderAccessor.getPasscode(Map.of())).isNull();
  }

}
