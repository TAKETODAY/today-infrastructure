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

package infra.web.socket;

import org.jspecify.annotations.Nullable;

import infra.util.concurrent.Future;

/**
 * The Web Socket Handler represents an object that can handle websocket conversations.
 * Developers may extend this class in order to implement a programmatic websocket
 * handler. The handler class holds lifecycle methods that may be
 * overridden to intercept websocket open, error and close events. By implementing
 * the {@link WebSocketHandler#onOpen(WebSocketSession) onOpen} method
 *
 * <p>It will be instantiated once for all the connections to the server.
 *
 * <p>Here is an example of a simple endpoint that echoes any incoming text message back to the sender.
 * <pre>{@code
 * public class EchoWebSocketHandler extends WebSocketHandler {
 *
 *  @Override
 *  public void handleMessage(WebSocketSession session, WebSocketMessage message) {
 *    session.sendText("Got your message (" + message.getPayloadAsText() + "). Thanks !");
 *  }
 *
 * }}</pre>
 * <p>
 * this handler Wraps another {@link WebSocketHandler} instance and delegates to it.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/4/3 13:41
 */
public abstract class WebSocketHandler {

  @Nullable
  protected final WebSocketHandler delegate;

  protected WebSocketHandler() {
    this(null);
  }

  protected WebSocketHandler(@Nullable WebSocketHandler delegate) {
    this.delegate = delegate;
  }

  /**
   * to go through all nested delegates and return the "raw" handler.
   */
  public WebSocketHandler getRawHandler() {
    WebSocketHandler result = this;
    while (result.delegate != null) {
      result = result.delegate;
    }
    return result;
  }

  /**
   * Developers must implement this method to be notified when a new conversation has
   * just begun.
   *
   * @param session the session that has just been activated.
   */
  public void onOpen(WebSocketSession session) throws Throwable {
    if (delegate != null) {
      delegate.onOpen(session);
    }
  }

  /**
   * Called when the message has been fully received.
   *
   * @param message the message data.
   * @return a {@code Future} which completes when the {@code WebSocketMessage}
   * may be reclaimed; or {@code null} if it may be reclaimed immediately
   * @since 5.0
   */
  @Nullable
  public Future<Void> handleMessage(WebSocketSession session, WebSocketMessage message) {
    if (delegate != null) {
      return delegate.handleMessage(session, message);
    }
    else {
      return switch (message.getType()) {
        case PING -> handlePingMessage(session, message);
        case PONG -> handlePongMessage(session, message);
        case TEXT -> handleTextMessage(session, message);
        case BINARY -> handleBinaryMessage(session, message);
      };
    }
  }

  /**
   * Receives a Close message indicating the WebSocket's input has been
   * closed.
   *
   * <p>This is the last invocation from the specified {@code WebSocketSession}.
   * By the time this invocation begins the WebSocket's input will have
   * been closed.
   *
   * <p>A Close message consists of a status code and a reason for
   * closing. The status code is an integer from the range
   * {@code 1000 <= code <= 65535}. The {@code reason} is a string which
   * has a UTF-8 representation not longer than {@code 123} bytes.
   *
   * <p> If the WebSocket's output is not already closed, the
   * {@code Future} returned by this method will be used as an
   * indication that the WebSocket's output may be closed. The WebSocket
   * will close its output at the earliest of completion of the returned
   * {@code Future} or invoking either of the {@code sendClose}
   * or {@code abort} methods.
   *
   * <p> To specify a custom closure code or reason code the
   * {@code close} method may be invoked from inside the
   * {@code onClose} invocation:
   * {@snippet :
   * public Future<Void> onClose(WebSocketSession session, CloseStatus status) {
   *    session.close(CUSTOM_STATUS_CODE, CUSTOM_REASON);
   *    return Future.ok();
   * }}
   *
   * @param session the session about to be closed.
   * @param status the reason the session was closed.
   * @return a {@code Future} which completes when the
   * {@code WebSocketSession} may be closed; or {@code null} if it may be
   * closed immediately
   * @implSpec The default implementation of this method returns
   * {@code null}, indicating that the output should be closed
   * immediately.
   * @apiNote Returning a {@code Future} that never completes,
   * effectively disables the reciprocating closure of the output.
   */
  @Nullable
  public Future<Void> onClose(WebSocketSession session, CloseStatus status) {
    if (delegate != null) {
      delegate.onClose(session, status);
    }
    return null;
  }

  /**
   * Developers may implement this method when the web socket session
   * creates some kind of error that is not modeled in the web socket
   * protocol. This may for example be a notification that an incoming
   * message is too big to handle, or that the incoming message could
   * not be encoded.
   *
   * @param session the session in use when the error occurs.
   * @param throwable the throwable representing the problem.
   */
  public void onError(WebSocketSession session, Throwable throwable) throws Throwable {
    if (delegate != null) {
      delegate.onError(session, throwable);
    }
  }

  /**
   * A Ping message has been received.
   *
   * <p> As guaranteed by the WebSocket Protocol, the message consists of
   * not more than {@code 125} bytes. These bytes are located from the
   * buffer's position to its limit.
   *
   * <p> Given that the WebSocket implementation will automatically send a
   * reciprocal pong when a ping is received, it is rarely required to
   * send a pong message explicitly when a ping is received.
   *
   * @param session the session on which the data has been received
   * @param message websocket message data
   * @return a {@code Future} which completes when the {@code WebSocketMessage}
   * may be reclaimed; or {@code null} if it may be reclaimed immediately
   */
  @Nullable
  protected Future<Void> handlePingMessage(WebSocketSession session, WebSocketMessage message) {
    if (delegate != null) {
      return delegate.handlePingMessage(session, message);
    }
    return null;
  }

  /**
   * A Pong message has been received.
   *
   * <p> As guaranteed by the WebSocket Protocol, the message consists of
   * not more than {@code 125} bytes. These bytes are located from the
   * buffer's position to its limit.
   *
   * @param session the session on which the data has been received
   * @param message websocket message data
   * @return a {@code Future} which completes when the {@code WebSocketMessage}
   * may be reclaimed; or {@code null} if it may be reclaimed immediately
   */
  @Nullable
  protected Future<Void> handlePongMessage(WebSocketSession session, WebSocketMessage message) {
    if (delegate != null) {
      delegate.handlePongMessage(session, message);
    }
    return null;
  }

  /**
   * A textual data has been received.
   *
   * <p> Return a {@code Future} which will be used by the
   * {@code WebSocketSession} as an indication it may reclaim the
   * {@link WebSocketMessage#getPayloadAsText}. Do not access the data after
   * this {@code Future} has completed.
   *
   * @param session the session on which the data has been received
   * @param message websocket message data
   * @return a {@code Future} which completes when the {@code WebSocketMessage}
   * may be reclaimed; or {@code null} if it may be reclaimed immediately
   * @see WebSocketMessage#getPayloadAsText()
   */
  @Nullable
  protected Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
    if (delegate != null) {
      delegate.handleTextMessage(session, message);
    }
    return null;
  }

  /**
   * A binary data has been received.
   *
   * @param session the session on which the data has been received
   * @param message websocket message data
   * @return a {@code Future} which completes when the {@code WebSocketMessage}
   * may be reclaimed; or {@code null} if it may be reclaimed immediately
   */
  @Nullable
  protected Future<Void> handleBinaryMessage(WebSocketSession session, WebSocketMessage message) {
    if (delegate != null) {
      delegate.handleBinaryMessage(session, message);
    }
    return null;
  }

}
