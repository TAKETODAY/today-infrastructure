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

import java.net.URI;
import java.util.List;

import infra.http.HttpHeaders;
import infra.web.socket.WebSocketExtension;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/7 11:05
 */
public interface ClientHandshakerFactory {

  /**
   * Creates a new handshaker.
   *
   * @param uri URL for web socket communications. e.g "ws://myhost.com/mypath".
   * Subsequent web socket frames will be sent to this URL.
   * @param subProtocols Sub protocol request sent to the server. Null if no sub-protocol support is required.
   * @param customHeaders Custom HTTP headers to send during the handshake
   * @return netty WebSocketClientHandshaker
   */
  WebSocketClientHandshaker create(URI uri, List<String> subProtocols,
          List<WebSocketExtension> extensions, HttpHeaders customHeaders);

}
