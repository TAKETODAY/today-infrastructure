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

package cn.taketoday.aop;

import cn.taketoday.context.ContextException;

/**
 * Exception that gets thrown when an AOP invocation failed
 * because of misconfiguration or unexpected runtime issues.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 18:52
 * @since 3.0
 */
public class AopInvocationException extends ContextException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructor for AopInvocationException.
   *
   * @param msg
   *         the detail message
   */
  public AopInvocationException(String msg) {
    super(msg);
  }

  /**
   * Constructor for AopInvocationException.
   *
   * @param msg
   *         the detail message
   * @param cause
   *         the root cause
   */
  public AopInvocationException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
