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

package infra.web.socket.handler;

import org.jspecify.annotations.Nullable;

import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;

/**
 * A convenient base class for {@link WebSocketHandler} implementations
 * that process text messages only.
 *
 * <p>Binary messages are rejected with {@link CloseStatus#NOT_ACCEPTABLE}.
 * All other methods have empty implementations.
 *
 * @author TODAY 2021/5/6 18:07
 * @since 3.0.1
 */
public class TextWebSocketHandler extends WebSocketHandler {

  public TextWebSocketHandler() {
    this(null);
  }

  public TextWebSocketHandler(@Nullable WebSocketHandler delegate) {
    super(delegate);
  }

  @Nullable
  @Override
  protected final Future<Void> handleBinaryMessage(WebSocketSession session, WebSocketMessage message) {
    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Binary messages not supported"));
    return null;
  }

}
