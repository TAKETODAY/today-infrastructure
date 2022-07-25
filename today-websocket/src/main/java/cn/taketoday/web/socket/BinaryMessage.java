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
 * A binary WebSocket message.
 *
 * @author TODAY 2021/4/3 11:57
 * @since 3.0
 */
public class BinaryMessage extends AbstractMessage<ByteBuffer> {

  /**
   * Create a new binary WebSocket message with the given ByteBuffer payload.
   *
   * @param payload the non-null payload
   */
  public BinaryMessage(ByteBuffer payload) {
    super(payload, true);
  }

  /**
   * Create a new binary WebSocket message with the given payload representing the
   * full or partial message content. When the {@code isLast} boolean flag is set
   * to {@code false} the message is sent as partial content and more partial
   * messages will be expected until the boolean flag is set to {@code true}.
   *
   * @param payload the non-null payload
   * @param isLast if the message is the last of a series of partial messages
   */
  public BinaryMessage(ByteBuffer payload, boolean isLast) {
    super(payload, isLast);
  }

  /**
   * Create a new binary WebSocket message with the given byte[] payload.
   *
   * @param payload a non-null payload; note that this value is not copied so care
   * must be taken not to modify the array.
   */
  public BinaryMessage(byte[] payload) {
    this(payload, true);
  }

  /**
   * Create a new binary WebSocket message with the given byte[] payload representing
   * the full or partial message content. When the {@code isLast} boolean flag is set
   * to {@code false} the message is sent as partial content and more partial
   * messages will be expected until the boolean flag is set to {@code true}.
   *
   * @param payload a non-null payload; note that this value is not copied so care
   * must be taken not to modify the array.
   * @param isLast if the message is the last of a series of partial messages
   */
  public BinaryMessage(byte[] payload, boolean isLast) {
    this(payload, 0, payload.length, isLast);
  }

  /**
   * Create a new binary WebSocket message by wrapping an existing byte array.
   *
   * @param payload a non-null payload; note that this value is not copied so care
   * must be taken not to modify the array.
   * @param offset the offset into the array where the payload starts
   * @param length the length of the array considered for the payload
   * @param isLast if the message is the last of a series of partial messages
   */
  public BinaryMessage(byte[] payload, int offset, int length, boolean isLast) {
    super(ByteBuffer.wrap(payload, offset, length), isLast);
  }

  	@Override
	public int getPayloadLength() {
		return getPayload().remaining();
	}

	@Override
	protected String toStringPayload() {
		return getPayload().toString();
	}

}
