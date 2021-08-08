/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.http;

import cn.taketoday.web.Constant;
import cn.taketoday.web.WebNestedRuntimeException;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * @author TODAY <br>
 *         2018-11-26 20:04
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends WebNestedRuntimeException {
  private static final long serialVersionUID = 1L;

  public NotFoundException(Throwable cause) {
    super(cause);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException() {
    super(Constant.NOT_FOUND);
  }

  public static NotFoundException notFound() {
    return new NotFoundException();
  }

  public static NotFoundException notFound(String msg) {
    return new NotFoundException(msg);
  }

  public static NotFoundException notFound(Throwable cause) {
    return new NotFoundException(cause);
  }

  public static NotFoundException notFound(String msg, Throwable cause) {
    return new NotFoundException(msg, cause);
  }

}
