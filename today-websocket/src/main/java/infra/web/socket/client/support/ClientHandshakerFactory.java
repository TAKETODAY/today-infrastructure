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
