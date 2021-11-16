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

package cn.taketoday.beans;

import java.io.Serial;

import cn.taketoday.beans.factory.BeansException;

/**
 * @author TODAY 2021/9/8 22:06
 * @since 4.0
 */
public class ArgumentResolvingFailedException extends BeansException {
  @Serial
  private static final long serialVersionUID = 1L;

  public ArgumentResolvingFailedException() { }

  /**
   * Construct a {@code NestedRuntimeException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public ArgumentResolvingFailedException(String msg) {
    super(msg);
  }

  /**
   * Construct a {@code NestedRuntimeException} with the specified nested exception.
   *
   * @param cause the nested exception
   */
  public ArgumentResolvingFailedException(Throwable cause) {
    super(cause);
  }

  /**
   * Construct a {@code NestedRuntimeException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public ArgumentResolvingFailedException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
