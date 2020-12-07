/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.web.exception;

import cn.taketoday.context.logger.LoggerFactory;

/**
 * @author TODAY <br>
 *         2019-07-27 09:26
 */
public class ExceptionUnhandledException extends WebRuntimeException {
  private static final long serialVersionUID = 1L;

  public ExceptionUnhandledException(Throwable cause) {
    super(cause);
  }

  public ExceptionUnhandledException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExceptionUnhandledException(String message) {
    super(message);
    LoggerFactory.getLogger(ExceptionUnhandledException.class).error(message);
  }

  public ExceptionUnhandledException() {
    super("Exception Unhandled");
  }

  /**
   * Get unhandled Throwable
   */
  public final Throwable getUnhandledException() {
    return super.getCause();
  }

}
