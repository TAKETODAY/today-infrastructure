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
package cn.taketoday.web;

import java.io.Serial;

import cn.taketoday.http.HttpStatus;

/**
 * @author TODAY <br>
 * 2018-12-02 09:14
 */
public class InternalServerException extends ResponseStatusException {
  @Serial
  private static final long serialVersionUID = 1L;
  public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

  public InternalServerException(Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, cause);
  }

  public InternalServerException(String message, Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
  }

  public InternalServerException(String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  public InternalServerException() {
    super(HttpStatus.INTERNAL_SERVER_ERROR, null);
  }

  public static InternalServerException failed() {
    return new InternalServerException();
  }

  public static InternalServerException failed(String msg) {
    return new InternalServerException(msg);
  }

  public static InternalServerException failed(Throwable cause) {
    return new InternalServerException(cause);
  }

  public static InternalServerException failed(String msg, Throwable cause) {
    return new InternalServerException(msg, cause);
  }

}
