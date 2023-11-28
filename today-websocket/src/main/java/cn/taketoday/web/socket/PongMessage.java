/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * The PongMessage interface represents a web socket pong.
 *
 * @author TODAY 2021/4/5 12:22
 * @since 3.0
 */
public final class PongMessage extends AbstractMessage<ByteBuffer> {

  /**
   * Create a new pong message with an empty payload.
   */
  public PongMessage() {
    super(ByteBuffer.allocate(0));
  }

  /**
   * Create a new pong message with the given ByteBuffer payload.
   *
   * @param payload the non-null payload
   */
  public PongMessage(ByteBuffer payload) {
    super(payload);
  }

  @Override
  public int getPayloadLength() {
    return getPayload().remaining();
  }

}
