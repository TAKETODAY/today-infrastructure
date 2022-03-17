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

import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/4/5 14:45
 * @since 3.0
 */
public abstract class AbstractMessage<T> implements Message<T> {
  private final T payload;
  private final boolean last;

  /**
   * Create a new WebSocket message with the given payload.
   *
   * @param payload the non-null payload
   */
  protected AbstractMessage(T payload) {
    this(payload, true);
  }

  /**
   * Create a new WebSocket message given payload representing the full or partial
   * message content. When the {@code isLast} boolean flag is set to {@code false}
   * the message is sent as partial content and more partial messages will be
   * expected until the boolean flag is set to {@code true}.
   *
   * @param payload the non-null payload
   * @param isLast if the message is the last of a series of partial messages
   */
  AbstractMessage(T payload, boolean isLast) {
    Assert.notNull(payload, "payload must not be null");
    this.payload = payload;
    this.last = isLast;
  }

  @Override
  public T getPayload() {
    return payload;
  }

  @Override
  public boolean isLast() {
    return last;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " payload=[" + toStringPayload() +
            "], byteCount=" + getPayloadLength() + ", last=" + isLast() + "]";
  }

  protected String toStringPayload() {
    return getPayload().toString();
  }

}
