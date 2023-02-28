/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.session;

import java.io.Serial;

/**
 * An exception that indicates the maximum number of active sessions has been
 * reached and the server is refusing to create any new sessions.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/28 22:35
 */
public class TooManyActiveSessionsException extends IllegalStateException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The maximum number of active sessions the server will tolerate.
   */
  private final int maxActiveSessions;

  /**
   * Creates a new TooManyActiveSessionsException.
   *
   * @param message A description for the exception.
   * @param maxActive The maximum number of active sessions allowed by the
   * session manager.
   */
  public TooManyActiveSessionsException(String message, int maxActive) {
    super(message);
    this.maxActiveSessions = maxActive;
  }

  /**
   * Gets the maximum number of sessions allowed by the session manager.
   *
   * @return The maximum number of sessions allowed by the session manager.
   */
  public int getMaxActiveSessions() {
    return maxActiveSessions;
  }

}

