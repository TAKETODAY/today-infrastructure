/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.socket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY 2021/11/12 16:48
 */
class WebSocketHttpHeadersTests {

  private WebSocketHttpHeaders headers;

  @BeforeEach
  public void setUp() {
    headers = new WebSocketHttpHeaders();
  }

  @Test
  public void parseWebSocketExtensions() {
    List<String> extensions = new ArrayList<>();
    extensions.add("x-foo-extension, x-bar-extension");
    extensions.add("x-test-extension");
    this.headers.put(WebSocketHttpHeaders.SEC_WEBSOCKET_EXTENSIONS, extensions);

    List<WebSocketExtension> parsedExtensions = this.headers.getSecWebSocketExtensions();
    assertThat(parsedExtensions).hasSize(3);
  }

  @Test
  void emptySecWebSocketExtensions() {
    assertThat(headers.getSecWebSocketExtensions()).isEmpty();
  }

  @Test
  void setAndGetSecWebSocketKey() {
    String key = "dGhlIHNhbXBsZSBub25jZQ==";
    headers.setSecWebSocketKey(key);
    assertThat(headers.getSecWebSocketKey()).isEqualTo(key);
  }

  @Test
  void setAndGetSecWebSocketVersion() {
    String version = "13";
    headers.setSecWebSocketVersion(version);
    assertThat(headers.getSecWebSocketVersion()).isEqualTo(version);
  }

  @Test
  void setAndGetSecWebSocketAccept() {
    String accept = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";
    headers.setSecWebSocketAccept(accept);
    assertThat(headers.getSecWebSocketAccept()).isEqualTo(accept);
  }

  @Test
  void setAndGetSecWebSocketProtocolSingle() {
    String protocol = "chat";
    headers.setSecWebSocketProtocol(protocol);
    assertThat(headers.getSecWebSocketProtocol()).containsExactly(protocol);
  }

  @Test
  void setAndGetSecWebSocketProtocolMultiple() {
    List<String> protocols = List.of("chat", "superchat");
    headers.setSecWebSocketProtocol(protocols);
    assertThat(headers.getSecWebSocketProtocol()).containsExactlyElementsOf(protocols);
  }

  @Test
  void setAndGetEmptySecWebSocketProtocol() {
    headers.setSecWebSocketProtocol((String) null);
    assertThat(headers.getSecWebSocketProtocol()).isEmpty();
  }

  @Test
  void setAndGetSecWebSocketExtensionsMultiple() {
    WebSocketExtension ext1 = new WebSocketExtension("x-foo-extension");
    WebSocketExtension ext2 = new WebSocketExtension("x-bar-extension");
    List<WebSocketExtension> extensions = List.of(ext1, ext2);

    headers.setSecWebSocketExtensions(extensions);
    assertThat(headers.getSecWebSocketExtensions()).hasSize(2)
            .containsExactlyElementsOf(extensions);
  }

  @Test
  void headerEquality() {
    WebSocketHttpHeaders other = new WebSocketHttpHeaders();
    assertThat(headers).isEqualTo(headers);
    assertThat(headers).isEqualTo(other);

    headers.setSecWebSocketKey("somekey");
    assertThat(headers).isNotEqualTo(other);
  }

  @Test
  void setAndGetSecWebSocketExtensionsSingle() {
    WebSocketExtension ext = new WebSocketExtension("permessage-deflate");
    headers.setSecWebSocketExtensions(List.of(ext));
    assertThat(headers.getSecWebSocketExtensions()).containsExactly(ext);
  }

  @Test
  void setAndGetSecWebSocketExtensionsEmpty() {
    headers.setSecWebSocketExtensions(List.of());
    assertThat(headers.getSecWebSocketExtensions()).isEmpty();
  }

  @Test
  void putAndGetHeaders() {
    headers.put("X-Custom-Header", List.of("value1", "value2"));
    assertThat(headers.get("X-Custom-Header")).containsExactly("value1", "value2");
  }

  @Test
  void addHeaderValue() {
    headers.add("X-Custom-Header", "value1");
    headers.add("X-Custom-Header", "value2");
    assertThat(headers.get("X-Custom-Header")).containsExactly("value1", "value2");
  }

  @Test
  void setAllHeaders() {
    var newHeaders = new WebSocketHttpHeaders();
    newHeaders.setSecWebSocketKey("key");
    newHeaders.setSecWebSocketVersion("13");

    headers.setAll(newHeaders);
    assertThat(headers.getSecWebSocketKey()).isEqualTo("key");
    assertThat(headers.getSecWebSocketVersion()).isEqualTo("13");
  }

