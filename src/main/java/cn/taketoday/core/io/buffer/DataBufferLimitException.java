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

package cn.taketoday.core.io.buffer;

/**
 * Exception that indicates the cumulative number of bytes consumed from a
 * stream of {@link DataBuffer DataBuffer}'s exceeded some pre-configured limit.
 * This can be raised when data buffers are cached and aggregated, e.g.
 * {@link DataBufferUtils#join}. Or it could also be raised when data buffers
 * have been released but a parsed representation is being aggregated, e.g. async
 * parsing with Jackson, SSE parsing and aggregating lines per event.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DataBufferLimitException extends IllegalStateException {

  public DataBufferLimitException(String message) {
    super(message);
  }

}
