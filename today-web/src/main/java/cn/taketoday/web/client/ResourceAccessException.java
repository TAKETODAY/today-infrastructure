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

package cn.taketoday.web.client;

import java.io.IOException;
import java.io.Serial;

/**
 * Exception thrown when an I/O error occurs.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ResourceAccessException extends RestClientException {

  @Serial
  private static final long serialVersionUID = -8513182514355844870L;

  /**
   * Construct a new {@code ResourceAccessException} with the given message.
   *
   * @param msg the message
   */
  public ResourceAccessException(String msg) {
    super(msg);
  }

  /**
   * Construct a new {@code ResourceAccessException} with the given message and {@link IOException}.
   *
   * @param msg the message
   * @param ex the {@code IOException}
   */
  public ResourceAccessException(String msg, IOException ex) {
    super(msg, ex);
  }

}
