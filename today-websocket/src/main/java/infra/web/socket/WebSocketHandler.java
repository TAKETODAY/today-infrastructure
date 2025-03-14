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

import infra.lang.Nullable;

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
   * @since 5.0
   */
  public void handleMessage(WebSocketSession session, WebSocketMessage message) throws Throwable {
    if (delegate != null) {
      delegate.handleMessage(session, message);
    }
    else {
      switch (message.getType()) {
        case PING -> handlePingMessage(session, message);
        case PONG -> handlePongMessage(session, message);
        case TEXT -> handleTextMessage(session, message);
        case BINARY -> handleBinaryMessage(session, message);
      }
    }
  }

  /**
   * This method is called immediately prior to the session with the remote
   * peer being closed. It is called whether the session is being closed
   * because the remote peer initiated a close and sent a close frame, or
   * whether the local websocket container or this endpoint requests to close
   * the session. The developer may take this last opportunity to retrieve
   * session attributes such as the ID, or any application data it holds before
   * it becomes unavailable after the completion of the method. Developers should
   * not attempt to modify the session from within this method, or send new
   * messages from this call as the underlying
   * connection will not be able to send them at this stage.
   *
   * @param session the session about to be closed.
   * @param status the reason the session was closed.
   */
  public void onClose(WebSocketSession session, CloseStatus status) throws Throwable {
    if (delegate != null) {
      delegate.onClose(session, status);
    }
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

  protected void handlePingMessage(WebSocketSession session, WebSocketMessage message) throws Throwable {
    if (delegate != null) {
      delegate.handlePingMessage(session, message);
    }
  }

  protected void handlePongMessage(WebSocketSession session, WebSocketMessage message) throws Throwable {
    if (delegate != null) {
      delegate.handlePongMessage(session, message);
    }
  }

  protected void handleTextMessage(WebSocketSession session, WebSocketMessage message) throws Throwable {
    if (delegate != null) {
      delegate.handleTextMessage(session, message);
    }
  }

  protected void handleBinaryMessage(WebSocketSession session, WebSocketMessage message) throws Throwable {
    if (delegate != null) {
      delegate.handleBinaryMessage(session, message);
    }
  }

}
