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

import java.nio.charset.StandardCharsets;

/**
 * @author TODAY 2021/4/3 11:57
 * @since 3.0
 */
public class TextMessage extends AbstractMessage<String> {
  private final byte[] bytes;

  /**
   * Create a new text WebSocket message from the given CharSequence payload.
   *
   * @param payload the non-null payload
   */
  public TextMessage(String payload) {
    super(payload);
    this.bytes = null;
  }

  /**
   * Create a new text WebSocket message from the given byte[]. It is assumed
   * the byte array can be encoded into an UTF-8 String.
   *
   * @param payload the non-null payload
   */
  public TextMessage(byte[] payload) {
    super(new String(payload, StandardCharsets.UTF_8));
    this.bytes = payload;
  }

  /**
   * Create a new text WebSocket message with the given payload representing the
   * full or partial message content. When the {@code isLast} boolean flag is set
   * to {@code false} the message is sent as partial content and more partial
   * messages will be expected until the boolean flag is set to {@code true}.
   *
   * @param payload the non-null payload
   * @param isLast whether this the last part of a series of partial messages
   */
  public TextMessage(CharSequence payload, boolean isLast) {
    super(payload.toString(), isLast);
    this.bytes = null;
  }

  @Override
  public int getPayloadLength() {
    return asBytes().length;
  }

  public byte[] asBytes() {
    return this.bytes != null ? this.bytes : getPayload().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected String toStringPayload() {
    String payload = getPayload();
    return payload.length() > 10 ? payload.substring(0, 10) + ".." : payload;
  }

}
