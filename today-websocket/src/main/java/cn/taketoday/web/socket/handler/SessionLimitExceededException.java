/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.socket.handler;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.CloseStatus;

/**
 * Raised when a WebSocket session has exceeded limits it has been configured
 * for, e.g. timeout, buffer size, etc.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
@SuppressWarnings("serial")
public class SessionLimitExceededException extends RuntimeException {

  private final CloseStatus status;

  public SessionLimitExceededException(String message, @Nullable CloseStatus status) {
    super(message);
    this.status = (status != null ? status : CloseStatus.NO_STATUS_CODE);
  }

  public CloseStatus getStatus() {
    return this.status;
  }

}
