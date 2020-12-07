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

import cn.taketoday.web.Constant;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.http.HttpStatus;

/**
 * @author TODAY <br>
 * 2019-07-20 15:51
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends WebRuntimeException {
  private static final long serialVersionUID = 1L;

  public UnauthorizedException() {
    super(Constant.UNAUTHORIZED);
  }

  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnauthorizedException(Throwable cause) {
    super(cause);
  }

  protected UnauthorizedException(String message, Throwable cause, //
                                  boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
