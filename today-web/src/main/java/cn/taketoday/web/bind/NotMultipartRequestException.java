/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.bind;

import java.io.Serial;

/**
 * Not A Multipart Request Exception
 *
 * @author TODAY 2021/3/21 21:08
 * @since 3.0
 */
public class NotMultipartRequestException extends MultipartException {

  @Serial
  private static final long serialVersionUID = 1L;

  public NotMultipartRequestException(String message) {
    super(message);
  }

  public NotMultipartRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
