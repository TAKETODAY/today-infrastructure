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

package infra.web.socket.client.support;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.util.DataSize;
import infra.web.socket.WebSocketExtension;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker08;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/7 11:42
 */
class DefaultClientHandshakerFactoryTests {

  private final URI uri = URI.create("/ws");

  private final List<String> subProtocols = List.of("test");

  private final List<WebSocketExtension> extensions = new ArrayList<>();

  private final HttpHeaders customHeaders = HttpHeaders.copyOf(
          Map.of("Auth", List.of("auth-id")));

  private final DefaultClientHandshakerFactory factory = new DefaultClientHandshakerFactory();

  @Test
  void webSocketVersion() {
    assertThat(factory.create(uri, subProtocols, extensions, customHeaders))
            .isInstanceOf(WebSocketClientHandshaker13.class);
    factory.setWebSocketVersion(WebSocketVersion.V08);

    assertThat(factory.create(uri, subProtocols, extensions, customHeaders))
            .isInstanceOf(WebSocketClientHandshaker08.class);

  }

  @Test
  void allowExtensions() {
    assertThatField("allowExtensions", true);
    factory.setAllowExtensions(false);
    assertThatField("allowExtensions", false);
  }

  @Test
  void performMasking() {
    assertThatField("performMasking", true);
    factory.setPerformMasking(false);
    assertThatField("performMasking", false);
  }

  @Test
  void allowMaskMismatch() {
    assertThatField("allowMaskMismatch", false);
    factory.setAllowMaskMismatch(true);
    assertThatField("allowMaskMismatch", true);
  }

  @Test
  void forceCloseTimeoutMillis() {
    assertThatField("forceCloseTimeoutMillis", -1L);
    factory.setForceCloseTimeout(Duration.ofMillis(10));
    assertThatField("forceCloseTimeoutMillis", Duration.ofMillis(10).toMillis());
  }

  @Test
  void generateOriginHeader() {
    assertThatField("generateOriginHeader", false);
    factory.setGenerateOriginHeader(true);
    assertThatField("generateOriginHeader", true);
  }

  @Test
  void absoluteUpgradeUrl() {
    assertThatField("absoluteUpgradeUrl", false);
    factory.setAbsoluteUpgradeUrl(true);
    assertThatField("absoluteUpgradeUrl", true);
  }

  @Test
  void maxFramePayloadLength() {
    assertThatField("maxFramePayloadLength", DataSize.ofKilobytes(512).toBytesInt());
    factory.setMaxFramePayload(DataSize.ofMegabytes(1));
    assertThatField("maxFramePayloadLength", DataSize.ofMegabytes(1).toBytesInt());
  }

  @Test
  void defaultHeaders() {
    var headers = new DefaultHttpHeaders();
    customHeaders.forEach(headers::add);
    assertThatField("customHeaders", headers);

    var defaultHeaders = HttpHeaders.forWritable();
    defaultHeaders.add("demo", "demo");

    defaultHeaders.forEach(headers::add);
    factory.setDefaultHeaders(defaultHeaders);
    assertThatField("customHeaders", headers);
  }

  private void assertThatField(String propertyOrField, Object expected) {
    WebSocketClientHandshaker actual = factory.create(uri, subProtocols, extensions, customHeaders);
    assertThat(actual)
            .isInstanceOf(WebSocketClientHandshaker13.class)
            .extracting(propertyOrField).isEqualTo(expected);

    assertThat(actual).extracting("expectedSubprotocol").isEqualTo("test");
  }

}