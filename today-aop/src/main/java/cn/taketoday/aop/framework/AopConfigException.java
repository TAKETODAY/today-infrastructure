/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.aop.framework;

import cn.taketoday.core.NestedRuntimeException;

/**
 * Exception that gets thrown on illegal AOP configuration arguments.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 19:04
 * @since 4.0
 */
public class AopConfigException extends NestedRuntimeException {

  /**
   * Constructor for AopConfigException.
   *
   * @param msg the detail message
   */
  public AopConfigException(String msg) {
    super(msg);
  }

  /**
   * Constructor for AopConfigException.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public AopConfigException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
