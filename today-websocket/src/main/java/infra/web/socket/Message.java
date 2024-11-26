/*
 * Copyright 2017 - 2024 the original author or authors.
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

/**
 * A message that can be handled or sent on a WebSocket connection.
 *
 * @author TODAY 2021/4/3 11:56
 * @since 3.0
 */
public interface Message<T> {

  T getPayload();

  /**
   * Return the number of bytes contained in the message.
   */
  int getPayloadLength();

  /**
   * @return Whether the partial message being sent is the last part of the message.
   */
  boolean isLast();

}
