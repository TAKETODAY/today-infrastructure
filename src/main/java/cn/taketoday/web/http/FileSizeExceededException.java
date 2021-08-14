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

import cn.taketoday.core.utils.DataSize;
import cn.taketoday.web.WebNestedRuntimeException;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * @author TODAY <br>
 * 2018-07-10 21:42:16
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileSizeExceededException extends WebNestedRuntimeException {

  private static final long serialVersionUID = 1L;

  /** The actual size of the request. */
  private DataSize actual;
  /** The maximum permitted size of the request. */
  private final DataSize permitted;

  public FileSizeExceededException(DataSize permitted, Throwable cause) {
    super("The upload file exceeds its maximum permitted size: [" + permitted + "]", cause);
    this.permitted = permitted;
  }

  public DataSize getActual() {
    return actual;
  }

  public DataSize getPermitted() {
    return permitted;
  }

  public FileSizeExceededException setActual(DataSize actual) {
    this.actual = actual;
    return this;
  }

}
