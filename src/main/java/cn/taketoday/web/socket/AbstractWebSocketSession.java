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

import java.io.IOException;
import java.io.Serializable;

import cn.taketoday.context.AttributeAccessorSupport;

/**
 * @author TODAY 2021/4/5 14:16
 * @since 3.0
 */
public abstract class AbstractWebSocketSession
        extends AttributeAccessorSupport implements WebSocketSession, Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  protected WebSocketHandler socketHandler;

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void sendMessage(Message<?> message) throws IOException {
    if (message instanceof TextMessage) {
      final boolean supportPartialMessage = socketHandler.supportPartialMessage();
//      final String payload = message.getPayload();
    }
    else if (message instanceof BinaryMessage) {

    }
    else if (message instanceof PingMessage) {

    }
    else if (message instanceof PongMessage) {

    }
    else {
      throw new IllegalStateException("Unexpected WebSocketMessage type: " + message);
    }
  }

  /**
   * Send a text message, blocking until all of the message has been transmitted.
   *
   * @param text
   *         the message to be sent.
   *
   * @throws IOException
   *         if there is a problem delivering the message.
   */
  protected abstract void sendText(String text) throws IOException;

  /**
   * Send a text message in parts, blocking until all of the message has been transmitted. The runtime
   * reads the message in order. Non-final parts of the message are sent with isLast set to false. The final part
   * must be sent with isLast set to true.
   *
   * @param partialMessage
   *         the parts of the message being sent.
   * @param isLast
   *         Whether the partial message being sent is the last part of the message.
   *
   * @throws IOException
   *         if there is a problem delivering the message fragment.
   */
  protected abstract void sendText(String partialMessage, boolean isLast) throws IOException;

  /**
   * Send a binary message, returning when all of the message has been transmitted.
   *
   * @param data
   *         the message to be sent.
   *
   * @throws IOException
   *         if there is a problem delivering the message.
   */
  protected abstract void sendBinary(BinaryMessage data) throws IOException;

  /**
   * Send a binary message in parts, blocking until all of the message has been transmitted. The runtime
   * reads the message in order. Non-final parts are sent with isLast set to false. The final piece
   * must be sent with isLast set to true.
   *
   * @param partialByte
   *         the part of the message being sent.
   * @param isLast
   *         Whether the partial message being sent is the last part of the message.
   *
   * @throws IOException
   *         if there is a problem delivering the partial message.
   */
  protected abstract void sendBinary(BinaryMessage partialByte, boolean isLast) throws IOException; // or Iterable<byte[]>

}
