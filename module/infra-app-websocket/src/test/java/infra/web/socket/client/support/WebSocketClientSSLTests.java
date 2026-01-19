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
import java.util.List;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslManagerBundle;
import infra.core.ssl.SslOptions;
import infra.http.HttpHeaders;
import infra.web.socket.WebSocketHandler;
import io.netty.handler.ssl.SslContextBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/17 12:40
 */
class WebSocketClientSSLTests {

  final NettyWebSocketClient client = new NettyWebSocketClient();

  @Test
  void configureSslViaBundle() throws Exception {
    var sslBundle = mock(SslBundle.class);
    var sslOptions = mock(SslOptions.class);
    var managers = mock(SslManagerBundle.class);

    when(sslBundle.getOptions()).thenReturn(sslOptions);
    when(sslBundle.getManagers()).thenReturn(managers);
    when(sslOptions.getCiphers()).thenReturn(new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA" });
    when(sslOptions.getEnabledProtocols()).thenReturn(new String[] { "TLSv1.2" });

    SslOptions options = sslBundle.getOptions();

    client.setSslContext(SslContextBuilder.forClient()
            .keyManager(managers.getKeyManagerFactory())
            .trustManager(managers.getTrustManagerFactory())
            .ciphers(SslOptions.asSet(options.getCiphers()))
            .protocols(options.getEnabledProtocols())
            .build());

    var uri = URI.create("wss://localhost:8080/ws");
    var handler = mock(WebSocketHandler.class);

    client.doHandshakeInternal(handler, HttpHeaders.forWritable(), uri, List.of(), List.of());
  }

}
