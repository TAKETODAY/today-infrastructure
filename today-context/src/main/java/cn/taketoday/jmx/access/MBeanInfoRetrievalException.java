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

package cn.taketoday.jmx.access;

import cn.taketoday.jmx.JmxException;

/**
 * Thrown if an exception is encountered when trying to retrieve
 * MBean metadata.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see MBeanClientInterceptor
 * @see MBeanProxyFactoryBean
 * @since 4.0
 */
@SuppressWarnings("serial")
public class MBeanInfoRetrievalException extends JmxException {

  /**
   * Create a new {@code MBeanInfoRetrievalException} with the
   * specified error message.
   *
   * @param msg the detail message
   */
  public MBeanInfoRetrievalException(String msg) {
    super(msg);
  }

  /**
   * Create a new {@code MBeanInfoRetrievalException} with the
   * specified error message and root cause.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public MBeanInfoRetrievalException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
