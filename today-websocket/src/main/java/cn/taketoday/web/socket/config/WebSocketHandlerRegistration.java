/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.config;

import java.util.List;

import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.server.HandshakeHandler;
import cn.taketoday.web.socket.server.HandshakeInterceptor;

/**
 * Provides methods for configuring a WebSocket handler.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/22 22:39
 */
public interface WebSocketHandlerRegistration {

  /**
   * Add more handlers that will share the same configuration (interceptors, SockJS
   * config, etc).
   */
  WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths);

  /**
   * Configure the HandshakeHandler to use.
   */
  WebSocketHandlerRegistration setHandshakeHandler(HandshakeHandler handshakeHandler);

  /**
   * Configure interceptors for the handshake request.
   */
  WebSocketHandlerRegistration addInterceptors(HandshakeInterceptor... interceptors);

  /**
   * Set the origins for which cross-origin requests are allowed from a browser.
   * Please, refer to {@link CorsConfiguration#setAllowedOrigins(List)} for
   * format details and considerations, and keep in mind that the CORS spec
   * does not allow use of {@code "*"} with {@code allowCredentials=true}.
   * For more flexible origin patterns use {@link #setAllowedOriginPatterns}
   * instead.
   *
   * <p>By default, no origins are allowed. When
   * {@link #setAllowedOriginPatterns(String...) allowedOriginPatterns} is also
   * set, then that takes precedence over this property.
   *
   * <p>Note when SockJS is enabled and origins are restricted, transport types
   * that do not allow to check request origin (Iframe based transports) are
   * disabled. As a consequence, IE 6 to 9 are not supported when origins are
   * restricted.
   *
   * @see #setAllowedOriginPatterns(String...)
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
   * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
   */
  WebSocketHandlerRegistration setAllowedOrigins(String... origins);

  /**
   * Alternative to {@link #setAllowedOrigins(String...)} that supports more
   * flexible patterns for specifying the origins for which cross-origin
   * requests are allowed from a browser. Please, refer to
   * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
   * details and other considerations.
   * <p>By default this is not set.
   */
  WebSocketHandlerRegistration setAllowedOriginPatterns(String... originPatterns);

}
