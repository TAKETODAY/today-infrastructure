/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.expression;

/**
 * An AccessException is thrown by an accessor if it has an unexpected problem.
 *
 * @author Andy Clement
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AccessException extends Exception {

  /**
   * Create an AccessException with a specific message.
   *
   * @param message the message
   */
  public AccessException(String message) {
    super(message);
  }

  /**
   * Create an AccessException with a specific message and cause.
   *
   * @param message the message
   * @param cause the cause
   */
  public AccessException(String message, Exception cause) {
    super(message, cause);
  }

}