  @Test
  void removeHeader() {
    headers.setSecWebSocketKey("key");
    headers.remove(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY);
    assertThat(headers.getSecWebSocketKey()).isNull();
  }

  @Test
  void containsKeyForExistingHeader() {
    headers.setSecWebSocketKey("key");
    assertThat(headers.containsKey(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY)).isTrue();
  }

  @Test
  void toSingleValueMapWithMultipleValues() {
    headers.add("X-Multi", "value1");
    headers.add("X-Multi", "value2");
    assertThat(headers.toSingleValueMap()).containsEntry("X-Multi", "value1");
  }

  @Test
  void clearRemovesAllHeaders() {
    headers.setSecWebSocketKey("key");
    headers.setSecWebSocketVersion("13");
    headers.clear();
    assertThat(headers.isEmpty()).isTrue();
  }

  @Test
  void containsValueFindsExistingValue() {
    headers.setSecWebSocketKey("key");
    assertThat(headers.containsValue(List.of("key"))).isTrue();
  }

  @Test
  void getFirstReturnsNullForMissingHeader() {
    assertThat(headers.getFirst("non-existent")).isNull();
  }

  @Test
  void putIfAbsentDoesNotOverwriteExistingValue() {
    headers.setSecWebSocketKey("key1");
    headers.putIfAbsent(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY, List.of("key2"));
    assertThat(headers.getSecWebSocketKey()).isEqualTo("key1");
  }

  @Test
  void forEachIteratesOverAllHeaders() {
    headers.setSecWebSocketKey("key");
    headers.setSecWebSocketVersion("13");

    List<String> keys = new ArrayList<>();
    headers.forEach((key, value) -> keys.add(key));

    assertThat(keys).containsExactlyInAnyOrder(
            WebSocketHttpHeaders.SEC_WEBSOCKET_KEY,
            WebSocketHttpHeaders.SEC_WEBSOCKET_VERSION);
  }

  @Test
  void valuesReturnsAllHeaderValues() {
    headers.setSecWebSocketKey("key");
    headers.setSecWebSocketVersion("13");

    Collection<List<String>> values = headers.values();
    assertThat(values).hasSize(2)
            .contains(List.of("key"), List.of("13"));
  }

  @Test
  void setHeaderOverwritesExistingValues() {
    headers.add("X-Custom", "value1");
    headers.setHeader("X-Custom", "value2");
    assertThat(headers.getFirst("X-Custom")).isEqualTo("value2");
  }

  @Test
  void putAllAddsMultipleHeaders() {
    Map<String, List<String>> map = Map.of(
            "X-Custom1", List.of("value1"),
            "X-Custom2", List.of("value2")
    );
    headers.putAll(map);
    assertThat(headers.keySet()).containsExactlyInAnyOrder("X-Custom1", "X-Custom2");
  }

  @Test
  void getValuesAsListReturnsOriginalValuesForSingleHeaderValue() {
    String value = "value1";
    headers.add("X-Test", value);
    assertThat(headers.getValuesAsList("X-Test")).containsExactly(value);
  }

  @Test
  void getValuesAsListReturnsSplitValuesForCommaDelimitedString() {
    headers.add("X-Test", "value1,value2,value3");
    assertThat(headers.getValuesAsList("X-Test"))
            .containsExactly("value1", "value2", "value3");
  }

  @Test
  void setOrRemoveWithNullValueRemovesHeader() {
    headers.setSecWebSocketKey("key");
    headers.setOrRemove(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY, (String) null);
    assertThat(headers.containsKey(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY)).isFalse();
  }

  @Test
  void setOrRemoveWithNullArrayRemovesHeader() {
    headers.setSecWebSocketKey("key");
    headers.setOrRemove(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY, (String[]) null);
    assertThat(headers.containsKey(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY)).isFalse();
  }

  @Test
  void setOrRemoveWithNullCollectionRemovesHeader() {
    headers.setSecWebSocketKey("key");
    headers.setOrRemove(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY, (Collection<String>) null);
    assertThat(headers.containsKey(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY)).isFalse();
  }

  @Test
  void wrappedHeadersReceiveChanges() {
    HttpHeaders original = HttpHeaders.forWritable();
    WebSocketHttpHeaders wrapper = new WebSocketHttpHeaders(original);
    wrapper.setSecWebSocketKey("key");

    assertThat(original.getFirst(WebSocketHttpHeaders.SEC_WEBSOCKET_KEY)).isEqualTo("key");
  }

