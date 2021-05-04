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

import cn.taketoday.context.AttributeAccessor;

/**
 * @author TODAY 2021/4/5 11:48
 * @since 3.0
 */
public interface WebSocketSession extends AttributeAccessor {

  /**
   * Session Id
   */
  String getId();

  boolean isSecure();

  boolean isOpen();

  /**
   * Get the idle timeout for this session.
   *
   * @return The current idle timeout for this session in milliseconds. Zero
   * or negative values indicate an infinite timeout.
   */
  long getMaxIdleTimeout();

  /**
   * Set the idle timeout for this session.
   *
   * @param timeout
   *         The new idle timeout for this session in milliseconds.
   *         Zero or negative values indicate an infinite timeout.
   */
  void setMaxIdleTimeout(long timeout);

  /**
   * Set the current maximum buffer size for binary messages.
   *
   * @param max
   *         The new maximum buffer size in bytes
   */
  void setMaxBinaryMessageBufferSize(int max);

  /**
   * Get the current maximum buffer size for binary messages.
   *
   * @return The current maximum buffer size in bytes
   */
  int getMaxBinaryMessageBufferSize();

  /**
   * Set the maximum buffer size for text messages.
   *
   * @param max
   *         The new maximum buffer size in characters.
   */
  void setMaxTextMessageBufferSize(int max);

  /**
   * Get the maximum buffer size for text messages.
   *
   * @return The maximum buffer size in characters.
   */
  int getMaxTextMessageBufferSize();

  /**
   * Send a message in parts, blocking until all of the message has been transmitted. The runtime
   * reads the message in order. Non-final parts of the message are sent with isLast set to false. The final part
   * must be sent with isLast set to true.
   *
   * @param message
   *         Message
   *
   * @throws IOException
   *         if there is a problem delivering the message.
   * @see Message#isLast()
   */
  void sendMessage(Message<?> message) throws IOException;

  /**
   * Close the current conversation with a normal status code and no reason phrase.
   *
   * @throws IOException
   *         if there was a connection error closing the connection.
   */
  default void close() throws IOException {
    close(CloseStatus.NORMAL);
  }

  /**
   * Close the current conversation, giving a reason for the closure. The close
   * call causes the implementation to attempt notify the client of the close as
   * soon as it can. This may cause the sending of unsent messages immediately
   * prior to the close notification. After the close notification has been sent
   * the implementation notifies the endpoint's onClose method. Note the websocket
   * specification defines the
   * acceptable uses of status codes and reason phrases. If the application cannot
   * determine a suitable close code to use for the closeReason, it is recommended
   * to use {@link CloseStatus#NO_STATUS_CODE}.
   *
   * @param status
   *         the reason for the closure.
   *
   * @throws IOException
   *         if there was a connection error closing the connection
   */
  void close(CloseStatus status) throws IOException;

}
