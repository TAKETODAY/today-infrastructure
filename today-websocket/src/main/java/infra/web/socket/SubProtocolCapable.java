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

package infra.web.socket;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An interface for WebSocket handlers that support sub-protocols as defined in RFC 6455.
 *
 * @author Rossen Stoyanchev
 * @see WebSocketHandler
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-1.9">RFC-6455 section 1.9</a>
 * @since 4.0
 */
public interface SubProtocolCapable {

  /**
   * Return the list of supported sub-protocols.
   */
  @Nullable
  List<String> getSubProtocols();

}