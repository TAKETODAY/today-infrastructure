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

package cn.taketoday.http;

import cn.taketoday.core.NoStackTraceRuntimeException;

/**
 * ResponseStatus
 *
 * @author TODAY 2021/5/6 19:16
 * @since 3.0.1
 */
public class ResponseStatusException
        extends NoStackTraceRuntimeException implements HttpStatusCapable {
  private static final long serialVersionUID = 1L;
  private HttpStatus status;

  public ResponseStatusException(Throwable cause) {
    super(cause);
  }

  public ResponseStatusException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResponseStatusException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public ResponseStatusException(String message) {
    super(message);
  }

  public ResponseStatusException(HttpStatus status) {
    this.status = status;
  }

  public ResponseStatusException() { }

  public void setStatus(HttpStatus status) {
    this.status = status;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return status;
  }
}
