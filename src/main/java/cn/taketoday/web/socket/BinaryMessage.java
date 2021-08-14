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

import java.nio.ByteBuffer;

/**
 * @author TODAY 2021/4/3 11:57
 * @since 3.0
 */
public class BinaryMessage extends AbstractMessage<ByteBuffer> {

  public BinaryMessage(ByteBuffer data) {
    super(data);
  }

  /**
   * Create a new binary WebSocket message with the given payload representing the
   * full or partial message content. When the {@code isLast} boolean flag is set
   * to {@code false} the message is sent as partial content and more partial
   * messages will be expected until the boolean flag is set to {@code true}.
   *
   * @param payload
   *         the non-null payload
   * @param isLast
   *         whether this the last part of a series of partial messages
   */
  public BinaryMessage(ByteBuffer payload, boolean isLast) {
    super(payload, isLast);
  }

}