  @Test
  void hashCodeEqualForSameHeaders() {
    WebSocketHttpHeaders headers1 = new WebSocketHttpHeaders();
    WebSocketHttpHeaders headers2 = new WebSocketHttpHeaders();

    headers1.setSecWebSocketKey("key");
    headers2.setSecWebSocketKey("key");

    assertThat(headers1.hashCode()).isEqualTo(headers2.hashCode());
  }

  @Test
  void entrySetContainsAllEntries() {
    headers.setSecWebSocketKey("key");
    headers.setSecWebSocketVersion("13");

    Set<Map.Entry<String, List<String>>> entries = headers.entrySet();
    assertThat(entries).hasSize(2);
  }

  @Test
  void nullCollectionValuesHandledGracefully() {
    headers.setSecWebSocketProtocol((List<String>) null);
    assertThat(headers.getSecWebSocketProtocol()).isEmpty();
  }

  @Test
  void multipleCommaDelimitedProtocols() {
    headers.setSecWebSocketProtocol("chat,superchat,v2");
    assertThat(headers.getSecWebSocketProtocol())
            .containsExactly("chat", "superchat", "v2");
  }

  @Test
  void webSocketExtensionWithParameters() {
    WebSocketExtension ext = new WebSocketExtension("permessage-deflate",
            Map.of("client_max_window_bits", "15", "server_no_context_takeover", ""));
    headers.setSecWebSocketExtensions(List.of(ext));

    List<WebSocketExtension> result = headers.getSecWebSocketExtensions();
    assertThat(result).hasSize(1);
    WebSocketExtension parsed = result.get(0);
    assertThat(parsed.getName()).isEqualTo("permessage-deflate");
    assertThat(parsed.getParameters())
            .containsEntry("client_max_window_bits", "15")
            .containsEntry("server_no_context_takeover", "");
  }

  @Test
  void emptyHeadersReturnEmptyMap() {
    assertThat(headers.toSingleValueMap()).isEmpty();
  }

  @Test
  void getReturnsEmptyListForNonExistentHeader() {
    assertThat(headers.get("non-existent")).isNull();
  }

  @Test
  void constructFromExistingHeaders() {
    HttpHeaders source = HttpHeaders.forWritable();
    source.add("X-Test", "value");

    WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders(source);
    assertThat(wsHeaders.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void protocolWithLeadingAndTrailingSpaces() {
    headers.setSecWebSocketProtocol(" chat , superchat , v2 ");
    assertThat(headers.getSecWebSocketProtocol())
            .containsExactly("chat", "superchat", "v2");
  }

  @Test
  void toStringWithEmptyHeaders() {
    assertThat(headers.toString()).isEqualTo("[]");
  }

  @Test
  void equalityWithDifferentTypes() {
    assertThat(headers).isNotEqualTo(new Object());
  }

  @Test
  void protocolWithEmptyValues() {
    headers.setSecWebSocketProtocol(",,");
    assertThat(headers.getSecWebSocketProtocol()).isEmpty();
  }

  @Test
  void sizeReflectsHeaderCount() {
    headers.setSecWebSocketKey("key");
    headers.setSecWebSocketVersion("13");
    headers.setSecWebSocketProtocol("chat");
    assertThat(headers.size()).isEqualTo(3);
  }

  @Test
  void constructWithNullHeaders() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new WebSocketHttpHeaders(null))
            .withMessage("headers is required");
  }

  @Test
  void emptySecWebSocketProtocolReturnsEmptyList() {
    assertThat(headers.getSecWebSocketProtocol()).isEmpty();
  }

  @Test
  void singleSecWebSocketProtocolValue() {
    headers.add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, "chat");
    assertThat(headers.getSecWebSocketProtocol()).containsExactly("chat");
  }

  @Test
  void multipleSecWebSocketProtocolValues() {
    headers.addAll(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, List.of("chat", "superchat"));
    assertThat(headers.getSecWebSocketProtocol()).containsExactly("chat", "superchat");
  }

  @Test
  void protocolWithMixedCaseAndWhitespace() {
    headers.add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, " Chat, SUPERCHAT ");
    assertThat(headers.getSecWebSocketProtocol()).containsExactly("Chat", "SUPERCHAT");
  }

  @Test
  void multipleProtocolHeaderLines() {
    headers.add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, "chat");
    headers.add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, "superchat");
    assertThat(headers.getSecWebSocketProtocol()).containsExactly("chat", "superchat");
  }

  @Test
  void nullProtocolReturnsEmptyList() {
    headers.setSecWebSocketProtocol((String) null);
    assertThat(headers.getSecWebSocketProtocol()).isEmpty();
  }

}
