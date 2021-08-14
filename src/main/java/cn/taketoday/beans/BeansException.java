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

package cn.taketoday.beans;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Abstract superclass for all exceptions thrown in the beans package
 * and subpackages.
 *
 * <p>Note that this is a runtime (unchecked) exception. Beans exceptions
 * are usually fatal; there is no reason for them to be checked.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/2 11:36
 * @since 3.0
 */
public abstract class BeansException extends NestedRuntimeException {
  private static final long serialVersionUID = 1L;

  public BeansException() { }

  /**
   * Construct a {@code BeansException} with the specified root cause.
   *
   * @param cause
   *         the nested exception
   */
  public BeansException(Throwable cause) {
    super(cause);
  }

  /**
   * Create a new BeansException with the specified message.
   *
   * @param msg
   *         the detail message
   */
  public BeansException(String msg) {
    super(msg);
  }

  /**
   * Create a new BeansException with the specified message
   * and root cause.
   *
   * @param msg
   *         the detail message
   * @param cause
   *         the root cause
   */
  public BeansException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
