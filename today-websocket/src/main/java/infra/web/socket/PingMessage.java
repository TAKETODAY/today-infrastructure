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

import java.nio.ByteBuffer;

/**
 * The PingMessage interface represents a web socket ping.
 *
 * @author TODAY 2021/4/5 16:49
 * @since 3.0
 */
public final class PingMessage extends AbstractMessage<ByteBuffer> {

  /**
   * Create a new ping message with an empty payload.
   */
  public PingMessage() {
    super(ByteBuffer.allocate(0));
  }

  /**
   * Create a new ping message with the given ByteBuffer payload.
   *
   * @param payload the non-null payload
   */
  public PingMessage(ByteBuffer payload) {
    super(payload);
  }

  @Override
  public int getPayloadLength() {
    return getPayload().remaining();
  }

}
