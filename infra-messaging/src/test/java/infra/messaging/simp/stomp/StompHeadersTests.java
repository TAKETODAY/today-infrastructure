/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.messaging.simp.stomp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import infra.messaging.Message;
import infra.messaging.support.MessageBuilder;
import infra.messaging.support.MessageHeaderAccessor;
import infra.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StompHeadersTests {

  private final StompHeaders headers = new StompHeaders();

  @Test
  void mapsConnectionHeadersToWireNames() {
    headers.setHost("broker.example");
    headers.setLogin("reader");
    headers.setPasscode("test-password");
    headers.setSession("session-1");
    headers.setServer("broker/1");

    assertThat(headers).containsExactlyInAnyOrderEntriesOf(Map.of("host", List.of("broker.example"),
            "login", List.of("reader"), "passcode", List.of("test-password"),
            "session", List.of("session-1"), "server", List.of("broker/1")));
    assertThat(headers.getHost()).isEqualTo("broker.example");
    assertThat(headers.getLogin()).isEqualTo("reader");
    assertThat(headers.getPasscode()).isEqualTo("test-password");
    assertThat(headers.getSession()).isEqualTo("session-1");
    assertThat(headers.getServer()).isEqualTo("broker/1");
    headers.setPasscode(null);
    assertThat(headers).doesNotContainKey("passcode");
  }

  @Test
  void mapsDeliveryAndReceiptHeadersToWireNames() {
    headers.setDestination("/queue/events");
    headers.setId("sub-1");
    headers.setAck("client-individual");
    headers.setSubscription("subscription-1");
    headers.setMessageId("message-1");
    headers.setReceipt("request-1");
    headers.setReceiptId("receipt-1");

    assertThat(headers).containsExactlyInAnyOrderEntriesOf(Map.of("destination", List.of("/queue/events"),
            "id", List.of("sub-1"), "ack", List.of("client-individual"), "subscription", List.of("subscription-1"),
            "message-id", List.of("message-1"), "receipt", List.of("request-1"), "receipt-id", List.of("receipt-1")));
    assertThat(headers.getDestination()).isEqualTo("/queue/events");
    assertThat(headers.getId()).isEqualTo("sub-1");
    assertThat(headers.getAck()).isEqualTo("client-individual");
    assertThat(headers.getSubscription()).isEqualTo("subscription-1");
    assertThat(headers.getMessageId()).isEqualTo("message-1");
    assertThat(headers.getReceipt()).isEqualTo("request-1");
    assertThat(headers.getReceiptId()).isEqualTo("receipt-1");
  }

  @Test
  void acceptsSupportedVersionsAndKeepsThemOnInvalidUpdates() {
    assertThat(headers.getAcceptVersion()).isNull();
    headers.setAcceptVersion("1.2", "1.1");
    assertThat(headers.getFirst("accept-version")).isEqualTo("1.2,1.1");
    assertThat(headers.getAcceptVersion()).containsExactly("1.2", "1.1");
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setAcceptVersion("1.0")).withMessage("Invalid version: 1.0");
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setAcceptVersion("1.2", null)).withMessage("Invalid version: null");
    assertThat(headers.getAcceptVersion()).containsExactly("1.2", "1.1");
    headers.setAcceptVersion();
    assertThat(headers).doesNotContainKey("accept-version");
  }

  @Test
  void preservesExplicitContentMetadataAndRejectsWildcardTypes() {
    assertThat(headers.getContentLength()).isEqualTo(-1);
    assertThat(headers.getContentType()).isNull();
    headers.setContentLength(4_294_967_296L);
    headers.setContentType(MimeType.APPLICATION_JSON);
    assertThat(headers.getContentLength()).isEqualTo(4_294_967_296L);
    assertThat(headers.getContentType()).isEqualTo(MimeType.APPLICATION_JSON);
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setContentType(new MimeType("*", "*")))
            .withMessageContaining("wildcard type");
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setContentType(new MimeType("text", "*")))
            .withMessageContaining("wildcard subtype");
    assertThat(headers.getContentType()).isEqualTo(MimeType.APPLICATION_JSON);
    headers.setContentType(null);
    assertThat(headers).doesNotContainKey("content-type");
  }

  @ParameterizedTest
  @CsvSource({ "0, 0, false", "1000, 0, false", "0, 2000, false", "1000, 2000, true" })
  void interpretsHeartbeatIntervals(long outgoing, long incoming, boolean enabled) {
    headers.setHeartbeat(new long[] { outgoing, incoming });
    assertThat(headers.getFirst("heart-beat")).isEqualTo(outgoing + "," + incoming);
    assertThat(headers.getHeartbeat()).containsExactly(outgoing, incoming);
    assertThat(headers.isHeartbeatEnabled()).isEqualTo(enabled);
  }

  @Test
  void validatesHeartbeatBeforeChangingTheHeader() {
    assertThat(headers.isHeartbeatEnabled()).isFalse();
    headers.set("heart-beat", "1000");
    assertThat(headers.getHeartbeat()).isNull();
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setHeartbeat(null)).withMessageContaining("not null");
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setHeartbeat(new long[] { 1 })).withMessageContaining("not 1");
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setHeartbeat(new long[] { -1, 0 })).withMessageEndingWith("-1,0");
    assertThatIllegalArgumentException().isThrownBy(() -> headers.setHeartbeat(new long[] { 0, -1 })).withMessageEndingWith("0,-1");
    assertThat(headers.getFirst("heart-beat")).isEqualTo("1000");
  }

  @Test
  void distinguishesAppendingReplacingAndRemovingHeaderValues() {
    headers.add("tag", "first");
    headers.add("tag", "second");
    assertThat(headers.getFirst("tag")).isEqualTo("first");
    assertThat(headers.set("tag", "third")).containsExactly("first", "second");

    String[] tags = { "a", "b" };
    assertThat(headers.setOrRemove("tag", tags)).containsExactly("third");
    tags[0] = "changed";
    assertThat(headers.setOrRemove("tag", (String[]) null)).containsExactly("a", "b");

    var values = new ArrayList<>(List.of("x", "y"));
    headers.setOrRemove("tag", values);
    values.clear();
    assertThat(headers.setOrRemove("tag", (Collection<String>) null)).containsExactly("x", "y");
    assertThat(headers).isEmpty();
  }

  @Test
  void readOnlyHeadersRejectChangesToBothMapAndValueLists() {
    headers.add("tag", "one");
    headers.putAll(Map.of("scope", List.of("local")));
    StompHeaders readOnly = StompHeaders.readOnlyStompHeaders(headers);

    assertThat(readOnly).isEqualTo(headers).hasSameHashCodeAs(headers).isNotEqualTo(new StompHeaders()).isNotEqualTo("tag");
    assertThat(readOnly.keySet()).containsExactlyInAnyOrder("tag", "scope");
    assertThat(readOnly.values()).containsExactlyInAnyOrder(List.of("one"), List.of("local"));
    assertThat(readOnly.containsValue(List.of("one"))).isTrue();
    assertThat(readOnly).hasToString(headers.toString());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> readOnly.put("tag", List.of("two")));
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> readOnly.add("tag", "two"));
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(readOnly::clear);
    headers.clear();
    assertThat(readOnly).hasSize(2);
    assertThat(StompHeaders.readOnlyStompHeaders(null)).isEmpty();
  }

  @Test
  void roundTripsAFrameWithRepeatedNativeHeadersAndBinaryPayload() {
    byte[] payload = { 65, 0, 66 };
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination("/queue/events");
    accessor.addNativeHeader("tag", "first");
    accessor.addNativeHeader("tag", "second");
    Message<byte[]> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    var codec = new StompTcpMessageCodec();

    var encoded = codec.encode(message);
    assertThat(StandardCharsets.UTF_8.decode(encoded.asReadOnlyBuffer()).toString())
            .startsWith("SEND\n").contains("destination:/queue/events\n", "content-length:3\n");
    assertThat(codec.decode(encoded)).singleElement().satisfies(decoded -> {
      var decodedHeaders = MessageHeaderAccessor.getAccessor(decoded, StompHeaderAccessor.class);
      assertThat(decodedHeaders).isNotNull();
      assertThat(decodedHeaders.getCommand()).isEqualTo(StompCommand.SEND);
      assertThat(decodedHeaders.getDestination()).isEqualTo("/queue/events");
      assertThat(decodedHeaders.getNativeHeader("tag")).containsExactly("first", "second");
      assertThat(decoded.getPayload()).containsExactly(payload);
    });
  }

}
