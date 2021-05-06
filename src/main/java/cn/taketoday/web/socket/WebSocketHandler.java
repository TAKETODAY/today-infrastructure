/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket;

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/4/3 13:41
 * @since 3.0
 */
public interface WebSocketHandler {

  /**
   * called after Handshake
   */
  void afterHandshake(RequestContext context);

  /**
   * Developers must implement this method to be notified when a new conversation has
   * just begun.
   *
   * @param session
   *         the session that has just been activated.
   */
  void onOpen(WebSocketSession session);

  /**
   * Called when the message has been fully received.
   *
   * @param message
   *         the message data.
   */
  void handleMessage(WebSocketSession session, Message<?> message);

  default void onClose(WebSocketSession session) {
    onClose(session, CloseStatus.NORMAL);
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
   * @param session
   *         the session about to be closed.
   * @param status
   *         the reason the session was closed.
   */
  void onClose(WebSocketSession session, CloseStatus status);

  /**
   * Developers may implement this method when the web socket session
   * creates some kind of error that is not modeled in the web socket
   * protocol. This may for example be a notification that an incoming
   * message is too big to handle, or that the incoming message could
   * not be encoded.
   *
   * @param session
   *         the session in use when the error occurs.
   * @param throwable
   *         the throwable representing the problem.
   */
  void onError(WebSocketSession session, Throwable throwable);

  boolean supportPartialMessage();

}
